# گزارش فاز ۲ پروژهٔ اصول طراحی کامپایلر

## مقدمه

در فاز دوم پروژهٔ کامپایلر، هدف اصلی پیاده‌سازی یک **تحلیلگر نحوی (Parser)** برای یک زبان برنامه‌نویسی فرضی است. این تحلیلگر باید قابلیت شناسایی و پردازش ساختارهای زیر را داشته باشد:

- تعریف متغیرها با انواع دادهٔ مختلف: `int`, `float`, `double`, `char`, `bool`, `string`
- سازوکارهای شرطی: `if/else` (و حالت‌های تو‌در‌تو و زنجیره‌ای `else if`)
- حلقه‌ها: `for` و `while`
- تعریف و **فراخوانی** توابع
- تعریف کلاس‌ها
- انجام عملیات‌های محاسباتی و منطقی و چاپ نتیجهٔ **عبارات محاسباتی به‌صورت آنی**
- چاپ اطلاعات ساختارها هنگام تشخیص (مطابق نیازمندی مستند پروژه)

همچنین بخش امتیازی شامل چک‌های معنایی زیر است:

- جلوگیری از **تعریف تکراری** (کلاس/تابع/متغیر) در یک اسکوپ
- **تطبیق امضای فراخوانی تابع** (تعداد و نوع آرگومان‌ها)
- مدیریت اسکوپ‌ها و تزریق پارامترهای تابع
- **ساخت درخت AST** همزمان با پارس

در این گزارش، معماری، قواعد گرامر، اکشن‌های CUP، طراحی AST و مؤلفه‌های معنایی به تفصیل آمده است.

---

## مراحل پیاده‌سازی (نمای کلی)

1) **طراحی گرامر** و افزودن قواعد انواع داده، توابع، کلاس‌ها، حلقه‌ها و عبارات با رعایت تقدم/انجمن عملگرها و رفع ابهام‌ها (مثل `dangling else`).  
2) **به‌روزرسانی اسکنر (Lexer)** برای تشخیص توکن‌های جدید (انواع داده، لیترال‌ها، عملگرهای چندکاراکتری، کلمات‌کلیدی کنترل جریان و کلاس).  
3) **تعریف اکشن‌های CUP** برای هر قاعده: چاپ خروجی‌های خواسته‌شده، ساخت گره‌های AST و انجام چک‌های معنایی.  
4) **مدیریت اسکوپ (Scope)** با جدول نماد پشته‌ای و ورود/خروج اسکوپ در `{ ... }` و تزریق پارامترهای تابع.  
5) **ساخت و به‌روزرسانی AST**: ایجاد گره‌های لازم برای اعلان‌ها، عبارات، دستورات، توابع، کلاس‌ها.  
6) **تست و رفع خطا** با ورودی‌های پوششی (covering) برای همهٔ ویژگی‌ها.  
7) **آماده‌سازی تحویل** (این مستند، کدها، نمونه‌خروجی‌ها).

---

## گرامر (فایل `parser.txt`) – بخش‌های کلیدی و اکشن‌ها

### اعلان متغیرها (نمونه برای `int`؛ سایر انواع مشابه)

```text
INT_KW ID:id ASSIGN expr:e SEMI {:
    String name=(String)id;
    boolean ok = Sem.declareVar(name, Sem.Type.INT);
    if (!ok) System.out.println("ERROR duplicate var: " + name);
    double v = e.val.doubleValue();
    System.out.println("VAR int " + name + " = " +
        ((Math.rint(v)==v)? Long.toString((long)Math.rint(v)): Double.toString(v)));
    RESULT = new AST.VarDecl("int", name, e.aexpr);
:}
| INT_KW ID:id SEMI {:
    String name=(String)id;
    if (!Sem.declareVar(name, Sem.Type.INT))
        System.out.println("ERROR duplicate var: " + name);
    else System.out.println("VAR int " + name);
    RESULT = new AST.VarDecl("int", name, null);
:}
```

> برای `float/double/bool/char/string` الگوی مشابه با تبدیل/چاپ مناسب مقدار (مثل چاپ `true/false` برای `bool` و نقل‌قول مناسب برای `char`/`string`) پیاده‌سازی شده است.

---

### ساختارهای شرطی و حلقه‌ها

```text
stmt ::=
    IF LPAR expr:e RPAR stmt:t 
        {: System.out.println("STRUCT IF"); RESULT = new AST.If(e.aexpr, t, null); :}
  | IF LPAR expr:e RPAR stmt_nse:t ELSE stmt:el 
        {: System.out.println("STRUCT IF/ELSE"); RESULT = new AST.If(e.aexpr, t, el); :}
  | WHILE LPAR expr:e RPAR stmt_nse:s 
        {: System.out.println("STRUCT WHILE"); RESULT = new AST.While(e.aexpr, s); :}
  | FOR LPAR for_init_opt:i SEMI for_cond_opt:c SEMI for_post_opt:p RPAR stmt_nse:s 
        {: System.out.println("STRUCT FOR");
           RESULT = new AST.For(i, (c==null? null: c.aexpr), p, s);
        :}
;
```

> رفع ابهام `dangling else` با تفکیک `stmt` و `stmt_nse` انجام شده است.

---

### تعریف تابع و سرخط (header)

```text
func_header ::=
    INT_KW ID:name LPAR RPAR {:
        String nm=(String)name; 
        boolean ok = Sem.ST.declare(nm, Sem.Kind.FUNC);
        if (!ok) System.out.println("ERROR duplicate function: " + nm);
        Sem.recordFunction(nm, Sem.Type.INT, java.util.Collections.<Sem.Type>emptyList());
        System.out.println("FUNC int " + nm + "()");
        RESULT = new FuncHead("int", Sem.Type.INT, nm, java.util.Collections.<AST.Param>emptyList());
    :}
  | INT_KW ID:name LPAR param_list:ps RPAR {:
        String nm=(String)name;
        boolean ok = Sem.ST.declare(nm, Sem.Kind.FUNC);
        if (!ok) System.out.println("ERROR duplicate function: " + nm);
        Sem.recordFunction(nm, Sem.Type.INT, ps.types);
        System.out.println("FUNC int " + nm + "(" + ps.text + ")");
        RESULT = new FuncHead("int", Sem.Type.INT, nm, ((Sem.ParamSig)ps).params);
    :}
  | /* نسخه‌های هم‌ارز برای سایر انواع بازگشت (float/double/char/bool/string) */
;
```

ترکیب سرخط تابع و بدنه:

```text
stmt_nse ::=
    func_header:h block:b {:
        RESULT = new AST.FuncDecl(h.retName, h.name, h.params, (AST.Block)b);
    :}
  | /* سایر تولیدها */
;
```

---

### پارامترها و لیست پارامترها

```text
param ::= INT_KW ID:n {:
            String nn=(String)n;
            Sem.addPendingParam(nn, Sem.Type.INT);
            java.util.ArrayList<Sem.Type> ts = new java.util.ArrayList<>(); ts.add(Sem.Type.INT);
            java.util.ArrayList<AST.Param> ps = new java.util.ArrayList<>(); ps.add(new AST.Param("int", nn));
            RESULT = new Sem.ParamSig("int " + nn, 1, ts, ps);
         :}
       | BOOL_KW ID:n {:
            String nn=(String)n;
            Sem.addPendingParam(nn, Sem.Type.BOOL);
            java.util.ArrayList<Sem.Type> ts = new java.util.ArrayList<>(); ts.add(Sem.Type.BOOL);
            java.util.ArrayList<AST.Param> ps = new java.util.ArrayList<>(); ps.add(new AST.Param("bool", nn));
            RESULT = new Sem.ParamSig("bool " + nn, 1, ts, ps);
         :}
       | /* سایر انواع */
;

param_list ::= param:p                      {: RESULT = p; :}
             | param_list:pl COMMA param:p  {:
                   java.util.ArrayList<Sem.Type> ts = new java.util.ArrayList<>(pl.types);
                   ts.addAll(p.types);
                   java.util.ArrayList<AST.Param> ps = new java.util.ArrayList<>(((Sem.ParamSig)pl).params);
                   ps.addAll(((Sem.ParamSig)p).params);
                   RESULT = new Sem.ParamSig(pl.text + ", " + p.text, pl.count + p.count, ts, ps);
               :}
;
```

---

### بلاک و مدیریت اسکوپ/تزریق پارامترها

```text
block ::= LBRACE 
            {: Sem.pushScope(); Sem.injectParamsIntoCurrentScope(); :}
          stmt_list:sl 
          RBRACE 
            {: Sem.popScope(); RESULT = new AST.Block(sl); :}
;
```

---

### فراخوانی تابع و عامل‌ها (factor)

```text
factor ::= ID:n LPAR RPAR {:
              String nm=(String)n;
              System.out.println("CALL " + nm + "()");
              Sem.checkCallSignature(nm, new Sem.ArgSig(
                        "", 0,
                        java.util.Collections.<Sem.Type>emptyList(),
                        java.util.Collections.<Double>emptyList(),
                        java.util.Collections.<AST.Expr>emptyList()));
              RESULT = new Sem.Eval(0.0, Sem.Type.INT, null, new AST.Call(nm, java.util.Collections.<AST.Expr>emptyList()));
           :}
         | ID:n LPAR arg_list_info:als RPAR {:
              String nm=(String)n;
              System.out.println("CALL " + nm + "(" + als.text + ")");
              Sem.checkCallSignature(nm, als);
              RESULT = new Sem.Eval(0.0, Sem.Type.INT, null, new AST.Call(nm, als.aexprs));
           :}
         | ID:n {:
              String nm=(String)n;
              Sem.Type t = Sem.VT.lookup(nm);
              RESULT = new Sem.Eval(0.0, (t==Sem.Type.UNKNOWN? Sem.Type.INT: t), null, new AST.Id(nm));
           :}
         | /* سایر حالات: لیترال‌ها، پرانتز، ... */
;
```

---

### عبارات، تقدم/انجمن و انتساب

**نکتهٔ کلیدی:** انتساب به‌عنوان یک **عبارت** مدل می‌شود (نه دستور مستقل) و سپس با `expr;` به شکل `Stmt` تبدیل شده و **در صورت نبودن انتساب**، مقدار `EXPR` چاپ می‌شود.

```text
assignment ::= ID:n ASSIGN assignment:r {:
                   String nm=(String)n;
                   RESULT = new Sem.Eval(r.val, r.type, null, new AST.AssignExpr(nm, r.aexpr));
               :}
             | or_expr:x               {: RESULT = x; :}
;

expr ::= assignment:x  {: RESULT = x; :} ;
```

سطوح تقدم نمونه (جمع/ضرب/یکانی):

```text
add_expr ::= add_expr:a PLUS  mul_expr:b {:
                 double v = a.val.doubleValue() + b.val.doubleValue();
                 Sem.Type t = Sem.promote(a.type, b.type);
                 RESULT = new Sem.Eval(v, t, null, new AST.Binary("+", a.aexpr, b.aexpr));
             :}
           | add_expr:a MINUS mul_expr:b {:
                 double v = a.val.doubleValue() - b.val.doubleValue();
                 Sem.Type t = Sem.promote(a.type, b.type);
                 RESULT = new Sem.Eval(v, t, null, new AST.Binary("-", a.aexpr, b.aexpr));
             :}
           | mul_expr:x {:
                 RESULT = x;
             :}
;

mul_expr ::= mul_expr:a STAR unary:b   {:
                 double v = a.val.doubleValue() * b.val.doubleValue();
                 Sem.Type t = Sem.promote(a.type, b.type);
                 RESULT = new Sem.Eval(v, t, null, new AST.Binary("*", a.aexpr, b.aexpr));
             :}
           | mul_expr:a SLASH unary:b  {:
                 double v = a.val.doubleValue() / b.val.doubleValue();
                 RESULT = new Sem.Eval(v, Sem.promoteDiv(a.type, b.type), null, new AST.Binary("/", a.aexpr, b.aexpr));
             :}
           | mul_expr:a PERCENT unary:b {:
                 double v = a.val.doubleValue() % b.val.doubleValue();
                 Sem.Type t = Sem.promote(a.type, b.type);
                 RESULT = new Sem.Eval(v, t, null, new AST.Binary("%", a.aexpr, b.aexpr));
             :}
           | unary:x {:
                 RESULT = x;
             :}
;

unary ::= MINUS unary:x {:
                 RESULT = new Sem.Eval(-x.val.doubleValue(), x.type, null, new AST.Unary("-", x.aexpr));
            :}
        | NOT unary:x   {:
                 RESULT = new Sem.Eval((x.val.doubleValue()==0.0)? 1.0: 0.0, Sem.Type.BOOL, null, new AST.Unary("!", x.aexpr));
            :}
        | factor:x      {: RESULT = x; :}
;
```

چاپ نتیجهٔ عبارت‌های بدون انتساب:

```text
stmt_nse ::= expr:e SEMI {:
                 if (!(e.aexpr instanceof AST.AssignExpr)) {
                     double v = e.val.doubleValue();
                     System.out.println("EXPR " + ((Math.rint(v)==v)?
                        Long.toString((long)Math.rint(v)): Double.toString(v)));
                 }
                 RESULT = new AST.ExprStmt(e.aexpr);
             :}
         | /* سایر تولیدها */
;
```

---

## اسکنر (فایل `scanner.txt`) – نکات مهم

- کلمات‌کلیدی انواع: `int`, `float`, `double`, `char`, `bool`, `string`  
- کلمات‌کلیدی ساختاری: `if`, `else`, `for`, `while`, `return`, `class`  
- لیترال‌ها:  
  - عددی: ده‌دهی، هگز (`0x...`)، دودویی (`0b...`)، اکتال (`0...`)، اعشاری/علمی  
  - `STRING_LIT` با **unescape** (مثل `\n`, `\t`, `\"`، `\\`)  
  - `CHAR_LIT` با تبدیل به `Character` (یا `'\0'` اگر تهی)
- عملگرها: ترتیب توکن‌های چندکاراکتری (`==`, `!=`, `<=`, `>=`, `&&`, `||`, `<<`, `>>`, `+=`, …) **قبل** از تک‌کاراکتری‌ها برای جلوگیری از تطابق نادرست.

نمونهٔ الگوی رشته و کاراکتر:

```text
{STRING} {
     String raw = yytext();
     String inner = raw.substring(1, raw.length()-1);
     return new Symbol(sym.STRING_LIT, unescape(inner));
}

{CHAR} {
     String raw = yytext();
     String inner = raw.substring(1, raw.length()-1);
     String un = unescape(inner);
     Character ch = un.isEmpty() ? '\0' : un.charAt(0);
     return new Symbol(sym.CHAR_LIT, ch);
}
```

---

## ساختار AST (فایل `AST.java`) – گره‌های کلیدی

- **Program**: ریشهٔ درخت، لیست دستورهای سطح بالا  
- **Block**: بدنهٔ `{ ... }` با لیست `Stmt`  
- **VarDecl**(type, name, init?)  
- **FuncDecl**(retType, name, params[], body: Block) + **Param**(type, name)  
- **ClassDecl**(name, body: Block)  
- **ExprStmt**(expr)  
- **AssignExpr**(name, expr) *(انتساب به‌صورت Expr)*  
- **ReturnStmt**(expr?)  
- **If**(cond, thenS, elseS?) / **While**(cond, body) / **For**(init?, cond?, post?, body)  
- **Expr**‌ها: `IntLit`, `FloatLit`, `BoolLit`, `CharLit`, `StringLit`, `Id`, `Call`, `Binary(op,left,right)`, `Unary(op,expr)`

---

## مؤلفهٔ معنایی (فایل `Sem.java`) – جدول نماد/انواع/چک‌ها

### انواع و چاپ امن

- `enum Type { INT, FLOAT, DOUBLE, CHAR, BOOL, STRING, UNKNOWN }`  
- `typeName(Type t)` → نام رشته‌ای نوع  
- `quote(String s)` و `quoteChar(char c)` → چاپ امن رشته/کاراکتر با escape

### ترفیع نوع

- `promote(a,b)` → سلسله‌مراتب عددی: `double > float > int` (و نگاشت `char/bool` به سطح عددی)  
- `promoteDiv(a,b)` → حداقل `float` برای تقسیم عددی

### جدول نماد و اسکوپ

- `SymbolTable ST` (نام → Kind: VAR/FUNC/CLASS)، پشته‌ای با `push()/pop()/declare()`  
- `VarTypes VT` (نام → Type)، پشته‌ای با `lookup()`  
- `declareVar(name,type)` → درج همزمان در ST و VT  
- `pushScope()/popScope()` در آغاز/پایان بلاک

### امضای تابع و چک فراخوانی

- دیکشنری‌ها: `funcSig: name → List<Type>`, `funcRet: name → Type`  
- `recordFunction(name, retType, paramTypes)` برای ثبت امضا  
- `getFunctionReturn(name)`  
- `checkCallSignature(name, ArgSig args)`:

```java
public static void checkCallSignature(String name, ArgSig args) {
    List<Type> expected = funcSig.get(name);
    if (expected == null) {
        System.out.println("ERROR unknown function: " + name);
        return;
    }
    if (expected.size() != args.count) {
        System.out.println("ERROR bad args: " + name + " expected " + 
                            expected.size() + " got " + args.count);
        return;
    }
    for (int i = 0; i < expected.size(); i++) {
        Type e = expected.get(i);
        Type a = args.types.get(i);
        if (e == Type.BOOL && (a == Type.INT || a == Type.FLOAT || 
                               a == Type.DOUBLE || a == Type.CHAR)) {
            Double v = args.vals.get(i);
            if (v == null || !(v == 0.0 || v == 1.0)) {
                System.out.println("ERROR type mismatch: " + name + " param " + (i+1) +
                                   " expected " + typeName(e) + " got " + typeName(a));
            }
        } else if (e != a) {
            System.out.println("ERROR type mismatch: " + name + " param " + (i+1) +
                               " expected " + typeName(e) + " got " + typeName(a));
        }
    }
}
```

### ساختارهای واسط برای حمل اطلاعات

- `Sem.Eval(val:Double, type:Type, sval:String?, aexpr:AST.Expr)`  
- `Sem.ParamSig(text,count,types[],params[])`  
- `Sem.ArgSig(text,count,types[],vals[],aexprs[])`

### مکانیزم پارامترهای معلق (PendingParam)

- `addPendingParam(name,type)` در سرخط تابع  
- `injectParamsIntoCurrentScope()` در ابتدای بلاک تابع → تعریف رسمی پارامترها در اسکوپ جدید و **پاکسازی** لیست

---

## جلوگیری از تعارض‌های گرامری

- رفع `dangling else` با `stmt`/`stmt_nse`
- مدل‌سازی **انتساب به‌عنوان Expr** و چاپ `EXPR` فقط برای **عبارات غیرانتساب**
- تفکیک سطوح تقدم (or → and → rel → add → mul → unary → factor)
- ترتیب‌دهی توکن‌های چندکاراکتری در lexer برای جلوگیری از تطابق ناقص

---

## نمونهٔ ورودی و خروجی

### ورودی نمونه

```c
int x = 5;
float y;
double d = 3.14;
bool b = false;
char ch = 'A';
string s = "Hello";
x = 2 + 3 * 4;
if (x) {
    y = 1.5;
} else {
    y = 2.5;
}
while (x) x = x - 1;
for (int i = 0; i < 3; i = i + 1) {
    x = x + i;
}
int f(int a, bool c) {
    if (c)
        return a;
    else
        return 0;
}
int z = f(x, true);
class C {
    int m() {
        return 0;
    }
}
```

### خروجی‌های چاپ‌شده حین پارس (نمونه)

```text
VAR int x = 5
VAR float y
VAR double d = 3.14
VAR bool b = false
VAR char ch = 'A'
VAR string s = "Hello"
STRUCT IF/ELSE
STRUCT WHILE
STRUCT FOR
FUNC int f(int a, bool c)
STRUCT IF/ELSE
CALL f(x, true)
VAR int z = 0
FUNC int m()
CLASS C
```

### ساختار AST (طرح خروجی چاپ درخت)

```text
Program
  VarDecl int x
    init:
      Int 5
  VarDecl float y
  VarDecl double d
    init:
      Float 3.14
  VarDecl bool b
    init:
      Bool false
  VarDecl char ch
    init:
      Char 'A'
  VarDecl string s
    init:
      String "Hello"
  ExprStmt
    AssignExpr x
      Binary +
        Int 2
        Binary *
          Int 3
          Int 4
  If
    Id x
    Block
      ExprStmt
        AssignExpr y
          Float 1.5
    Block
      ExprStmt
        AssignExpr y
          Float 2.5
  While
    Id x
    ExprStmt
      AssignExpr x
        Binary -
          Id x
          Int 1
  For
    VarDecl int i
      init:
        Int 0
    Binary <
      Id i
      Int 3
    ExprStmt
      AssignExpr i
        Binary +
          Id i
          Int 1
    Block
      ExprStmt
        AssignExpr x
          Binary +
            Id x
            Id i
  Func int f
    Param int a
    Param bool c
    Block
      If
        Id c
        Return
          Id a
        Return
          Int 0
  VarDecl int z
    init:
      Call f
        Id x
        Bool true
  Func int m
    Block
      Return
        Int 0
  Class C
    Block
      (Func m)
```

---

## نحوهٔ Build/Run

1) تولید `Lexer.java` از `scanner.txt` با **JFlex**  
2) تولید `parser.java` و `sym.java` از `parser.txt` با **JavaCUP**  
3) کامپایل همهٔ فایل‌های جاوا (از جمله `AST.java`, `Sem.java`, `ASTPrinter.java`, خروجی‌های JFlex/CUP)  
4) اجرا: ساخت نمونهٔ `Lexer` و `Parser` و فراخوانی `parse()`  
5) چاپ خروجی‌های میانی (console) و چاپ درخت AST با `ASTPrinter`

> **نسخه‌ها:** JDK 17، JFlex 1.8، CUP 0.11a

---

## پوشش نیازمندی‌ها و بخش امتیازی

- ✅ تعریف متغیر برای همهٔ انواع خواسته‌شده  
- ✅ `if/else`, تو در تو، چاپ نوع ساختار  
- ✅ `while` و `for` + چاپ ساختار  
- ✅ تعریف/فراخوانی توابع + چاپ سرخط و چاپ `CALL ...`  
- ✅ تعریف کلاس + چاپ `CLASS <Name>`  
- ✅ محاسبه و چاپ نتیجهٔ عبارات مستقل (`EXPR <value>`)  
- ✅ جلوگیری از تعریف تکراری در یک اسکوپ (متغیر/تابع/کلاس)  
- ✅ چک امضای تابع (تعداد/نوع‌ها + حالت ویژهٔ `bool`)  
- ✅ ساخت کامل **AST** همزمان با پارس  
- ⭕️ وابستگی حلقوی بین کلاس‌ها: خارج از محدودهٔ این فاز (نیاز به مدل داده‌ای/روابط پیشرفته‌تر)

---

## تصمیمات طراحی مهم

- **بولین/کاراکتر** به صورت عددی داخلی پردازش می‌شوند (برای یکپارچگی محاسبات) و فقط در چاپ به صورت انسانی نمایش می‌یابند.  
- برای شناسهٔ ناشناخته، نوع موقت `INT` در نظر گرفته می‌شود تا پارس ادامه یابد (گزارش این خطا به فازهای بعد قابل موکول‌سازی است).  
- **چاپ‌نکردن مقدار برای انتساب‌ها** (صرفاً `ExprStmt` ساخته می‌شود) مطابق نیاز پروژه.  
- سطوح تقدم به‌صورت ساختاری مدل شدند؛ نیازی به `%left/%right` نبود.  
- ساده‌سازی `for_init_opt` برای پرهیز از کانفلیکت‌ها (تعریف نوع در init؛ انتساب‌ها را به `ExprStmt` بیرون یا `post` منتقل کنید).  
- توکن‌های `++/--/+=` در lexer وجود دارند ولی در این فاز در گرامر استفاده نشده‌اند (قابل توسعهٔ آتی).

---

## جمع‌بندی

پیاده‌سازی فاز ۲ شامل گسترش گرامر، به‌روزرسانی اسکنر، **چاپ خروجی‌های لحظه‌ای ساختارها**، **ساخت AST** و **چک‌های معنایی کلیدی** است. با مجموعهٔ تست‌های پوششی، پارسر بدون تعارض ساخته شد و مطابق نیازمندی‌ها عمل می‌کند. این زیرساخت آمادهٔ توسعهٔ فازهای بعدی (Type Checking کامل، تولید کد میانی/نهایی و بهینه‌سازی‌ها) است.

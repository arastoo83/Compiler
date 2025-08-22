<div dir="rtl">

# پروژهٔ کامپایلر دانشگاه (JFlex + CUP)

این مخزن شامل پیاده‌سازی فازهای کامپایلر (لغوی، نحوی و لایهٔ سِمانتیک سبک) با **JFlex** و **Java CUP** است.  
محیط هدف: ویندوز (سازگار با لینوکس/مک).

## ساختار پوشه‌ها
```
.
├─ src/        # کدهای جاوا: Main، AST، ASTPrinter، Sem
├─ grammar/    # گرامرها: Lexer.jflex و parser.cup
├─ docs/       # PDFهای شرح پروژه و اسلایدهای سمانتیک
└─ samples/    # نمونه‌های ورودی برای تست (اختیاری)
```

## امکانات و وضعیت فعلی (فاز ۲)
- **Lexer/Parser** با **JFlex** و **Java CUP** پیاده‌سازی شده‌اند.
- **چاپ استاندارد خروجی‌ها** (طبق قالب توافق‌شده):
  - `VAR ...`
  - `EXPR ...`
  - `STRUCT IF/ELSE/WHILE/FOR`
  - `FUNC ...`
  - `CLASS ...`
  - `CALL ...`
- **لایهٔ سِمانتیک سبک**:
  - جدول نمادها (VAR/FUNC/CLASS) با اسکوپ.
  - ثبت امضای تایپ‌دار توابع و **چک تعداد/نوع آرگومان‌ها**.
  - پذیرش `true/false` و همچنین **۰/۱ (و 0.0/1.0)** برای پارامترهای بولی.
  - جلوگیری از نام‌های تکراری (var/func/class/param).
  - پیام‌های خطا: `duplicate`، `unknown function`، `type-mismatch`، `bad-args`.
- **AST کامل** (Program/Block/VarDecl/Assign/ExprStmt/Return/If/While/For/FuncDecl/ClassDecl و Exprها) و چاپ با `ASTPrinter`.
- **رشته‌ها** در اسکنر **unescape** می‌شوند و چاپ امن با `Sem.quote` انجام می‌شود.

> **یادداشت:** پشتیبانی `char` و `double` در برخی مسیرها در حال تکمیل است (بخش «نقشهٔ راه» را ببینید). همچنین چاپ مقدار اولیهٔ `bool` در اعلان متغیرها در نسخهٔ بعدی به فرم `true/false` اصلاح می‌شود.

## پیش‌نیازها
- Java JDK 8 یا بالاتر  
- JFlex 1.8.x  
- Java CUP 11b (یا سازگار) + CUP Runtime (`java-cup-11b-runtime.jar`)

## ساخت (Build)
> برای جلوگیری از به‌هم‌ریختگی دستورات، **بلاک‌های کد زیر چپ‌به‌راست** نمایش داده می‌شوند، اما کل سند راست‌به‌چپ است.

<div dir="ltr">

### 1) تولید Lexer با JFlex
```bash
jflex grammar/Lexer.jflex
# خروجی معمولاً: Lexer.java (در دایرکتوری جاری)
```

### 2) تولید Parser با CUP
# روش با JAR:
```bash
java -jar java-cup-11b.jar -parser parser -symbols sym grammar/parser.cup
# خروجی: parser.java و sym.java
```
# یا اگر JAR در classpath است:
```bash
java -cp java-cup-11b.jar java_cup.Main -parser parser -symbols sym grammar/parser.cup
```

### 3) کامپایل جاوا (به‌همراه CUP Runtime)
# ویندوز (PowerShell/CMD):
```bat
javac -cp .;java-cup-11b-runtime.jar src\*.java *.java
```
# لینوکس/مک:
```bash
javac -cp .:java-cup-11b-runtime.jar src/*.java *.java
```

### 4) اجرا
# ویندوز:
```bat
java -cp .;java-cup-11b-runtime.jar Main samples\input.txt
```
# لینوکس/مک:
```bash
java -cp .:java-cup-11b-runtime.jar Main samples/input.txt
```

</div>

## نمونهٔ ورودی (samples)
می‌توانید فایل‌های تست خود را در `samples/` قرار دهید. مثال ساده:
```
FUNC int demo(int n)
IF (n) { 1; } ELSE { 2; }
WHILE (n) { n = n - 1; 3; }
FOR (int i = 0; i < 2; i = i + 1) { 4; }
CALL demo(5);
0;
```
خروجی استاندارد (تقریبی) انتظار می‌رود شامل خطوطی از جنس `EXPR ...`، `STRUCT ...`، `FUNC ...`، `CALL ...` و در نهایت چاپ AST باشد.

## نکات توسعه (برای هم‌تیمی‌ها)
- Start symbol و **برچسب‌گذاری production**‌ها در CUP رعایت شده تا از ارورهای `$1/$2` جلوگیری شود.
- الگوی رفع **dangling-else** با جداسازی `stmt` و `stmt_nse` استفاده شده است.
- هنگام ورود/خروج بلاک، اسکوپ‌ها در `Sem` (push/pop) مدیریت می‌شود.
- پارامترهای تابع ابتدا به‌صورت pending ثبت و در ابتدای بلاک تابع تزریق می‌شوند.
- `Main` فقط **یک‌بار** `parse()` را اجرا می‌کند و AST را از `parse().value` می‌گیرد.

## نقشهٔ راه (Roadmap)
- تکمیل سرتاسری `char` و `double` (lexer/parser/semantics/چاپ).
- چاپ مقدار اولیهٔ `bool` در اعلان متغیرها به‌شکل `true/false`.
- تعیین نوع بازگشتی تابع برای همهٔ انواع (نه فقط `int`).

## لایسنس
MIT (می‌توانید فایل `LICENSE` را نگه دارید/تغییر دهید).

---

> هرگونه باگ/پیشنهاد را در Issues ثبت کنید. برای Pull Request، لطفاً یک یا چند تست در `samples/` اضافه کنید.

</div>

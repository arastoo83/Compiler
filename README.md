# University Compiler Project (JFlex + CUP)

این مخزن شامل پیاده‌سازی فازهای کامپایلر (لغوی، نحوی و لایهٔ سمانتیک سبک) با **JFlex** و **Java CUP** است.  
پلتفرم: ویندوز (اما روی لینوکس/مک نیز قابل اجراست).

## پوشه‌ها
```
.
├─ src/        # کدهای جاوا: Main، AST، ASTPrinter، Sem
├─ grammar/    # گرامرها: Lexer.jflex و parser.cup
├─ docs/       # PDFهای شرح پروژه و اسلایدهای سمانتیک
└─ samples/    # (اختیاری) نمونه کد ورودی برای تست
```

## امکانات
- 
Lexer و Parser با **JFlex** و **Java CUP** مطابق سرفصل پروژه- گزارش‌های استاندارد: `VAR ...`, `EXPR ...`, `STRUCT IF/ELSE/WHILE/FOR`, `FUNC ...`, `CLASS ...`, `CALL ...`- خطاهای معنایی: `duplicate`, `unknown function`, `type-mismatch`, `bad-args`- ساخت کامل **AST** و چاپ آن با `ASTPrinter`- انواع داده: `int`, `float`, `double`, `char`, `bool`, `string` + پذیرش `true/false` و ۰/۱ برای bool- ساختارها: if/else/while/for، توابع و کلاس‌ها، فراخوانی تابع با چک امضا

## پیش‌نیازها
- Java 8+ (JDK)
- JFlex 1.8.x
- Java CUP 11b (یا نسخهٔ سازگار) + Runtime آن

## ساخت (Build) – نمونهٔ مینیمال
> مسیر jar ها را با توجه به سیستم خودتان تنظیم کنید.

### 1) تولید Lexer با JFlex
```bash
jflex grammar/Lexer.jflex
# خروجی معمولاً: Lexer.java
```

### 2) تولید Parser با CUP
```bash
java java_cup.MainDrawTree parser.cup grammar/parser.cup
# خروجی: parser.java و sym.java
```

### 3) کامپایل جاوا
ویندوز (PowerShell / CMD):
```bat
javac src\*.java
```
لینوکس/مک:
```bash
javac -cp .:java-cup-11b-runtime.jar src/*.java *.java
```

### 4) اجرا
```bash
# ورودیِ فایل کد منبع را به برنامه بدهید
java Main samples/example_expr.txt
```

> نکته: خروجی‌های استاندارد پروژه باید دقیقاً مطابق فرمت توافق‌شده چاپ شوند.

## راهنمای آغاز کار
1. این مخزن را کلون یا دانلود کنید.
2. JFlex و CUP را نصب/دانلود کنید.
3. مراحل «Build» بالا را اجرا کنید.
4. تست‌های خود را در `samples/` قرار دهید و اجرا کنید.

## وضعیت فایل‌ها
- فایل‌های منتقل‌شده از محیط شما به این ساختار:  
  - src/Main.java
  - src/AST.java
  - src/ASTPrinter.java
  - src/Sem.java

> اگر بعضی فایل‌ها را نمی‌بینید (مثل Main.java)، کافیست آن‌ها را در `src/` قرار دهید.

## لایسنس
MIT (اختیاری). در صورت تمایل فایل `LICENSE` اضافه کنید.

---

> آمادهٔ Push روی GitHub است. دستورالعمل‌ها در پایین آمده است.

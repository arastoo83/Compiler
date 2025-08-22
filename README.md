<div dir="rtl" lang="fa">

# پروژهٔ کامپایلر دانشگاه (JFlex + CUP)

این مخزن شامل پیاده‌سازی فازهای کامپایلر (لغوی، نحوی و لایهٔ سِمانتیک سبک) با <strong>JFlex</strong> و <strong>Java CUP</strong> است.  
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
<ul dir="rtl">
  <li><strong>Lexer/Parser</strong> با <strong>JFlex</strong> و <strong>Java CUP</strong> پیاده‌سازی شده‌اند.</li>
  <li><strong>چاپ استاندارد خروجی‌ها</strong> (طبق قالب توافق‌شده):</li>
</ul>
<ul dir="rtl">
  <li><code>VAR ...</code></li>
  <li><code>EXPR ...</code></li>
  <li><code>STRUCT IF/ELSE/WHILE/FOR</code></li>
  <li><code>FUNC ...</code></li>
  <li><code>CLASS ...</code></li>
  <li><code>CALL ...</code></li>
</ul>
<ul dir="rtl">
  <li><strong>لایهٔ سِمانتیک سبک</strong>:
    <ul dir="rtl">
      <li>جدول نمادها (VAR/FUNC/CLASS) با اسکوپ.</li>
      <li>ثبت امضای تایپ‌دار توابع و <strong>چک تعداد/نوع آرگومان‌ها</strong>.</li>
      <li>پذیرش <code>true/false</code> و همچنین <strong>۰/۱ (و 0.0/1.0)</strong> برای پارامترهای بولی.</li>
      <li>جلوگیری از نام‌های تکراری (var/func/class/param).</li>
      <li>پیام‌های خطا: <code>duplicate</code>، <code>unknown function</code>، <code>type-mismatch</code>، <code>bad-args</code>.</li>
    </ul>
  </li>
  <li><strong>AST کامل</strong> (Program/Block/VarDecl/Assign/ExprStmt/Return/If/While/For/FuncDecl/ClassDecl و Exprها) و چاپ با <code>ASTPrinter</code>.</li>
  <li><strong>رشته‌ها</strong> در اسکنر <em>unescape</em> می‌شوند و چاپ امن با <code>Sem.quote</code> انجام می‌شود.</li>
</ul>

<blockquote>
<p><strong>یادداشت:</strong> پشتیبانی <code>char</code> و <code>double</code> در برخی مسیرها در حال تکمیل است (بخش «نقشهٔ راه»). همچنین چاپ مقدار اولیهٔ <code>bool</code> در اعلان متغیرها در نسخهٔ بعدی به فرم <code>true/false</code> اصلاح می‌شود.</p>
</blockquote>

## پیش‌نیازها
<ul dir="rtl">
  <li>Java JDK 8 یا بالاتر</li>
  <li>JFlex 1.8.x</li>
  <li>Java CUP 11b (یا سازگار) + CUP Runtime (<code>java-cup-11b-runtime.jar</code>)</li>
</ul>

## ساخت (Build)

<div dir="ltr">

### 1) تولید Lexer با JFlex
```bash
jflex grammar/Lexer.jflex
# خروجی معمولاً: Lexer.java (در دایرکتوری جاری)
```

### 2) تولید Parser با CUP
```bash
java -jar java-cup-11b.jar -parser parser -symbols sym grammar/parser.cup
# خروجی: parser.java و sym.java
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
<p>می‌توانید فایل‌های تست خود را در <code>samples/</code> قرار دهید. مثال ساده:</p>

```
FUNC int demo(int n)
IF (n) { 1; } ELSE { 2; }
WHILE (n) { n = n - 1; 3; }
FOR (int i = 0; i < 2; i = i + 1) { 4; }
CALL demo(5);
0;
```

<p>خروجی استاندارد (تقریبی) انتظار می‌رود شامل خطوطی از جنس <code>EXPR ...</code>، <code>STRUCT ...</code>، <code>FUNC ...</code>، <code>CALL ...</code> و در نهایت چاپ AST باشد.</p>

## نکات توسعه (برای هم‌تیمی‌ها)
<ul dir="rtl">
  <li>Start symbol و <strong>برچسب‌گذاری production</strong>‌ها در CUP رعایت شده تا از ارورهای <code>$1/$2</code> جلوگیری شود.</li>
  <li>الگوی رفع <strong>dangling-else</strong> با جداسازی <code>stmt</code> و <code>stmt_nse</code> استفاده شده است.</li>
  <li>هنگام ورود/خروج بلاک، اسکوپ‌ها در <code>Sem</code> (push/pop) مدیریت می‌شود.</li>
  <li>پارامترهای تابع ابتدا به‌صورت pending ثبت و در ابتدای بلاک تابع تزریق می‌شوند.</li>
  <li><code>Main</code> فقط <strong>یک‌بار</strong> <code>parse()</code> را اجرا می‌کند و AST را از <code>parse().value</code> می‌گیرد.</li>
</ul>

## نقشهٔ راه (Roadmap)
<ul dir="rtl">
  <li>تکمیل سرتاسری <code>char</code> و <code>double</code> (lexer/parser/semantics/چاپ).</li>
  <li>چاپ مقدار اولیهٔ <code>bool</code> در اعلان متغیرها به‌شکل <code>true/false</code>.</li>
  <li>تعیین نوع بازگشتی تابع برای همهٔ انواع (نه فقط <code>int</code>).</li>
</ul>

## لایسنس
<ul dir="rtl">
  <li>MIT (می‌توانید فایل <code>LICENSE</code> را نگه دارید/تغییر دهید).</li>
</ul>

<hr/>

<p>هرگونه باگ/پیشنهاد را در Issues ثبت کنید. برای Pull Request، لطفاً یک یا چند تست در <code>samples/</code> اضافه کنید.</p>

</div>

import java_cup.runtime.*;

%%
%unicode
%cup
%class Lexer
%line
%column
%state COMMENT

%{
  private Symbol sym(int id) { return new Symbol(id, yyline+1, yycolumn+1); }
  private Symbol sym(int id, Object v) { return new Symbol(id, yyline+1, yycolumn+1, v); }

  private static String unescape(String s){
    StringBuilder sb=new StringBuilder();
    for(int i=0;i<s.length();i++){
      char c=s.charAt(i);
      if(c=='\\' && i+1<s.length()){
        char n=s.charAt(++i);
        switch(n){
          case 'n': sb.append('\n'); break;
          case 't': sb.append('\t'); break;
          case 'r': sb.append('\r'); break;
          case '\\': sb.append('\\'); break;
          case '"': sb.append('\"'); break;
          case '\'': sb.append('\''); break;
          default: sb.append(n); break;
        }
      } else sb.append(c);
    }
    return sb.toString();
  }
%}

/* ===== Macros (درست بین دو %% قرار دارند) ===== */
ALPHA        = [_A-Za-z]
ALNUM        = [_A-Za-z0-9]
ID           = {ALPHA}{ALNUM}*

DIGIT        = [0-9]
NZDIGIT      = [1-9]
INT10        = 0|{NZDIGIT}{DIGIT}*
HEX          = 0[xX][0-9a-fA-F]+
BIN          = 0[bB][01]+
OCT          = 0[0-7]+
EXP          = [eE][+-]?{DIGIT}+
FLOAT1       = {DIGIT}+\.{DIGIT}+({EXP})?
FLOAT2       = {DIGIT}+{EXP}
WS           = [ \t\f\r\n]+

STRING_BODY  = ([^\\\"\n]|\\.)*
STRING       = \"{STRING_BODY}\"
CHAR_BODY    = ([^\\'\n]|\\.)
CHAR         = \'{CHAR_BODY}\'

PP           = \#(include|define|ifndef|ifdef|undef|if|else|endif|elif)
LINE_COMMENT = "//"[^\r\n]*

%%

/* ===== Rules ===== */

/* whitespace & comments */
{WS}                        { /* skip */ }
{LINE_COMMENT}              { /* skip */ }
"/*"                        { yybegin(COMMENT); }
<COMMENT>{
  "*/"                      { yybegin(YYINITIAL); }
  [^*]+                     { /* skip */ }
  \*+[^/]                   { /* skip */ }
  \*+                       { /* stay */ }
}

/* preprocessor directives (#...) */
{PP}                        { return new Symbol(sym.PP_DIRECTIVE, yytext()); }

/* keywords: types */
"int"                       { return new Symbol(sym.INT_KW); }
"float"                     { return new Symbol(sym.FLOAT_KW); }
"double"                    { return new Symbol(sym.DOUBLE_KW); }
"char"                      { return new Symbol(sym.CHAR_KW); }
"bool"                      { return new Symbol(sym.BOOL_KW); }
"string"                    { return new Symbol(sym.STRING_KW); }

/* keywords: control/other */
"if"                        { return new Symbol(sym.IF); }
"else"                      { return new Symbol(sym.ELSE); }
"switch"                    { return new Symbol(sym.SWITCH); }
"case"                      { return new Symbol(sym.CASE); }
"for"                       { return new Symbol(sym.FOR); }
"while"                     { return new Symbol(sym.WHILE); }
"do"                        { return new Symbol(sym.DO); }
"break"                     { return new Symbol(sym.BREAK); }
"continue"                  { return new Symbol(sym.CONTINUE); }
"return"                    { return new Symbol(sym.RETURN); }
"default"                   { return new Symbol(sym.DEFAULT); }
"class"                     { return new Symbol(sym.CLASS); }
"public"                    { return new Symbol(sym.PUBLIC); }
"private"                   { return new Symbol(sym.PRIVATE); }
"protected"                 { return new Symbol(sym.PROTECTED); }
"template"                  { return new Symbol(sym.TEMPLATE); }
"static"                    { return new Symbol(sym.STATIC); }
"const"                     { return new Symbol(sym.CONST); }
"true"                      { return new Symbol(sym.BOOL_LIT, Boolean.TRUE); }
"false"                     { return new Symbol(sym.BOOL_LIT, Boolean.FALSE); }


/* boolean literals (optional) */
"true"                      { return new Symbol(sym.BOOL_LIT, Boolean.TRUE); }
"false"                     { return new Symbol(sym.BOOL_LIT, Boolean.FALSE); }

/* identifiers */
{ID}                        { return new Symbol(sym.ID, yytext()); }

/* literals (ترتیب مهم) */
{HEX}                       { return new Symbol(sym.HEX_LIT, yytext()); }
{BIN}                       { return new Symbol(sym.BIN_LIT, yytext()); }
{OCT}                       { return new Symbol(sym.OCT_LIT, yytext()); }
{FLOAT2}                    { return new Symbol(sym.SCI_LIT, yytext()); }
{FLOAT1}                    { return new Symbol(sym.FLOAT_LIT, Double.valueOf(yytext())); }
{INT10}                     { return new Symbol(sym.INT_LIT, Integer.valueOf(yytext())); }

{STRING}                    {
                               String raw = yytext();
                               String inner = raw.substring(1, raw.length()-1);
                               return new Symbol(sym.STRING_LIT, unescape(inner));
                            }
{CHAR}                      {
                               String raw = yytext();
                               String inner = raw.substring(1, raw.length()-1);
                               String un = unescape(inner);
                               Character ch = un.isEmpty() ? '\0' : un.charAt(0);
                               return new Symbol(sym.CHAR_LIT, ch);
                            }

/* operators (چندکاراکتری‌ها قبل از تک‌کاراکتری‌ها) */
/* arithmetic */
"++"                        { return new Symbol(sym.INC); }
"--"                        { return new Symbol(sym.DEC); }
"+"                         { return new Symbol(sym.PLUS); }
"-"                         { return new Symbol(sym.MINUS); }
"*"                         { return new Symbol(sym.STAR); }
"/"                         { return new Symbol(sym.SLASH); }
"%"                         { return new Symbol(sym.PERCENT); }
/* comparison (با موارد خاص پروژه) */
"=="                        { return new Symbol(sym.EQ); }
"!="                        { return new Symbol(sym.NE); }
"<="                        { return new Symbol(sym.LE); }
">="                        { return new Symbol(sym.GE); }
"<"                         { return new Symbol(sym.LT); }
">"                         { return new Symbol(sym.GT); }
"=>"                        { return new Symbol(sym.GE_ALT); }
"=<"                        { return new Symbol(sym.LE_ALT); }
"=!"                        { return new Symbol(sym.NE_ALT); }
/* logical */
"&&"                        { return new Symbol(sym.AND); }
"||"                        { return new Symbol(sym.OR); }
"!"                         { return new Symbol(sym.NOT); }
/* bitwise */
"<<"                        { return new Symbol(sym.LSHIFT); }
">>"                        { return new Symbol(sym.RSHIFT); }
"~"                         { return new Symbol(sym.BIT_NOT); }
"^"                         { return new Symbol(sym.BIT_XOR); }
"|"                         { return new Symbol(sym.BIT_OR); }
"&"                         { return new Symbol(sym.BIT_AND); }
/* assignment */
"+="                        { return new Symbol(sym.PLUS_EQ); }
"-="                        { return new Symbol(sym.MINUS_EQ); }
"*="                        { return new Symbol(sym.STAR_EQ); }
"/="                        { return new Symbol(sym.SLASH_EQ); }
"%="                        { return new Symbol(sym.PERCENT_EQ); }
"="                         { return new Symbol(sym.ASSIGN); }

/* delimiters */
"("                         { return new Symbol(sym.LPAR); }
")"                         { return new Symbol(sym.RPAR); }
"{"                         { return new Symbol(sym.LBRACE); }
"}"                         { return new Symbol(sym.RBRACE); }
"["                         { return new Symbol(sym.LBRACK); }
"]"                         { return new Symbol(sym.RBRACK); }
";"                         { return new Symbol(sym.SEMI); }
","                         { return new Symbol(sym.COMMA); }
":"                         { return new Symbol(sym.COLON); }

/* EOF & fallback */
<<EOF>>                     { return new Symbol(sym.EOF); }
.                           { /* ignore unknown */ }

import java.util.*;

public class Sem {
    /* ---------- Kinds & Types ---------- */
    public enum Kind { VAR, FUNC, CLASS }
    public enum Type { INT, FLOAT, DOUBLE, CHAR, BOOL, STRING, UNKNOWN }

    public static String typeName(Type t){
        switch (t){
            case INT:    return "int";
            case FLOAT:  return "float";
            case DOUBLE: return "double";
            case CHAR:   return "char";
            case BOOL:   return "bool";
            case STRING: return "string";
            default:     return "unknown";
        }
    }

    /** چاپ امن رشته با کوتیشن و escape */
    public static String quote(String s){
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"' : sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default  : sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** چاپ امن کاراکتر با کوتیشن تکی و escape */
    public static String quoteChar(char c){
        if (c == '\n') return "'\\n'";
        if (c == '\r') return "'\\r'";
        if (c == '\t') return "'\\t'";
        if (c == '\'') return "'\\''";
        if (c == '\\') return "'\\\\'";
        return "'" + c + "'";
    }

    /** ترفیع نوع عددی برای عملگرهای دودویی */
    public static Type promote(Type a, Type b){
        if (a == Type.DOUBLE || b == Type.DOUBLE) return Type.DOUBLE;
        if (a == Type.FLOAT  || b == Type.FLOAT)  return Type.FLOAT;
        // CHAR را معادل INT در نظر می‌گیریم
        return Type.INT;
    }
    /** ترفیع نوع برای تقسیم */
    public static Type promoteDiv(Type a, Type b){
        if (a == Type.DOUBLE || b == Type.DOUBLE) return Type.DOUBLE;
        return Type.FLOAT; // حداقل float (حتی اگر هر دو int باشند)
    }

    /* ---------- Symbol kinds per scope ---------- */
    public static class SymbolTable {
        private final Deque<Map<String,Kind>> stack = new ArrayDeque<>();
        public SymbolTable() { push(); }           // global scope
        public void push() { stack.push(new HashMap<>()); }
        public void pop()  { if (!stack.isEmpty()) stack.pop(); }
        /** declare kind in current scope; false if duplicate in this scope */
        public boolean declare(String name, Kind k) {
            Map<String,Kind> cur = stack.peek();
            if (cur.containsKey(name)) return false;
            cur.put(name, k);
            return true;
        }
        public boolean isDeclaredHere(String name){ return stack.peek().containsKey(name); }
    }

    /* ---------- Variable types per scope ---------- */
    public static class VarTypes {
        private final Deque<Map<String,Type>> stack = new ArrayDeque<>();
        public VarTypes(){ push(); }               // global scope
        public void push(){ stack.push(new HashMap<>()); }
        public void pop(){ if(!stack.isEmpty()) stack.pop(); }
        public void define(String name, Type t){ stack.peek().put(name, t); }
        public Type lookup(String name){
            for (Map<String,Type> m : stack) {
                Type t = m.get(name);
                if (t != null) return t;
            }
            return Type.UNKNOWN;
        }
    }

    public static final SymbolTable ST = new SymbolTable();
    public static final VarTypes   VT = new VarTypes();

    public static void pushScope(){ ST.push(); VT.push(); }
    public static void popScope(){  ST.pop();  VT.pop();  }

    /** declare var + type در اسکوپ جاری (false اگر تکراری باشد) */
    public static boolean declareVar(String name, Type t){
        if (!ST.declare(name, Kind.VAR)) return false;
        VT.define(name, t);
        return true;
    }

    /* ---------- Function signatures (typed) ---------- */
    private static final Map<String, List<Type>> funcSig = new HashMap<>();
    private static final Map<String, Type> funcRet = new HashMap<>();

    public static void recordFunction(String name, List<Type> paramTypes) {
        recordFunction(name, Type.INT, paramTypes);
    }

    public static void recordFunction(String name, Type retType, List<Type> paramTypes) {
        funcSig.put(name, new ArrayList<>(paramTypes));
        funcRet.put(name, retType);
    }
    public static Type getFunctionReturn(String name){
        Type t = funcRet.get(name);
        return (t==null)?Type.UNKNOWN:t;
    }

    /** چک امضای کامل: ناشناخته / تعداد / تطابق نوع؛ برای bool فقط 0 یا 1 از عدد پذیرفته می‌شود */
    public static void checkCallSignature(String name, ArgSig args) {
        List<Type> expected = funcSig.get(name);
        if (expected == null) {
            System.out.println("ERROR unknown function: " + name);
            return;
        }
        if (expected.size() != args.count) {
            System.out.println("ERROR bad args: " + name + " expected " + expected.size() + " got " + args.count);
            return;
        }
        for (int i = 0; i < expected.size(); i++) {
            Type e = expected.get(i);
            Type a = args.types.get(i);
            if (e == Type.BOOL && (a == Type.INT || a == Type.FLOAT || a == Type.DOUBLE || a == Type.CHAR)) {
                // فقط 0/1 (یا 0.0/1.0) مجاز است
                Double v = args.vals.get(i);
                if (v == null || !(v.doubleValue() == 0.0 || v.doubleValue() == 1.0)) {
                    System.out.println("ERROR type mismatch: " + name + " param " + (i+1) +
                            " expected " + typeName(e) + " got " + typeName(a));
                }
            } else if (e != a) {
                System.out.println("ERROR type mismatch: " + name + " param " + (i+1) +
                        " expected " + typeName(e) + " got " + typeName(a));
            }
        }
    }

    /* ---------- Params injection into next block ---------- */
    private static class PendingParam {
        final String name; final Type type;
        PendingParam(String n, Type t){ name=n; type=t; }
    }
    private static final List<PendingParam> pendingParams = new ArrayList<>();

    public static void addPendingParam(String name, Type t) { pendingParams.add(new PendingParam(name,t)); }

    /** هنگام باز شدن بلاک تابع صدا زده می‌شود تا پارامترها وارد اسکوپ شوند */
    public static void injectParamsIntoCurrentScope() {
        if (pendingParams.isEmpty()) return;
        for (PendingParam p : pendingParams) {
            if (!declareVar(p.name, p.type)) {
                System.out.println("ERROR duplicate param: " + p.name);
            }
        }
        pendingParams.clear();
    }

    /* ---------- CUP value classes (with AST hookup) ---------- */
    public static class Eval {
        public final Double   val;    // مقدار عددی (برای string/bool/char هم جهت سازگاری)
        public final Type     type;   // نوع استاتیک
        public final String   sval;   // مقدار متنی رشته (STRING_LIT)
        public final AST.Expr aexpr;  // نود AST متناظر با این expr

        public Eval(Double v, Type t){ this(v, t, null, null); }
        public Eval(Double v, Type t, String s){ this(v, t, s, null); }
        public Eval(Double v, Type t, String s, AST.Expr e){
            this.val=v; this.type=t; this.sval=s; this.aexpr=e;
        }
    }

    public static class ParamSig {
        public final String text;
        public final int count;
        public final List<Type> types;
        public final List<AST.Param> params;

        public ParamSig(String t, int c, List<Type> ts){
            this(t, c, ts, new ArrayList<AST.Param>());
        }
        public ParamSig(String t, int c, List<Type> ts, List<AST.Param> ps){
            text=t; count=c; types=ts; params=ps;
        }
    }

    public static class ArgSig {
        public final String text;
        public final int count;
        public final List<Type>   types;
        public final List<Double> vals;
        public final List<AST.Expr> aexprs;

        public ArgSig(String t, int c, List<Type> ts, List<Double> vs){
            this(t, c, ts, vs, new ArrayList<AST.Expr>());
        }
        public ArgSig(String t, int c, List<Type> ts, List<Double> vs, List<AST.Expr> es){
            text=t; count=c; types=ts; vals=vs; aexprs=es;
        }
    }
}

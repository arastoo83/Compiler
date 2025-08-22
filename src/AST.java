public final class AST {
    public static abstract class Node {}
    public static abstract class Stmt extends Node {}
    public static abstract class Expr extends Node {}

    public static final class Program extends Node {
        public final java.util.List<Stmt> stmts;
        public Program(java.util.List<Stmt> s){ this.stmts = s; }
    }

    public static final class Block extends Stmt {
        public final java.util.List<Stmt> stmts;
        public Block(java.util.List<Stmt> s){ this.stmts = s; }
    }

    public static final class VarDecl extends Stmt {
        public final String type, name; public final Expr init;
        public VarDecl(String t, String n, Expr i){ type=t; name=n; init=i; }
    }

    /* (اختیاری قدیمی) اگر جایی در کدهای قبلی‌ت از Assign به‌عنوان Stmt استفاده شده، نگهش می‌داریم. */
    public static final class Assign extends Stmt {
        public final String name; public final Expr expr;
        public Assign(String n, Expr e){ name=n; expr=e; }
    }

    /* ✅ نسخهٔ درست برای انتساب در اکسپرشن‌ها */
    public static final class AssignExpr extends Expr {
        public final String name; public final Expr expr;
        public AssignExpr(String n, Expr e){ name=n; expr=e; }
    }

    public static final class ExprStmt extends Stmt {
        public final Expr expr; public ExprStmt(Expr e){ expr=e; }
    }

    public static final class ReturnStmt extends Stmt {
        public final Expr expr; public ReturnStmt(Expr e){ expr=e; }
    }

    public static final class If extends Stmt {
        public final Expr cond; public final Stmt thenS, elseS;
        public If(Expr c, Stmt t, Stmt e){ cond=c; thenS=t; elseS=e; }
    }

    public static final class While extends Stmt {
        public final Expr cond; public final Stmt body;
        public While(Expr c, Stmt b){ cond=c; body=b; }
    }

    public static final class For extends Stmt {
        public final Stmt init, post, body; public final Expr cond;
        public For(Stmt i, Expr c, Stmt p, Stmt b){ init=i; cond=c; post=p; body=b; }
    }

    public static final class Param extends Node {
        public final String type, name;
        public Param(String t, String n){ type=t; name=n; }
    }

    public static final class FuncDecl extends Stmt {
        public final String retType, name; public final java.util.List<Param> params; public final Block body;
        public FuncDecl(String rt, String n, java.util.List<Param> ps, Block b){ retType=rt; name=n; params=ps; body=b; }
    }

    public static final class ClassDecl extends Stmt {
        public final String name; public final Block body;
        public ClassDecl(String n, Block b){ name=n; body=b; }
    }

    // ===== Expressions
    public static final class Id extends Expr { public final String name; public Id(String n){ name=n; } }
    public static final class Call extends Expr { public final String name; public final java.util.List<Expr> args;
        public Call(String n, java.util.List<Expr> a){ name=n; args=a; } }
    public static final class Binary extends Expr { public final String op; public final Expr left, right;
        public Binary(String o, Expr l, Expr r){ op=o; left=l; right=r; } }
    public static final class Unary extends Expr { public final String op; public final Expr expr;
        public Unary(String o, Expr e){ op=o; expr=e; } }
    public static final class IntLit extends Expr { public final long value; public IntLit(long v){ value=v; } }
    public static final class FloatLit extends Expr { public final double value; public FloatLit(double v){ value=v; } }
    public static final class StringLit extends Expr { public final String value; public StringLit(String v){ value=v; } }
    public static final class BoolLit extends Expr { public final boolean value; public BoolLit(boolean v){ value=v; } }
    public static final class CharLit extends Expr { public final char value; public CharLit(char v){ value=v; } }
}

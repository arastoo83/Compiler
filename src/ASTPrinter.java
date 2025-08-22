public class ASTPrinter {
    private int ind=0;
    private void p(String s){ for(int i=0;i<ind;i++) System.out.print("  "); System.out.println(s); }

    public void print(AST.Program pr){
        p("Program");
        ind++;
        for (AST.Stmt s: pr.stmts) print(s);
        ind--;
    }
    private void print(AST.Stmt s){
        if (s==null){ p("null"); return; }
        if (s instanceof AST.Block){ p("Block"); ind++; for (AST.Stmt x: ((AST.Block)s).stmts) print(x); ind--; }
        else if (s instanceof AST.VarDecl){ AST.VarDecl v=(AST.VarDecl)s; p("VarDecl "+v.type+" "+v.name); if(v.init!=null){ ind++; p("init:"); ind++; print(v.init); ind-=2; } }
        else if (s instanceof AST.Assign){ AST.Assign a=(AST.Assign)s; p("AssignStmt "+a.name); ind++; print(a.expr); ind--; }
        else if (s instanceof AST.ExprStmt){ p("ExprStmt"); ind++; print(((AST.ExprStmt)s).expr); ind--; }
        else if (s instanceof AST.ReturnStmt){ p("Return"); ind++; print(((AST.ReturnStmt)s).expr); ind--; }
        else if (s instanceof AST.If){ AST.If i=(AST.If)s; p("If"); ind++; print(i.cond); print(i.thenS); print(i.elseS); ind--; }
        else if (s instanceof AST.While){ AST.While w=(AST.While)s; p("While"); ind++; print(w.cond); print(w.body); ind--; }
        else if (s instanceof AST.For){ AST.For f=(AST.For)s; p("For"); ind++; print(f.init); print(f.cond); print(f.post); print(f.body); ind--; }
        else if (s instanceof AST.FuncDecl){ AST.FuncDecl fn=(AST.FuncDecl)s; p("Func "+fn.retType+" "+fn.name); ind++; for(AST.Param pa: fn.params) p("Param "+pa.type+" "+pa.name); print(fn.body); ind--; }
        else if (s instanceof AST.ClassDecl){ AST.ClassDecl c=(AST.ClassDecl)s; p("Class "+c.name); ind++; print(c.body); ind--; }
        else p("Stmt?");
    }
    private void print(AST.Expr e){
        if (e==null){ p("null"); return; }
        if (e instanceof AST.AssignExpr){ AST.AssignExpr a=(AST.AssignExpr)e; p("AssignExpr "+a.name); ind++; print(a.expr); ind--; }
        else if (e instanceof AST.IntLit){ p("Int "+((AST.IntLit)e).value); }
        else if (e instanceof AST.FloatLit){ p("Float "+((AST.FloatLit)e).value); }
        else if (e instanceof AST.StringLit){ p("String " + Sem.quote(((AST.StringLit)e).value)); } // ✅ escape امن
        else if (e instanceof AST.BoolLit){ p("Bool "+(((AST.BoolLit)e).value?"true":"false")); }
        else if (e instanceof AST.CharLit){ p("Char " + Sem.quoteChar(((AST.CharLit)e).value)); }   // ✅ escape امن
        else if (e instanceof AST.Id){ p("Id "+((AST.Id)e).name); }
        else if (e instanceof AST.Call){ AST.Call c=(AST.Call)e; p("Call "+c.name); ind++; for(AST.Expr a: c.args) print(a); ind--; }
        else if (e instanceof AST.Unary){ AST.Unary u=(AST.Unary)e; p("Unary "+u.op); ind++; print(u.expr); ind--; }
        else if (e instanceof AST.Binary){ AST.Binary b=(AST.Binary)e; p("Binary "+b.op); ind++; print(b.left); print(b.right); ind--; }
        else p("Expr?");
    }
}

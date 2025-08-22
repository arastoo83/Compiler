/* Es 1 - Ex 1 (Main) */
import java.io.*;
import java_cup.runtime.*;
   
public class Main {
  static public void main(String argv[]) {    
    try {
      /* Scanner instantiation */
      Lexer l = new Lexer(new FileReader(argv[0]));
      /* Parser instantiation */
      parser p = new parser(l);
      /* Start the parser */
      //Object result = p.parse(); 
      java_cup.runtime.Symbol sym = p.parse();       // parse انجام شد
      AST.Program root = (AST.Program) sym.value;    // AST ریشه
      // برای نمایش:
      new ASTPrinter().print(root);
      // new ASTPrinter().print(p.astRoot);     
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}



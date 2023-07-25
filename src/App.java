import ast.AST;
import csem.*;
import scanner.*;
import parser.*;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws Exception {
        String fileName = args[0];//input file
        AST ast = null;
        ast = buildAST(fileName, true);
        // ast.print();
        ast.standardize();
        evaluateST(ast);
    
    }
    private static AST buildAST(String fileName, boolean printOutput){
        AST ast = null;
        try{
          Scanner scanner = new Scanner(fileName);
          Parser parser = new Parser(scanner);
          ast = parser.buildAST();
        }catch(IOException e){
          throw new Parser_Exception("ERROR: Could not read from file: " + fileName);
        }
        return ast;
      }
    
      private static void evaluateST(AST ast){
        CSEMachine cseMachine = new CSEMachine(ast);
        cseMachine.evaluate_Program();
        System.out.println();
      }

    
}

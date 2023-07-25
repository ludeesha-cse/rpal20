package csem;

import java.util.Stack;

import ast.AST_Nd;
import ast.AST_Nd_Type;

/**
 * Used to evaluate conditionals.
 * 'cond -> then | else' in  becomes 'Beta cond' on the control stack where
 * Beta.thenBody = standardized version of then
 * Beta.elseBody = standardized version of else 
 
 */
public class Beta extends AST_Nd{
  public Stack<AST_Nd> then_Part;
  public Stack<AST_Nd> else_Part;
  
  public Beta(){
    setType(AST_Nd_Type.BETA);
    then_Part = new Stack<AST_Nd>();
    else_Part = new Stack<AST_Nd>();
  }
  
  public Beta acceptNode(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }




  public void setThen_Part(Stack<AST_Nd> thenBody){
    this.then_Part = thenBody;
  }

  public void setElse_Part(Stack<AST_Nd> elseBody){
    this.else_Part = elseBody;
  }
  
}

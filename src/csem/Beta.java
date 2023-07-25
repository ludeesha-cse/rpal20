package csem;

import java.util.Stack;

import ast.AST_Nd;
import ast.AST_Nd_Type;

/**
 * Used to evaluate conditionals.
 * 'cond -> then | else' in source becomes 'Beta cond' on the control stack where
 * Beta.thenBody = standardized version of then
 * Beta.elseBody = standardized version of else 
 * 
 * This inversion is key to implementing a program order evaluation
 * (critical for recursion where putting the then and else nodes above the Conditional
 * node on the control stack will cause infinite recursion if the then and else
 * nodes call the recursive function themselves). Putting the cond node before Beta (and, since
 * Beta contains the then and else nodes, effectively before the then and else nodes), allows
 * evaluating the cond first and then (in the base case) choosing the non-recursive option. This
 * allows breaking out of infinite recursion.
 * @author Group 9
 */
public class Beta extends AST_Nd{
  private Stack<AST_Nd> thenBody;
  private Stack<AST_Nd> elseBody;
  
  public Beta(){
    setType(AST_Nd_Type.BETA);
    thenBody = new Stack<AST_Nd>();
    elseBody = new Stack<AST_Nd>();
  }
  
  public Beta accept(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }

  public Stack<AST_Nd> getThenBody(){
    return thenBody;
  }

  public Stack<AST_Nd> getElseBody(){
    return elseBody;
  }

  public void setThenBody(Stack<AST_Nd> thenBody){
    this.thenBody = thenBody;
  }

  public void setElseBody(Stack<AST_Nd> elseBody){
    this.elseBody = elseBody;
  }
  
}

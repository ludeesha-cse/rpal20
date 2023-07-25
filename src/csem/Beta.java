package csem;

import java.util.Stack;

import ast.ASTNode;
import ast.ASTNodeType;

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
public class Beta extends ASTNode{
  private Stack<ASTNode> thenBody;
  private Stack<ASTNode> elseBody;
  
  public Beta(){
    setType(ASTNodeType.BETA);
    thenBody = new Stack<ASTNode>();
    elseBody = new Stack<ASTNode>();
  }
  
  public Beta accept(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }

  public Stack<ASTNode> getThenBody(){
    return thenBody;
  }

  public Stack<ASTNode> getElseBody(){
    return elseBody;
  }

  public void setThenBody(Stack<ASTNode> thenBody){
    this.thenBody = thenBody;
  }

  public void setElseBody(Stack<ASTNode> elseBody){
    this.elseBody = elseBody;
  }
  
}

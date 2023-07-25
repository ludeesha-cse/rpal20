package csem;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ast.ASTNode;
import ast.ASTNodeType;

/**
 * Represents a lambda closure.
 * @author Group 9
 */
public class Delta extends ASTNode{
  private List<String> boundVars;
  private Environment linkedEnv; //environment in effect when this Delta was pushed on to the value stack
  private Stack<ASTNode> body;
  private int index;
  
  public Delta(){
    setType(ASTNodeType.DELTA);
    boundVars = new ArrayList<String>();
  }
  
  public Delta accept(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }
  
  //used if the program evaluation results in a partial application
  @Override
  public String getValue(){
    return "[lambda closure: "+boundVars.get(0)+": "+index+"]";
  }

  public List<String> getBoundVars(){
    return boundVars;
  }
  
  public void addBoundVars(String boundVar){
    boundVars.add(boundVar);
  }
  
  public void setBoundVars(List<String> boundVars){
    this.boundVars = boundVars;
  }
  
  public Stack<ASTNode> getBody(){
    return body;
  }
  
  public void setBody(Stack<ASTNode> body){
    this.body = body;
  }
  
  public int getIndex(){
    return index;
  }

  public void setIndex(int index){
    this.index = index;
  }

  public Environment getLinkedEnv(){
    return linkedEnv;
  }

  public void setLinkedEnv(Environment linkedEnv){
    this.linkedEnv = linkedEnv;
  }
}

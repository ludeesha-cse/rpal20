package csem;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ast.AST_Nd;
import ast.AST_Nd_Type;

/**
 * 
 * 
 */
public class Delta extends AST_Nd{
  public List<String> boundVars;
  public Environment linkedEnv; 
  public Stack<AST_Nd> body;
  public int index;
  
  public Delta(){
    setType(AST_Nd_Type.DELTA);
    boundVars = new ArrayList<String>();
  }
  
  public Delta acceptNode(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }
  
  
  public String getValue(){
    return "[lambda closure: "+boundVars.get(0)+": "+index+"]";
  }

  
  
  public void addBoundVars(String boundVar){
    boundVars.add(boundVar);
  }
  
  public void setBoundVars(List<String> boundVars){
    this.boundVars = boundVars;
  }
  

  
  public void setBody(Stack<AST_Nd> body){
    this.body = body;
  }
  


  public void setIndex(int index){
    this.index = index;
  }

  

  public void setLinkedEnv(Environment linkedEnv){
    this.linkedEnv = linkedEnv;
  }
}

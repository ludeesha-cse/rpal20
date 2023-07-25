package csem;

import ast.AST_Nd;
import ast.AST_Nd_Type;

public class Tau extends AST_Nd{
  
  public Tau(){
    setType(AST_Nd_Type.TUPLE);
  }
  
  public String getValue(){
    AST_Nd childNode = child;
    if(childNode==null)
      return "nil";
    
    String printValue = "(";
    while(childNode.sibling!=null){
      printValue += childNode.value + ", ";
      childNode = childNode.sibling;
    }
    printValue += childNode.value + ")";
    return printValue;
  }
  
  public Tau acceptNode(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }
  
}

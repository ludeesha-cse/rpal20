package csem;

import ast.AST_Nd;
import ast.AST_Nd_Type;

public class Tuple extends AST_Nd{
  
  public Tuple(){
    setType(AST_Nd_Type.TUPLE);
  }
  
  @Override
  public String getValue(){
    AST_Nd childNode = getChild();
    if(childNode==null)
      return "nil";
    
    String printValue = "(";
    while(childNode.getSibling()!=null){
      printValue += childNode.getValue() + ", ";
      childNode = childNode.getSibling();
    }
    printValue += childNode.getValue() + ")";
    return printValue;
  }
  
  public Tuple accept(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }
  
}

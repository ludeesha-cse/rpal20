package ast;

import csem.NodeCopier;

/**
 * Abstract Syntax Tree node. Uses a first-child, next-sibling representation.
 * @author Group 9
 */
public class AST_Nd{
  public AST_Nd_Type type;
  public String value;
  public AST_Nd child;
  public AST_Nd sibling;
  public int sourceLineNumber;
  
  public String getName(){
    return type.name();
  }

  // public AST_Nd_Type getType(){
  //   return type;
  // }

  public void setType(AST_Nd_Type type){
    this.type = type;
  }

  public AST_Nd getChild(){
    return child;
  }

  public void setChild(AST_Nd child){
    this.child = child;
  }

  public AST_Nd getSibling(){
    return sibling;
  }

  public void setSibling(AST_Nd sibling){
    this.sibling = sibling;
  }

  public String getValue(){
    return value;
  }

  public void setValue(String value){
    this.value = value;
  }

  public AST_Nd accept(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }

  public int getSourceLineNumber(){
    return sourceLineNumber;
  }

  public void setSourceLineNumber(int sourceLineNumber){
    this.sourceLineNumber = sourceLineNumber;
  }
}

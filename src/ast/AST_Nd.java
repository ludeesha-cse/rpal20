package ast;

import csem.NodeCopier;

/**
 * Abstract Syntax Tree node
 * 
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

  

  public void setType(AST_Nd_Type type){
    this.type = type;
  }

  

  public void setChild(AST_Nd child){
    this.child = child;
  }

  

  public void setSibling(AST_Nd sibling){
    this.sibling = sibling;
  }

  

  public void setValue(String value){
    this.value = value;
  }

  public AST_Nd acceptNode(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }

  

  public void setSourceLineNumber(int sourceLineNumber){
    this.sourceLineNumber = sourceLineNumber;
  }
}

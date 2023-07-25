package csem;

import ast.AST_Nd;
import ast.AST_Nd_Type;

/**
 * Represents the fixed-point
 */
/**
 * The Eta class represents an eta closure with a delta object.
 */
public class Eta extends AST_Nd{
  private Delta delta;
  
  public Eta(){
    setType(AST_Nd_Type.ETA);
  }
  
  
  public String getValue(){
    return "[eta closure: "+delta.boundVars.get(0)+": "+delta.index+"]";
  }
  
  public Eta acceptNode(NodeCopier nodeCopier){
    return nodeCopier.copy(this);
  }

  public Delta getDelta(){
    return delta;
  }

  public void setDelta(Delta delta){
    this.delta = delta;
  }
  
}

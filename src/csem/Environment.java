package csem;

import java.util.HashMap;
import java.util.Map;

import ast.AST_Nd;

public class Environment{
  private Environment parent;
  private Map<String, AST_Nd> nameValueMap;
  
  public Environment(){
    nameValueMap = new HashMap<String, AST_Nd>();
  }

  public Environment getParent(){
    return parent;
  }

  public void setParent(Environment parent){
    this.parent = parent;
  }
  
  /**
   * binding of the given key in the mappings of this Environment'
   */
  public AST_Nd lookup(String key){
    AST_Nd retValue = null;
    Map<String, AST_Nd> map = nameValueMap;
    
    retValue = map.get(key);
    
    if(retValue!=null)
      return retValue.acceptNode(new NodeCopier());
    
    if(parent!=null)
      return parent.lookup(key);
    else
      return null;
  }
  
  public void doMapping(String key, AST_Nd value){
    nameValueMap.put(key, value);
  }
}

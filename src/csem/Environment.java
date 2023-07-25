package csem;

import java.util.HashMap;
import java.util.Map;

import ast.ASTNode;

public class Environment{
  private Environment parent;
  private Map<String, ASTNode> nameValueMap;
  
  public Environment(){
    nameValueMap = new HashMap<String, ASTNode>();
  }

  public Environment getParent(){
    return parent;
  }

  public void setParent(Environment parent){
    this.parent = parent;
  }
  
  /**
   * Tries to find the binding of the given key in the mappings of this Environment's
   * inheritance hierarchy, starting with the Environment this method is invoked on.
   * 
   * @param key key the mapping of which to find
   * @return ASTNode that corresponds to the mapping of the key passed in as an argument
   *         or null if no mapping was found
   */
  public ASTNode lookup(String key){
    ASTNode retValue = null;
    Map<String, ASTNode> map = nameValueMap;
    
    retValue = map.get(key);
    
    if(retValue!=null)
      return retValue.accept(new NodeCopier());
    
    if(parent!=null)
      return parent.lookup(key);
    else
      return null;
  }
  
  public void addMapping(String key, ASTNode value){
    nameValueMap.put(key, value);
  }
}

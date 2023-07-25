package csem;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import ast.AST_Nd;

/**
 * Class to make copies of nodes on value stack
 */
public class NodeCopier{
  
  public AST_Nd copy(AST_Nd astNode){
    AST_Nd copy = new AST_Nd();
    if(astNode.child!=null)
      copy.setChild(astNode.child.acceptNode(this));
    if(astNode.sibling!=null)
      copy.setSibling(astNode.sibling.acceptNode(this));
    copy.setType(astNode.type);
    copy.setValue(astNode.value);
    copy.setSourceLineNumber(astNode.sourceLineNumber);
    return copy;
  }
  
  public Beta copy(Beta beta){
    Beta copy = new Beta();
    if(beta.child!=null)
      copy.setChild(beta.child.acceptNode(this));
    if(beta.sibling!=null)
      copy.setSibling(beta.sibling.acceptNode(this));
    copy.setType(beta.type);
    copy.setValue(beta.value);
    copy.setSourceLineNumber(beta.sourceLineNumber);
    
    Stack<AST_Nd> thenBodyCopy = new Stack<AST_Nd>();
    for(AST_Nd thenBodyElement: beta.then_Part){
      thenBodyCopy.add(thenBodyElement.acceptNode(this));
    }
    copy.setThen_Part(thenBodyCopy);
    
    Stack<AST_Nd> elseBodyCopy = new Stack<AST_Nd>();
    for(AST_Nd elseBodyElement: beta.else_Part){
      elseBodyCopy.add(elseBodyElement.acceptNode(this));
    }
    copy.setElse_Part(elseBodyCopy);
    
    return copy;
  }
  
  public Eta copy(Eta eta){
    Eta copy = new Eta();
    if(eta.child!=null)
      copy.setChild(eta.child.acceptNode(this));
    if(eta.sibling!=null)
      copy.setSibling(eta.sibling.acceptNode(this));
    copy.setType(eta.type);
    copy.setValue(eta.value);
    copy.setSourceLineNumber(eta.sourceLineNumber);
    
    copy.setDelta(eta.getDelta().acceptNode(this));
    
    return copy;
  }
  
  public Delta copy(Delta delta){
    Delta copy = new Delta();
    if(delta.child!=null)
      copy.setChild(delta.child.acceptNode(this));
    if(delta.sibling!=null)
      copy.setSibling(delta.sibling.acceptNode(this));
    copy.setType(delta.type);
    copy.setValue(delta.value);
    copy.setIndex(delta.index);
    copy.setSourceLineNumber(delta.sourceLineNumber);
    
    Stack<AST_Nd> bodyCopy = new Stack<AST_Nd>();
    for(AST_Nd bodyElement: delta.body){
      bodyCopy.add(bodyElement.acceptNode(this));
    }
    copy.setBody(bodyCopy);
    
    List<String> boundVarsCopy = new ArrayList<String>();
    boundVarsCopy.addAll(delta.boundVars);
    copy.setBoundVars(boundVarsCopy);
    
    copy.setLinkedEnv(delta.linkedEnv);
    
    return copy;
  }
  
  public Tau copy(Tau tuple){
    Tau copy = new Tau();
    if(tuple.child!=null)
      copy.setChild(tuple.child.acceptNode(this));
    if(tuple.sibling!=null)
      copy.setSibling(tuple.sibling.acceptNode(this));
    copy.setType(tuple.type);
    copy.setValue(tuple.value);
    copy.setSourceLineNumber(tuple.sourceLineNumber);
    return copy;
  }
}

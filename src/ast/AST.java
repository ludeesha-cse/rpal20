package ast;

import java.util.ArrayDeque;
import java.util.Stack;

import csem.Beta;
import csem.Delta;

/*
 * Abstract Syntax Tree:
 */
public class AST{
  public AST_Nd root;
  private ArrayDeque<PendingDeltaBody> pendingDeltaQ;
  private boolean standardized;
  private Delta currentDelta;
  private Delta rootDelta;
  private int deltaIndex;

  public AST(AST_Nd node){
    this.root = node;
  }

  /**
   * Prints the tree nodes in pre-order style.
   */
  public void print(){
    preOrderPrint(root,"");
  }

  private void preOrderPrint(AST_Nd node, String printPrefix){
    if(node==null)
      return;

    Print_AST_Node(node, printPrefix);
    preOrderPrint(node.child,printPrefix+".");
    preOrderPrint(node.sibling,printPrefix);
  }

  private void Print_AST_Node(AST_Nd node, String printPrefix){
    if(node.type == AST_Nd_Type.IDENTIFIER ||
        node.type == AST_Nd_Type.INTEGER){
      System.out.printf(printPrefix+node.type.getPrintName()+"\n",node.value);
    }
    else if(node.type == AST_Nd_Type.STRING)
      System.out.printf(printPrefix+node.type.getPrintName()+"\n",node.value);
    else
      System.out.println(printPrefix+node.type.getPrintName());
  }

  /**
   * Standardize the tree
   */
  public void standardize(){
    standardizeTree(root);
    standardized = true;
  }

  /**
   * Standardize the tree bottom-up
   * 
   */
  private void standardizeTree(AST_Nd node){ //
    //standardize the children first
    if(node.child!=null){
      AST_Nd childNode = node.child;
      while(childNode!=null){
        standardizeTree(childNode);
        childNode = childNode.sibling;
      }
    }

    //. now standardize the current node
    switch(node.type){
      case LET:
        //       LET              GAMMA
        //     /     \           /     \
        //    EQUAL   P   ->   LAMBDA   E
        //   /   \             /    \
        //  X     E           X      P
        AST_Nd equalNode = node.child;
        if(equalNode.type!=AST_Nd_Type.EQUAL)
          throw new ST_expct("LET/WHERE: left child is not EQUAL"); //safety
        AST_Nd e = equalNode.child.sibling;
        equalNode.child.setSibling(equalNode.sibling);
        equalNode.setSibling(e);
        equalNode.setType(AST_Nd_Type.LAMBDA);
        node.setType(AST_Nd_Type.GAMMA);
        break;
      case WHERE:
        //make this is a LET node and standardize that
        //       WHERE               LET
        //       /   \             /     \
        //      P    EQUAL   ->  EQUAL   P
        //           /   \       /   \
        //          X     E     X     E
        equalNode = node.child.sibling;
        node.child.setSibling(null);
        equalNode.setSibling(node.child);
        node.setChild(equalNode);
        node.setType(AST_Nd_Type.LET);
        standardizeTree(node);
        break;
      case FCNFORM:
        //       FCN_FORM                EQUAL
        //       /   |   \              /    \
        //      P    V+   E    ->      P     +LAMBDA
        //                                    /     \
        //                                    V     .E
        AST_Nd childSibling = node.child.sibling;
        node.child.setSibling(constructLambdaChain(childSibling));
        node.setType(AST_Nd_Type.EQUAL);
        break;
      case AT:
        //         AT              GAMMA
        //       / | \    ->       /    \
        //      E1 N E2          GAMMA   E2
        //                       /    \
        //                      N     E1
        AST_Nd e1 = node.child;
        AST_Nd n = e1.sibling;
        AST_Nd e2 = n.sibling;
        AST_Nd gammaNode = new AST_Nd();
        gammaNode.setType(AST_Nd_Type.GAMMA);
        gammaNode.setChild(n);
        n.setSibling(e1);
        e1.setSibling(null);
        gammaNode.setSibling(e2);
        node.setChild(gammaNode);
        node.setType(AST_Nd_Type.GAMMA);
        break;
      case WITHIN:
        //           WITHIN                  EQUAL
        //          /      \                /     \
        //        EQUAL   EQUAL    ->      X2     GAMMA
        //       /    \   /    \                  /    \
        //      X1    E1 X2    E2               LAMBDA  E1
        //                                      /    \
        //                                     X1    E2
        if(node.child.type!=AST_Nd_Type.EQUAL || node.child.sibling.type!=AST_Nd_Type.EQUAL)
          throw new ST_expct("WITHIN: one of the children is not EQUAL"); //safety
        AST_Nd x1 = node.child.child;
        e1 = x1.sibling;
        AST_Nd x2 = node.child.sibling.child;
        e2 = x2.sibling;
        AST_Nd lambdaNode = new AST_Nd();
        lambdaNode.setType(AST_Nd_Type.LAMBDA);
        x1.setSibling(e2);
        lambdaNode.setChild(x1);
        lambdaNode.setSibling(e1);
        gammaNode = new AST_Nd();
        gammaNode.setType(AST_Nd_Type.GAMMA);
        gammaNode.setChild(lambdaNode);
        x2.setSibling(gammaNode);
        node.setChild(x2);
        node.setType(AST_Nd_Type.EQUAL);
        break;
      case SIMULTDEF:
        //         SIMULTDEF            EQUAL
        //             |               /     \
        //           EQUAL++  ->     COMMA   TAU
        //           /   \             |      |
        //          X     E           X++    E++
        AST_Nd commaNode = new AST_Nd();
        commaNode.setType(AST_Nd_Type.COMMA);
        AST_Nd tauNode = new AST_Nd();
        tauNode.setType(AST_Nd_Type.TAU);
        AST_Nd childNode = node.child;
        while(childNode!=null){
          populateCommaAndTauNode(childNode, commaNode, tauNode);
          childNode = childNode.sibling;
        }
        commaNode.setSibling(tauNode);
        node.setChild(commaNode);
        node.setType(AST_Nd_Type.EQUAL);
        break;
      case REC:
        //        REC                 EQUAL
        //         |                 /     \
        //       EQUAL     ->       X     GAMMA
        //      /     \                   /    \
        //     X       E                YSTAR  LAMBDA
        //                                     /     \
        //                                    X       E
        childNode = node.child;
        if(childNode.type!=AST_Nd_Type.EQUAL)
          throw new ST_expct("REC: child is not EQUAL"); //safety
        AST_Nd x = childNode.child;
        lambdaNode = new AST_Nd();
        lambdaNode.setType(AST_Nd_Type.LAMBDA);
        lambdaNode.setChild(x); //x is already attached to e
        AST_Nd yStarNode = new AST_Nd();
        yStarNode.setType(AST_Nd_Type.YSTAR);
        yStarNode.setSibling(lambdaNode);
        gammaNode = new AST_Nd();
        gammaNode.setType(AST_Nd_Type.GAMMA);
        gammaNode.setChild(yStarNode);
        AST_Nd xWithSiblingGamma = new AST_Nd(); //same as x except the sibling is not e but gamma
        xWithSiblingGamma.setChild(x.child);
        xWithSiblingGamma.setSibling(gammaNode);
        xWithSiblingGamma.setType(x.type);
        xWithSiblingGamma.setValue(x.value);
        node.setChild(xWithSiblingGamma);
        node.setType(AST_Nd_Type.EQUAL);
        break;
      case LAMBDA:
        //     LAMBDA        LAMBDA
        //      /   \   ->   /    \
        //     V++   E      V     .E
        childSibling = node.child.sibling;
        node.child.setSibling(constructLambdaChain(childSibling));
        break;
      default:
      
        break;
    }
  }

  private void populateCommaAndTauNode(AST_Nd equalNode, AST_Nd commaNode, AST_Nd tauNode){
    if(equalNode.type!=AST_Nd_Type.EQUAL)
      throw new ST_expct("SIMULTDEF: one of the children is not EQUAL"); //safety
    AST_Nd x = equalNode.child;
    AST_Nd e = x.sibling;
    setChild(commaNode, x);
    setChild(tauNode, e);
  }

  /**
   * Either creates a new child of the parent or attaches the child node passed in
   * as the last sibling of the parent's childs
   */
  private void setChild(AST_Nd parentNode, AST_Nd childNode){
    if(parentNode.child==null)
      parentNode.setChild(childNode);
    else{
      AST_Nd lastSibling = parentNode.child;
      while(lastSibling.sibling!=null)
        lastSibling = lastSibling.sibling;
      lastSibling.setSibling(childNode);
    }
    childNode.setSibling(null);
  }

  private AST_Nd constructLambdaChain(AST_Nd node){
    if(node.sibling==null)
      return node;
    
    AST_Nd lambdaNode = new AST_Nd();
    lambdaNode.setType(AST_Nd_Type.LAMBDA);
    lambdaNode.setChild(node);
    if(node.sibling.sibling!=null)
      node.setSibling(constructLambdaChain(node.sibling));
    return lambdaNode;
  }

  /**
   * Creates delta structures from the standardized tree
   * 
   */
  public Delta createDeltas(){
    pendingDeltaQ = new ArrayDeque<PendingDeltaBody>();
    deltaIndex = 0;
    currentDelta = createDelta(root);
    processPendingDeltaStack();
    return rootDelta;
  }

  private Delta createDelta(AST_Nd startBodyNode){
    
    PendingDeltaBody pendingDelta = new PendingDeltaBody();
    pendingDelta.startNode = startBodyNode;
    pendingDelta.body = new Stack<AST_Nd>();
    pendingDeltaQ.add(pendingDelta);
    
    Delta d = new Delta();
    d.setBody(pendingDelta.body);
    d.setIndex(deltaIndex++);
    currentDelta = d;
    
    if(startBodyNode==root)
      rootDelta = currentDelta;
    
    return d;
  }

  private void processPendingDeltaStack(){
    while(!pendingDeltaQ.isEmpty()){
      PendingDeltaBody pendingDeltaBody = pendingDeltaQ.pop();
      buildDeltaBody(pendingDeltaBody.startNode, pendingDeltaBody.body);
    }
  }
  
  private void buildDeltaBody(AST_Nd node, Stack<AST_Nd> body){
    if(node.type==AST_Nd_Type.LAMBDA){ //create a new delta
      Delta d = createDelta(node.child.sibling); 
      if(node.child.type==AST_Nd_Type.COMMA){ 
        AST_Nd commaNode = node.child;
        AST_Nd childNode = commaNode.child;
        while(childNode!=null){
          d.addBoundVars(childNode.value);
          childNode = childNode.sibling;
        }
      }
      else
        d.addBoundVars(node.child.value);
      body.push(d); //add  new delta to the existing delta's body
      return;
    }
    else if(node.type==AST_Nd_Type.CONDITIONAL){
      //traverse the children in reverse order so the condition leads
      // cond -> then else becomes then else Beta cond
      AST_Nd conditionNode = node.child;
      AST_Nd thenNode = conditionNode.sibling;
      AST_Nd elseNode = thenNode.sibling;
      
      //Add a Beta node.
      Beta betaNode = new Beta();
      
      buildDeltaBody(thenNode, betaNode.then_Part);
      buildDeltaBody(elseNode, betaNode.else_Part);
      
      body.push(betaNode);
      
      buildDeltaBody(conditionNode, body);
      
      return;
    }
    
    //
    body.push(node);
    AST_Nd childNode = node.child;
    while(childNode!=null){
      buildDeltaBody(childNode, body);
      childNode = childNode.sibling;
    }
  }

  private class PendingDeltaBody{
    Stack<AST_Nd> body;
    AST_Nd startNode;
  }

  public boolean isStandardized(){
    return standardized;
  }
}

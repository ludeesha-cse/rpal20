package ast;

import java.util.ArrayDeque;
import java.util.Stack;

import csem.Beta;
import csem.Delta;

/*
 * Abstract Syntax Tree: The nodes use a first-child
 * next-sibling representation.
 */
public class AST{
  private AST_Nd root;
  private ArrayDeque<PendingDeltaBody> pendingDeltaBodyQueue;
  private boolean standardized;
  private Delta currentDelta;
  private Delta rootDelta;
  private int deltaIndex;

  public AST(AST_Nd node){
    this.root = node;
  }

  /**
   * Prints the tree nodes in pre-order fashion.
   */
  public void print(){
    preOrderPrint(root,"");
  }

  private void preOrderPrint(AST_Nd node, String printPrefix){
    if(node==null)
      return;

    Print_AST_Node(node, printPrefix);
    preOrderPrint(node.getChild(),printPrefix+".");
    preOrderPrint(node.getSibling(),printPrefix);
  }

  private void Print_AST_Node(AST_Nd node, String printPrefix){
    if(node.type == AST_Nd_Type.IDENTIFIER ||
        node.type == AST_Nd_Type.INTEGER){
      System.out.printf(printPrefix+node.type.getPrintName()+"\n",node.getValue());
    }
    else if(node.type == AST_Nd_Type.STRING)
      System.out.printf(printPrefix+node.type.getPrintName()+"\n",node.getValue());
    else
      System.out.println(printPrefix+node.type.getPrintName());
  }

  /**
   * Standardize this tree
   */
  public void standardize(){
    standardize(root);
    standardized = true;
  }

  /**
   * Standardize the tree bottom-up
   * @param node node to standardize
   */
  private void standardize(AST_Nd node){
    //standardize the children first
    if(node.getChild()!=null){
      AST_Nd childNode = node.getChild();
      while(childNode!=null){
        standardize(childNode);
        childNode = childNode.getSibling();
      }
    }

    //all children standardized. now standardize this node
    switch(node.type){
      case LET:
        //       LET              GAMMA
        //     /     \           /     \
        //    EQUAL   P   ->   LAMBDA   E
        //   /   \             /    \
        //  X     E           X      P
        AST_Nd equalNode = node.getChild();
        if(equalNode.type!=AST_Nd_Type.EQUAL)
          throw new ST_expct("LET/WHERE: left child is not EQUAL"); //safety
        AST_Nd e = equalNode.getChild().getSibling();
        equalNode.getChild().setSibling(equalNode.getSibling());
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
        equalNode = node.getChild().getSibling();
        node.getChild().setSibling(null);
        equalNode.setSibling(node.getChild());
        node.setChild(equalNode);
        node.setType(AST_Nd_Type.LET);
        standardize(node);
        break;
      case FCNFORM:
        //       FCN_FORM                EQUAL
        //       /   |   \              /    \
        //      P    V+   E    ->      P     +LAMBDA
        //                                    /     \
        //                                    V     .E
        AST_Nd childSibling = node.getChild().getSibling();
        node.getChild().setSibling(constructLambdaChain(childSibling));
        node.setType(AST_Nd_Type.EQUAL);
        break;
      case AT:
        //         AT              GAMMA
        //       / | \    ->       /    \
        //      E1 N E2          GAMMA   E2
        //                       /    \
        //                      N     E1
        AST_Nd e1 = node.getChild();
        AST_Nd n = e1.getSibling();
        AST_Nd e2 = n.getSibling();
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
        if(node.getChild().type!=AST_Nd_Type.EQUAL || node.getChild().getSibling().type!=AST_Nd_Type.EQUAL)
          throw new ST_expct("WITHIN: one of the children is not EQUAL"); //safety
        AST_Nd x1 = node.getChild().getChild();
        e1 = x1.getSibling();
        AST_Nd x2 = node.getChild().getSibling().getChild();
        e2 = x2.getSibling();
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
        AST_Nd childNode = node.getChild();
        while(childNode!=null){
          populateCommaAndTauNode(childNode, commaNode, tauNode);
          childNode = childNode.getSibling();
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
        childNode = node.getChild();
        if(childNode.type!=AST_Nd_Type.EQUAL)
          throw new ST_expct("REC: child is not EQUAL"); //safety
        AST_Nd x = childNode.getChild();
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
        xWithSiblingGamma.setChild(x.getChild());
        xWithSiblingGamma.setSibling(gammaNode);
        xWithSiblingGamma.setType(x.type);
        xWithSiblingGamma.setValue(x.getValue());
        node.setChild(xWithSiblingGamma);
        node.setType(AST_Nd_Type.EQUAL);
        break;
      case LAMBDA:
        //     LAMBDA        LAMBDA
        //      /   \   ->   /    \
        //     V++   E      V     .E
        childSibling = node.getChild().getSibling();
        node.getChild().setSibling(constructLambdaChain(childSibling));
        break;
      default:
        // Node types we do NOT standardize:
        // CSE Optimization Rule 6 (binops)
        // OR
        // AND
        // PLUS
        // MINUS
        // MULT
        // DIV
        // EXP
        // GR
        // GE
        // LS
        // LE
        // EQ
        // NE
        // CSE Optimization Rule 7 (unops)
        // NOT
        // NEG
        // CSE Optimization Rule 8 (conditionals)
        // CONDITIONAL
        // CSE Optimization Rule 9, 10 (tuples)
        // TAU
        // CSE Optimization Rule 11 (n-ary functions)
        // COMMA
        break;
    }
  }

  private void populateCommaAndTauNode(AST_Nd equalNode, AST_Nd commaNode, AST_Nd tauNode){
    if(equalNode.type!=AST_Nd_Type.EQUAL)
      throw new ST_expct("SIMULTDEF: one of the children is not EQUAL"); //safety
    AST_Nd x = equalNode.getChild();
    AST_Nd e = x.getSibling();
    setChild(commaNode, x);
    setChild(tauNode, e);
  }

  /**
   * Either creates a new child of the parent or attaches the child node passed in
   * as the last sibling of the parent's existing children 
   * @param parentNode
   * @param childNode
   */
  private void setChild(AST_Nd parentNode, AST_Nd childNode){
    if(parentNode.getChild()==null)
      parentNode.setChild(childNode);
    else{
      AST_Nd lastSibling = parentNode.getChild();
      while(lastSibling.getSibling()!=null)
        lastSibling = lastSibling.getSibling();
      lastSibling.setSibling(childNode);
    }
    childNode.setSibling(null);
  }

  private AST_Nd constructLambdaChain(AST_Nd node){
    if(node.getSibling()==null)
      return node;
    
    AST_Nd lambdaNode = new AST_Nd();
    lambdaNode.setType(AST_Nd_Type.LAMBDA);
    lambdaNode.setChild(node);
    if(node.getSibling().getSibling()!=null)
      node.setSibling(constructLambdaChain(node.getSibling()));
    return lambdaNode;
  }

  /**
   * Creates delta structures from the standardized tree
   * @return the first delta structure (&delta;0)
   */
  public Delta createDeltas(){
    pendingDeltaBodyQueue = new ArrayDeque<PendingDeltaBody>();
    deltaIndex = 0;
    currentDelta = createDelta(root);
    processPendingDeltaStack();
    return rootDelta;
  }

  private Delta createDelta(AST_Nd startBodyNode){
    //we'll create this delta's body later
    PendingDeltaBody pendingDelta = new PendingDeltaBody();
    pendingDelta.startNode = startBodyNode;
    pendingDelta.body = new Stack<AST_Nd>();
    pendingDeltaBodyQueue.add(pendingDelta);
    
    Delta d = new Delta();
    d.setBody(pendingDelta.body);
    d.setIndex(deltaIndex++);
    currentDelta = d;
    
    if(startBodyNode==root)
      rootDelta = currentDelta;
    
    return d;
  }

  private void processPendingDeltaStack(){
    while(!pendingDeltaBodyQueue.isEmpty()){
      PendingDeltaBody pendingDeltaBody = pendingDeltaBodyQueue.pop();
      buildDeltaBody(pendingDeltaBody.startNode, pendingDeltaBody.body);
    }
  }
  
  private void buildDeltaBody(AST_Nd node, Stack<AST_Nd> body){
    if(node.type==AST_Nd_Type.LAMBDA){ //create a new delta
      Delta d = createDelta(node.getChild().getSibling()); //the new delta's body starts at the right child of the lambda
      if(node.getChild().type==AST_Nd_Type.COMMA){ //the left child of the lambda is the bound variable
        AST_Nd commaNode = node.getChild();
        AST_Nd childNode = commaNode.getChild();
        while(childNode!=null){
          d.addBoundVars(childNode.getValue());
          childNode = childNode.getSibling();
        }
      }
      else
        d.addBoundVars(node.getChild().getValue());
      body.push(d); //add this new delta to the existing delta's body
      return;
    }
    else if(node.type==AST_Nd_Type.CONDITIONAL){
      //to enable programming order evaluation, traverse the children in reverse order so the condition leads
      // cond -> then else becomes then else Beta cond
      AST_Nd conditionNode = node.getChild();
      AST_Nd thenNode = conditionNode.getSibling();
      AST_Nd elseNode = thenNode.getSibling();
      
      //Add a Beta node.
      Beta betaNode = new Beta();
      
      buildDeltaBody(thenNode, betaNode.getThenBody());
      buildDeltaBody(elseNode, betaNode.getElseBody());
      
      body.push(betaNode);
      
      buildDeltaBody(conditionNode, body);
      
      return;
    }
    
    //preOrder walk
    body.push(node);
    AST_Nd childNode = node.getChild();
    while(childNode!=null){
      buildDeltaBody(childNode, body);
      childNode = childNode.getSibling();
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

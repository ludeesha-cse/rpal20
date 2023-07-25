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
  private ASTNode root;
  private ArrayDeque<PendingDeltaBody> pendingDeltaBodyQueue;
  private boolean standardized;
  private Delta currentDelta;
  private Delta rootDelta;
  private int deltaIndex;

  public AST(ASTNode node){
    this.root = node;
  }

  /**
   * Prints the tree nodes in pre-order fashion.
   */
  public void print(){
    preOrderPrint(root,"");
  }

  private void preOrderPrint(ASTNode node, String printPrefix){
    if(node==null)
      return;

    printASTNodeDetails(node, printPrefix);
    preOrderPrint(node.getChild(),printPrefix+".");
    preOrderPrint(node.getSibling(),printPrefix);
  }

  private void printASTNodeDetails(ASTNode node, String printPrefix){
    if(node.getType() == ASTNodeType.IDENTIFIER ||
        node.getType() == ASTNodeType.INTEGER){
      System.out.printf(printPrefix+node.getType().getPrintName()+"\n",node.getValue());
    }
    else if(node.getType() == ASTNodeType.STRING)
      System.out.printf(printPrefix+node.getType().getPrintName()+"\n",node.getValue());
    else
      System.out.println(printPrefix+node.getType().getPrintName());
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
  private void standardize(ASTNode node){
    //standardize the children first
    if(node.getChild()!=null){
      ASTNode childNode = node.getChild();
      while(childNode!=null){
        standardize(childNode);
        childNode = childNode.getSibling();
      }
    }

    //all children standardized. now standardize this node
    switch(node.getType()){
      case LET:
        //       LET              GAMMA
        //     /     \           /     \
        //    EQUAL   P   ->   LAMBDA   E
        //   /   \             /    \
        //  X     E           X      P
        ASTNode equalNode = node.getChild();
        if(equalNode.getType()!=ASTNodeType.EQUAL)
          throw new StandardizeException("LET/WHERE: left child is not EQUAL"); //safety
        ASTNode e = equalNode.getChild().getSibling();
        equalNode.getChild().setSibling(equalNode.getSibling());
        equalNode.setSibling(e);
        equalNode.setType(ASTNodeType.LAMBDA);
        node.setType(ASTNodeType.GAMMA);
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
        node.setType(ASTNodeType.LET);
        standardize(node);
        break;
      case FCNFORM:
        //       FCN_FORM                EQUAL
        //       /   |   \              /    \
        //      P    V+   E    ->      P     +LAMBDA
        //                                    /     \
        //                                    V     .E
        ASTNode childSibling = node.getChild().getSibling();
        node.getChild().setSibling(constructLambdaChain(childSibling));
        node.setType(ASTNodeType.EQUAL);
        break;
      case AT:
        //         AT              GAMMA
        //       / | \    ->       /    \
        //      E1 N E2          GAMMA   E2
        //                       /    \
        //                      N     E1
        ASTNode e1 = node.getChild();
        ASTNode n = e1.getSibling();
        ASTNode e2 = n.getSibling();
        ASTNode gammaNode = new ASTNode();
        gammaNode.setType(ASTNodeType.GAMMA);
        gammaNode.setChild(n);
        n.setSibling(e1);
        e1.setSibling(null);
        gammaNode.setSibling(e2);
        node.setChild(gammaNode);
        node.setType(ASTNodeType.GAMMA);
        break;
      case WITHIN:
        //           WITHIN                  EQUAL
        //          /      \                /     \
        //        EQUAL   EQUAL    ->      X2     GAMMA
        //       /    \   /    \                  /    \
        //      X1    E1 X2    E2               LAMBDA  E1
        //                                      /    \
        //                                     X1    E2
        if(node.getChild().getType()!=ASTNodeType.EQUAL || node.getChild().getSibling().getType()!=ASTNodeType.EQUAL)
          throw new StandardizeException("WITHIN: one of the children is not EQUAL"); //safety
        ASTNode x1 = node.getChild().getChild();
        e1 = x1.getSibling();
        ASTNode x2 = node.getChild().getSibling().getChild();
        e2 = x2.getSibling();
        ASTNode lambdaNode = new ASTNode();
        lambdaNode.setType(ASTNodeType.LAMBDA);
        x1.setSibling(e2);
        lambdaNode.setChild(x1);
        lambdaNode.setSibling(e1);
        gammaNode = new ASTNode();
        gammaNode.setType(ASTNodeType.GAMMA);
        gammaNode.setChild(lambdaNode);
        x2.setSibling(gammaNode);
        node.setChild(x2);
        node.setType(ASTNodeType.EQUAL);
        break;
      case SIMULTDEF:
        //         SIMULTDEF            EQUAL
        //             |               /     \
        //           EQUAL++  ->     COMMA   TAU
        //           /   \             |      |
        //          X     E           X++    E++
        ASTNode commaNode = new ASTNode();
        commaNode.setType(ASTNodeType.COMMA);
        ASTNode tauNode = new ASTNode();
        tauNode.setType(ASTNodeType.TAU);
        ASTNode childNode = node.getChild();
        while(childNode!=null){
          populateCommaAndTauNode(childNode, commaNode, tauNode);
          childNode = childNode.getSibling();
        }
        commaNode.setSibling(tauNode);
        node.setChild(commaNode);
        node.setType(ASTNodeType.EQUAL);
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
        if(childNode.getType()!=ASTNodeType.EQUAL)
          throw new StandardizeException("REC: child is not EQUAL"); //safety
        ASTNode x = childNode.getChild();
        lambdaNode = new ASTNode();
        lambdaNode.setType(ASTNodeType.LAMBDA);
        lambdaNode.setChild(x); //x is already attached to e
        ASTNode yStarNode = new ASTNode();
        yStarNode.setType(ASTNodeType.YSTAR);
        yStarNode.setSibling(lambdaNode);
        gammaNode = new ASTNode();
        gammaNode.setType(ASTNodeType.GAMMA);
        gammaNode.setChild(yStarNode);
        ASTNode xWithSiblingGamma = new ASTNode(); //same as x except the sibling is not e but gamma
        xWithSiblingGamma.setChild(x.getChild());
        xWithSiblingGamma.setSibling(gammaNode);
        xWithSiblingGamma.setType(x.getType());
        xWithSiblingGamma.setValue(x.getValue());
        node.setChild(xWithSiblingGamma);
        node.setType(ASTNodeType.EQUAL);
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

  private void populateCommaAndTauNode(ASTNode equalNode, ASTNode commaNode, ASTNode tauNode){
    if(equalNode.getType()!=ASTNodeType.EQUAL)
      throw new StandardizeException("SIMULTDEF: one of the children is not EQUAL"); //safety
    ASTNode x = equalNode.getChild();
    ASTNode e = x.getSibling();
    setChild(commaNode, x);
    setChild(tauNode, e);
  }

  /**
   * Either creates a new child of the parent or attaches the child node passed in
   * as the last sibling of the parent's existing children 
   * @param parentNode
   * @param childNode
   */
  private void setChild(ASTNode parentNode, ASTNode childNode){
    if(parentNode.getChild()==null)
      parentNode.setChild(childNode);
    else{
      ASTNode lastSibling = parentNode.getChild();
      while(lastSibling.getSibling()!=null)
        lastSibling = lastSibling.getSibling();
      lastSibling.setSibling(childNode);
    }
    childNode.setSibling(null);
  }

  private ASTNode constructLambdaChain(ASTNode node){
    if(node.getSibling()==null)
      return node;
    
    ASTNode lambdaNode = new ASTNode();
    lambdaNode.setType(ASTNodeType.LAMBDA);
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

  private Delta createDelta(ASTNode startBodyNode){
    //we'll create this delta's body later
    PendingDeltaBody pendingDelta = new PendingDeltaBody();
    pendingDelta.startNode = startBodyNode;
    pendingDelta.body = new Stack<ASTNode>();
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
  
  private void buildDeltaBody(ASTNode node, Stack<ASTNode> body){
    if(node.getType()==ASTNodeType.LAMBDA){ //create a new delta
      Delta d = createDelta(node.getChild().getSibling()); //the new delta's body starts at the right child of the lambda
      if(node.getChild().getType()==ASTNodeType.COMMA){ //the left child of the lambda is the bound variable
        ASTNode commaNode = node.getChild();
        ASTNode childNode = commaNode.getChild();
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
    else if(node.getType()==ASTNodeType.CONDITIONAL){
      //to enable programming order evaluation, traverse the children in reverse order so the condition leads
      // cond -> then else becomes then else Beta cond
      ASTNode conditionNode = node.getChild();
      ASTNode thenNode = conditionNode.getSibling();
      ASTNode elseNode = thenNode.getSibling();
      
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
    ASTNode childNode = node.getChild();
    while(childNode!=null){
      buildDeltaBody(childNode, body);
      childNode = childNode.getSibling();
    }
  }

  private class PendingDeltaBody{
    Stack<ASTNode> body;
    ASTNode startNode;
  }

  public boolean isStandardized(){
    return standardized;
  }
}

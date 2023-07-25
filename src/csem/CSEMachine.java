package csem;

import java.util.Stack;
import ast.AST;
import ast.AST_Nd;
import ast.AST_Nd_Type;

//group 44
public class CSEMachine{

  private Stack<AST_Nd> stack_Value;
  private Delta delta_Root;

  public CSEMachine(AST ast){
    if(!ast.isStandardized())
      throw new RuntimeException("AST has NOT been standardized!"); 
    delta_Root = ast.createDeltas();
    delta_Root.setLinkedEnv(new Environment()); //PE
    stack_Value = new Stack<AST_Nd>();
  }

  public void evaluate_Program(){
    process_CS(delta_Root, delta_Root.linkedEnv);
  }

  private void process_CS(Delta currentDelta, Environment currentEnv){
    //create a new control stack 
    Stack<AST_Nd> controlStack = new Stack<AST_Nd>();
    controlStack.addAll(currentDelta.body);
    
    while(!controlStack.isEmpty())
      processCurrentNode(currentDelta, currentEnv, controlStack);
  }

  private void processCurrentNode(Delta currentDelta, Environment currentEnv, Stack<AST_Nd> currentControlStack){
    AST_Nd node = currentControlStack.pop();
    if(applyBinaryOperation(node))
      return;
    else if(applyUnaryOperation(node))
      return;
    else{
      switch(node.type){
        case IDENTIFIER:
          handleIdentifiers(node, currentEnv);
          break;
        case NIL:
        case TAU:
          createTuple(node);
          break;
        case BETA:
          handleBeta((Beta)node, currentControlStack);
          break;
        case GAMMA:
          applyGamma(currentDelta, node, currentEnv, currentControlStack);
          break;
        case DELTA:
          ((Delta)node).setLinkedEnv(currentEnv); //RULE 2
          stack_Value.push(node);
          break;
        default:
          
          stack_Value.push(node);
          break;
      }
    }
  }

  // RULE 6
  private boolean applyBinaryOperation(AST_Nd rator){
    switch(rator.type){
      case PLUS:
      case MINUS:
      case MULT:
      case DIV:
      case EXP:
      case LS:
      case LE:
      case GR:
      case GE:
        binaryArithmeticOp(rator.type);
        return true;
      case EQ:
      case NE:
        binaryLogicalEqNeOp(rator.type);
        return true;
      case OR:
      case AND:
        binaryLogicalOrAndOp(rator.type);
        return true;
      case AUG:
        augTuples();
        return true;
      default:
        return false;
    }
  }

  private void binaryArithmeticOp(AST_Nd_Type type){
    AST_Nd rand1 = stack_Value.pop();
    AST_Nd rand2 = stack_Value.pop();
    if(rand1.type!=AST_Nd_Type.INTEGER || rand2.type!=AST_Nd_Type.INTEGER)
      Evl_Err.printError(rand1.sourceLineNumber, "Expected two integers; was given \""+rand1.value+"\", \""+rand2.value+"\"");

    AST_Nd result = new AST_Nd();
    result.setType(AST_Nd_Type.INTEGER);

    switch(type){
      case PLUS:
        result.setValue(Integer.toString(Integer.parseInt(rand1.value)+Integer.parseInt(rand2.value)));
        break;
      case MINUS:
        result.setValue(Integer.toString(Integer.parseInt(rand1.value)-Integer.parseInt(rand2.value)));
        break;
      case MULT:
        result.setValue(Integer.toString(Integer.parseInt(rand1.value)*Integer.parseInt(rand2.value)));
        break;
      case DIV:
        result.setValue(Integer.toString(Integer.parseInt(rand1.value)/Integer.parseInt(rand2.value)));
        break;
      case EXP:
        result.setValue(Integer.toString((int)Math.pow(Integer.parseInt(rand1.value), Integer.parseInt(rand2.value))));
        break;
      case LS:
        if(Integer.parseInt(rand1.value)<Integer.parseInt(rand2.value))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      case LE:
        if(Integer.parseInt(rand1.value)<=Integer.parseInt(rand2.value))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      case GR:
        if(Integer.parseInt(rand1.value)>Integer.parseInt(rand2.value))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      case GE:
        if(Integer.parseInt(rand1.value)>=Integer.parseInt(rand2.value))
          pushTrueNode();
        else
          pushFalseNode();
        return;
      default:
        break;
    }
    stack_Value.push(result);
  }

  private void binaryLogicalEqNeOp(AST_Nd_Type type){
    AST_Nd rand1 = stack_Value.pop();
    AST_Nd rand2 = stack_Value.pop();

    if(rand1.type==AST_Nd_Type.TRUE || rand1.type==AST_Nd_Type.FALSE){
      if(rand2.type!=AST_Nd_Type.TRUE && rand2.type!=AST_Nd_Type.FALSE)
        Evl_Err.printError(rand1.sourceLineNumber, "Cannot compare dissimilar types; was given \""+rand1.value+"\", \""+rand2.value+"\"");
      compareTruthValues(rand1, rand2, type);
      return;
    }

    if(rand1.type!=rand2.type)
      Evl_Err.printError(rand1.sourceLineNumber, "Cannot compare dissimilar types; was given \""+rand1.value+"\", \""+rand2.value+"\"");

    if(rand1.type==AST_Nd_Type.STRING)
      compareStrings(rand1, rand2, type);
    else if(rand1.type==AST_Nd_Type.INTEGER)
      compareIntegers(rand1, rand2, type);
    else
      Evl_Err.printError(rand1.sourceLineNumber, "Don't know how to " + type + " \""+rand1.value+"\", \""+rand2.value+"\"");

  }

  private void compareTruthValues(AST_Nd rand1, AST_Nd rand2, AST_Nd_Type type){
    if(rand1.type==rand2.type)
      if(type==AST_Nd_Type.EQ)
        pushTrueNode();
      else
        pushFalseNode();
    else
      if(type==AST_Nd_Type.EQ)
        pushFalseNode();
      else
        pushTrueNode();
  }

  private void compareStrings(AST_Nd rand1, AST_Nd rand2, AST_Nd_Type type){
    if(rand1.value.equals(rand2.value))
      if(type==AST_Nd_Type.EQ)
        pushTrueNode();
      else
        pushFalseNode();
    else
      if(type==AST_Nd_Type.EQ)
        pushFalseNode();
      else
        pushTrueNode();
  }

  private void compareIntegers(AST_Nd rand1, AST_Nd rand2, AST_Nd_Type type){
    if(Integer.parseInt(rand1.value)==Integer.parseInt(rand2.value))
      if(type==AST_Nd_Type.EQ)
        pushTrueNode();
      else
        pushFalseNode();
    else
      if(type==AST_Nd_Type.EQ)
        pushFalseNode();
      else
        pushTrueNode();
  }

  private void binaryLogicalOrAndOp(AST_Nd_Type type){
    AST_Nd rand1 = stack_Value.pop();
    AST_Nd rand2 = stack_Value.pop();

    if((rand1.type==AST_Nd_Type.TRUE || rand1.type==AST_Nd_Type.FALSE) &&
        (rand2.type==AST_Nd_Type.TRUE || rand2.type==AST_Nd_Type.FALSE)){
      orAndTruthValues(rand1, rand2, type);
      return;
    }

    Evl_Err.printError(rand1.sourceLineNumber, "Don't know how to " + type + " \""+rand1.value+"\", \""+rand2.value+"\"");
  }

  private void orAndTruthValues(AST_Nd rand1, AST_Nd rand2, AST_Nd_Type type){
    if(type==AST_Nd_Type.OR){
      if(rand1.type==AST_Nd_Type.TRUE || rand2.type==AST_Nd_Type.TRUE)
        pushTrueNode();
      else
        pushFalseNode();
    }
    else{
      if(rand1.type==AST_Nd_Type.TRUE && rand2.type==AST_Nd_Type.TRUE)
        pushTrueNode();
      else
        pushFalseNode();
    }
  }

  private void augTuples(){
    AST_Nd rand1 = stack_Value.pop();
    AST_Nd rand2 = stack_Value.pop();

    if(rand1.type!=AST_Nd_Type.TUPLE)
      Evl_Err.printError(rand1.sourceLineNumber, "Cannot augment a non-tuple \""+rand1.value+"\"");

    AST_Nd childNode = rand1.child;
    if(childNode==null)
      rand1.setChild(rand2);
    else{
      while(childNode.sibling!=null)
        childNode = childNode.sibling;
      childNode.setSibling(rand2);
    }
    rand2.setSibling(null);

    stack_Value.push(rand1);
  }

  // RULE 7
  private boolean applyUnaryOperation(AST_Nd rator){
    switch(rator.type){
      case NOT:
        not();
        return true;
      case NEG:
        neg();
        return true;
      default:
        return false;
    }
  }

  private void not(){
    AST_Nd rand = stack_Value.pop();
    if(rand.type!=AST_Nd_Type.TRUE && rand.type!=AST_Nd_Type.FALSE)
      Evl_Err.printError(rand.sourceLineNumber, "Expecting a truthvalue; was given \""+rand.value+"\"");

    if(rand.type==AST_Nd_Type.TRUE)
      pushFalseNode();
    else
      pushTrueNode();
  }

  private void neg(){
    AST_Nd rand = stack_Value.pop();
    if(rand.type!=AST_Nd_Type.INTEGER)
      Evl_Err.printError(rand.sourceLineNumber, "Expecting a truthvalue; was given \""+rand.value+"\"");

    AST_Nd result = new AST_Nd();
    result.setType(AST_Nd_Type.INTEGER);
    result.setValue(Integer.toString(-1*Integer.parseInt(rand.value)));
    stack_Value.push(result);
  }

  //RULE 3
  private void applyGamma(Delta currentDelta, AST_Nd node, Environment currentEnv, Stack<AST_Nd> currentControlStack){
    AST_Nd rator = stack_Value.pop();
    AST_Nd rand = stack_Value.pop();

    if(rator.type==AST_Nd_Type.DELTA){
      Delta nextDelta = (Delta) rator;
      
    
      Environment newEnv = new Environment();
      newEnv.setParent(nextDelta.linkedEnv);
      
      //RULE 4
      if(nextDelta.boundVars.size()==1){
        newEnv.doMapping(nextDelta.boundVars.get(0), rand);
      }
      //RULE 11
      else{
        if(rand.type!=AST_Nd_Type.TUPLE)
          Evl_Err.printError(rand.sourceLineNumber, "Expected a tuple; was given \""+rand.value+"\"");
        
        for(int i = 0; i < nextDelta.boundVars.size(); i++){
          newEnv.doMapping(nextDelta.boundVars.get(i), getNthTupleChild((Tau)rand, i+1)); //+ 1 coz tuple indexing starts at 1
        }
      }
      
      process_CS(nextDelta, newEnv);
      return;
    }
    else if(rator.type==AST_Nd_Type.YSTAR){
      //RULE 12
      if(rand.type!=AST_Nd_Type.DELTA)
        Evl_Err.printError(rand.sourceLineNumber, "Expected a Delta; was given \""+rand.value+"\"");
      
      Eta etaNode = new Eta();
      etaNode.setDelta((Delta)rand);
      stack_Value.push(etaNode);
      return;
    }
    else if(rator.type==AST_Nd_Type.ETA){
      //RULE 13
      
      stack_Value.push(rand);
      stack_Value.push(rator);
      stack_Value.push(((Eta)rator).getDelta());
      //push back two gammas 
      currentControlStack.push(node);
      currentControlStack.push(node);
      return;
    }
    else if(rator.type==AST_Nd_Type.TUPLE){
      tupleSelection((Tau)rator, rand);
      return;
    }
    else if(evaluateReservedIdentifiers(rator, rand, currentControlStack))
      return;
    else
      Evl_Err.printError(rator.sourceLineNumber, "Don't know how to evaluate \""+rator.value+"\"");
  }

  private boolean evaluateReservedIdentifiers(AST_Nd rator, AST_Nd rand, Stack<AST_Nd> currentControlStack){
    switch(rator.value){
      case "Isinteger":
        checkTypeAndPushTrueOrFalse(rand, AST_Nd_Type.INTEGER);
        return true;
      case "Isstring":
        checkTypeAndPushTrueOrFalse(rand, AST_Nd_Type.STRING);
        return true;
      case "Isdummy":
        checkTypeAndPushTrueOrFalse(rand, AST_Nd_Type.DUMMY);
        return true;
      case "Isfunction":
        checkTypeAndPushTrueOrFalse(rand, AST_Nd_Type.DELTA);
        return true;
      case "Istuple":
        checkTypeAndPushTrueOrFalse(rand, AST_Nd_Type.TUPLE);
        return true;
      case "Istruthvalue":
        if(rand.type==AST_Nd_Type.TRUE||rand.type==AST_Nd_Type.FALSE)
          pushTrueNode();
        else
          pushFalseNode();
        return true;
      case "Stem":
        stem(rand);
        return true;
      case "Stern":
        stern(rand);
        return true;
      case "Conc":
      case "conc": //typos
        conc(rand, currentControlStack);
        return true;
      case "Print":
      case "print": //typos
        printNodeValue(rand);
        pushDummyNode();
        return true;
      case "ItoS":
        itos(rand);
        return true;
      case "Order":
        order(rand);
        return true;
      case "Null":
        isNullTuple(rand);
        return true;
      default:
        return false;
    }
  }

  private void checkTypeAndPushTrueOrFalse(AST_Nd rand, AST_Nd_Type type){
    if(rand.type==type)
      pushTrueNode();
    else
      pushFalseNode();
  }

  private void pushTrueNode(){
    AST_Nd trueNode = new AST_Nd();
    trueNode.setType(AST_Nd_Type.TRUE);
    trueNode.setValue("true");
    stack_Value.push(trueNode);
  }
  
  private void pushFalseNode(){
    AST_Nd falseNode = new AST_Nd();
    falseNode.setType(AST_Nd_Type.FALSE);
    falseNode.setValue("false");
    stack_Value.push(falseNode);
  }

  private void pushDummyNode(){
    AST_Nd falseNode = new AST_Nd();
    falseNode.setType(AST_Nd_Type.DUMMY);
    stack_Value.push(falseNode);
  }

  private void stem(AST_Nd rand){
    if(rand.type!=AST_Nd_Type.STRING)
      Evl_Err.printError(rand.sourceLineNumber, "Expected a string; was given \""+rand.value+"\"");
    
    if(rand.value.isEmpty())
      rand.setValue("");
    else
      rand.setValue(rand.value.substring(0,1));
    
    stack_Value.push(rand);
  }

  private void stern(AST_Nd rand){
    if(rand.type!=AST_Nd_Type.STRING)
      Evl_Err.printError(rand.sourceLineNumber, "Expected a string; was given \""+rand.value+"\"");
    
    if(rand.value.isEmpty() || rand.value.length()==1)
      rand.setValue("");
    else
      rand.setValue(rand.value.substring(1));
    
    stack_Value.push(rand);
  }

  private void conc(AST_Nd rand1, Stack<AST_Nd> currentControlStack){
    currentControlStack.pop();
    AST_Nd rand2 = stack_Value.pop();
    if(rand1.type!=AST_Nd_Type.STRING || rand2.type!=AST_Nd_Type.STRING)
      Evl_Err.printError(rand1.sourceLineNumber, "Expected two strings; was given \""+rand1.value+"\", \""+rand2.value+"\"");

    AST_Nd result = new AST_Nd();
    result.setType(AST_Nd_Type.STRING);
    result.setValue(rand1.value+rand2.value);
    
    stack_Value.push(result);
  }

  private void itos(AST_Nd rand){
    if(rand.type!=AST_Nd_Type.INTEGER)
      Evl_Err.printError(rand.sourceLineNumber, "Expected an integer; was given \""+rand.value+"\"");
    
    rand.setType(AST_Nd_Type.STRING); //all values are stored internally as strings
    stack_Value.push(rand);
  }

  private void order(AST_Nd rand){
    if(rand.type!=AST_Nd_Type.TUPLE)
      Evl_Err.printError(rand.sourceLineNumber, "Expected a tuple; was given \""+rand.value+"\"");

    AST_Nd result = new AST_Nd();
    result.setType(AST_Nd_Type.INTEGER);
    result.setValue(Integer.toString(getNumChildren(rand)));
    
    stack_Value.push(result);
  }

  private void isNullTuple(AST_Nd rand){
    if(rand.type!=AST_Nd_Type.TUPLE)
      Evl_Err.printError(rand.sourceLineNumber, "Expected a tuple; was given \""+rand.value+"\"");

    if(getNumChildren(rand)==0)
      pushTrueNode();
    else
      pushFalseNode();
  }

  // RULE 10
  private void tupleSelection(Tau rator, AST_Nd rand){
    if(rand.type!=AST_Nd_Type.INTEGER)
      Evl_Err.printError(rand.sourceLineNumber, "Non-integer tuple selection with \""+rand.value+"\"");

    AST_Nd result = getNthTupleChild(rator, Integer.parseInt(rand.value));
    if(result==null)
      Evl_Err.printError(rand.sourceLineNumber, "Tuple selection index "+rand.value+" out of bounds");

    stack_Value.push(result);
  }

  /**
   * Get the nth element of the tuple
   * 
   * 
   * 
   */
  private AST_Nd getNthTupleChild(Tau tupleNode, int n){
    AST_Nd childNode = tupleNode.child;
    for(int i=1;i<n;++i){ //tuple selection index starts at 1
      if(childNode==null)
        break;
      childNode = childNode.sibling;
    }
    return childNode;
  }

  private void handleIdentifiers(AST_Nd node, Environment currentEnv){
    if(currentEnv.lookup(node.value)!=null) // RULE 1
      stack_Value.push(currentEnv.lookup(node.value));
    else if(isReservedIdentifier(node.value))
      stack_Value.push(node);
    else
      Evl_Err.printError(node.sourceLineNumber, "Undeclared identifier \""+node.value+"\"");
  }

  //RULE 9
  private void createTuple(AST_Nd node){
    int numChildren = getNumChildren(node);
    Tau tupleNode = new Tau();
    if(numChildren==0){
      stack_Value.push(tupleNode);
      return;
    }

    AST_Nd childNode = null, tempNode = null;
    for(int i=0;i<numChildren;++i){
      if(childNode==null)
        childNode = stack_Value.pop();
      else if(tempNode==null){
        tempNode = stack_Value.pop();
        childNode.setSibling(tempNode);
      }
      else{
        tempNode.setSibling(stack_Value.pop());
        tempNode = tempNode.sibling;
      }
    }
    tempNode.setSibling(null);
    tupleNode.setChild(childNode);
    stack_Value.push(tupleNode);
  }

  // RULE 8
  private void handleBeta(Beta node, Stack<AST_Nd> currentControlStack){
    AST_Nd conditionResultNode = stack_Value.pop();

    if(conditionResultNode.type!=AST_Nd_Type.TRUE && conditionResultNode.type!=AST_Nd_Type.FALSE)
      Evl_Err.printError(conditionResultNode.sourceLineNumber, "Expecting a truthvalue; found \""+conditionResultNode.value+"\"");

    if(conditionResultNode.type==AST_Nd_Type.TRUE)
      currentControlStack.addAll(node.then_Part);
    else
      currentControlStack.addAll(node.else_Part);
  }

  private int getNumChildren(AST_Nd node){
    int numChildren = 0;
    AST_Nd childNode = node.child;
    while(childNode!=null){
      numChildren++;
      childNode = childNode.sibling;
    }
    return numChildren;
  }
  
  private void printNodeValue(AST_Nd rand){
    String evaluationResult = rand.value;
    evaluationResult = evaluationResult.replace("\\t", "\t");
    evaluationResult = evaluationResult.replace("\\n", "\n");
    System.out.print(evaluationResult);
  }

  
  private boolean isReservedIdentifier(String value){
    switch(value){
      case "Isinteger":
      case "Isstring":
      case "Istuple":
      case "Isdummy":
      case "Istruthvalue":
      case "Isfunction":
      case "ItoS":
      case "Order":
      case "Conc":
      case "conc": //typos
      case "Stern":
      case "Stem":
      case "Null":
      case "Print":
      case "print": //typos
      case "neg":
        return true;
    }
    return false;
  }

}

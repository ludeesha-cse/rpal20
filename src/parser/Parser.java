package parser;

import java.util.Stack;

import ast.AST;
import ast.AST_Nd;
import ast.AST_Nd_Type;
import scanner.Scanner;
import scanner.Token;
import scanner.TKN_Type;

/**
 * Recursive descent parser that complies with RPAL's phrase structure grammar.
 
 */
public class Parser{
  private Scanner s;
  private Token currentToken;
  Stack<AST_Nd> stack;

  public Parser(Scanner s){
    this.s = s;
    stack = new Stack<AST_Nd>();
  }
  
  public AST buildAST(){
    startParse();
    return new AST(stack.pop());
  }

  public void startParse(){
    readNT();
    procE(); 
    if(currentToken!=null)
      throw new Parser_Exception("Expected EOF.");
  }

  private void readNT(){
    do{
      currentToken = s.readNextToken(); 
    }while(isCurrentTokenType(TKN_Type.DELETE));
    if(null != currentToken){
      if(currentToken.type==TKN_Type.IDENTIFIER){
        createTerminalASTNode(AST_Nd_Type.IDENTIFIER, currentToken.value);
      }
      else if(currentToken.type==TKN_Type.INTEGER){
        createTerminalASTNode(AST_Nd_Type.INTEGER, currentToken.value);
      } 
      else if(currentToken.type==TKN_Type.STRING){
        createTerminalASTNode(AST_Nd_Type.STRING, currentToken.value);
      }
    }
  }
  
  private boolean isCurrentToken(TKN_Type type, String value){
    if(currentToken==null)
      return false;
    if(currentToken.type!=type || !currentToken.value.equals(value))
      return false;
    return true;
  }
  
  private boolean isCurrentTokenType(TKN_Type type){
    if(currentToken==null)
      return false;
    if(currentToken.type==type)
      return true;
    return false;
  }
  
  /**
   * Builds an N-ary AST node
   */
  private void buildNAryASTNode(AST_Nd_Type type, int ariness){
    AST_Nd node = new AST_Nd();
    node.setType(type);
    while(ariness>0){
      AST_Nd child = stack.pop();
      if(node.child!=null)
        child.setSibling(node.child);
      node.setChild(child);
      node.setSourceLineNumber(child.sourceLineNumber);
      ariness--;
    }
    stack.push(node);
  }

  private void createTerminalASTNode(AST_Nd_Type type, String value){
    AST_Nd node = new AST_Nd();
    node.setType(type);
    node.setValue(value);
    node.setSourceLineNumber(currentToken.sourceLineNumber);
    stack.push(node);
  }
  
  /******************************
   * Expressions
   *******************************/
  
  /**
   * <pre>
   * E-> 'let' D 'in' E => 'let'
   *  -> 'fn' Vb+ '.' E => 'lambda'
   *  -> Ew;
   * </pre>
   */
  private void procE(){
    if(isCurrentToken(TKN_Type.RESERVED, "let")){ //E -> 'let' D 'in' E => 'let'
      readNT();
      procD();
      if(!isCurrentToken(TKN_Type.RESERVED, "in"))
        throw new Parser_Exception("E:  'in' expected");
      readNT();
      procE(); //extra readNT in procE()
      buildNAryASTNode(AST_Nd_Type.LET, 2);
    }
    else if(isCurrentToken(TKN_Type.RESERVED, "fn")){ //E -> 'fn' Vb+ '.' E => 'lambda'
      int treesToPop = 0;
      
      readNT();
      while(isCurrentTokenType(TKN_Type.IDENTIFIER) || isCurrentTokenType(TKN_Type.L_PAREN)){
        procVB(); //extra readNT in procVB()
        treesToPop++;
      }
      
      if(treesToPop==0)
        throw new Parser_Exception("E: at least one 'Vb' expected");
      
      if(!isCurrentToken(TKN_Type.OPERATOR, "."))
        throw new Parser_Exception("E: '.' expected");
      
      readNT();
      procE(); //extra readNT in procE()
      
      buildNAryASTNode(AST_Nd_Type.LAMBDA, treesToPop+1); //+1 for the last E 
    }
    else //E -> Ew
      procEW();
  }

  /**
   * <pre>
   * Ew -> T 'where' Dr => 'where'
   *    -> T;
   * </pre>
   */
  private void procEW(){
    procT(); //Ew -> T
    //extra readToken done in procT()
    if(isCurrentToken(TKN_Type.RESERVED, "where")){ //Ew -> T 'where' Dr => 'where'
      readNT();
      procDR(); //extra readToken() in procDR()
      buildNAryASTNode(AST_Nd_Type.WHERE, 2);
    }
  }
  
  /******************************
   * Tuple Expressions
   *******************************/
  
  /**
   * <pre>
   * T -> Ta ( ',' Ta )+ => 'tau'
   *   -> Ta;
   * </pre>
   */
  private void procT(){
    procTA(); //T -> Ta
    //extra readToken() in procTA()
    int treesToPop = 0;
    while(isCurrentToken(TKN_Type.OPERATOR, ",")){ //T -> Ta (',' Ta )+ => 'tau'
      readNT();
      procTA(); //extra readToken() done in procTA()
      treesToPop++;
    }
    if(treesToPop > 0) buildNAryASTNode(AST_Nd_Type.TAU, treesToPop+1);
  }

  /**
   * <pre>
   * Ta -> Ta 'aug' Tc => 'aug'
   *    -> Tc;
   * </pre>
   */
  private void procTA(){
    procTC(); //Ta -> Tc
    //extra readNT done in procTC()
    while(isCurrentToken(TKN_Type.RESERVED, "aug")){ //Ta -> Ta 'aug' Tc => 'aug'
      readNT();
      procTC(); //extra readNT done in procTC()
      buildNAryASTNode(AST_Nd_Type.AUG, 2);
    }
  }

  /**
   * <pre>
   * Tc -> B '->' Tc '|' Tc => '->'
   *    -> B;
   * </pre>
   */
  private void procTC(){
    procB(); //Tc -> B
    //extra readNT in procBT()
    if(isCurrentToken(TKN_Type.OPERATOR, "->")){ //Tc -> B '->' Tc '|' Tc => '->'
      readNT();
      procTC(); //extra readNT done in procTC
      if(!isCurrentToken(TKN_Type.OPERATOR, "|"))
        throw new Parser_Exception("TC: '|' expected");
      readNT();
      procTC();  //extra readNT done in procTC
      buildNAryASTNode(AST_Nd_Type.CONDITIONAL, 3);
    }
  }
  
  /******************************
   * Boolean Expressions
   *******************************/
  
  /**
   * <pre>
   * B -> B 'or' Bt => 'or'
   *   -> Bt;
   * </pre>
   */
  private void procB(){
    procBT(); //B -> Bt
    //extra readNT in procBT()
    while(isCurrentToken(TKN_Type.RESERVED, "or")){ //B -> B 'or' Bt => 'or'
      readNT();
      procBT();
      buildNAryASTNode(AST_Nd_Type.OR, 2);
    }
  }
  
  /**
   * <pre>
   * Bt -> Bs '&' Bt => '&'
   *    -> Bs;
   * </pre>
   */
  private void procBT(){
    procBS(); //Bt -> Bs;
    //extra readNT in procBS()
    while(isCurrentToken(TKN_Type.OPERATOR, "&")){ //Bt -> Bt '&' Bs => '&'
      readNT();
      procBS(); //extra readNT in procBS()
      buildNAryASTNode(AST_Nd_Type.AND, 2);
    }
  }
  
  /**
   * <pre>
   * Bs -> 'not Bp => 'not'
   *    -> Bp;
   * </pre>
   */
  private void procBS(){
    if(isCurrentToken(TKN_Type.RESERVED, "not")){ //Bs -> 'not' Bp => 'not'
      readNT();
      procBP(); //extra readNT in procBP()
      buildNAryASTNode(AST_Nd_Type.NOT, 1);
    }
    else
      procBP(); //Bs -> Bp
      //extra readNT in procBP()
  }
  
  /**
   * <pre>
   * Bp -> A ('gr' | '>' ) A => 'gr'
   *    -> A ('ge' | '>=' ) A => 'ge'
   *    -> A ('ls' | '<' ) A => 'ge'
   *    -> A ('le' | '<=' ) A => 'ge'
   *    -> A 'eq' A => 'eq'
   *    -> A 'ne' A => 'ne'
   *    -> A;
   * </pre>
   */
  private void procBP(){
    procA(); //Bp -> A
    if(isCurrentToken(TKN_Type.RESERVED,"gr")||isCurrentToken(TKN_Type.OPERATOR,">")){ //Bp -> A('gr' | '>' ) A => 'gr'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(AST_Nd_Type.GR, 2);
    }
    else if(isCurrentToken(TKN_Type.RESERVED,"ge")||isCurrentToken(TKN_Type.OPERATOR,">=")){ //Bp -> A ('ge' | '>=') A => 'ge'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(AST_Nd_Type.GE, 2);
    }
    else if(isCurrentToken(TKN_Type.RESERVED,"ls")||isCurrentToken(TKN_Type.OPERATOR,"<")){ //Bp -> A ('ls' | '<' ) A => 'ls'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(AST_Nd_Type.LS, 2);
    }
    else if(isCurrentToken(TKN_Type.RESERVED,"le")||isCurrentToken(TKN_Type.OPERATOR,"<=")){ //Bp -> A ('le' | '<=') A => 'le'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(AST_Nd_Type.LE, 2);
    }
    else if(isCurrentToken(TKN_Type.RESERVED,"eq")){ //Bp -> A 'eq' A => 'eq'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(AST_Nd_Type.EQ, 2);
    }
    else if(isCurrentToken(TKN_Type.RESERVED,"ne")){ //Bp -> A 'ne' A => 'ne'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(AST_Nd_Type.NE, 2);
    }
  }
  
  
  /******************************
   * Arithmetic Expressions
   *******************************/
  
  /**
   * <pre>
   * A -> A '+' At => '+'
   *   -> A '-' At => '-'
   *   ->   '+' At
   *   ->   '-' At => 'neg'
   *   -> At;
   * </pre>
   */
  private void procA(){
    if(isCurrentToken(TKN_Type.OPERATOR, "+")){ //A -> '+' At
      readNT();
      procAT(); //extra readNT in procAT()
    }
    else if(isCurrentToken(TKN_Type.OPERATOR, "-")){ //A -> '-' At => 'neg'
      readNT();
      procAT(); //extra readNT in procAT()
      buildNAryASTNode(AST_Nd_Type.NEG, 1);
    }
    else
      procAT(); //extra readNT in procAT()
    
    boolean plus = true;
    while(isCurrentToken(TKN_Type.OPERATOR, "+")||isCurrentToken(TKN_Type.OPERATOR, "-")){
      if(currentToken.value.equals("+"))
        plus = true;
      else if(currentToken.value.equals("-"))
        plus = false;
      readNT();
      procAT(); //extra readNT in procAT()
      if(plus) //A -> A '+' At => '+'
        buildNAryASTNode(AST_Nd_Type.PLUS, 2);
      else //A -> A '-' At => '-'
        buildNAryASTNode(AST_Nd_Type.MINUS, 2);
    }
  }
  
  /**
   * <pre>
   * At -> At '*' Af => '*'
   *    -> At '/' Af => '/'
   *    -> Af;
   * </pre>
   */
  private void procAT(){
    procAF(); //At -> Af;
    //extra readNT in procAF()
    boolean mult = true;
    while(isCurrentToken(TKN_Type.OPERATOR, "*")||isCurrentToken(TKN_Type.OPERATOR, "/")){
      if(currentToken.value.equals("*"))
        mult = true;
      else if(currentToken.value.equals("/"))
        mult = false;
      readNT();
      procAF(); //extra readNT in procAF()
      if(mult) //At -> At '*' Af => '*'
        buildNAryASTNode(AST_Nd_Type.MULT, 2);
      else //At -> At '/' Af => '/'
        buildNAryASTNode(AST_Nd_Type.DIV, 2);
    }
  }
  
  /**
   * <pre>
   * Af -> Ap '**' Af => '**'
   *    -> Ap;
   * </pre>
   */
  private void procAF(){
    procAP(); // Af -> Ap;
    //extra readNT in procAP()
    if(isCurrentToken(TKN_Type.OPERATOR, "**")){ //Af -> Ap '**' Af => '**'
      readNT();
      procAF();
      buildNAryASTNode(AST_Nd_Type.EXP, 2);
    }
  }
  
  
  /**
   * <pre>
   * Ap -> Ap '@' '&lt;IDENTIFIER&gt;' R => '@'
   *    -> R; 
   * </pre>
   */
  private void procAP(){
    procR(); //Ap -> R;
    //extra readNT in procR()
    while(isCurrentToken(TKN_Type.OPERATOR, "@")){ //Ap -> Ap '@' '<IDENTIFIER>' R => '@'
      readNT();
      if(!isCurrentTokenType(TKN_Type.IDENTIFIER))
        throw new Parser_Exception("AP: expected Identifier");
      readNT();
      procR(); //extra readNT in procR()
      buildNAryASTNode(AST_Nd_Type.AT, 3);
    }
  }
  
  /******************************
   * Rators and Rands
   *******************************/
  
  /**
   * <pre>
   * R -> R Rn => 'gamma'
   *   -> Rn;
   * </pre>
   */
  private void procR(){
    procRN(); //R -> Rn; NO extra readNT in procRN(). See while loop below for reason.
    readNT();
    while(isCurrentTokenType(TKN_Type.INTEGER)||
        isCurrentTokenType(TKN_Type.STRING)|| 
        isCurrentTokenType(TKN_Type.IDENTIFIER)||
        isCurrentToken(TKN_Type.RESERVED, "true")||
        isCurrentToken(TKN_Type.RESERVED, "false")||
        isCurrentToken(TKN_Type.RESERVED, "nil")||
        isCurrentToken(TKN_Type.RESERVED, "dummy")||
        isCurrentTokenType(TKN_Type.L_PAREN)){ //R -> R Rn => 'gamma'
      procRN(); //NO extra readNT in procRN(). This is important because if we do an extra readNT in procRN and currentToken happens to
                //be an INTEGER, IDENTIFIER, or STRING, it will get pushed on the stack. Then, the GAMMA node that we build will have the
                //wrong kids. There are workarounds, e.g. keeping the extra readNT in procRN() and checking here if the last token read
                //(which was read in procRN()) is an INTEGER, IDENTIFIER, or STRING and, if so, to pop it, call buildNAryASTNode, and then
                //push it again. I chose this option because it seems cleaner.
      buildNAryASTNode(AST_Nd_Type.GAMMA, 2);
      readNT();
    }
  }

  /**
   * NOTE: NO extra readNT in procRN. See comments in {@link #procR()} for explanation.
   * <pre>
   * Rn -> '&lt;IDENTIFIER&gt;'
   *    -> '&lt;INTEGER&gt;'
   *    -> '&lt;STRING&gt;'
   *    -> 'true' => 'true'
   *    -> 'false' => 'false'
   *    -> 'nil' => 'nil'
   *    -> '(' E ')'
   *    -> 'dummy' => 'dummy'
   * </pre>
   */
  private void procRN(){
    if(isCurrentTokenType(TKN_Type.IDENTIFIER)|| //R -> '<IDENTIFIER>'
       isCurrentTokenType(TKN_Type.INTEGER)|| //R -> '<INTEGER>' 
       isCurrentTokenType(TKN_Type.STRING)){ //R-> '<STRING>'
    }
    else if(isCurrentToken(TKN_Type.RESERVED, "true")){ //R -> 'true' => 'true'
      createTerminalASTNode(AST_Nd_Type.TRUE, "true");
    }
    else if(isCurrentToken(TKN_Type.RESERVED, "false")){ //R -> 'false' => 'false'
      createTerminalASTNode(AST_Nd_Type.FALSE, "false");
    } 
    else if(isCurrentToken(TKN_Type.RESERVED, "nil")){ //R -> 'nil' => 'nil'
      createTerminalASTNode(AST_Nd_Type.NIL, "nil");
    }
    else if(isCurrentTokenType(TKN_Type.L_PAREN)){
      readNT();
      procE(); //extra readNT in procE()
      if(!isCurrentTokenType(TKN_Type.R_PAREN))
        throw new Parser_Exception("RN: ')' expected");
    }
    else if(isCurrentToken(TKN_Type.RESERVED, "dummy")){ //R -> 'dummy' => 'dummy'
      createTerminalASTNode(AST_Nd_Type.DUMMY, "dummy");
    }
  }

  /******************************
   * Definitions
   *******************************/
  
  /**
   * <pre>
   * D -> Da 'within' D => 'within'
   *   -> Da;
   * </pre>
   */
  private void procD(){
    procDA(); //D -> Da
    //extra readToken() in procDA()
    if(isCurrentToken(TKN_Type.RESERVED, "within")){ //D -> Da 'within' D => 'within'
      readNT();
      procD();
      buildNAryASTNode(AST_Nd_Type.WITHIN, 2);
    }
  }
  
  /**
   * <pre>
   * Da -> Dr ('and' Dr)+ => 'and'
   *    -> Dr;
   * </pre>
   */
  private void procDA(){
    procDR(); //Da -> Dr
    //extra readToken() in procDR()
    int treesToPop = 0;
    while(isCurrentToken(TKN_Type.RESERVED, "and")){ //Da -> Dr ( 'and' Dr )+ => 'and'
      readNT();
      procDR(); //extra readToken() in procDR()
      treesToPop++;
    }
    if(treesToPop > 0) buildNAryASTNode(AST_Nd_Type.SIMULTDEF, treesToPop+1);
  }
  
  /**
   * Dr -> 'rec' Db => 'rec'
   *    -> Db;
   */
  private void procDR(){
    if(isCurrentToken(TKN_Type.RESERVED, "rec")){ //Dr -> 'rec' Db => 'rec'
      readNT();
      procDB(); //extra readToken() in procDB()
      buildNAryASTNode(AST_Nd_Type.REC, 1);
    }
    else{ //Dr -> Db
      procDB(); //extra readToken() in procDB()
    }
  }
  
  /**
   * <pre>
   * Db -> Vl '=' E => '='
   *    -> '&lt;IDENTIFIER&gt;' Vb+ '=' E => 'fcn_form'
   *    -> '(' D ')';
   * </pre>
   */
  private void procDB(){
    if(isCurrentTokenType(TKN_Type.L_PAREN)){ //Db -> '(' D ')'
      procD();
      readNT();
      if(!isCurrentTokenType(TKN_Type.R_PAREN))
        throw new Parser_Exception("DB: ')' expected");
      readNT();
    }
    else if(isCurrentTokenType(TKN_Type.IDENTIFIER)){
      readNT();
      if(isCurrentToken(TKN_Type.OPERATOR, ",")){ //Db -> Vl '=' E => '='
        readNT();
        procVL(); //extra readNT in procVB()
        //VL makes its COMMA nodes for all the tokens EXCEPT the ones
        //we just read above (i.e., the first identifier and the comma after it)
        //Hence, we must pop the top of the tree VL just made and put it under a
        //comma node with the identifier it missed.
        if(!isCurrentToken(TKN_Type.OPERATOR, "="))
          throw new Parser_Exception("DB: = expected.");
        buildNAryASTNode(AST_Nd_Type.COMMA, 2);
        readNT();
        procE(); //extra readNT in procE()
        buildNAryASTNode(AST_Nd_Type.EQUAL, 2);
      }
      else{ //Db -> '<IDENTIFIER>' Vb+ '=' E => 'fcn_form'
        if(isCurrentToken(TKN_Type.OPERATOR, "=")){ //Db -> Vl '=' E => '='; if Vl had only one IDENTIFIER (no commas)
          readNT();
          procE(); //extra readNT in procE()
          buildNAryASTNode(AST_Nd_Type.EQUAL, 2);
        }
        else{ //Db -> '<IDENTIFIER>' Vb+ '=' E => 'fcn_form'
          int treesToPop = 0;

          while(isCurrentTokenType(TKN_Type.IDENTIFIER) || isCurrentTokenType(TKN_Type.L_PAREN)){
            procVB(); //extra readNT in procVB()
            treesToPop++;
          }

          if(treesToPop==0)
            throw new Parser_Exception("E: at least one 'Vb' expected");

          if(!isCurrentToken(TKN_Type.OPERATOR, "="))
            throw new Parser_Exception("DB: = expected.");

          readNT();
          procE(); //extra readNT in procE()

          buildNAryASTNode(AST_Nd_Type.FCNFORM, treesToPop+2); //+1 for the last E and +1 for the first identifier
        }
      }
    }
  }
  
  /******************************
   * Variables
   *******************************/
  
  /**
   * <pre>
   * Vb -> '&lt;IDENTIFIER&gt;'
   *    -> '(' Vl ')'
   *    -> '(' ')' => '()'
   * </pre>
   */
  private void procVB(){
    if(isCurrentTokenType(TKN_Type.IDENTIFIER)){ //Vb -> '<IDENTIFIER>'
      readNT();
    }
    else if(isCurrentTokenType(TKN_Type.L_PAREN)){
      readNT();
      if(isCurrentTokenType(TKN_Type.R_PAREN)){ //Vb -> '(' ')' => '()'
        createTerminalASTNode(AST_Nd_Type.PAREN, "");
        readNT();
      }
      else{ //Vb -> '(' Vl ')'
        procVL(); //extra readNT in procVB()
        if(!isCurrentTokenType(TKN_Type.R_PAREN))
          throw new Parser_Exception("VB: ')' expected");
        readNT();
      }
    }
  }

  /**
   * <pre>
   * Vl -> '&lt;IDENTIFIER&gt;' list ',' => ','?;
   * </pre>
   */
  private void procVL(){
    if(!isCurrentTokenType(TKN_Type.IDENTIFIER))
      throw new Parser_Exception("VL: Identifier expected");
    else{
      readNT();
      int treesToPop = 0;
      while(isCurrentToken(TKN_Type.OPERATOR, ",")){ //Vl -> '<IDENTIFIER>' list ',' => ','?;
        readNT();
        if(!isCurrentTokenType(TKN_Type.IDENTIFIER))
          throw new Parser_Exception("VL: Identifier expected");
        readNT();
        treesToPop++;
      }
      if(treesToPop > 0) buildNAryASTNode(AST_Nd_Type.COMMA, treesToPop+1); //increment
      
    }
  }

}


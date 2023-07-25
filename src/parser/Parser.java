package parser;

import java.util.Stack;

import ast.AST;
import ast.ASTNode;
import ast.ASTNodeType;
import scanner.Scanner;
import scanner.Token;
import scanner.TokenType;

/**
 * Recursive descent parser that complies with RPAL's phrase structure grammar.
 * <p>This class does all the heavy lifting:
 * <ul>
 * <li>It gets input from the scanner for every clause in the phase structure grammar.
 * <li>It builds the abstract syntax tree.
 * </ul>
 * @author Group 9
 */
public class Parser{
  private Scanner s;
  private Token currentToken;
  Stack<ASTNode> stack;

  public Parser(Scanner s){
    this.s = s;
    stack = new Stack<ASTNode>();
  }
  
  public AST buildAST(){
    startParse();
    return new AST(stack.pop());
  }

  public void startParse(){
    readNT();
    procE(); //extra readNT in procE()
    if(currentToken!=null)
      throw new ParseException("Expected EOF.");
  }

  private void readNT(){
    do{
      currentToken = s.readNextToken(); //load next token
    }while(isCurrentTokenType(TokenType.DELETE));
    if(null != currentToken){
      if(currentToken.getType()==TokenType.IDENTIFIER){
        createTerminalASTNode(ASTNodeType.IDENTIFIER, currentToken.getValue());
      }
      else if(currentToken.getType()==TokenType.INTEGER){
        createTerminalASTNode(ASTNodeType.INTEGER, currentToken.getValue());
      } 
      else if(currentToken.getType()==TokenType.STRING){
        createTerminalASTNode(ASTNodeType.STRING, currentToken.getValue());
      }
    }
  }
  
  private boolean isCurrentToken(TokenType type, String value){
    if(currentToken==null)
      return false;
    if(currentToken.getType()!=type || !currentToken.getValue().equals(value))
      return false;
    return true;
  }
  
  private boolean isCurrentTokenType(TokenType type){
    if(currentToken==null)
      return false;
    if(currentToken.getType()==type)
      return true;
    return false;
  }
  
  /**
   * Builds an N-ary AST node. <p>For example, if the stack at a given point in time
   * looks like so:
   * <pre>
   * a <- top of stack
   * b
   * c
   * d
   * ...
   * </pre>
   * Then, after the call buildNAryASTNode(Z, 3), the stack will look like so:
   * <pre>
   * X <- top of stack
   * d
   * ...
   * </pre>
   * where X has three children a, b, and c, and is of type Z. Or, in the first-child, next-sibling representation:      
   * <pre>
   * X
   * |
   * a -> b -> c
   * </pre>
   * @param type type of node to build
   * @param ariness number of children to create for the new node
   */
  private void buildNAryASTNode(ASTNodeType type, int ariness){
    ASTNode node = new ASTNode();
    node.setType(type);
    while(ariness>0){
      ASTNode child = stack.pop();
      if(node.getChild()!=null)
        child.setSibling(node.getChild());
      node.setChild(child);
      node.setSourceLineNumber(child.getSourceLineNumber());
      ariness--;
    }
    stack.push(node);
  }

  private void createTerminalASTNode(ASTNodeType type, String value){
    ASTNode node = new ASTNode();
    node.setType(type);
    node.setValue(value);
    node.setSourceLineNumber(currentToken.getSourceLineNumber());
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
    if(isCurrentToken(TokenType.RESERVED, "let")){ //E -> 'let' D 'in' E => 'let'
      readNT();
      procD();
      if(!isCurrentToken(TokenType.RESERVED, "in"))
        throw new ParseException("E:  'in' expected");
      readNT();
      procE(); //extra readNT in procE()
      buildNAryASTNode(ASTNodeType.LET, 2);
    }
    else if(isCurrentToken(TokenType.RESERVED, "fn")){ //E -> 'fn' Vb+ '.' E => 'lambda'
      int treesToPop = 0;
      
      readNT();
      while(isCurrentTokenType(TokenType.IDENTIFIER) || isCurrentTokenType(TokenType.L_PAREN)){
        procVB(); //extra readNT in procVB()
        treesToPop++;
      }
      
      if(treesToPop==0)
        throw new ParseException("E: at least one 'Vb' expected");
      
      if(!isCurrentToken(TokenType.OPERATOR, "."))
        throw new ParseException("E: '.' expected");
      
      readNT();
      procE(); //extra readNT in procE()
      
      buildNAryASTNode(ASTNodeType.LAMBDA, treesToPop+1); //+1 for the last E 
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
    if(isCurrentToken(TokenType.RESERVED, "where")){ //Ew -> T 'where' Dr => 'where'
      readNT();
      procDR(); //extra readToken() in procDR()
      buildNAryASTNode(ASTNodeType.WHERE, 2);
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
    while(isCurrentToken(TokenType.OPERATOR, ",")){ //T -> Ta (',' Ta )+ => 'tau'
      readNT();
      procTA(); //extra readToken() done in procTA()
      treesToPop++;
    }
    if(treesToPop > 0) buildNAryASTNode(ASTNodeType.TAU, treesToPop+1);
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
    while(isCurrentToken(TokenType.RESERVED, "aug")){ //Ta -> Ta 'aug' Tc => 'aug'
      readNT();
      procTC(); //extra readNT done in procTC()
      buildNAryASTNode(ASTNodeType.AUG, 2);
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
    if(isCurrentToken(TokenType.OPERATOR, "->")){ //Tc -> B '->' Tc '|' Tc => '->'
      readNT();
      procTC(); //extra readNT done in procTC
      if(!isCurrentToken(TokenType.OPERATOR, "|"))
        throw new ParseException("TC: '|' expected");
      readNT();
      procTC();  //extra readNT done in procTC
      buildNAryASTNode(ASTNodeType.CONDITIONAL, 3);
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
    while(isCurrentToken(TokenType.RESERVED, "or")){ //B -> B 'or' Bt => 'or'
      readNT();
      procBT();
      buildNAryASTNode(ASTNodeType.OR, 2);
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
    while(isCurrentToken(TokenType.OPERATOR, "&")){ //Bt -> Bt '&' Bs => '&'
      readNT();
      procBS(); //extra readNT in procBS()
      buildNAryASTNode(ASTNodeType.AND, 2);
    }
  }
  
  /**
   * <pre>
   * Bs -> 'not Bp => 'not'
   *    -> Bp;
   * </pre>
   */
  private void procBS(){
    if(isCurrentToken(TokenType.RESERVED, "not")){ //Bs -> 'not' Bp => 'not'
      readNT();
      procBP(); //extra readNT in procBP()
      buildNAryASTNode(ASTNodeType.NOT, 1);
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
    if(isCurrentToken(TokenType.RESERVED,"gr")||isCurrentToken(TokenType.OPERATOR,">")){ //Bp -> A('gr' | '>' ) A => 'gr'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(ASTNodeType.GR, 2);
    }
    else if(isCurrentToken(TokenType.RESERVED,"ge")||isCurrentToken(TokenType.OPERATOR,">=")){ //Bp -> A ('ge' | '>=') A => 'ge'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(ASTNodeType.GE, 2);
    }
    else if(isCurrentToken(TokenType.RESERVED,"ls")||isCurrentToken(TokenType.OPERATOR,"<")){ //Bp -> A ('ls' | '<' ) A => 'ls'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(ASTNodeType.LS, 2);
    }
    else if(isCurrentToken(TokenType.RESERVED,"le")||isCurrentToken(TokenType.OPERATOR,"<=")){ //Bp -> A ('le' | '<=') A => 'le'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(ASTNodeType.LE, 2);
    }
    else if(isCurrentToken(TokenType.RESERVED,"eq")){ //Bp -> A 'eq' A => 'eq'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(ASTNodeType.EQ, 2);
    }
    else if(isCurrentToken(TokenType.RESERVED,"ne")){ //Bp -> A 'ne' A => 'ne'
      readNT();
      procA(); //extra readNT in procA()
      buildNAryASTNode(ASTNodeType.NE, 2);
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
    if(isCurrentToken(TokenType.OPERATOR, "+")){ //A -> '+' At
      readNT();
      procAT(); //extra readNT in procAT()
    }
    else if(isCurrentToken(TokenType.OPERATOR, "-")){ //A -> '-' At => 'neg'
      readNT();
      procAT(); //extra readNT in procAT()
      buildNAryASTNode(ASTNodeType.NEG, 1);
    }
    else
      procAT(); //extra readNT in procAT()
    
    boolean plus = true;
    while(isCurrentToken(TokenType.OPERATOR, "+")||isCurrentToken(TokenType.OPERATOR, "-")){
      if(currentToken.getValue().equals("+"))
        plus = true;
      else if(currentToken.getValue().equals("-"))
        plus = false;
      readNT();
      procAT(); //extra readNT in procAT()
      if(plus) //A -> A '+' At => '+'
        buildNAryASTNode(ASTNodeType.PLUS, 2);
      else //A -> A '-' At => '-'
        buildNAryASTNode(ASTNodeType.MINUS, 2);
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
    while(isCurrentToken(TokenType.OPERATOR, "*")||isCurrentToken(TokenType.OPERATOR, "/")){
      if(currentToken.getValue().equals("*"))
        mult = true;
      else if(currentToken.getValue().equals("/"))
        mult = false;
      readNT();
      procAF(); //extra readNT in procAF()
      if(mult) //At -> At '*' Af => '*'
        buildNAryASTNode(ASTNodeType.MULT, 2);
      else //At -> At '/' Af => '/'
        buildNAryASTNode(ASTNodeType.DIV, 2);
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
    if(isCurrentToken(TokenType.OPERATOR, "**")){ //Af -> Ap '**' Af => '**'
      readNT();
      procAF();
      buildNAryASTNode(ASTNodeType.EXP, 2);
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
    while(isCurrentToken(TokenType.OPERATOR, "@")){ //Ap -> Ap '@' '<IDENTIFIER>' R => '@'
      readNT();
      if(!isCurrentTokenType(TokenType.IDENTIFIER))
        throw new ParseException("AP: expected Identifier");
      readNT();
      procR(); //extra readNT in procR()
      buildNAryASTNode(ASTNodeType.AT, 3);
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
    while(isCurrentTokenType(TokenType.INTEGER)||
        isCurrentTokenType(TokenType.STRING)|| 
        isCurrentTokenType(TokenType.IDENTIFIER)||
        isCurrentToken(TokenType.RESERVED, "true")||
        isCurrentToken(TokenType.RESERVED, "false")||
        isCurrentToken(TokenType.RESERVED, "nil")||
        isCurrentToken(TokenType.RESERVED, "dummy")||
        isCurrentTokenType(TokenType.L_PAREN)){ //R -> R Rn => 'gamma'
      procRN(); //NO extra readNT in procRN(). This is important because if we do an extra readNT in procRN and currentToken happens to
                //be an INTEGER, IDENTIFIER, or STRING, it will get pushed on the stack. Then, the GAMMA node that we build will have the
                //wrong kids. There are workarounds, e.g. keeping the extra readNT in procRN() and checking here if the last token read
                //(which was read in procRN()) is an INTEGER, IDENTIFIER, or STRING and, if so, to pop it, call buildNAryASTNode, and then
                //push it again. I chose this option because it seems cleaner.
      buildNAryASTNode(ASTNodeType.GAMMA, 2);
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
    if(isCurrentTokenType(TokenType.IDENTIFIER)|| //R -> '<IDENTIFIER>'
       isCurrentTokenType(TokenType.INTEGER)|| //R -> '<INTEGER>' 
       isCurrentTokenType(TokenType.STRING)){ //R-> '<STRING>'
    }
    else if(isCurrentToken(TokenType.RESERVED, "true")){ //R -> 'true' => 'true'
      createTerminalASTNode(ASTNodeType.TRUE, "true");
    }
    else if(isCurrentToken(TokenType.RESERVED, "false")){ //R -> 'false' => 'false'
      createTerminalASTNode(ASTNodeType.FALSE, "false");
    } 
    else if(isCurrentToken(TokenType.RESERVED, "nil")){ //R -> 'nil' => 'nil'
      createTerminalASTNode(ASTNodeType.NIL, "nil");
    }
    else if(isCurrentTokenType(TokenType.L_PAREN)){
      readNT();
      procE(); //extra readNT in procE()
      if(!isCurrentTokenType(TokenType.R_PAREN))
        throw new ParseException("RN: ')' expected");
    }
    else if(isCurrentToken(TokenType.RESERVED, "dummy")){ //R -> 'dummy' => 'dummy'
      createTerminalASTNode(ASTNodeType.DUMMY, "dummy");
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
    if(isCurrentToken(TokenType.RESERVED, "within")){ //D -> Da 'within' D => 'within'
      readNT();
      procD();
      buildNAryASTNode(ASTNodeType.WITHIN, 2);
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
    while(isCurrentToken(TokenType.RESERVED, "and")){ //Da -> Dr ( 'and' Dr )+ => 'and'
      readNT();
      procDR(); //extra readToken() in procDR()
      treesToPop++;
    }
    if(treesToPop > 0) buildNAryASTNode(ASTNodeType.SIMULTDEF, treesToPop+1);
  }
  
  /**
   * Dr -> 'rec' Db => 'rec'
   *    -> Db;
   */
  private void procDR(){
    if(isCurrentToken(TokenType.RESERVED, "rec")){ //Dr -> 'rec' Db => 'rec'
      readNT();
      procDB(); //extra readToken() in procDB()
      buildNAryASTNode(ASTNodeType.REC, 1);
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
    if(isCurrentTokenType(TokenType.L_PAREN)){ //Db -> '(' D ')'
      procD();
      readNT();
      if(!isCurrentTokenType(TokenType.R_PAREN))
        throw new ParseException("DB: ')' expected");
      readNT();
    }
    else if(isCurrentTokenType(TokenType.IDENTIFIER)){
      readNT();
      if(isCurrentToken(TokenType.OPERATOR, ",")){ //Db -> Vl '=' E => '='
        readNT();
        procVL(); //extra readNT in procVB()
        //VL makes its COMMA nodes for all the tokens EXCEPT the ones
        //we just read above (i.e., the first identifier and the comma after it)
        //Hence, we must pop the top of the tree VL just made and put it under a
        //comma node with the identifier it missed.
        if(!isCurrentToken(TokenType.OPERATOR, "="))
          throw new ParseException("DB: = expected.");
        buildNAryASTNode(ASTNodeType.COMMA, 2);
        readNT();
        procE(); //extra readNT in procE()
        buildNAryASTNode(ASTNodeType.EQUAL, 2);
      }
      else{ //Db -> '<IDENTIFIER>' Vb+ '=' E => 'fcn_form'
        if(isCurrentToken(TokenType.OPERATOR, "=")){ //Db -> Vl '=' E => '='; if Vl had only one IDENTIFIER (no commas)
          readNT();
          procE(); //extra readNT in procE()
          buildNAryASTNode(ASTNodeType.EQUAL, 2);
        }
        else{ //Db -> '<IDENTIFIER>' Vb+ '=' E => 'fcn_form'
          int treesToPop = 0;

          while(isCurrentTokenType(TokenType.IDENTIFIER) || isCurrentTokenType(TokenType.L_PAREN)){
            procVB(); //extra readNT in procVB()
            treesToPop++;
          }

          if(treesToPop==0)
            throw new ParseException("E: at least one 'Vb' expected");

          if(!isCurrentToken(TokenType.OPERATOR, "="))
            throw new ParseException("DB: = expected.");

          readNT();
          procE(); //extra readNT in procE()

          buildNAryASTNode(ASTNodeType.FCNFORM, treesToPop+2); //+1 for the last E and +1 for the first identifier
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
    if(isCurrentTokenType(TokenType.IDENTIFIER)){ //Vb -> '<IDENTIFIER>'
      readNT();
    }
    else if(isCurrentTokenType(TokenType.L_PAREN)){
      readNT();
      if(isCurrentTokenType(TokenType.R_PAREN)){ //Vb -> '(' ')' => '()'
        createTerminalASTNode(ASTNodeType.PAREN, "");
        readNT();
      }
      else{ //Vb -> '(' Vl ')'
        procVL(); //extra readNT in procVB()
        if(!isCurrentTokenType(TokenType.R_PAREN))
          throw new ParseException("VB: ')' expected");
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
    if(!isCurrentTokenType(TokenType.IDENTIFIER))
      throw new ParseException("VL: Identifier expected");
    else{
      readNT();
      int treesToPop = 0;
      while(isCurrentToken(TokenType.OPERATOR, ",")){ //Vl -> '<IDENTIFIER>' list ',' => ','?;
        readNT();
        if(!isCurrentTokenType(TokenType.IDENTIFIER))
          throw new ParseException("VL: Identifier expected");
        readNT();
        treesToPop++;
      }
      if(treesToPop > 0) buildNAryASTNode(ASTNodeType.COMMA, treesToPop+1); //+1 for the first identifier
    }
  }

}


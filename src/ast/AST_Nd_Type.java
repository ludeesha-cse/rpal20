package ast;

/**
 * Type of abstract syntax tree node
 
 */
public enum AST_Nd_Type{
  //General
  IDENTIFIER("<ID:%s>"),
  STRING("<STR:'%s'>"),
  INTEGER("<INT:%s>"),
  
  //Expressions
  LET("let"),
  LAMBDA("lambda"),
  WHERE("where"),
  
  //Tuple expressions
  TAU("tau"),
  AUG("aug"),
  CONDITIONAL("->"),
  
  //Boolean Expressions
  OR("or"),
  AND("&"),
  NOT("not"),
  GR("gr"),
  GE("ge"),
  LS("ls"),
  LE("le"),
  EQ("eq"),
  NE("ne"),
  
  //Arithmetic Expressions
  PLUS("+"),
  MINUS("-"),
  NEG("neg"),
  MULT("*"),
  DIV("/"),
  EXP("**"),
  AT("@"),
  
  //Rators and Rands
  GAMMA("gamma"),
  TRUE("<true>"),
  FALSE("<false>"),
  NIL("<nil>"),
  DUMMY("<dummy>"),
  
  //Definitions
  WITHIN("within"),
  SIMULTDEF("and"),
  REC("rec"),
  EQUAL("="),
  FCNFORM("function_form"),
  
  //Variables
  PAREN("<()>"),
  COMMA(","),
  
  //Post-standardize
  YSTAR("<Y*>"),
  
  //For program evaluation only
  BETA(""),
  DELTA(""),
  ETA(""),
  TUPLE("");
  
  private String printName; //printing AST representation
  
  private AST_Nd_Type(String name){
    printName = name;
  }

  public String getPrintName(){
    return printName;
  }
}

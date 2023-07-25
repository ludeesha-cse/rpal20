package csem;



public class EvaluationError{
  
  public static void printError(int srcLineNumber, String msg){
    
    System.out.println(":"+srcLineNumber+": "+msg);
    System.exit(1);
  }

}

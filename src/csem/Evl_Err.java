package csem;



public class Evl_Err{
  
  public static void printError(int srcLineNumber, String msg){
    
    System.out.println(":"+srcLineNumber+": "+msg);
    System.exit(1);
  }

}

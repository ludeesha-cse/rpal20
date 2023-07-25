package scanner;

/**
 * Token given by the scanner to the parser
 *
 */
public class Token{
  public TKN_Type type;
  public  String value;
  public int sourceLineNumber;
  
 
  
  public void setType(TKN_Type type){
    this.type = type;
  }
  
  
  public void setValue(String value){
    this.value = value;
  }

  

  public void setSourceLineNumber(int sourceLineNumber){
    this.sourceLineNumber = sourceLineNumber;
  }
}

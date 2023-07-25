package scanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;


//group 44

/**
 * Scanner
 */
public class Scanner{
  private BufferedReader buffer;
  private String charRead;
  private final List<String> reservedID = Arrays.asList(new String[]{"let","in","within","fn","where","aug","or",
                                                                              "not","gr","ge","ls","le","eq","ne","true",
                                                                              "false","nil","dummy","rec","and"});
  private int sourceLineNumber;
  
  public Scanner(String inputFile) throws IOException{
    sourceLineNumber = 1;
    buffer = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputFile))));
  }
  
  
  // The `readNextToken()` method is responsible for reading the next token from the input file.
  public Token readNextToken(){
    Token nextToken = null;
    String nextChar;
    if(charRead!=null){
      nextChar = charRead;
      charRead = null;
    } else
      nextChar = readNextChar();
    if(nextChar!=null)
      nextToken = buildToken(nextChar);
    return nextToken;
  }

  private String readNextChar(){
    String nextChar = null;
    try{
      int c = buffer.read();
      if(c!=-1){
        nextChar = Character.toString((char)c);
        if(nextChar.equals("\n")) sourceLineNumber++;
      } else
          buffer.close();
    }catch(IOException e){
    }
    return nextChar;
  }

  
  // The `buildToken` method is responsible for determining the type of token based on the current
  // character being processed and calling the appropriate method to build that token. It checks the
  // current character against different regular expression patterns to determine the token type. If
  // the current character matches a letter pattern, it calls the `buildIdentifierToken` method. If it
  // matches a digit pattern, it calls the `buildIntegerToken` method. If it matches an operator symbol
  // pattern, it calls the `buildOperatorToken` method. If the current character is a single quote, it
  // calls the `buildStringToken` method. If it matches a space pattern, it calls the `buildSpaceToken`
  // method. If it matches a punctuation pattern, it calls the `buildPunctuationPattern` method. The
  // method returns the built token.
  private Token buildToken(String currentChar){
    Token nextToken = null;
    if(Lxcl_Rgx_Patterns.LetterPattern.matcher(currentChar).matches()){
      nextToken = buildIdentifierToken(currentChar);
    }
    else if(Lxcl_Rgx_Patterns.DigitPattern.matcher(currentChar).matches()){
      nextToken = buildIntegerToken(currentChar);
    }
    else if(Lxcl_Rgx_Patterns.OpSymbolPattern.matcher(currentChar).matches()){ 
      nextToken = buildOperatorToken(currentChar);
    }
    else if(currentChar.equals("\'")){
      nextToken = buildStringToken(currentChar);
    }
    else if(Lxcl_Rgx_Patterns.SpacePattern.matcher(currentChar).matches()){
      nextToken = buildSpaceToken(currentChar);
    }
    else if(Lxcl_Rgx_Patterns.PunctuationPattern.matcher(currentChar).matches()){
      nextToken = buildPunctuationPattern(currentChar);
    }
    return nextToken;
  }

  
  private Token buildIdentifierToken(String current_C){
    Token identifier_T = new Token();
    identifier_T.setType(TKN_Type.IDENTIFIER);
    identifier_T.setSourceLineNumber(sourceLineNumber);
    StringBuilder sBuilder = new StringBuilder(current_C);
    
    String nextChar = readNextChar();
    while(nextChar!=null){
      if(Lxcl_Rgx_Patterns.IdentifierPattern.matcher(nextChar).matches()){
        sBuilder.append(nextChar);
        nextChar = readNextChar();
      }
      else{
        charRead = nextChar;
        break;
      }
    }
    
    String value = sBuilder.toString();
    if(reservedID.contains(value))
      identifier_T.setType(TKN_Type.RESERVED);
    
    identifier_T.setValue(value);
    return identifier_T;
  }

  
  private Token buildIntegerToken(String current_C){
    Token integer_T = new Token();
    integer_T.setType(TKN_Type.INTEGER);
    integer_T.setSourceLineNumber(sourceLineNumber);
    StringBuilder sBuilder = new StringBuilder(current_C);
    
    String nextChar = readNextChar();
    while(nextChar!=null){ 
      if(Lxcl_Rgx_Patterns.DigitPattern.matcher(nextChar).matches()){
        sBuilder.append(nextChar);
        nextChar = readNextChar();
      }
      else{
        charRead = nextChar;
        break;
      }
    }
    
    integer_T.setValue(sBuilder.toString());
    return integer_T;
  }


  private Token buildOperatorToken(String current_C){
    Token opSymbol_T = new Token();
    opSymbol_T.setType(TKN_Type.OPERATOR);
    opSymbol_T.setSourceLineNumber(sourceLineNumber);
    StringBuilder sBuilder = new StringBuilder(current_C);
    
    String nextChar = readNextChar();
    
    if(current_C.equals("/") && nextChar.equals("/"))
      return buildCommentToken(current_C+nextChar);
    
    while(nextChar!=null){ 
      if(Lxcl_Rgx_Patterns.OpSymbolPattern.matcher(nextChar).matches()){
        sBuilder.append(nextChar);
        nextChar = readNextChar();
      }
      else{
        charRead = nextChar;
        break;
      }
    }
    
    opSymbol_T.setValue(sBuilder.toString());
    return opSymbol_T;
  }

  /**
   * Builds string token.
  
   */
  private Token buildStringToken(String current_C){
    Token string_T = new Token();
    string_T.setType(TKN_Type.STRING);
    string_T.setSourceLineNumber(sourceLineNumber);
    StringBuilder sBuilder = new StringBuilder("");
    
    String nextChar = readNextChar();
    while(nextChar!=null){ 
      if(nextChar.equals("\'")){ 
        
        string_T.setValue(sBuilder.toString());
        return string_T;
      }
      else if(Lxcl_Rgx_Patterns.StringPattern.matcher(nextChar).matches()){ 
        sBuilder.append(nextChar);
        nextChar = readNextChar();
      }
    }
    
    return null;
  }
  
  private Token buildSpaceToken(String current_C){
    Token delete_T = new Token();
    delete_T.setType(TKN_Type.DELETE);
    delete_T.setSourceLineNumber(sourceLineNumber);
    StringBuilder sBuilder = new StringBuilder(current_C);
    
    String nextChar = readNextChar();
    while(nextChar!=null){ 
      if(Lxcl_Rgx_Patterns.SpacePattern.matcher(nextChar).matches()){
        sBuilder.append(nextChar);
        nextChar = readNextChar();
      }
      else{
        charRead = nextChar;
        break;
      }
    }
    
    delete_T.setValue(sBuilder.toString());
    return delete_T;
  }
  
  private Token buildCommentToken(String current_C){
    Token comment_T = new Token();
    comment_T.setType(TKN_Type.DELETE);
    comment_T.setSourceLineNumber(sourceLineNumber);
    StringBuilder sBuilder = new StringBuilder(current_C);
    
    String nextChar = readNextChar();
    while(nextChar!=null){ 
      if(Lxcl_Rgx_Patterns.CommentPattern.matcher(nextChar).matches()){
        sBuilder.append(nextChar);
        nextChar = readNextChar();
      }
      else if(nextChar.equals("\n"))
        break;
    }
    
    comment_T.setValue(sBuilder.toString());
    return comment_T;
  }

  private Token buildPunctuationPattern(String current_C){
    Token punctuation_T = new Token();
    punctuation_T.setSourceLineNumber(sourceLineNumber);
    punctuation_T.setValue(current_C);
    if(current_C.equals("("))
      punctuation_T.setType(TKN_Type.L_PAREN);
    else if(current_C.equals(")"))
      punctuation_T.setType(TKN_Type.R_PAREN);
    else if(current_C.equals(";"))
      punctuation_T.setType(TKN_Type.SEMICOLON);
    else if(current_C.equals(","))
      punctuation_T.setType(TKN_Type.COMMA);
    
    return punctuation_T;
  }
}


package scanner;

import java.util.regex.Pattern;

/**
 * regex_patterns
 */
public class Lxcl_Rgx_Patterns{
  private static final String LETTER_REGEX = "a-zA-Z";
  private static final String DIGIT_REGEX = "\\d";
  private static final String SPACE_REGEX = "[\\s\\t\\n]";
  private static final String PUNCTUATION_REGEX = "();,";
  private static final String OP_SYMBOL_REGEX = "+-/~:=|!#%_{}\"*<>.&$^\\[\\]?@";
  private static final String OP_SYMBOL_TO_ESCAPE = "([*<>.&$^?])";
  
  public static final Pattern LetterPattern = Pattern.compile("["+LETTER_REGEX+"]");
  
  public static final Pattern IdentifierPattern = Pattern.compile("["+LETTER_REGEX+DIGIT_REGEX+"_]");

  public static final Pattern DigitPattern = Pattern.compile(DIGIT_REGEX);

  public static final Pattern PunctuationPattern = Pattern.compile("["+PUNCTUATION_REGEX+"]");

  public static final String opSymbolRegex = "[" + escapeMetaChars(OP_SYMBOL_REGEX, OP_SYMBOL_TO_ESCAPE) + "]";
  public static final Pattern OpSymbolPattern = Pattern.compile(opSymbolRegex);
  
  public static final Pattern StringPattern = Pattern.compile("[ \\t\\n\\\\"+PUNCTUATION_REGEX+LETTER_REGEX+DIGIT_REGEX+escapeMetaChars(OP_SYMBOL_REGEX, OP_SYMBOL_TO_ESCAPE) +"]");
  
  public static final Pattern SpacePattern = Pattern.compile(SPACE_REGEX);
  
  public static final Pattern CommentPattern = Pattern.compile("[ \\t\\'\\\\ \\r"+PUNCTUATION_REGEX+LETTER_REGEX+DIGIT_REGEX+escapeMetaChars(OP_SYMBOL_REGEX, OP_SYMBOL_TO_ESCAPE)+"]"); //the \\r is for Windows LF; not really required since we're targeting *nix systems
  
  private static String escapeMetaChars(String input, String escapeChar){
    return input.replaceAll(escapeChar,"\\\\\\\\$1");
  }
}
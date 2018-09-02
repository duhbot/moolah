package org.duh102.duhbot.moolah;

import java.util.Pattern;
import java.util.Matcher;

public enum HiLoBetType {
  HIGH("high"), LOW("low"), EQUAL("equal");
  private String nicename;
  //Helpfully, our three choices have single character unique prefixes
  private static Pattern = Pattern.compile("^(h(i(gh?)?)?|l(ow?)?|e(q(u(al?)?)?)?)");
  HiLoBetType(String nicename) {
    this.nicename = nicename;
  }
  public String toString() {
    return this.nicename;
  }
  public HiLoBetType fromString(String input) throws InvalidInputError {
    input = input.trim().toLowerCase();
    Matcher mat = Pattern.match(input);
    if( !mat.matches() )
      throw new InvalidInputError(input, "[high, low, equal]");
    switch( input.charAt(0) ) {
      case 'h':
        return HiLoBetType.HIGH;
      case 'l':
        return HiLoBetType.LOW;
      case 'e':
        return HiLoBetType.EQUAL;
      default:
        throw new InvalidInputError(input, "unknown");
    }
  }
}

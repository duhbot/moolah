package org.duh102.duhbot.moolah;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.duh102.duhbot.moolah.exceptions.*;

public enum HiLoBetType {
  HIGH("HIGH", 3.0), LOW("LOW", 3.0), EQUAL("EQUAL", 15.0);
  private String nicename;
  private double multiplier;
  private static Pattern typePat = Pattern.compile("^(h(i(gh?)?)?|l(ow?)?|e(q(u(al?)?)?)?)");
  HiLoBetType(String nicename, double multiplier) {
    this.nicename = nicename;
    this.multiplier = multiplier;
  }
  public String toString() {
    return nicename;
  }
  public double getMultiplier() {
    return multiplier;
  }
  public boolean getSatisfied(int result) {
    return (this.equals(HiLoBetType.EQUAL) && result == HiLoRecord.MID)
      || (this.equals(HiLoBetType.HIGH) && result > HiLoRecord.MID)
      || (this.equals(HiLoBetType.LOW) && result < HiLoRecord.MID);
  }
  public static HiLoBetType fromString(String input) throws InvalidInputError {
    input = input.trim().toLowerCase();
    Matcher mat = typePat.matcher(input);
    if( !mat.matches() )
      throw new InvalidInputError(input, "[high, low, equal]");
    //Helpfully, our three choices have single character unique prefixes
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

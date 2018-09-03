package org.duh102.duhbot.moolah.exceptions;

public class InvalidInputError extends Exception {
  public InvalidInputError() {
    super();
  }
  public InvalidInputError(String message) {
    super(message);
  }
  public InvalidInputError(String message, Throwable source) {
    super(message, source);
  }
  public InvalidInputError(Throwable cause) {
    super(cause);
  }
  private static String expectedMessage(String input, String expected) {
    return String.format("Got '%s', expected %s", input, expected);
  }
  public InvalidInputError(String input, String expected) {
    super(InvalidInputError.expectedMessage(input, expected));
  }
  public InvalidInputError(String input, String expected, Throwable source) {
    super(InvalidInputError.expectedMessage(input, expected), source);
  }
}

package org.duh102.duhbot.moolah.exceptions;

public class SameAccountException extends Exception {
  public SameAccountException() {
    super();
  }
  public SameAccountException(String message) {
    super(message);
  }
  public SameAccountException(String message, Throwable source) {
    super(message, source);
  }
  public SameAccountException(Throwable cause) {
    super(cause);
  }
}

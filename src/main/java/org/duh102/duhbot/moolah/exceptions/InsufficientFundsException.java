package org.duh102.duhbot.moolah.exceptions;

public class InsufficientFundsException extends Exception {
  public InsufficientFundsException() {
    super();
  }
  public InsufficientFundsException(String message) {
    super(message);
  }
  public InsufficientFundsException(String message, Throwable source) {
    super(message, source);
  }
  public InsufficientFundsException(Throwable cause) {
    super(cause);
  }
}

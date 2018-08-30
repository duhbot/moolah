package org.duh102.duhbot.moolah.exceptions;

public class InsufficientFundsException extends Exception {
  public InsufficientFundsException() {}

  public InsufficientFundsException(String message) {
    super(message);
  }
}

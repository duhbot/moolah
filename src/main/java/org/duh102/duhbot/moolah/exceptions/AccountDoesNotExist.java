package org.duh102.duhbot.moolah.exceptions;

public class AccountDoesNotExist extends Exception {
  public AccountDoesNotExist() {
    super();
  }
  public AccountDoesNotExist(String message) {
    super(message);
  }
  public AccountDoesNotExist(String message, Throwable source) {
    super(message, source);
  }
  public AccountDoesNotExist(Throwable cause) {
    super(cause);
  }
}

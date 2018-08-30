package org.duh102.duhbot.moolah.exceptions;

public class AccountAlreadyExists extends Exception {
  public AccountAlreadyExists() {
    super();
  }
  public AccountAlreadyExists(String message) {
    super(message);
  }
  public AccountAlreadyExists(String message, Throwable source) {
    super(message, source);
  }
  public AccountAlreadyExists(Throwable cause) {
    super(cause);
  }
}

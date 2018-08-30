package org.duh102.duhbot.moolah.exceptions;

public class AccountAlreadyExists extends Exception {
  public AccountAlreadyExists() {}

  public AccountAlreadyExists(String message) {
    super(message);
  }
}

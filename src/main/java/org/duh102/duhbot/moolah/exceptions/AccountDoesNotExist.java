package org.duh102.duhbot.moolah.exceptions;

public class AccountDoesNotExist extends Exception {
  public AccountDoesNotExist() {}

  public AccountDoesNotExist(String message) {
    super(message);
  }
}

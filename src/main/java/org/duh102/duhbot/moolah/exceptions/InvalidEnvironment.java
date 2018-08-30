package org.duh102.duhbot.moolah.exceptions;

public class InvalidEnvironment extends Exception {
  public InvalidEnvironment() {
    super();
  }
  public InvalidEnvironment(String message) {
    super(message);
  }
  public InvalidEnvironment(String message, Throwable source) {
    super(message, source);
  }
  public InvalidEnvironment(Throwable cause) {
    super(cause);
  }
}

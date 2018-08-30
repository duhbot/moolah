package org.duh102.duhbot.moolah.exceptions;

public class InvalidDBConfiguration extends Exception {
  public InvalidDBConfiguration() {
    super();
  }
  public InvalidDBConfiguration(String message) {
    super(message);
  }
  public InvalidDBConfiguration(String message, Throwable source) {
    super(message, source);
  }
  public InvalidDBConfiguration(Throwable cause) {
    super(cause);
  }
}

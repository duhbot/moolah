package org.duh102.duhbot.moolah.exceptions;

public class InsufficientPrivilegesException extends Exception {
  public InsufficientPrivilegesException() {
    super();
  }
  public InsufficientPrivilegesException(String message) {
    super(message);
  }
  public InsufficientPrivilegesException(String message, Throwable source) {
    super(message, source);
  }
  public InsufficientPrivilegesException(Throwable cause) {
    super(cause);
  }
}

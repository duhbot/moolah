package org.duh102.duhbot.moolah.exceptions;

public class DBAlreadyConnected extends Exception {
  public DBAlreadyConnected() {
    super();
  }
  public DBAlreadyConnected(String message) {
    super(message);
  }
  public DBAlreadyConnected(String message, Throwable source) {
    super(message, source);
  }
  public DBAlreadyConnected(Throwable cause) {
    super(cause);
  }
}

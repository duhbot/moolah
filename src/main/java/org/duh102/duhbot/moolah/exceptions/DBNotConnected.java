package org.duh102.duhbot.moolah.exceptions;

public class DBNotConnected extends Exception {
  public DBNotConnected() {
    super();
  }
  public DBNotConnected(String message) {
    super(message);
  }
  public DBNotConnected(String message, Throwable source) {
    super(message, source);
  }
  public DBNotConnected(Throwable cause) {
    super(cause);
  }
}

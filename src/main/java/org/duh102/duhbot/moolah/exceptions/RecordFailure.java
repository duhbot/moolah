package org.duh102.duhbot.moolah.exceptions;

public class RecordFailure extends Exception {
  public RecordFailure() {
    super();
  }
  public RecordFailure(String message) {
    super(message);
  }
  public RecordFailure(String message, Throwable source) {
    super(message, source);
  }
  public RecordFailure(Throwable cause) {
    super(cause);
  }
}

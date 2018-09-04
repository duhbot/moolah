package org.duh102.duhbot.moolah.exceptions;

public class MineAttemptTooSoon extends Exception {
  public MineAttemptTooSoon() {
    super();
  }
  public MineAttemptTooSoon(String message) {
    super(message);
  }
  public MineAttemptTooSoon(String message, Throwable source) {
    super(message, source);
  }
  public MineAttemptTooSoon(Throwable cause) {
    super(cause);
  }
}

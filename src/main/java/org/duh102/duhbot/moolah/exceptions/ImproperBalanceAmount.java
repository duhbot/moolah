package org.duh102.duhbot.moolah.exceptions;

public class ImproperBalanceAmount extends Exception {
  public ImproperBalanceAmount() {
    super();
  }
  public ImproperBalanceAmount(String message) {
    super(message);
  }
  public ImproperBalanceAmount(String message, Throwable source) {
    super(message, source);
  }
  public ImproperBalanceAmount(Throwable cause) {
    super(cause);
  }
  public ImproperBalanceAmount(long invalidValue) {
    super(String.format("%d", invalidValue));
  }
  public ImproperBalanceAmount(int invalidValue) {
    super(String.format("%d", invalidValue));
  }
  public ImproperBalanceAmount(long invalidValue, Throwable cause) {
    super(String.format("%d", invalidValue), cause);
  }
  public ImproperBalanceAmount(int invalidValue, Throwable cause) {
    super(String.format("%d", invalidValue), cause);
  }
}

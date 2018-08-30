package org.duh102.duhbot.moolah.exceptions;

public class ImproperBalanceAmount extends Exception {
  public ImproperBalanceAmount() {}

  public ImproperBalanceAmount(String message) {
    super(message);
  }
  public ImproperBalanceAmount(long invalidValue) {
    super(String.format("%d", invalidValue));
  }
  public ImproperBalanceAmount(int invalidValue) {
    super(String.format("%d", invalidValue));
  }
}

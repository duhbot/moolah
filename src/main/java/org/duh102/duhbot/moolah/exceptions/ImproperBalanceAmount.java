package org.duh102.duhbot.moolah.exceptions;

import java.math.BigInteger;

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
  public ImproperBalanceAmount(BigInteger invalidValue) {
    super(invalidValue.toString());
  }
  public ImproperBalanceAmount(BigInteger invalidValue, Throwable cause) {
    super(invalidValue.toString(), cause);
  }
  public ImproperBalanceAmount(long invalidValue) {
    this(new BigInteger(String.format("%d", invalidValue)));
  }
  public ImproperBalanceAmount(int invalidValue) {
    this(new BigInteger(String.format("%d", invalidValue)));
  }
  public ImproperBalanceAmount(long invalidValue, Throwable cause) {
    this(new BigInteger(String.format("%d", invalidValue)), cause);
  }
  public ImproperBalanceAmount(int invalidValue, Throwable cause) {
    this(new BigInteger(String.format("%d", invalidValue)), cause);
  }
}

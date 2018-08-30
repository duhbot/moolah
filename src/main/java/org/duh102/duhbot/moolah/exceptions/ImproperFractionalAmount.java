package org.duh102.duhbot.moolah.exceptions;

public class ImproperFractionalAmount extends Exception {
  public ImproperFractionalAmount() {}

  public ImproperFractionalAmount(String message) {
    super(message);
  }
  public ImproperFractionalAmount(byte invalidValue) {
    super(String.format("%d", invalidValue));
  }
  public ImproperFractionalAmount(int invalidValue) {
    super(String.format("%d", invalidValue));
  }
}

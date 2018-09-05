package org.duh102.duhbot.moolah;

public enum SlotReelImage {
  CHERRIES("🍒", "a"), DOLLAR("$", "b"), FIVE("5", "c"), BELL("🔔", "d"), LEMON("🍋", "e"), SEVEN("7", "f"), BAR("█", "g");
  private String image;
  private String regexChar;
  private SlotReelImage(String image, String regexChar) {
    this.image = image;
    this.regexChar = regexChar;
  }
  public String toString() {
    return image;
  }
  public String toRegexChar() {
    return regexChar;
  }
}

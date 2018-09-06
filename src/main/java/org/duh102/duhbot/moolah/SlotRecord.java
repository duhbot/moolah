package org.duh102.duhbot.moolah;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.duh102.duhbot.moolah.exceptions.*;

public class SlotRecord {
  public static final SlotReelImage[] choices = new SlotReelImage[] {
    SlotReelImage.CHERRIES, SlotReelImage.DOLLAR, SlotReelImage.SEVEN,
    SlotReelImage.FIVE, SlotReelImage.BELL, SlotReelImage.LEMON,
    SlotReelImage.BAR
  };
  private static final HashMap<String, SlotReelImage> reelLookup;
  static {
    reelLookup = new HashMap<String, SlotReelImage>();
    for( SlotReelImage image : SlotReelImage.values() ) {
      reelLookup.put(image.toRegexChar(), image);
    }
  }
  public static final int NUM_SLOTS = 3;
  public static final double BAR_MULT = 2.0, SEVEN_ADD_MULT = 1.1, SEVEN_ALL_MULT = 7.0,
         TWO_MATCH_MULT = 1.2, THREE_MATCH_MULT = 2.0,
         DOLLAR_FIVE_BONUS = 1.0, FRUIT_BONUS = 2.0, BELL_BONUS = 3.0;
  private static final Random rand = new Random();

  public long outcomeID;
  public long uid;
  public SlotReelImage[] slotImages;
  public long wager;
  public long payout;
  public double multiplier;
  public Timestamp timestamp;
  public SlotRecord(long outcomeID, long uid, SlotReelImage[] slotImages, long wager, long payout, double multiplier, Timestamp timestamp) {
    this.outcomeID = outcomeID;
    this.uid = uid;
    this.slotImages = slotImages;
    this.wager = wager;
    this.payout = payout;
    this.multiplier = multiplier;
    this.timestamp = timestamp;
  }

  public static void setSeed(long seed) {
    rand.setSeed(seed);
  }

  public boolean equals(Object other) {
    if( !(other instanceof SlotRecord) )
      return false;
    return this.equals((SlotRecord)other);
  }
  public boolean equals(SlotRecord other) {
    return this.outcomeID == other.outcomeID && this.uid == other.uid
      && Arrays.equals(this.slotImages, other.slotImages) && this.wager == other.wager
      && this.payout == other.payout && this.multiplier == other.multiplier
      && this.timestamp.equals(other.timestamp);
  }

  public static SlotRecord slotAttempt(BankAccount account, long wager) throws InsufficientFundsException, ImproperBalanceAmount {
    account.subFunds(wager);
    Timestamp now = LocalTimestamp.now();
    SlotReelImage[] reels = getSlotImages();
    double multiplier = getImagesMultiplier(reels);
    long payout = Math.abs(Math.round(Math.ceil(wager * multiplier)));
    try {
      account.addFunds(payout);
    } catch( ImproperBalanceAmount iba ) {
      iba.printStackTrace();
      try {
        account.addFunds(wager)
      } catch( ImproperBalanceAmount iba2 ) {
        iba2.printStackTrace();
      }
    }
    return new SlotRecord(0l, account.uid, reels, wager, payout, multiplier, now);
  }

  public static SlotReelImage[] getSlotImages() {
    SlotReelImage[] reelSet = new SlotReelImage[NUM_SLOTS];
    StringBuilder slotBuilt = new StringBuilder();
    for( int i = 0; i < NUM_SLOTS; i++ ){
      reelSet[i] = choices[rand.nextInt(choices.length)];
    }
    return reelSet;
  }
  public static SlotReelImage[] getSlotImages(String regexRecording) {
    SlotReelImage[] reelSet = new SlotReelImage[regexRecording.length()];
    int i = 0;
    for( String chara : regexRecording.split("") ) {
      reelSet[i] = reelLookup.get(chara);
      i++;
    }
    return reelSet;
  }
  private static Pattern barFinder = Pattern.compile(SlotReelImage.BAR.toRegexChar() + "+");
  private static Pattern sevenFinder = Pattern.compile(SlotReelImage.SEVEN.toRegexChar() + "+");
  // If we ever upgrade to more than 3 reels we'll need to modify the regex here
  private static Pattern symbolFinder = Pattern.compile(
      "(" + SlotReelImage.CHERRIES.toRegexChar() + "{2,3}|" + SlotReelImage.DOLLAR.toRegexChar() + "{2,3}|"
      + SlotReelImage.FIVE.toRegexChar() + "{2,3}|" + SlotReelImage.BELL.toRegexChar() + "{2,3}|"
      + SlotReelImage.LEMON.toRegexChar() + "{2,3})"
      );

  public static String getImagesString(SlotReelImage[] reels) {
    StringBuilder builder = new StringBuilder();
    for(int i = 0; i < reels.length; i++) {
      builder.append(reels[i].toString());
    }
    return builder.toString();
  }
  public static String getRegexString(SlotReelImage[] reels) {
    StringBuilder builder = new StringBuilder();
    for(int i = 0; i < reels.length; i++) {
      builder.append(reels[i].toRegexChar());
    }
    return builder.toString();
  }
  public static double getImagesMultiplier(SlotReelImage[] reelSet) {
    String slotImages = getRegexString(reelSet);
    double multiplier = 0.0;
    Matcher barMatcher = barFinder.matcher(slotImages);
    if( barMatcher.find() ) {
      if( barMatcher.group().length() == NUM_SLOTS )
        multiplier = 2.0;
    } else {
      int numSevens = 0;
      Matcher sevenMatcher = sevenFinder.matcher(slotImages);
      while( sevenMatcher.find() ) {
        numSevens += sevenMatcher.group().length();
      }
      if( numSevens == NUM_SLOTS ) {
        multiplier = 7.0;
      } else {
        Matcher symbolMatcher = symbolFinder.matcher(slotImages);
        while( symbolMatcher.find() ) {
          int len = symbolMatcher.group().length();
          if( len == 0 )
            continue;
          if( len == NUM_SLOTS ) {
            multiplier = THREE_MATCH_MULT;
            SlotReelImage symbol = null;
            symbol = reelLookup.get(symbolMatcher.group().substring(0,1));
            switch(symbol) {
              case DOLLAR:
              case FIVE:
                multiplier += DOLLAR_FIVE_BONUS;
                break;
              case CHERRIES:
              case LEMON:
                multiplier += FRUIT_BONUS;
                break;
              case BELL:
                multiplier += BELL_BONUS;
            }
          } else
            multiplier = TWO_MATCH_MULT;
        }
        multiplier += SEVEN_ADD_MULT*numSevens;
      }
    }
    return multiplier;
  }
}

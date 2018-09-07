package org.duh102.duhbot.moolah;

import java.sql.*;
import java.text.ParseException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SlotRecordTest {
  static Timestamp defTime;
  static Timestamp defTime2;
  static {
    try {
      defTime = LocalTimestamp.parse("2011-11-11 11:11:11.111T-0400");
      defTime2 = LocalTimestamp.parse("2011-11-11 11:11:11.112T-0400");
    } catch( ParseException pe ) {
      defTime = LocalTimestamp.now();
      defTime2 = LocalTimestamp.now();
    }
  }
  private static SlotReelImage CH = SlotReelImage.CHERRIES, DO = SlotReelImage.DOLLAR,
          FI = SlotReelImage.FIVE, BE = SlotReelImage.BELL, LE = SlotReelImage.LEMON,
          SE = SlotReelImage.SEVEN, BA = SlotReelImage.BAR;

  @Test
  public void testGetSlotImages() throws Exception {
    SlotRecord.setSeed(101l);
    SlotReelImage[] images = SlotRecord.getSlotImages();
    for( SlotReelImage img : images ) {
      assertNotEquals(null, img);
    }
  }
  @Test
  public void testGetSlotImagesSymmetric() throws Exception {
    SlotReelImage[] images = new SlotReelImage[] {CH, DO, SE, FI, BE, LE, SE, BA};
    assertArrayEquals(images, SlotRecord.getSlotImages(SlotRecord.getRegexString(images)));
  }
  @Test
  public void testGetRegexString() throws Exception {
    SlotReelImage[] images = new SlotReelImage[] {CH, DO, SE, FI, BE, LE, SE, BA};
  String expected = CH.toRegexChar() + DO.toRegexChar() + SE.toRegexChar()
    + FI.toRegexChar() + BE.toRegexChar() + LE.toRegexChar() + SE.toRegexChar() + BA.toRegexChar();
    assertEquals(expected, SlotRecord.getRegexString(images));
  }
  @Test
  public void testGetImagesString() throws Exception {
    SlotReelImage[] images = new SlotReelImage[] {CH, DO, SE, FI, BE, LE, SE, BA};
  String expected = CH.toString() + DO.toString() + SE.toString()
    + FI.toString() + BE.toString() + LE.toString() + SE.toString() + BA.toString();
    assertEquals(expected, SlotRecord.getImagesString(images));
  }
  @Test
  public void testGetSlotImagesDiff() throws Exception {
    Mine.setSeed(105l);
    SlotReelImage[] a = SlotRecord.getSlotImages();
    SlotReelImage[] b = SlotRecord.getSlotImages();
    assertFalse(Arrays.equals(a, b));
  }
  private static Stream<Arguments> imagesMultiplierSource() {
    return Stream.of(
        Arguments.of(new SlotReelImage[] {SE,SE,SE}, SlotRecord.SEVEN_ALL_MULT), //1
        Arguments.of(new SlotReelImage[] {BA,BA,BA}, SlotRecord.BAR_MULT),       //2
        Arguments.of(new SlotReelImage[] {SE,SE,BA}, 0.0),                       //3
        Arguments.of(new SlotReelImage[] {BA,BA,SE}, 0.0),                       //4
        Arguments.of(new SlotReelImage[] {FI,FI,BA}, 0.0),                       //5
        Arguments.of(new SlotReelImage[] {BA,FI,FI}, 0.0),                       //6
        Arguments.of(new SlotReelImage[] {CH,FI,CH}, 0.0),                      //7
        Arguments.of(new SlotReelImage[] {CH,CH,FI}, SlotRecord.TWO_MATCH_MULT),//8
        Arguments.of(new SlotReelImage[] {CH,LE,CH}, 0.0),                      //9
        Arguments.of(new SlotReelImage[] {BE,SE,BE}, SlotRecord.SEVEN_ADD_MULT),//10
        Arguments.of(new SlotReelImage[] {BE,BE,SE}, SlotRecord.TWO_MATCH_MULT + SlotRecord.SEVEN_ADD_MULT),//11
        Arguments.of(new SlotReelImage[] {DO,DO,DO}, SlotRecord.THREE_MATCH_MULT + SlotRecord.DOLLAR_FIVE_BONUS),//12
        Arguments.of(new SlotReelImage[] {FI,FI,FI}, SlotRecord.THREE_MATCH_MULT + SlotRecord.DOLLAR_FIVE_BONUS),//13
        Arguments.of(new SlotReelImage[] {CH,CH,CH}, SlotRecord.THREE_MATCH_MULT + SlotRecord.FRUIT_BONUS),//14
        Arguments.of(new SlotReelImage[] {LE,LE,LE}, SlotRecord.THREE_MATCH_MULT + SlotRecord.FRUIT_BONUS),//15
        Arguments.of(new SlotReelImage[] {BE,BE,BE}, SlotRecord.THREE_MATCH_MULT + SlotRecord.BELL_BONUS)//16
      );
  }

  @ParameterizedTest(name = "Value of {0} == {1}")
  @MethodSource("imagesMultiplierSource")
  public void testImagesMultiplier(SlotReelImage[] input, double expOutput) {
    assertEquals(expOutput, SlotRecord.getImagesMultiplier(input));
  }

  @Test
  public void testSlotAttempt() throws Exception {
    SlotRecord.setSeed(110l);
    BankAccount acct = new BankAccount(101l, "auser", 1000l, LocalTimestamp.now());
    SlotRecord atmpt = SlotRecord.slotAttempt(acct, 100l);
    assertEquals(acct.uid, atmpt.uid);
    assertEquals(100l, atmpt.wager);
    assertEquals(Math.round(Math.ceil(atmpt.wager * atmpt.multiplier)), atmpt.payout);
  }

  private static long rid1 = 10l, rid2 = 11l;
  private static long uid1 = 11l, uid2 = 101l;
  private static SlotReelImage[] images1 = new SlotReelImage[] {SE,SE,SE}, images2 = new SlotReelImage[] {CH,CH,FI};
  private static long wager1 = 10l, wager2 = 20l;
  private static long payout1 = 70l, payout2 = 24l;
  private static double multiplier1 = 7.0, multiplier2 = 1.2;
  private static Timestamp time1 = defTime, time2 = defTime2;
  private static SlotRecord matchWith = new SlotRecord(rid1, uid1, images1, wager1, payout1, multiplier1, time1);
  private static Stream<Arguments> equalsProvider() {
    return Stream.of(
        Arguments.of(new SlotRecord(rid1, uid1, images1, wager1, payout1, multiplier1, time1), true),
        Arguments.of(new SlotRecord(rid2, uid1, images1, wager1, payout1, multiplier1, time1), false),
        Arguments.of(new SlotRecord(rid1, uid2, images1, wager1, payout1, multiplier1, time1), false),
        Arguments.of(new SlotRecord(rid1, uid1, images2, wager1, payout1, multiplier1, time1), false),
        Arguments.of(new SlotRecord(rid1, uid1, images1, wager2, payout1, multiplier1, time1), false),
        Arguments.of(new SlotRecord(rid1, uid1, images1, wager1, payout2, multiplier1, time1), false),
        Arguments.of(new SlotRecord(rid1, uid1, images1, wager1, payout1, multiplier2, time1), false),
        Arguments.of(new SlotRecord(rid1, uid1, images1, wager1, payout1, multiplier1, time2), false)
      );
  }
  @ParameterizedTest(name = "== {0}? {1}")
  @MethodSource("equalsProvider")
  public void testEquals(SlotRecord b, boolean matches) throws Exception {
    assertTrue(matchWith.equals(b) == matches, String.format("a %s should %s b %s", matchWith.toString(), matches?"==":"!=", b.toString()));
  }

  @Test
  public void testSlotAttemptInsufficientFunds() throws Exception {
    SlotRecord.setSeed(130l);
    long oldBalance = 100l;
    BankAccount acct = new BankAccount(101l, "auser", oldBalance, LocalTimestamp.now());
    assertThrows(InsufficientFundsException.class, () -> {
      SlotRecord atmpt = SlotRecord.slotAttempt(acct, oldBalance + 1l);
    });
  }

  @Test
  public void testSlotAttemptBalanceChange() throws Exception {
    SlotRecord.setSeed(160l);
    long oldBalance = 100l;
    BankAccount acct = new BankAccount(101l, "auser", oldBalance, LocalTimestamp.now());
    SlotRecord atmpt = SlotRecord.slotAttempt(acct, oldBalance/2);
    assertEquals(oldBalance - atmpt.wager + atmpt.payout, acct.balance);
  }

  @Test
  public void testObjectEquals() throws Exception {
    assertNotEquals(matchWith, new Object());
  }

  @Test
  public void testToString() throws Exception {
    assertTrue(matchWith.toString().length() > 0);
  }

  /*
   * Persistence and rollback tests
   */
  @Test
  public void testRecordSlotAttempt() throws Exception {
    long startFunds = 100l, wager = startFunds/2;
    SlotRecord.setSeed(990l);
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount("test", startFunds);
      SlotRecord record = SlotRecord.recordSlotAttempt(db, acct, wager);
      assertEquals(startFunds - wager + record.payout, acct.balance);
      BankAccount stored = db.getAccountExcept(acct.uid);
      assertEquals(stored, acct);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordSlotAttemptInsufficientFunds() throws Exception {
    long startFunds = 100l, wager = startFunds+2;
    SlotRecord.setSeed(910l);
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount("test", startFunds);
      assertThrows(InsufficientFundsException.class, () -> {
        SlotRecord record = SlotRecord.recordSlotAttempt(db, acct, wager);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordSlotAttemptImproperBalance() throws Exception {
    long startFunds = 100l, wager = -startFunds;
    SlotRecord.setSeed(920l);
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount("test", startFunds);
      assertThrows(ImproperBalanceAmount.class, () -> {
        SlotRecord record = SlotRecord.recordSlotAttempt(db, acct, wager);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordSlotAttemptNoAccount() throws Exception {
    long startFunds = 100l, wager = startFunds+2;
    SlotRecord.setSeed(930l);
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = new BankAccount(0l, "test", startFunds, LocalTimestamp.now());
      assertThrows(InsufficientFundsException.class, () -> {
        SlotRecord record = SlotRecord.recordSlotAttempt(db, acct, wager);
      });
    } finally {
      conn.rollback();
    }
  }
}

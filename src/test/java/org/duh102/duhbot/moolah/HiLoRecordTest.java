package org.duh102.duhbot.moolah;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.function.IntPredicate;
import java.util.stream.Stream;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class HiLoRecordTest {
  static final int MIN = HiLoRecord.MIN, MID = HiLoRecord.MID, MAX = HiLoRecord.MAX;
  static HiLoBetType HI = HiLoBetType.HIGH, LO = HiLoBetType.LOW, EQ = HiLoBetType.EQUAL;
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

  //TODO: might want to seed the random to reduce potential test thrash
  private static Stream<Arguments> hiLoBetProvider() {
    return Stream.of(
        Arguments.of(HI),
        Arguments.of(EQ),
        Arguments.of(LO)
      );
  }
  @ParameterizedTest(name = "Bet on {0}")
  @MethodSource("hiLoBetProvider")
  public void testBetHiLo(HiLoBetType type) throws Exception {
    boolean won = false, lost = false;
    long wager = 1l;
    long bal = 10l;
    Timestamp tempTime = LocalTimestamp.now(), now;
    HiLoRecord temp = null;
    BankAccount acct = new BankAccount(101l, "a username", bal, tempTime);
    int iterCount = 0, iterLimit = 10000;
    while( ! (won && lost) ) {
      assertTrue(iterCount < iterLimit, String.format("Something's up, not both win (%b) and loss (%b) in %d iterations", won, lost, iterLimit));
      acct.balance = bal;
      now = LocalTimestamp.now();
      temp = HiLoRecord.betHiLo(acct, type, wager);
      assertNotEquals(temp, null, "HiLoRecord result was null");
      assertNotEquals(temp.timestamp, null, "HiLoRecord timestamp was null");
      assertTrue(Math.abs(temp.timestamp.getTime() - now.getTime()) < 1000, String.format("Timestamps too far apart; expected a few ms between %s and %s", LocalTimestamp.format(now), LocalTimestamp.format(temp.timestamp)));
      assertEquals(temp.wager, wager, String.format("Wager %d incorrectly recorded as %d", wager, temp.wager));
      assertNotEquals(temp.hiLo, null, "HiLoRecord HiLoBetType null");
      assertEquals(temp.uid, acct.uid, "HiLoRecord uid does not match BankAccount");
      assertTrue( temp.payout >= 0l, String.format("Payout should never be negative, got %d", temp.payout) );
      if( temp.payout > 0l ) {
        won = true;
        assertTrue(type.getSatisfied(temp.resultInt), String.format("Result test failed result %d even though payout > 0", temp.resultInt));
        assertEquals(temp.payout, Math.round(1l * temp.multiplier), String.format("Payout %d != wager %d * multiplier %.2f", temp.payout, wager, temp.multiplier));
      } else {
        lost = true;
        assertFalse(type.getSatisfied(temp.resultInt), String.format("Result test succeeded result %d even though payout == 0", temp.resultInt));
        assertEquals(temp.multiplier, 0.0, "Multiplier > 0 though payout == 0");
      }
      iterCount++;
    }
  }

  private static long rid1 = 10l, rid2 = 11l;
  private static long uid1 = 11l, uid2 = 101l;
  private static int result1 = 1, result2 = 2;
  private static HiLoBetType bet1 = HiLoBetType.LOW, bet2 = HiLoBetType.HIGH;
  private static long wager1 = 1l, wager2 = 20l;
  private static long payout1 = 2l, payout2 = 0l;
  private static double mult1 = 2.0, mult2 = 0.0;
  private static Timestamp time1 = defTime, time2 = defTime2;
  private static HiLoRecord matchWith = new HiLoRecord(rid1, uid1, result1, bet1, wager1, payout1, mult1, time1);
  private static Stream<Arguments> equalsProvider() {
    return Stream.of(
        Arguments.of(new HiLoRecord(rid1, uid1, result1, bet1, wager1, payout1, mult1, time1), true),
        Arguments.of(new HiLoRecord(rid2, uid1, result1, bet1, wager1, payout1, mult1, time1), false),
        Arguments.of(new HiLoRecord(rid1, uid2, result1, bet1, wager1, payout1, mult1, time1), false),
        Arguments.of(new HiLoRecord(rid1, uid1, result2, bet1, wager1, payout1, mult1, time1), false),
        Arguments.of(new HiLoRecord(rid1, uid1, result1, bet2, wager1, payout1, mult1, time1), false),
        Arguments.of(new HiLoRecord(rid1, uid1, result1, bet1, wager2, payout1, mult1, time1), false),
        Arguments.of(new HiLoRecord(rid1, uid1, result1, bet1, wager1, payout2, mult1, time1), false),
        Arguments.of(new HiLoRecord(rid1, uid1, result1, bet1, wager1, payout1, mult2, time1), false),
        Arguments.of(new HiLoRecord(rid1, uid1, result1, bet1, wager1, payout1, mult1, time2), false)
      );
  }
  @ParameterizedTest(name = "== {0}? {1}")
  @MethodSource("equalsProvider")
  public void testEquals(HiLoRecord b, boolean matches) throws Exception {
    assertTrue(matchWith.equals(b) == matches, String.format("a %s should %s b %s", matchWith.toString(), matches?"==":"!=", b.toString()));
  }
}

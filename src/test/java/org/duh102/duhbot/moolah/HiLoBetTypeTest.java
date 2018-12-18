package org.duh102.duhbot.moolah;

import java.util.stream.Stream;
import java.util.stream.IntStream;

import org.duh102.duhbot.moolah.db.HiLoBetType;
import org.duh102.duhbot.moolah.db.HiLoRecord;
import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HiLoBetTypeTest {
  static final int MIN = HiLoRecord.MIN, MID = HiLoRecord.MID, MAX = HiLoRecord.MAX;
  static HiLoBetType HI = HiLoBetType.HIGH, LO = HiLoBetType.LOW, EQ = HiLoBetType.EQUAL;

  /*
   * Providers
   */
  private static Stream<Arguments> satisfactionProvider() {
    return Stream.of(
        IntStream.range(MIN, MAX).mapToObj(i -> Arguments.of(EQ, i, i == MID, "==")),
        IntStream.range(MIN, MAX).mapToObj(i -> Arguments.of(LO, i, i <  MID, "<")),
        IntStream.range(MIN, MAX).mapToObj(i -> Arguments.of(HI, i, i >  MID, ">"))
      ).flatMap(i -> i);
  }
  private static Stream<Arguments> fromStringProvider() {
    return Stream.of(
        Arguments.of(HI, "high", true),
        Arguments.of(HI, "hig", true),
        Arguments.of(HI, "hi", true),
        Arguments.of(HI, "h", true),
        Arguments.of(HI, "a", false),
        Arguments.of(HI, "higha", false),
        Arguments.of(HI, "horp", false),
        Arguments.of(LO, "low", true),
        Arguments.of(LO, "lo", true),
        Arguments.of(LO, "l", true),
        Arguments.of(EQ, "equal", true),
        Arguments.of(EQ, "equa", true),
        Arguments.of(EQ, "equ", true),
        Arguments.of(EQ, "eq", true),
        Arguments.of(EQ, "e", true)
      );
  }

  /*
   * Satisfaction of the various bets
   */
  @ParameterizedTest(name = "{1} {0} mid? {2}")
  @MethodSource("satisfactionProvider")
  public void testSatisfaction(HiLoBetType betType, int inp, boolean shouldSatisfy, String operator) throws Exception {
    assertTrue(betType.getSatisfied(inp) == shouldSatisfy, String.format("%s %s be satisfied by %d %s %d", betType.toString(), shouldSatisfy?"should":"should not", inp, operator, MID));
  }

  /*
   * fromString parsing
   */
  @ParameterizedTest(name = "{1} => {0}? {2}")
  @MethodSource("fromStringProvider")
  public void testFromString(HiLoBetType betType, String inp, boolean shouldSucceed) throws Exception {
    if( shouldSucceed ) {
      HiLoBetType parsedTo = HiLoBetType.fromString(inp);
      assertEquals(betType, parsedTo, String.format("%s should parse to %s, parsed to %s", inp, betType.toString(), parsedTo.toString()));
    } else {
      assertThrows(InvalidInputError.class, () -> {
        HiLoBetType.fromString(inp);
      });
    }
  }
}

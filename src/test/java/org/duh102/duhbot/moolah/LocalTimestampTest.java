package org.duh102.duhbot.moolah;

import java.sql.Timestamp;
import java.util.regex.*;
import java.text.ParseException;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LocalTimestampTest {

  @Test
  public void testParseGoodUnzonedFormat() throws Exception {
    String input = "2018-01-01 01:01:01.000";
    Timestamp a=  LocalTimestamp.parse(input);
  }
  @Test
  public void testParseGoodZonedFormat() throws Exception {
    String input = "2018-01-01 01:01:01.000-0400";
    Timestamp a = LocalTimestamp.parse(input);
  }
  @Test
  public void testParseBadUnzonedFormat() throws Exception {
    String input = "2018-01-01 01:01:01";
    assertThrows(ParseException.class, () -> {
      Timestamp a = LocalTimestamp.parse(input);
    });
  }
  @Test
  public void testParseBadZonedFormat() throws Exception {
    String input = "2018-01-01 01:01:01-0400";
    assertThrows(ParseException.class, () -> {
      Timestamp a = LocalTimestamp.parse(input);
    });
  }
  @Test
  public void testAssumeLocalTZ() throws Exception {
    Timestamp now = LocalTimestamp.now();
    //remove the time zone part of the text
    Pattern timezoneRemove = Pattern.compile("[\\+-][0-9]{4}$");
    String in1 = LocalTimestamp.format(now);
    String in2 = in1;
    Matcher match = timezoneRemove.matcher(in1);
    in1 = match.replaceAll("");
    assertNotEquals(in1, in2, String.format("Pattern matching should remove time zone, input was '%s' and output was '%s'", in2, in1));
    Timestamp nowPNZ = LocalTimestamp.parse(in1);
    Timestamp nowPZ = LocalTimestamp.parse(in2);
    assertEquals(nowPNZ, nowPZ, String.format("Timestamp strings '%s' and '%s' should parse to the same timestamp (got '%s' and '%s')", in1, in2, LocalTimestamp.format(nowPNZ), LocalTimestamp.format(nowPZ)));
  }
}

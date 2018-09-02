package org.duh102.duhbot.moolah;

import java.sql.Timestamp;
import java.sql.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class LocalTimestamp {
  public static SimpleDateFormat noZonePattern, zonePattern;
  static {
    // SQLite doesn't have time zones so we assume it's the current time zone
    noZonePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    // We want to have time zones, and RFC time zones are nice (-0800 etc)
    zonePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'T'Z");
    noZonePattern.setTimeZone(TimeZone.getDefault());
    zonePattern.setTimeZone(TimeZone.getDefault());
  }
  public static Timestamp currentTimestamp() {
    return new Timestamp(System.currentTimeMillis());
  }
  // When parsing timestamps we return a local-zone timestamp, as timestamps do not have knowledge of zones
  // However, we respect the zone given in the original input (if it exists), we just immediately convert it
  public static Timestamp tryParseDate(String input) throws ParseException {
    try {
      return new Timestamp(zonePattern.parse(input).getTime());
    } catch( ParseException pe ) {
      return new Timestamp(noZonePattern.parse(input).getTime());
    }
  }
  public static String formatTimestamp(Timestamp input) {
    return zonePattern.format(input);
  }
}

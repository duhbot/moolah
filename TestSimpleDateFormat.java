import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.text.ParseException;

public class TestSimpleDateFormat {
  public static SimpleDateFormat noZonePattern, zonePattern;
  static {
    noZonePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    zonePattern = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'T'Z");
    noZonePattern.setTimeZone(TimeZone.getDefault());
    zonePattern.setTimeZone(TimeZone.getDefault());
  }
  public static Timestamp tryParseDate(String input) {
    try {
      return new Timestamp(zonePattern.parse(input).getTime());
    } catch( ParseException pe ) {
      try {
        System.out.printf("Could not parse time with zoned pattern, trying with unzoned pattern: %s\n", input);
        return new Timestamp(noZonePattern.parse(input).getTime());
      } catch( ParseException pe2 ) {
        pe2.printStackTrace();
        return null;
      }
    }
  }
  public static void main(String args[]) {
    String nozone = "2018-10-12 10:24:01.0123";
    String zone = "2018-10-12 10:24:01.0123T-0400";
    String nonLocalZone = "2018-10-12 11:24:01.0123T-0300";
    Timestamp a = null, b = null, c = null;
    a = tryParseDate(nozone);
    b = tryParseDate(zone);
    c = tryParseDate(nonLocalZone);
    System.out.printf("a %s, b %s, c %s, equal? %b\n", zonePattern.format(a), zonePattern.format(b), zonePattern.format(c), (a!=null)?a.equals(b) && a.equals(c):false );
  }
}

package org.duh102.duhbot.moolah;

import java.sql.*;
import java.text.ParseException;
import java.util.stream.Stream;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MineRecordTest {
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

  @Test
  public void testConstructor() throws Exception {
    MineRecord a = new MineRecord(0l, 0l, 0, 0.0, 0, LocalTimestamp.now());
  }
  @Test
  public void testMineAttempt() throws Exception {
    Mine.setSeed(101l);
    Timestamp now = LocalTimestamp.now();
    BankAccount acct = new BankAccount(101l, "user", 0l, new Timestamp(0l));
    MineRecord record = MineRecord.mineAttempt(acct);
    assertEquals(acct.uid, record.uid);
    assertEquals(record.mineFractions, MineRecord.MAX_CHUNKS);
    assertTrue(record.richness >= 1.0);
    assertEquals(record.yield, Math.round(record.richness * record.mineFractions));
    assertTrue(record.timestamp.compareTo(now) >= 0);
    assertTrue(acct.lastMined.compareTo(now) >= 0);
  }
  @Test
  public void testMineAttemptException() throws Exception {
    Mine.setSeed(102l);
    BankAccount acct = new BankAccount(101l, "user", 0l, LocalTimestamp.now());
    assertThrows(MineAttemptTooSoon.class, () -> {
      MineRecord record = MineRecord.mineAttempt(acct);
    });
  }
  @Test
  public void testMineAttemptChunks() throws Exception {
    Mine.setSeed(105l);
    Timestamp now = LocalTimestamp.now();
    //3*chunk time (plus some wiggle room)
    Timestamp threeChunksAgo = new Timestamp(System.currentTimeMillis() - (3*MineRecord.MINE_CHUNK_LENGTH + 10));
    BankAccount acct = new BankAccount(101l, "user", 0l, threeChunksAgo);
    MineRecord record = MineRecord.mineAttempt(acct);
    assertEquals(acct.uid, record.uid);
    assertEquals(record.mineFractions, 3);
    assertTrue(record.richness >= 1.0);
    assertEquals(record.yield, Math.round(record.richness * record.mineFractions));
    assertTrue(acct.lastMined.compareTo(now) >= 0);
  }
  @Test
  public void testMineAttemptBalanceChange() throws Exception {
    Mine.setSeed(120l);
    Timestamp now = LocalTimestamp.now();
    //3*chunk time (plus some wiggle room)
    Timestamp threeChunksAgo = new Timestamp(System.currentTimeMillis() - (3*MineRecord.MINE_CHUNK_LENGTH + 10));
    long beforeBalance = 100l;
    BankAccount acct = new BankAccount(101l, "user", beforeBalance, threeChunksAgo);
    MineRecord record = MineRecord.mineAttempt(acct);
    assertEquals(beforeBalance + record.yield, acct.balance);
    assertTrue(acct.lastMined.compareTo(now) >= 0);
  }

  private static long rid1 = 10l, rid2 = 11l;
  private static long uid1 = 11l, uid2 = 101l;
  private static int chunks1 = 1, chunks2 = 2;
  private static double richness1 = 1.0, richness2 = 2.0;
  private static long yield1 = 1l, yield2 = 4l;
  private static Timestamp time1 = defTime, time2 = defTime2;
  private static MineRecord matchWith = new MineRecord(rid1, uid1, chunks1, richness1, yield1, time1);
  private static Stream<Arguments> equalsProvider() {
    return Stream.of(
        Arguments.of(new MineRecord(rid1, uid1, chunks1, richness1, yield1, time1), true),
        Arguments.of(new MineRecord(rid2, uid1, chunks1, richness1, yield1, time1), false),
        Arguments.of(new MineRecord(rid1, uid2, chunks1, richness1, yield1, time1), false),
        Arguments.of(new MineRecord(rid1, uid1, chunks2, richness1, yield1, time1), false),
        Arguments.of(new MineRecord(rid1, uid1, chunks1, richness2, yield1, time1), false),
        Arguments.of(new MineRecord(rid1, uid1, chunks1, richness1, yield2, time1), false),
        Arguments.of(new MineRecord(rid1, uid1, chunks1, richness1, yield1, time2), false)
      );
  }
  @ParameterizedTest(name = "== {0}? {1}")
  @MethodSource("equalsProvider")
  public void testEquals(MineRecord b, boolean matches) throws Exception {
    assertTrue(matchWith.equals(b) == matches, String.format("a %s should %s b %s", matchWith.toString(), matches?"==":"!=", b.toString()));
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
  public void testRecordMineAttempt() throws Exception {
    long startFunds = 100l;
    Mine.setSeed(990l);
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    Timestamp now = LocalTimestamp.now();
    try {
      BankAccount acct = db.openAccount("test", startFunds);
      MineRecord record = MineRecord.recordMineAttempt(db, acct);
      assertEquals(startFunds + record.yield, acct.balance);
      BankAccount stored = db.getAccountExcept(acct.uid);
      assertEquals(stored, acct);
      assertTrue(stored.lastMined.getTime() - now.getTime() <= 1000);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordMineAttemptTooSoon() throws Exception {
    Mine.setSeed(991l);
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount("test");
      MineRecord record = MineRecord.recordMineAttempt(db, acct);
      assertThrows(MineAttemptTooSoon.class, () -> {
        MineRecord record2 = MineRecord.recordMineAttempt(db, acct);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordMineAttemptNoAcct() throws Exception {
    Mine.setSeed(992l);
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = new BankAccount(0l, "auser", 0l, new Timestamp(System.currentTimeMillis()-1000*60*60*3));
      assertThrows(AccountDoesNotExist.class, () -> {
        MineRecord record = MineRecord.recordMineAttempt(db, acct);
      });
    } finally {
      conn.rollback();
    }
  }
}

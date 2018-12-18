package org.duh102.duhbot.moolah;

import java.sql.*;
import java.text.ParseException;
import java.util.stream.Stream;

import org.duh102.duhbot.moolah.db.BankDB;
import org.duh102.duhbot.moolah.db.TransferRecord;
import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TransferRecordTest {
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
    TransferRecord a = new TransferRecord(0l, 0l, 0l, 0, LocalTimestamp.now());
  }

  @Test
  public void testMakeTransferInsufficientFunds() throws Exception {
    long oldBalance = 100l;
    BankAccount source = new BankAccount(101l, "auser", oldBalance, LocalTimestamp.now());
    BankAccount dest = new BankAccount(102l, "auser", 0l, LocalTimestamp.now());
    assertThrows(InsufficientFundsException.class, () -> {
      TransferRecord.makeTransfer(source, dest, oldBalance+1l);
    });
  }

  @Test
  public void testMakeTransferSameAccount() throws Exception {
    long oldBalance = 100l;
    BankAccount source = new BankAccount(101l, "auser", oldBalance, LocalTimestamp.now());
    assertThrows(SameAccountException.class, () -> {
      TransferRecord.makeTransfer(source, source, oldBalance);
    });
  }

  @Test
  public void testMakeTransferBalanceChange() throws Exception {
    long oldBalance = 100l, transferAmount = oldBalance / 2;
    long oldDestBalance = 0l;
    Timestamp now = LocalTimestamp.now();
    BankAccount source = new BankAccount(101l, "auser", oldBalance, LocalTimestamp.now());
    BankAccount dest = new BankAccount(102l, "auser", oldDestBalance, LocalTimestamp.now());
    TransferRecord record = TransferRecord.makeTransfer(source, dest, transferAmount);
    assertEquals(oldBalance - transferAmount, source.balance);
    assertEquals(oldDestBalance + transferAmount, dest.balance);
    assertEquals(transferAmount, record.amount);
    assertTrue(record.timestamp.compareTo(now) >= 0);
  }

  private static long rid1 = 10l, rid2 = 11l;
  private static long suid1 = 11l, suid2 = 101l;
  private static long duid1 = 12l, duid2 = 102l;
  private static long amount1 = 1l, amount2 = 4l;
  private static Timestamp time1 = defTime, time2 = defTime2;
  private static TransferRecord matchWith = new TransferRecord(rid1, suid1, duid1, amount1, time1);
  private static Stream<Arguments> equalsProvider() {
    return Stream.of(
        Arguments.of(new TransferRecord(rid1, suid1, duid1, amount1, time1), true),
        Arguments.of(new TransferRecord(rid2, suid1, duid1, amount1, time1), false),
        Arguments.of(new TransferRecord(rid1, suid2, duid1, amount1, time1), false),
        Arguments.of(new TransferRecord(rid1, suid1, duid2, amount1, time1), false),
        Arguments.of(new TransferRecord(rid1, suid1, duid1, amount2, time1), false),
        Arguments.of(new TransferRecord(rid1, suid1, duid1, amount1, time2), false)
      );
  }
  @ParameterizedTest(name = "== {0}? {1}")
  @MethodSource("equalsProvider")
  public void testEquals(TransferRecord b, boolean matches) throws Exception {
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
  public void testRecordTransfer() throws Exception {
    long sourceFunds = 1000l, destFunds = 100l, transferAmount = 50l;
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount("auser", sourceFunds);
      BankAccount dest = db.openAccount("buser", destFunds);
      TransferRecord record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      assertEquals(sourceFunds - transferAmount, source.balance);
      assertEquals(destFunds + transferAmount, dest.balance);
      BankAccount sourceStored = db.getAccountExcept(source.uid);
      BankAccount destStored = db.getAccountExcept(dest.uid);
      assertEquals(source, sourceStored);
      assertEquals(dest, destStored);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordTransferInsufficientFunds() throws Exception {
    long sourceFunds = 1000l, destFunds = 100l, transferAmount = sourceFunds+2l;
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount("auser", sourceFunds);
      BankAccount dest = db.openAccount("buser", destFunds);
      assertThrows(InsufficientFundsException.class, () -> {
        TransferRecord record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordTransferImproperBalance() throws Exception {
    long sourceFunds = 1000l, destFunds = 100l, transferAmount = -1l;
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount("auser", sourceFunds);
      BankAccount dest = db.openAccount("buser", destFunds);
      assertThrows(ImproperBalanceAmount.class, () -> {
        TransferRecord record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordTransferSameAccount() throws Exception {
    long sourceFunds = 1000l, transferAmount = 50l;
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount("auser", sourceFunds);
      assertThrows(SameAccountException.class, () -> {
        TransferRecord record = TransferRecord.recordTransfer(db, source, source, transferAmount);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordTransferSourceNotAccount() throws Exception {
    long sourceFunds = 1000l, destFunds = 100l, transferAmount = 50l;
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = new BankAccount(0l, "auser", sourceFunds, LocalTimestamp.now());
      BankAccount dest = db.openAccount("buser", destFunds);
      assertThrows(AccountDoesNotExist.class, () -> {
        TransferRecord record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordTransferDestNotAccount() throws Exception {
    long sourceFunds = 1000l, destFunds = 100l, transferAmount = 50l;
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = db.openAccount("auser", sourceFunds);
      BankAccount dest = new BankAccount(0l, "buser", destFunds, LocalTimestamp.now());
      assertThrows(AccountDoesNotExist.class, () -> {
        TransferRecord record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testRecordTransferBothNotAccount() throws Exception {
    long sourceFunds = 1000l, destFunds = 100l, transferAmount = 50l;
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount source = new BankAccount(-1l, "auser", sourceFunds, LocalTimestamp.now());
      BankAccount dest = new BankAccount(-2l, "buser", destFunds, LocalTimestamp.now());
      assertThrows(AccountDoesNotExist.class, () -> {
        TransferRecord record = TransferRecord.recordTransfer(db, source, dest, transferAmount);
      });
    } finally {
      conn.rollback();
    }
  }
}

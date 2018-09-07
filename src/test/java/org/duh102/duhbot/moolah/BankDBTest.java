package org.duh102.duhbot.moolah;

import java.sql.*;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BankDBTest {
  // Basic functionality (connecting/making tables)
  @Test
  public void testGetInstance() throws Exception {
    BankDB memoryDB = BankDB.getMemoryInstance();
  }
  @Test
  public void testGetInstanceStatic() throws Exception {
    BankDB memoryDB = BankDB.getMemoryInstance();
    BankDB memoryDB2 = BankDB.getMemoryInstance();
    assertSame(memoryDB, memoryDB2);
  }
  // Account
  @Test
  public void testMakeAccount() throws Exception {
    long startingBalance = 0l;
    String acctName = "test";
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(acctName);
      assertEquals(startingBalance, acct.balance);
      assertEquals(acctName, acct.user);
      Timestamp now = LocalTimestamp.now();
      long timeDiff = now.getTime() - acct.lastMined.getTime();
      assertTrue(timeDiff >= 24*60*60*1000, String.format("Time less than 24 hours back ( %,d < %,d ) (lastMined is %s, now is %s)", timeDiff, 24*60*60*1000, LocalTimestamp.format(acct.lastMined), LocalTimestamp.format(now)));
    } finally {
      // do not persist test changes (even though this is an in-memory DB, always good to clean up)
      conn.rollback();
    }
  }
  @Test
  public void testMakeAccountStartingBalance() throws Exception {
    long startingBalance = 100l;
    String acctName = "test";
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount(acctName,  startingBalance);
      assertEquals(startingBalance, acct.balance);
      assertEquals(acctName, acct.user);
      Timestamp now = LocalTimestamp.now();
      long timeDiff = now.getTime() - acct.lastMined.getTime();
      assertTrue(timeDiff >= 24*60*60*1000, String.format("Time less than 24 hours back ( %,d < %,d ) (lastMined is %s, now is %s)", timeDiff, 24*60*60*1000, LocalTimestamp.format(acct.lastMined), LocalTimestamp.format(now)));
    } finally {
      // do not persist test changes (even though this is an in-memory DB, always good to clean up)
      conn.rollback();
    }
  }
  @Test
  public void testMakeDuplicateAccount() throws Exception {
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      db.openAccount("test");
      assertThrows(AccountAlreadyExists.class, () -> {
        db.openAccount("test");
      });
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testGetAccount() throws Exception {
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acctE = db.getAccount("test");
      assertTrue(acctE == null);
      BankAccount acct = db.openAccount("test");
      acctE = db.getAccount("test");
      assertEquals(acct, acctE);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testGetAccountExcept() throws Exception {
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      assertThrows(AccountDoesNotExist.class, () -> {
        db.getAccountExcept("test");
      });
      BankAccount acct = db.openAccount("test");
      BankAccount acctE = db.getAccountExcept("test");
      assertEquals(acct, acctE);
    } finally {
      conn.rollback();
    }
  }
  @Test
  public void testPushAccount() throws Exception {
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount("test", 100l);
      db.pushAccount(acct);
      BankAccount newAcct = db.getAccountExcept(acct.uid);
      assertEquals(acct, newAcct);
    } finally {
      conn.rollback();
    }
  }
}

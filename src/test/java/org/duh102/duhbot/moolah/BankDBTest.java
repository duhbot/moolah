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
    BankDB db = BankDB.getMemoryInstance();
    Connection conn = db.getDBConnection();
    conn.setAutoCommit(false);
    try {
      BankAccount acct = db.openAccount("test");
      assertEquals(acct.balance, 0l);
      assertEquals(acct.user, "test");
      long now = System.currentTimeMillis()/1000;
      long timeDiff = now - acct.lastMined;
      assertTrue(timeDiff >= 24*60*60, String.format("Time less than 24 hours back ( %,d < %,d )", timeDiff, 24*60*60));
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
      BankAccount acct = db.openAccount("test");
      BankAccount acctE = db.getAccount("test");
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
}

package org.duh102.duhbot.moolah.db;

import java.math.BigInteger;
import java.sql.*;

import org.duh102.duhbot.moolah.BankAccount;
import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.db.dao.BankAccountDAO;
import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankDBTest {
  BankDB db;
  Connection conn;
  @BeforeEach
  public void setUp() throws Exception {
    db = BankDB.getMemoryInstance();
    conn = db.getDBConnection();
    conn.setAutoCommit(false);
  }
  @AfterEach
  public void tearDown() throws Exception {
    conn.rollback();
  }
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
  @Test
  public void testMigrate() throws Exception {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("select * from bankAccount;");
    int size = 0;
    while(rs.next()) {
      size++;
    }
    assertEquals(0, size, "ResultSet should be empty");
  }
  // Account
  @Test
  public void testMakeAccount() throws Exception {
    long startingBalance = 0l;
    String acctName = "test";

    BankAccountDAO accountDAO = new BankAccountDAO(db);
    BankAccount acct = accountDAO.openAccount(acctName);
    assertEquals(startingBalance, acct.balance);
    assertEquals(acctName, acct.user);
    Timestamp now = LocalTimestamp.now();
    long timeDiff = now.getTime() - acct.lastMined.getTime();
    assertTrue(timeDiff >= 24*60*60*1000, String.format("Time less than 24 hours back ( %,d < %,d ) (lastMined is %s, now is %s)", timeDiff, 24*60*60*1000, LocalTimestamp.format(acct.lastMined), LocalTimestamp.format(now)));

  }
  @Test
  public void testMakeAccountStartingBalance() throws Exception {
    long startingBalance = 100l;
    String acctName = "test";

    BankAccountDAO accountDAO = new BankAccountDAO(db);
    BankAccount acct = accountDAO.openAccount(acctName,  startingBalance);
    assertEquals(startingBalance, acct.balance);
    assertEquals(acctName, acct.user);
    Timestamp now = LocalTimestamp.now();
    long timeDiff = now.getTime() - acct.lastMined.getTime();
    assertTrue(timeDiff >= 24*60*60*1000, String.format("Time less than 24 hours back ( %,d < %,d ) (lastMined is %s, now is %s)", timeDiff, 24*60*60*1000, LocalTimestamp.format(acct.lastMined), LocalTimestamp.format(now)));
  }
  @Test
  public void testMakeDuplicateAccount() throws Exception {
    BankAccountDAO accountDAO = new BankAccountDAO(db);
    accountDAO.openAccount("test");
    assertThrows(AccountAlreadyExists.class, () -> {
      accountDAO.openAccount("test");
    });
  }
  @Test
  public void testGetAccount() throws Exception {
    BankAccountDAO accountDAO = new BankAccountDAO(db);
    BankAccount acctE = accountDAO.getAccount("test");
    assertTrue(acctE == null);
    BankAccount acct = accountDAO.openAccount("test");
    acctE = accountDAO.getAccount("test");
    assertEquals(acct, acctE);
  }
  @Test
  public void testGetAccountExcept() throws Exception {
    BankAccountDAO accountDAO = new BankAccountDAO(db);
    assertThrows(AccountDoesNotExist.class, () -> {
      accountDAO.getAccountExcept("test");
    });
    BankAccount acct = accountDAO.openAccount("test");
    BankAccount acctE = accountDAO.getAccountExcept("test");
    assertEquals(acct, acctE);
  }
  @Test
  public void testPushAccount() throws Exception {
    BankAccountDAO accountDAO = new BankAccountDAO(db);
    BankAccount acct = accountDAO.openAccount("test", 100l);
    accountDAO.pushAccount(acct);
    BankAccount newAcct = accountDAO.getAccountExcept(acct.uid);
    assertEquals(acct, newAcct);
  }
}

package org.duh102.duhbot.moolah;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BankAccountTest {
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
  /*
   * Constructor tests
   */
  @Test
  public void testConstructorEasy() throws Exception {
    BankAccount a = new BankAccount(0l, "user", 100l, defTime);
    assertEquals(a.balance, 100l);
  }
  @Test
  public void testConstructorNegativeBal() throws Exception {
    assertThrows(ImproperBalanceAmount.class, () -> {
        BankAccount a = new BankAccount(0l, "user", -100l, defTime);
    });
  }

  /*
   * Adding funds tests
   */
  @Test
  public void testAddEasy() throws Exception {
    BankAccount a = new BankAccount(0l, "user", 0l, defTime);
    a.addFunds(100l);
    assertEquals(a.balance, 100l);
  }

  @Test
  public void testAddTypeCastBal() throws Exception {
    BankAccount a = new BankAccount(0l, "user", 0l, defTime);
    a.addFunds(100);
    assertEquals(a.balance, 100l);
  }

  @Test
  public void testAddNegativeBal() throws Exception {
    BankAccount a = new BankAccount(0l, "User", 0l, defTime);
    assertThrows(ImproperBalanceAmount.class, () -> {
        a.addFunds(-1l);
    });
  }

  /*
   * Subtracting funds tests
   */

  @Test
  public void testSubEasy() throws Exception {
    BankAccount a = new BankAccount(0l, "user", 100l, defTime);
    a.subFunds(50l);
    assertEquals(a.balance, 50l);
  }

  @Test
  public void testSubInsufficientEasy() throws Exception {
    BankAccount a = new BankAccount(0l, "user", 50l, defTime);
    assertThrows(InsufficientFundsException.class, () -> {
        a.subFunds(51l);
    });
  }

  @Test
  public void testSubTypeCastBal() throws Exception {
    BankAccount a = new BankAccount(0l, "user", 100l, defTime);
    a.subFunds(50);
    assertEquals(a.balance, 50l);
  }

  @Test
  public void testSubNegativeBal() throws Exception {
    BankAccount a = new BankAccount(0l, "User", 1000l, defTime);
    assertThrows(ImproperBalanceAmount.class, () -> {
        a.subFunds(-1l);
    });
  }

  /*
   * BigInt balance tests
   */

  @Test
  public void testBigIntBalance() throws Exception {
    BankAccount a = new BankAccount(0l, "user", new BigInteger("100"), defTime);
    a.addFunds(1000);
    assertEquals(new BigInteger("1100"), a.balance, "Balance should equal and " +
            "be a biginteger");
  }

  @Test
  public void testBigIntBalanceLong() throws Exception {
    BankAccount a = new BankAccount(0l, "user", new BigInteger(
            "1000000000000000000000000000000000000000000"), defTime);
    a.addFunds(new BigInteger("1000000000000000"));
    assertEquals(new BigInteger("1000000000000000000000000001000000000000000"), a.balance, "Balance should be equal and " +
            "be a value much larger than a long can handle");
  }

  /*
   * Object equality
   */
  @Test
  public void testEqualsYes() throws Exception {
    BankAccount a = new BankAccount(0l, "test", 0l, defTime);
    BankAccount b = new BankAccount(0l, "test", 0l, defTime);
    assertEquals(a, b, String.format("!( %d == %d && %d == %d && '%s' == '%s' && %s == %s )", a.uid, b.uid, a.balance, b.balance, a.user, b.user, LocalTimestamp.format(a.lastMined), LocalTimestamp.format(b.lastMined)) );
  }
  @Test
  public void testEqualsNoUID() throws Exception {
    BankAccount a = new BankAccount(0l, "test", 0l, defTime);
    BankAccount b = new BankAccount(1l, "test", 0l, defTime);
    assertNotEquals(a, b);
  }
  @Test
  public void testEqualsNoUser() throws Exception {
    BankAccount a = new BankAccount(0l, "test", 0l, defTime);
    BankAccount b = new BankAccount(0l, "test1", 0l, defTime);
    assertNotEquals(a, b);
  }
  @Test
  public void testEqualsNoBalance() throws Exception {
    BankAccount a = new BankAccount(0l, "test", 0l, defTime);
    BankAccount b = new BankAccount(0l, "test", 1l, defTime);
    assertNotEquals(a, b);
  }
  @Test
  public void testEqualsNoLastMined() throws Exception {
    BankAccount a = new BankAccount(0l, "test", 0l, defTime);
    BankAccount b = new BankAccount(0l, "test", 0l, defTime2);
    assertNotEquals(a, b);
  }

  /*
   * toString(), basic testing to ensure it outputs something
   */
  @Test
  public void testToString() throws Exception {
    String a = (new BankAccount(0l, "test", 0l, defTime)).toString();
    assertTrue(a != null);
    assertTrue(a.length() > 0);
  }
}

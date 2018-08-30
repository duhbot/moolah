package org.duh102.duhbot.moolah;

import org.duh102.duhbot.moolah.exceptions.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BankAccountTest {
  /*
   * Constructor tests
   */
  @Test
  public void testConstructorEasy() throws Exception {
    BankAccount a = new BankAccount("user", 100l);
    assertEquals(a.balance, 100l);
  }
  @Test
  public void testConstructorNegativeBal() throws Exception {
    assertThrows(ImproperBalanceAmount.class, () -> {
        BankAccount a = new BankAccount("user", -100l);
    });
  }

  /*
   * Adding funds tests
   */
  @Test
  public void testAddEasy() throws Exception {
    BankAccount a = new BankAccount("user", 0l);
    a.addFunds(100l);
    assertEquals(a.balance, 100l);
  }

  @Test
  public void testAddTypeCastBal() throws Exception {
    BankAccount a = new BankAccount("user", 0l);
    a.addFunds(100);
    assertEquals(a.balance, 100l);
  }

  @Test
  public void testAddNegativeBal() throws Exception {
    BankAccount a = new BankAccount("User", 0l);
    assertThrows(ImproperBalanceAmount.class, () -> {
        a.addFunds(-1l);
    });
  }

  /*
   * Subtracting funds tests
   */

  @Test
  public void testSubEasy() throws Exception {
    BankAccount a = new BankAccount("user", 100l);
    a.subFunds(50l);
    assertEquals(a.balance, 50l);
  }

  @Test
  public void testSubInsufficientEasy() throws Exception {
    BankAccount a = new BankAccount("user", 50l);
    assertThrows(InsufficientFundsException.class, () -> {
        a.subFunds(51l);
    });
  }

  @Test
  public void testSubTypeCastBal() throws Exception {
    BankAccount a = new BankAccount("user", 100l);
    a.subFunds(50);
    assertEquals(a.balance, 50l);
  }

  @Test
  public void testSubNegativeBal() throws Exception {
    BankAccount a = new BankAccount("User", 1000l);
    assertThrows(ImproperBalanceAmount.class, () -> {
        a.subFunds(-1l);
    });
  }
}

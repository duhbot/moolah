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
    BankAccount a = new BankAccount("user", 100l, (byte)26);
    assertEquals(a.balance, 100l);
    assertEquals(a.fracbalance, (byte)26);
  }
  @Test
  public void testConstructorNegativeFrac() throws Exception {
    assertThrows(ImproperFractionalAmount.class, () -> {
        BankAccount a = new BankAccount("user", 100l, (byte)-26);
    });
  }
  @Test
  public void testConstructorBigFrac() throws Exception {
    assertThrows(ImproperFractionalAmount.class, () -> {
        BankAccount a = new BankAccount("user", 100l, (byte)101);
    });
  }
  @Test
  public void testConstructorNegativeBal() throws Exception {
    assertThrows(ImproperBalanceAmount.class, () -> {
        BankAccount a = new BankAccount("user", -100l, (byte)26);
    });
  }

  /*
   * Adding funds tests
   */
  @Test
  public void testAddEasy() throws Exception {
    BankAccount a = new BankAccount("user", 0l, (byte)0);
    a.addFunds(100l, (byte)0);
    assertEquals(a.balance, 100l);
    assertEquals(a.fracbalance, (byte)0);
  }

  @Test
  public void testAddFractionalNoCarry() throws Exception {
    BankAccount a = new BankAccount("user", 0l, (byte)50);
    a.addFunds(0l, (byte)49);
    assertEquals(a.balance, 0l);
    assertEquals(a.fracbalance, (byte)99);
  }

  @Test
  public void testAddFractionalCarryZero() throws Exception {
    BankAccount a = new BankAccount("user", 0l, (byte)50);
    a.addFunds(0l, (byte)50);
    assertEquals(a.balance, 1l);
    assertEquals(a.fracbalance, (byte)0);
  }

  @Test
  public void testAddFractionalCarryOne() throws Exception {
    BankAccount a = new BankAccount("user", 0l, (byte)50);
    a.addFunds(0l, (byte)51);
    assertEquals(a.balance, 1l);
    assertEquals(a.fracbalance, (byte)1);
  }

  @Test
  public void testAddTypeCastBal() throws Exception {
    BankAccount a = new BankAccount("user", 0l, (byte)0);
    a.addFunds(100, (byte)0);
    assertEquals(a.balance, 100l);
    assertEquals(a.fracbalance, (byte)0);
  }

  @Test
  public void testAddTypeCastFrac() throws Exception {
    BankAccount a = new BankAccount("user", 0l, (byte)0);
    a.addFunds(100l, 0);
    assertEquals(a.balance, 100l);
    assertEquals(a.fracbalance, (byte)0);
  }

  @Test
  public void testAddTypeCastBoth() throws Exception {
    BankAccount a = new BankAccount("user", 0l, (byte)0);
    a.addFunds(100, 0);
    assertEquals(a.balance, 100l);
    assertEquals(a.fracbalance, (byte)0);
  }

  @Test
  public void testAddNegativeFrac() throws Exception {
    BankAccount a = new BankAccount("User", 0l, (byte)0);
    assertThrows(ImproperFractionalAmount.class, () -> {
        a.addFunds(0l, (byte)-1);
    });
  }

  @Test
  public void testAddBigFrac() throws Exception {
    BankAccount a = new BankAccount("User", 0l, (byte)0);
    assertThrows(ImproperFractionalAmount.class, () -> {
        a.addFunds(0l, (byte)101);
    });
  }

  @Test
  public void testAddBigCastFrac() throws Exception {
    BankAccount a = new BankAccount("User", 0l, (byte)0);
    assertThrows(ImproperFractionalAmount.class, () -> {
        a.addFunds(0l, 200);
    });
  }

  @Test
  public void testAddNegativeBal() throws Exception {
    BankAccount a = new BankAccount("User", 0l, (byte)0);
    assertThrows(ImproperBalanceAmount.class, () -> {
        a.addFunds(-1l, (byte)1);
    });
  }

  /*
   * Subtracting funds tests
   */

  @Test
  public void testSubEasy() throws Exception {
    BankAccount a = new BankAccount("user", 100l, (byte)0);
    a.subFunds(50l, (byte)0);
    assertEquals(a.balance, 50l);
    assertEquals(a.fracbalance, (byte)0);
  }

  @Test
  public void testSubInsufficientEasy() throws Exception {
    BankAccount a = new BankAccount("user", 50l, (byte)0);
    assertThrows(InsufficientFundsException.class, () -> {
        a.subFunds(51l, (byte)0);
    });
  }

  @Test
  public void testSubInsufficientCarry() throws Exception {
    BankAccount a = new BankAccount("user", 50l, (byte)0);
    assertThrows(InsufficientFundsException.class, () -> {
        a.subFunds(50l, (byte)1);
    });
  }

  @Test
  public void testSubCarry() throws Exception {
    BankAccount a = new BankAccount("user", 51l, (byte)0);
    a.subFunds(50l, (byte)1);
    assertEquals(a.balance, 0l);
    assertEquals(a.fracbalance, (byte)99);
  }

  @Test
  public void testSubTypeCastBal() throws Exception {
    BankAccount a = new BankAccount("user", 100l, (byte)0);
    a.subFunds(50, (byte)10);
    assertEquals(a.balance, 49l);
    assertEquals(a.fracbalance, (byte)90);
  }

  @Test
  public void testSubTypeCastFrac() throws Exception {
    BankAccount a = new BankAccount("user", 100l, (byte)0);
    a.subFunds(50l, 10);
    assertEquals(a.balance, 49l);
    assertEquals(a.fracbalance, (byte)90);
  }

  @Test
  public void testSubTypeCastBoth() throws Exception {
    BankAccount a = new BankAccount("user", 100l, (byte)0);
    a.subFunds(50, 10);
    assertEquals(a.balance, 49l);
    assertEquals(a.fracbalance, (byte)90);
  }

  @Test
  public void testSubNegativeFrac() throws Exception {
    BankAccount a = new BankAccount("User", 1000l, (byte)0);
    assertThrows(ImproperFractionalAmount.class, () -> {
        a.subFunds(0l, (byte)-1);
    });
  }

  @Test
  public void testSubBigFrac() throws Exception {
    BankAccount a = new BankAccount("User", 1000l, (byte)0);
    assertThrows(ImproperFractionalAmount.class, () -> {
        a.subFunds(0l, (byte)101);
    });
  }

  @Test
  public void testSubBigCastFrac() throws Exception {
    BankAccount a = new BankAccount("User", 1000l, (byte)0);
    assertThrows(ImproperFractionalAmount.class, () -> {
        a.subFunds(0l, 200);
    });
  }

  @Test
  public void testSubNegativeBal() throws Exception {
    BankAccount a = new BankAccount("User", 1000l, (byte)0);
    assertThrows(ImproperBalanceAmount.class, () -> {
        a.subFunds(-1l, (byte)1);
    });
  }
}

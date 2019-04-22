package org.duh102.duhbot.moolah;

import java.math.BigInteger;
import java.sql.Timestamp;

import org.duh102.duhbot.moolah.exceptions.*;

public class BankAccount {
  public static BigInteger ZERO_BALANCE = new BigInteger("0");
  public long uid;
  public String user;
  public BigInteger balance;
  public Timestamp lastMined;
  public BankAccount(long uid, String user, BigInteger balance,
                     Timestamp lastMined) throws ImproperBalanceAmount {
    this.uid = uid;
    this.user = user;
    this.balance = balance;
    this.lastMined = lastMined;
    if( isInvalidBalance(balance) )
      throw new ImproperBalanceAmount(this.balance);
  }
  public BankAccount(long uid, String user, long balance, Timestamp lastMined) throws ImproperBalanceAmount {
    this(uid, user, new BigInteger(String.format("%d", balance)), lastMined);
  }
  public BankAccount(BankAccount toCopy) throws ImproperBalanceAmount {
    this(toCopy.uid, toCopy.user, toCopy.balance, toCopy.lastMined);
  }
  public BankAccount revertTo(BankAccount copy) throws ImproperBalanceAmount {
    if( isInvalidBalance(copy.balance) )
      throw new ImproperBalanceAmount(copy.balance);
    this.uid = copy.uid;
    this.user = copy.user;
    this.balance = copy.balance;
    this.lastMined = copy.lastMined;
    return this;
  }

  public BankAccount addFunds(BigInteger bal) throws ImproperBalanceAmount {
    if( isInvalidBalance(bal) )
      throw new ImproperBalanceAmount(this.balance);
    this.balance = balance.add(bal);
    return this;
  }

  public BankAccount subFunds(BigInteger bal) throws InsufficientFundsException,
          ImproperBalanceAmount {
    if( isInvalidBalance(bal) )
      throw new ImproperBalanceAmount(bal);
    if( balance.compareTo(bal) < 0 )
      throw new InsufficientFundsException();
    this.balance = balance.subtract(bal);
    return this;
  }

  // Casting overrides
  public BankAccount addFunds(long bal) throws ImproperBalanceAmount {
    return this.addFunds(new BigInteger(String.format("%d", bal)));
  }
  public BankAccount subFunds(long bal) throws InsufficientFundsException,
          ImproperBalanceAmount {
    return this.subFunds(new BigInteger(String.format("%d", bal)));
  }
  public BankAccount addFunds(int bal) throws ImproperBalanceAmount {
    return this.addFunds(new BigInteger(String.format("%d", bal)));
  }
  public BankAccount subFunds(int bal) throws InsufficientFundsException, ImproperBalanceAmount {
    return this.subFunds(new BigInteger(String.format("%d", bal)));
  }

  public boolean equals(Object obj) {
    if(! (obj instanceof BankAccount) )
      return false;
    return this.equals((BankAccount)obj);
  }
  public boolean equals(BankAccount other) {
    return (this.uid == other.uid && this.balance == other.balance
      && this.user.equals(other.user) && this.lastMined.equals(other.lastMined));
  }

  public String toString() {
    return String.format("BankAccount(%d, \"%s\", $%,d, %s)", uid, user, balance, LocalTimestamp.format(lastMined));
  }

  public static boolean isInvalidBalance(BigInteger balance) {
    return balance.compareTo(ZERO_BALANCE) < 0;
  }
}

package org.duh102.duhbot.moolah;

import java.sql.Timestamp;

import org.duh102.duhbot.moolah.exceptions.*;

public class BankAccount {
  public long uid;
  public String user;
  public long balance;
  public Timestamp lastMined;
  public BankAccount(long uid, String user, long balance, Timestamp lastMined) throws ImproperBalanceAmount {
    this.uid = uid;
    this.user = user;
    this.balance = balance;
    this.lastMined = lastMined;
    if( this.balance < 0l )
      throw new ImproperBalanceAmount(this.balance);
  }
  public BankAccount(BankAccount toCopy) throws ImproperBalanceAmount {
    this.uid = toCopy.uid;
    this.user = toCopy.user;
    this.balance = toCopy.balance;
    this.lastMined = toCopy.lastMined;
  }
  public BankAccount revertTo(BankAccount copy) throws ImproperBalanceAmount {
    if( copy.balance < 0l )
      throw new ImproperBalanceAmount(copy.balance);
    this.uid = copy.uid;
    this.user = copy.user;
    this.balance = copy.balance;
    this.lastMined = copy.lastMined;
    return this;
  }

  public BankAccount addFunds(long bal) throws ImproperBalanceAmount {
    if( bal < 0l )
      throw new ImproperBalanceAmount(bal);
    this.balance = balance + bal;
    return this;
  }

  public BankAccount subFunds(long bal) throws InsufficientFundsException, ImproperBalanceAmount {
    if( bal < 0l )
      throw new ImproperBalanceAmount(bal);
    if( balance < bal )
      throw new InsufficientFundsException();
    this.balance = balance - bal;
    return this;
  }

  // Casting overrides
  public BankAccount addFunds(int bal) throws ImproperBalanceAmount {
    return this.addFunds((long)bal);
  }
  public BankAccount subFunds(int bal) throws InsufficientFundsException, ImproperBalanceAmount {
    return this.subFunds((long)bal);
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
}

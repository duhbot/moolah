package org.duh102.duhbot.moolah;

import org.duh102.duhbot.moolah.exceptions.*;

public class BankAccount {
  public long uid;
  public String user;
  public long balance;
  public long lastMined;
  public BankAccount(long uid, String user, long balance, long lastMined) throws ImproperBalanceAmount {
    this.uid = uid;
    this.user = user;
    this.balance = balance;
    this.lastMined = lastMined;
    if( this.balance < 0l )
      throw new ImproperBalanceAmount(this.balance);
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
}

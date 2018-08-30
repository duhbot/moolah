package org.duh102.duhbot.moolah;

import org.duh102.duhbot.moolah.exceptions.*;

public class BankAccount {
  public String user;
  public long balance;
  public byte fracbalance;
  public BankAccount(String user, long balance, byte fracbalance) throws ImproperFractionalAmount, ImproperBalanceAmount {
    this.user = user;
    this.balance = balance;
    this.fracbalance = fracbalance;
    if( this.balance < 0l )
      throw new ImproperBalanceAmount(this.balance);
    if( this.fracbalance < (byte)0 || this.fracbalance > (byte)99 )
      throw new ImproperFractionalAmount(this.fracbalance);
  }

  public BankAccount addFunds(long bal, byte frac) throws ImproperFractionalAmount, ImproperBalanceAmount {
    if( bal < 0l )
      throw new ImproperBalanceAmount(bal);
    if( frac < (byte)0 || frac > (byte)99 )
      throw new ImproperFractionalAmount(frac);
    int newfrac = this.fracbalance + frac;
    boolean carry = false;
    carry = newfrac > 99;
    newfrac = newfrac % 100;
    long newBal = balance + bal + (carry?1:0);
    this.balance = newBal;
    this.fracbalance = (byte)newfrac;
    return this;
  }

  public BankAccount subFunds(long bal, byte frac) throws InsufficientFundsException, ImproperBalanceAmount, ImproperFractionalAmount {
    if( bal < 0l )
      throw new ImproperBalanceAmount(bal);
    if( frac < (byte)0 || frac > (byte)99 )
      throw new ImproperFractionalAmount(frac);
    boolean borrow = frac > fracbalance;
    if( balance < (bal + (borrow?1:0)) )
      throw new InsufficientFundsException();
    long newbalance = balance - (bal + (borrow?1:0));
    byte newfrac = (byte)(((int)fracbalance) + (borrow?100:0) - (int)(frac));
    this.balance = newbalance;
    this.fracbalance = newfrac;
    return this;
  }

  // Casting overrides
  public BankAccount addFunds(int bal, int frac) throws ImproperFractionalAmount, ImproperBalanceAmount {
    return this.addFunds((long)bal, frac);
  }
  public BankAccount addFunds(int bal, byte frac) throws ImproperFractionalAmount, ImproperBalanceAmount {
    return this.addFunds((long)bal, frac);
  }
  public BankAccount addFunds(long bal, int frac) throws ImproperFractionalAmount, ImproperBalanceAmount {
    if( frac > (int)Byte.MAX_VALUE || frac < (int)Byte.MIN_VALUE )
      throw new ImproperFractionalAmount(frac);
    return this.addFunds(bal, (byte)frac);
  }
  public BankAccount subFunds(int bal, int frac) throws InsufficientFundsException, ImproperFractionalAmount, ImproperBalanceAmount {
    return this.subFunds((long)bal, frac);
  }
  public BankAccount subFunds(int bal, byte frac) throws InsufficientFundsException, ImproperFractionalAmount, ImproperBalanceAmount {
    return this.subFunds((long)bal, frac);
  }
  public BankAccount subFunds(long bal, int frac) throws InsufficientFundsException, ImproperFractionalAmount, ImproperBalanceAmount {
    if( frac > (int)Byte.MAX_VALUE || frac < (int)Byte.MIN_VALUE )
      throw new ImproperFractionalAmount(frac);
    return this.subFunds(bal, (byte)frac);
  }
}

package org.duh102.duhbot.moolah;

import org.duh102.duhbot.moolah.exceptions.*;

public class BankAccount {
  public String user;
  public long balance;
  public byte fracbalance;
  public BankAccount(String user, long balance, byte fracbalance){
    this.user = user;
    this.balance = balance;
    this.fracbalance = fracbalance;
  }

  public addFunds(long bal, byte frac) {
    int newfrac = this.fracbalance + frac;
    boolean carry = false;
    carry = newfrac > 99;
    newfrac = (byte)(newfrac % 100);
    long newBal = balance + bal + (carry?1:0);
    this.balance = newBal;
    this.fracbalance = newfrac;
  }

  public subFunds(long bal, byte frac) throw InsufficientFundsException {
    boolean borrow = frac > fracbalance;
    if( balance < (bal + (borrow?1:0)) )
      throw new InsufficientFundsException();
    long newbalance = balance - (bal + (borrow?1:0));
    byte newfrac = (byte)((int)(fracbalance+ borrow?100:0) - frac);
    this.balance = newbalance;
    this.fracbalance = newfrac;
  }
}

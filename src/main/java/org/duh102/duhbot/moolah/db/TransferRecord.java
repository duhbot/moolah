package org.duh102.duhbot.moolah.db;

import java.math.BigInteger;
import java.sql.*;

import org.duh102.duhbot.moolah.BankAccount;
import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.db.dao.BankAccountDAO;
import org.duh102.duhbot.moolah.db.dao.TransferRecordDAO;
import org.duh102.duhbot.moolah.exceptions.*;

public class TransferRecord {
  public long outcomeID;
  public long uidSource;
  public long uidDestination;
  public BigInteger amount;
  public Timestamp timestamp;
  public TransferRecord(long outcomeID, long uidSource, long uidDestination,
                        BigInteger amount, Timestamp timestamp) {
    this.outcomeID = outcomeID;
    this.uidSource = uidSource;
    this.uidDestination = uidDestination;
    this.amount = amount;
    this.timestamp = timestamp;
  }

  public static TransferRecord makeTransfer(BankAccount source,
                                            BankAccount destination,
                                            BigInteger amount) throws InsufficientFundsException, ImproperBalanceAmount, SameAccountException {
    if( source.balance.compareTo(amount) < 0 )
      throw new InsufficientFundsException();
    if( source.equals(destination) || source.uid == destination.uid )
      throw new SameAccountException();
    source.subFunds(amount);
    try {
      destination.addFunds(amount);
    } catch( ImproperBalanceAmount iba ) {
      iba.printStackTrace();
      try {
        source.addFunds(amount);
      } catch( ImproperBalanceAmount iba2 ) {
        iba2.printStackTrace();
      }
    }
    return new TransferRecord(0l, source.uid, destination.uid, amount, LocalTimestamp.now());
  }
  public static TransferRecord recordTransfer(BankDB db, BankAccount source,
                                              BankAccount destination,
                                              BigInteger amount) throws InsufficientFundsException, ImproperBalanceAmount, SameAccountException, RecordFailure, AccountDoesNotExist {
    synchronized(db) {
      BankAccount preAttemptSource = null, preAttemptDest = null;
      try {
        preAttemptSource = new BankAccount(source);
      } catch( ImproperBalanceAmount iba ) {
        iba.printStackTrace();
      }
      try {
        preAttemptDest = new BankAccount(destination);
      } catch( ImproperBalanceAmount iba ) {
        iba.printStackTrace();
      }
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      TransferRecordDAO transferRecordDAO = new TransferRecordDAO(db);
      try {
        TransferRecord record = makeTransfer(source, destination, amount);
        accountDAO.pushAccount(source);
        accountDAO.pushAccount(destination);
        return transferRecordDAO.recordTransfer(record);
      } catch( RecordFailure | AccountDoesNotExist e ) {
        try {
          source.revertTo(preAttemptSource);
        } catch( ImproperBalanceAmount iba ) {
          iba.printStackTrace();
        }
        try {
          destination.revertTo(preAttemptDest);
        } catch( ImproperBalanceAmount iba ) {
          iba.printStackTrace();
        }
        throw e;
      }
    }
  }

  public boolean equals(Object other) {
    if( !(other instanceof TransferRecord) )
      return false;
    return this.equals((TransferRecord)other);
  }
  public boolean equals(TransferRecord other) {
    return this.outcomeID == other.outcomeID && this.uidSource == other.uidSource
      && this.uidDestination == other.uidDestination && this.amount.equals(other.amount)
      && this.timestamp.equals(other.timestamp);
  }
  public String toString() {
    return String.format("TransferRecord(outcomeID %d, source %d, destination %d, amount %,d, timestamp %s)", outcomeID, uidSource, uidDestination, amount, LocalTimestamp.format(timestamp));
  }
}

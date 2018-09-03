package org.duh102.duhbot.moolah;

import java.sql.Timestamp;

public class TransferRecord {
  public long outcomeID;
  public long uidSource;
  public long uidDestination;
  public long amount;
  public Timestamp timestamp;
  public TransferRecord(long outcomeID, long uidSource, long uidDestination, long amount, Timestamp timestamp) {
    this.outcomeID = outcomeID;
    this.uidSource = uidSource;
    this.uidDestination = uidDestination;
    this.amount = amount;
    this.timestamp = timestamp;
  }
}

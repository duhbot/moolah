package org.duh102.duhbot.moolah;

public class TransferRecord {
  public long outcomeID;
  public long uidSource;
  public long uidDestination;
  public long amount;
  public long timestamp;
  public TransferRecord(long outcomeID, long uidSource, long uidDestination, long amount, long timestamp) {
    this.outcomeID = outcomeID;
    this.uidSource = uidSource;
    this.uidDestination = uidDestination;
    this.amount = amount;
    this.timestamp = timestamp;
  }
}

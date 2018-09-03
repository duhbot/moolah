package org.duh102.duhbot.moolah;

import java.sql.Timestamp;

public class MineRecord {
  public long outcomeID;
  public long uid;
  public int mineFractions;
  public double richness;
  public long yield;
  public Timestamp timestamp;
  public MineRecord(long outcomeID, long uid, int mineFractions, double richness, long yield, Timestamp timestamp) {
    this.outcomeID = outcomeID;
    this.uid = uid;
    this.mineFractions = mineFractions;
    this.richness = richness;
    this.yield = yield;
    this.timestamp = timestamp;
  }
}

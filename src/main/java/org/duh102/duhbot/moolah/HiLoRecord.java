package org.duh102.duhbot.moolah;

public class HiLoRecord {
  public long outcomeID;
  public long uid;
  public int resultInt;
  public String hiLo;
  public long wager;
  public long payout;
  public double multiplier;
  public long timestamp;
  public HiLoRecord(long outcomeID, long uid, int resultInt, String hiLo, long wager, long payout, double muliplier, long timestamp) {
    this.outcomeID = outcomeID;
    this.uid = uid;
    this.resultInt = resultInt;
    this.hiLo = hiLo;
    this.wager = wager;
    this.payout = payout;
    this.multiplier = multiplier;
    this.timestamp = timestamp;
  }
}

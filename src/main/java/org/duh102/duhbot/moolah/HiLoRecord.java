package org.duh102.duhbot.moolah;

import java.sql.Timestamp;

public class HiLoRecord {
  public long outcomeID;
  public long uid;
  public int resultInt;
  public HiLoBetType hiLo;
  public long wager;
  public long payout;
  public double multiplier;
  public Timestamp timestamp;
  public HiLoRecord(long outcomeID, long uid, int resultInt, HiLoBetType hiLo, long wager, long payout, double muliplier, Timestamp timestamp) {
    this.outcomeID = outcomeID;
    this.uid = uid;
    this.resultInt = resultInt;
    this.hiLo = hiLo;
    this.wager = wager;
    this.payout = payout;
    this.multiplier = multiplier;
    this.timestamp = timestamp;
  }

  public boolean equals(Object other) {
    if( !(other instanceof HiLoRecord) )
      return false;
    return this.equals((HiLoRecord)other);
  }
  public boolean equals(HiLoRecord other) {
    return this.outcomeID == other.outcomeID && this.uid == other.uid
      && this.resultInt == other.resultInt && this.hiLo.equals(other.hiLo)
      && this.wager == other.wager && this.payout == other.payout
      && this.multiplier == other.multiplier && this.timestamp.equals(other.timestamp);
  }
  public static HiLoRecord betHiLo(HiLoBetType hiLo, long wager) {
    long timestamp = new Timestamp(System.currentTimeMillis());
    
  }
}

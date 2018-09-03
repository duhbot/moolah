package org.duh102.duhbot.moolah;

import java.sql.Timestamp;
import java.util.Random;

import org.duh102.duhbot.moolah.exceptions.*;

public class HiLoRecord {
  private static final Random rand = new Random();
  public static final int MIN = 0, MAX = 11, MID = (MAX + MIN / 2);
  public long outcomeID;
  public long uid;
  public int resultInt;
  public HiLoBetType hiLo;
  public long wager;
  public long payout;
  public double multiplier;
  public Timestamp timestamp;
  public HiLoRecord(long outcomeID, long uid, int resultInt, HiLoBetType hiLo, long wager, long payout, double multiplier, Timestamp timestamp) {
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
  public static HiLoRecord betHiLo(BankAccount account, HiLoBetType hiLo, long wager) throws InsufficientFundsException {
    Timestamp timestamp = LocalTimestamp.now();
    int result = rand.nextInt(MAX-MIN)+MIN;
    double mult = hiLo.getSatisfied(result, MID)?hiLo.getMultiplier():0.0;
    long payout = Math.round(wager * mult);
    return new HiLoRecord(0, 0, result, hiLo, wager, payout, mult, timestamp);
  }
}

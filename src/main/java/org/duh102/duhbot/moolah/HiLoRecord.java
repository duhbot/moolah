package org.duh102.duhbot.moolah;

import java.sql.Timestamp;
import java.util.Random;

import org.duh102.duhbot.moolah.exceptions.*;

public class HiLoRecord {
  private static final Random rand = new Random();
  public static final int MIN = 0, MAX = 11, MID = (MAX + MIN) / 2;
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

  public static void setSeed(long seed) {
    rand.setSeed(seed);
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
  public static HiLoRecord betHiLo(BankAccount account, HiLoBetType hiLo, long wager) throws InsufficientFundsException, ImproperBalanceAmount {
    account.subFunds(wager);
    Timestamp timestamp = LocalTimestamp.now();
    int result = rand.nextInt(MAX-MIN)+MIN;
    double mult = Math.abs(hiLo.getSatisfied(result)?hiLo.getMultiplier():0.0);
    long payout = Math.abs(Math.round(wager * mult));
    try {
      account.addFunds(payout);
    } catch( ImproperBalanceAmount iba ) {
      try {
        account.addFunds(wager);
      } catch( ImproperBalanceAmount iba2 ) {
        iba2.printStackTrace();
      }
    }
    return new HiLoRecord(0l, account.uid, result, hiLo, wager, payout, mult, timestamp);
  }

  public String toString() {
    return String.format("HiLoRecord(rid %d, uid %d, result %d, hiLo %s, wager %d, payout %d, mult %.2f, timestamp %s)", outcomeID, uid, resultInt, hiLo.toString(), wager, payout, multiplier, LocalTimestamp.format(timestamp));
  }
}

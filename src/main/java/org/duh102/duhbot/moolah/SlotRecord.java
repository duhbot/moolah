package org.duh102.duhbot.moolah;

import java.sql.Timestamp;

public class SlotRecord {
  public long outcomeID;
  public long uid;
  public String slotImages;
  public long wager;
  public long payout;
  public double multiplier;
  public Timestamp timestamp;
  public SlotRecord(long outcomeID, long uid, String slotImages, long wager, long payout, double multiplier, Timestamp timestamp) {
    this.outcomeID = outcomeID;
    this.uid = uid;
    this.slotImages = slotImages;
    this.wager = wager;
    this.payout = payout;
    this.multiplier = multiplier;
    this.timestamp = timestamp;
  }
}

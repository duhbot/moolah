package org.duh102.duhbot.moolah;

public class SlotRecord {
  public long outcomeID;
  public long uid;
  public String slotImages;
  public long wager;
  public long payout;
  public double multiplier;
  public long timestamp;
  public SlotRecord(long outcomeID, long uid, String slotImages, long wager, long payout, double multiplier, long timestamp) {
    this.outcomeID = outcomeID;
    this.uid = uid;
    this.slotImages = slotImages;
    this.wager = wager;
    this.payout = payout;
    this.multiplier = multiplier;
    this.timestamp = timestamp;
  }
}

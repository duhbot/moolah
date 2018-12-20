package org.duh102.duhbot.moolah.db;

import java.sql.*;

import org.duh102.duhbot.moolah.BankAccount;
import org.duh102.duhbot.moolah.LocalTimestamp;
import org.duh102.duhbot.moolah.Mine;
import org.duh102.duhbot.moolah.db.dao.BankAccountDAO;
import org.duh102.duhbot.moolah.db.dao.MineRecordDAO;
import org.duh102.duhbot.moolah.exceptions.*;

public class MineRecord {
  //half an hour per chunk, one day's worth of chunks
  public static final long MINE_CHUNK_LENGTH = 30*60*1000, MAX_CHUNKS = 48;

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

  public boolean equals(Object other) {
    if( !(other instanceof MineRecord) )
      return false;
    return this.equals((MineRecord)other);
  }
  public boolean equals(MineRecord other) {
    return this.outcomeID == other.outcomeID && this.uid == other.uid && this.mineFractions == other.mineFractions
      && this.richness == other.richness && this.yield == other.yield && this.timestamp.equals(other.timestamp);
  }
  public static MineRecord mineAttempt(BankAccount account) throws MineAttemptTooSoon {
    Timestamp now = LocalTimestamp.now();
    long timeSinceLastMine = now.getTime() - account.lastMined.getTime();
    if( timeSinceLastMine < MINE_CHUNK_LENGTH )
      throw new MineAttemptTooSoon("30 minutes"); //keep this synched up as an english rep of MINE_CHUNK_LENGTH
    double richness = Mine.getMine().getRichness();
    //use only the chunks we've fully "generated", discard partials
    int chunks = (int)Math.max(1, Math.min(MAX_CHUNKS, Math.floor(timeSinceLastMine / (double)MINE_CHUNK_LENGTH)));
    long payout = Math.abs(Math.round(chunks * richness));
    try {
      account.addFunds(payout);
      account.lastMined = now;
    } catch( ImproperBalanceAmount iba ) {
      iba.printStackTrace();
    }
    return new MineRecord(0l, account.uid, chunks, richness, payout, now);
  }

  public static MineRecord recordMineAttempt(BankDB db, BankAccount account) throws MineAttemptTooSoon, RecordFailure, AccountDoesNotExist {
    synchronized(db) {
      BankAccount preAttempt = null;
      BankAccountDAO accountDAO = new BankAccountDAO(db);
      MineRecordDAO mineRecordDAO = new MineRecordDAO(db);
      try {
        preAttempt = new BankAccount(account);
      } catch( ImproperBalanceAmount iba ) {
        iba.printStackTrace();
      }
      try {
        MineRecord record = mineAttempt(account);
        accountDAO.pushAccount(account);
        return mineRecordDAO.recordMineOutcome(record);
      } catch( RecordFailure | AccountDoesNotExist e ) {
        try {
          account.revertTo(preAttempt);
        } catch( ImproperBalanceAmount iba ) {
          iba.printStackTrace();
        }
        throw e;
      }
    }
  }

  public String toString() {
    return String.format("MineRecord(rid %d, uid %d, fracs %d, richness %.2f, yield %df, timestamp %s)", outcomeID, uid, mineFractions, richness, yield, LocalTimestamp.format(timestamp));
  }
}

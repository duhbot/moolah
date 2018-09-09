package org.duh102.duhbot.moolah;

import org.duh102.duhbot.moolah.exceptions.*;

public class PayoutTest {
  public static void main(String args[]) {
    int iterations = 10*1000*1000;
    long resetBalance = 1000000l, wager = 1000l;
    double expectedMultiplier = 0, lossChance = 0;
    int completeLosses = 0;
    BankAccount acct = null;
    try {
      acct = new BankAccount(0l, "user", resetBalance, null);
    } catch( ImproperBalanceAmount iba ) {
      iba.printStackTrace();
    }
    SlotRecord[] slotRecords = new SlotRecord[iterations];
    for( int i = 0; i < iterations; i++ ) {
      acct.balance = resetBalance;
      try {
        slotRecords[i] = SlotRecord.slotAttempt(acct, wager);
      } catch( InsufficientFundsException | ImproperBalanceAmount ie ) {
        break;
      }
    }
    for( SlotRecord record : slotRecords ) {
      if( record == null )
        continue;
      expectedMultiplier += record.multiplier;
      if( record.multiplier == 0.0 )
        completeLosses++;
    }
    lossChance = (double)completeLosses/iterations*100.0;
    expectedMultiplier /= iterations;
    System.out.printf("Slots results: Complete loss chance: %.2f%% Expected multiplier over %,d iterations: %.2fx\n",
        lossChance, iterations, expectedMultiplier);

    expectedMultiplier = 0;
    completeLosses = 0;
    HiLoRecord[] hiLoRecords = new HiLoRecord[iterations];
    for( int i = 0; i < iterations; i++ ) {
      acct.balance = resetBalance;
      try {
        hiLoRecords[i] = HiLoRecord.betHiLo(acct, HiLoBetType.LOW, wager);
      } catch( InsufficientFundsException | ImproperBalanceAmount ie ) {
        break;
      }
    }
    for( HiLoRecord record : hiLoRecords ) {
      if( record == null )
        continue;
      expectedMultiplier += record.multiplier;
      if( record.multiplier == 0.0 )
        completeLosses++;
    }
    lossChance = (double)completeLosses/iterations*100.0;
    expectedMultiplier /= iterations;
    System.out.printf("HiLo (low) results: Complete loss chance: %.2f%% Expected multiplier over %,d iterations: %.2fx\n",
        lossChance, iterations, expectedMultiplier);
    for( int i = 0; i < iterations; i++ ) {
      hiLoRecords[i] = null;
    }

    expectedMultiplier = 0;
    completeLosses = 0;
    for( int i = 0; i < iterations; i++ ) {
      acct.balance = resetBalance;
      try {
        hiLoRecords[i] = HiLoRecord.betHiLo(acct, HiLoBetType.EQUAL, wager);
      } catch( InsufficientFundsException | ImproperBalanceAmount ie ) {
        break;
      }
    }
    for( HiLoRecord record : hiLoRecords ) {
      if( record == null )
        continue;
      expectedMultiplier += record.multiplier;
      if( record.multiplier == 0.0 )
        completeLosses++;
    }
    lossChance = (double)completeLosses/iterations*100.0;
    expectedMultiplier /= iterations;
    System.out.printf("HiLo (equal) results: Complete loss chance: %.2f%% Expected multiplier over %,d iterations: %.2fx\n",
        lossChance, iterations, expectedMultiplier);
    for( int i = 0; i < iterations; i++ ) {
      hiLoRecords[i] = null;
    }

    expectedMultiplier = 0;
    completeLosses = 0;
    for( int i = 0; i < iterations; i++ ) {
      acct.balance = resetBalance;
      try {
        hiLoRecords[i] = HiLoRecord.betHiLo(acct, HiLoBetType.HIGH, wager);
      } catch( InsufficientFundsException | ImproperBalanceAmount ie ) {
        break;
      }
    }
    for( HiLoRecord record : hiLoRecords ) {
      if( record == null )
        continue;
      expectedMultiplier += record.multiplier;
      if( record.multiplier == 0.0 )
        completeLosses++;
    }
    lossChance = (double)completeLosses/iterations*100.0;
    expectedMultiplier /= iterations;
    System.out.printf("HiLo (high) results: Complete loss chance: %.2f%%  Expected multiplier over %,d iterations: %.2fx\n",
        lossChance, iterations, expectedMultiplier);
    for( int i = 0; i < iterations; i++ ) {
      hiLoRecords[i] = null;
    }
  }
}

package org.duh102.duhbot.moolah;

import java.util.Random;

public class Mine {
  private static Mine MINE = null;
  private static final long AVG_UPDATE = 2*60*60*1000;
  private static final Random rand = new Random();
  private long nextUpdate = 0;
  private double currentRichness = 0.0, currGauss = 0.0;
  private Mine() {
    refreshMine();
  }
  private void refreshMine() {
    refreshMine(System.currentTimeMillis());
  }
  private synchronized void refreshMine(long whenFrom) {
    //ensure the refreshed richness is good for at least five minutes
    long targetTime = whenFrom+(5*60*1000);
    Pair<Double, Long> pairRet;
    // recalc the richness to make up for lost time, so we use every gaussian value produced by the gen
    // (And potentially get a richness "in the middle" of its time slot)
    while(nextUpdate < targetTime ) {
      pairRet = genMineRichness();
      currentRichness = pairRet.first;
      nextUpdate = nextUpdate + pairRet.second;
    }
  }
  public static void setSeed(long seed) {
    rand.setSeed(seed);
  }
  public Pair<Double, Long> genMineRichness() {
    currGauss = rand.nextGaussian();
    // For 10k runs, saw a distribution of about [min, q1, q2, q3, max] [3.84, 60.72, 101.035, 169.26, 2844.74]
    Double richness = new Double(Math.pow(10.0, (currGauss/3.0+2.0))+1); //richness in $ per mining chunk
    // We want poorer veins to last longer, ex $1/chunk expected to last around 4 hours while $2800/chunk 10 minutes
    // For 10k runs, saw a distribution of about [min, q1, q2, q3, max] [12716295, 3203895, 2549358, 2005776, 614268]
    Long time = new Long(Math.round((AVG_UPDATE/Math.max(Math.pow(2, ((currGauss+1)/2)), 0.025))/2.0)+1); // in ms
    return new Pair<Double, Long>(richness, time);
  }

  public static Mine getMine() {
    if( MINE == null ) {
      MINE = new Mine();
    }
    return MINE;
  }
  public synchronized double getRichness() {
    long now = System.currentTimeMillis();
    if( now >= nextUpdate )
      refreshMine(now);
    return currentRichness;
  }
  // gaussian values > 2 (2 standard deviations above 0) are particularly rich
  public synchronized boolean isRichVein() {
    return currGauss >= 2.0;
  }
}

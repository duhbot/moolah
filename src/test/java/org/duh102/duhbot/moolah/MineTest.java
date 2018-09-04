package org.duh102.duhbot.moolah;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MineTest {
  @Test
  public void testSingleton() throws Exception {
    Mine mine1 = Mine.getMine();
    Mine mine2 = Mine.getMine();
    assertSame(mine1, mine2);
  }
  @Test
  public void testRichness() throws Exception {
    double richness = Mine.getMine().getRichness();
    assertTrue(richness >= 1.0);
  }
  @Test
  public void testGenMineRichnessDiff() throws Exception {
    for(int i = 0; i < 10000; i++ ) {
      Pair<Double, Long> pairRet1 = Mine.getMine().genMineRichness();
      Pair<Double, Long> pairRet2 = Mine.getMine().genMineRichness();
      assertNotEquals(pairRet1.first, pairRet2.first);
      assertNotEquals(pairRet1.second, pairRet2.second);
      assertTrue(pairRet1.first >= 1.0);
      assertTrue(pairRet2.first >= 1.0);
      assertTrue(pairRet1.second >= 1);
      assertTrue(pairRet2.second >= 1);
    }
  }
  @Test
  public void testRichnessUpdateTime() throws Exception {
    double richness1 = Mine.getMine().getRichness();
    double richness2 = Mine.getMine().getRichness();
    assertEquals(richness1, richness2);
  }
}

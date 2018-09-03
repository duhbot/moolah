import java.util.Random;

public class TestHiLo {
  public static final void main(String args[]) {
    int MIN = 0, MAX = 11, MID = (MIN + MAX)/2;
    Random rand = new Random();
    int[] numHits = new int[11];
    for( int i = 0; i < 11; i++ ){
      numHits[i] = 0;
    }
    int total = 100000;
    for( int i = 0; i < total; i++ ){
      numHits[rand.nextInt(MAX-MIN)+MIN]++;
    }
    for( int i = 0; i < 11; i++ ){
      System.out.printf("%d: %.2f%%\n", i, (double)numHits[i]/(double)total*100.0);
    }
  }
}

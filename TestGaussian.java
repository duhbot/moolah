import java.util.Random;
import java.util.Arrays;

public class TestGaussian {
  public static final long AVG_UPDATE = 2*60*60*1000;
  private static class RichTimeCombo implements Comparable<RichTimeCombo> {
    public double richness;
    public long time;
    public RichTimeCombo(double richness, long time) {
      this.richness = richness;
      this.time = time;
    }
    public int compareTo(RichTimeCombo other) {
      return (new Double(this.richness)).compareTo(new Double(other.richness));
    }
  }
  public static double[] quartiles(double[] numbers) {
    double med = median(numbers);
    double q1 = median(Arrays.copyOfRange(numbers, 0, numbers.length/2));
    double q3 = median(Arrays.copyOfRange(numbers, numbers.length/2, numbers.length));
    return new double[]{q1, med, q3};
  }
  public static long[] quartiles(long[] numbers) {
    long med = median(numbers);
    long q1 = median(Arrays.copyOfRange(numbers, 0, numbers.length/2));
    long q3 = median(Arrays.copyOfRange(numbers, numbers.length/2, numbers.length));
    return new long[]{q1, med, q3};
  }
  public static double median(double[] numbers) {
    int mid = numbers.length/2;
    if( numbers.length % 2 == 0 )
      return (numbers[mid-1] + numbers[mid])/2.0;
    else
      return numbers[mid];
  }
  public static long median(long[] numbers) {
    int mid = numbers.length/2;
    if( numbers.length % 2 == 0 )
      return Math.round((numbers[mid-1] + numbers[mid])/2.0);
    else
      return numbers[mid];
  }
  public static void main(String args[]) {
    int numIters = 10000;
    RichTimeCombo[] richnesses = new RichTimeCombo[numIters];
    Random rand = new Random(100);
    for( int i = 0; i < numIters; i++ ){
      double gauss = rand.nextGaussian();
      double richness = Math.pow(10.0, (gauss/2.0))+1; //richness in $ per mining chunk
      long time = Math.round((AVG_UPDATE/Math.max(Math.pow(2, ((gauss+1)/2)), 0.025))/2.0)+1; // in ms
      richnesses[i] = new RichTimeCombo(richness, time);
    }
    Arrays.sort(richnesses);
    double[] richnessVals = new double[richnesses.length];
    long[] timeVals = new long[richnesses.length];
    int i = 0;
    for( RichTimeCombo a : richnesses ) {
      richnessVals[i] = a.richness;
      timeVals[i] = a.time;
      i++;
      System.out.printf("%.2f richness, lasts %d ms\n", a.richness, a.time);
    }
    double[] richnessQuartiles = quartiles(richnessVals);
    long[] timeQuartiles = quartiles(timeVals);
    double[] minmaxRichness = new double[] {richnessVals[0], richnessVals[richnessVals.length-1]};
    long[] minmaxTime = new long[] {timeVals[0], timeVals[timeVals.length-1]};
    System.out.printf("Richness [%.2f, %.2f, %.2f, %.2f, %.2f]\nTime [%d, %d, %d, %d, %d]\n", minmaxRichness[0], richnessQuartiles[0], richnessQuartiles[1], richnessQuartiles[2], minmaxRichness[1], minmaxTime[0], timeQuartiles[0], timeQuartiles[1], timeQuartiles[2], minmaxTime[1]);
  }
}

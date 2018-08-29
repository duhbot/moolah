import java.util.Random;

public class TestGaussian {
  public static void main(String args[]) {
    Random rand = new Random(100);
    for( int i = 0; i < 10000; i++ ){
      double newGauss = rand.nextGaussian()/2;
      System.out.printf("%.2f\n", Math.pow(10.0, newGauss));
    }
  }
}

package at.borkowski.spicej.proxy;

import static org.junit.Assert.fail;

import org.junit.Test;

import at.borkowski.spicej.rt.DelayCalculator;
import at.borkowski.spicej.rt.DelayCalculator.Result;

public class DelayCalculatorTest {
   private static final double MAX_ERROR = DelayCalculator.DEFAULT_EPSILON * 2;

   @Test
   public void test() {
      test(0);
      for (int scale = 0; scale < 18; scale++)
         for (int digit = 0; digit < 10; digit++)
            test((long) ((digit + 1) * Math.pow(10, scale)));
      test(Long.MAX_VALUE);
   }

   private void test(long givenDelay) {
      Result r = DelayCalculator.calculate(givenDelay);
      long delay = r.getDelay();
      long interval = r.getTickNanosecondsInterval();

      if (interval > DelayCalculator.MAX_INTERVAL)
         fail("interval > MAX_INTERVAL");

      long resultingDelay = delay * interval;
      double error = (double) resultingDelay / givenDelay - 1;

      if (Math.abs(error) > MAX_ERROR)
         fail("error too high: " + error + " for " + givenDelay);
   }
}

package org.spicej.rt;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.spicej.RateCalculator;
import org.spicej.RateCalculator.Result;
import org.spicej.Streams;
import org.spicej.bytes.RateLimitInputStream;
import org.spicej.impl.RealTimeTickSource;

/*
 * UGLY CODE
 * 
 * This class has been written as a single method and refactored several times
 * by extracting methods. It uses static variables heavily, so it's really not
 * nice code. However, since it odes its jost alright, I haven't gotten around
 * to rewriting it. However, it needs it. Urgently.
 */

/**
 * This class performs a series of tests measuring the performance of
 * {@link RateLimitInputStream}. Since these tests do not fulfill some crucial
 * criteria, they are not classified as regular (unit) tests in the classical
 * meaning, and are not executed automatically upon builds.
 * 
 * The class, in contrast to {@link RealTimeTickSource}, does not really perform
 * performance tests in a way that good/bad status is reported. It merely runs
 * several throughput measurement tests using {@link RateLimitInputStream} and
 * reports the errors.
 *
 * You can use this class to find out the upper bound of throughput (minus the
 * overhead introduced by {@link RateLimitInputStream}) of your system.
 * 
 * <ul>
 * <li>They don't have a binary yes/no result since they measure system
 * performance</li>
 * <li>They use a real-time actions (sleeping), in large numbers -- and thus
 * take a significant amount of time</li>
 * <li>They rely on real time clocks</li>
 * <li>They can be flakey on heavy load conditions</li>
 * </ul>
 */
public class RateLimitInputStreamTests {

   public static void main(String[] args) throws IOException {
      run();
   }

   public static final int MAX_BYTES = 1000 * 10;

   private static int sequence = 0;

   public static void run() throws IOException {
      // scale: indicated the power of 10 (in b/s) of orders of magnitude to test
      // eg. scale = 2 -> test in orders of magnitude of 10^2 b/s

      for (int scale = 1; scale < 10; scale++)
         testScale(scale);
   }

   // 10^scale bytes per second
   private static void testScale(int scale) throws IOException {
      double base = Math.pow(10, scale);

      System.out.print("scale: 10E" + scale + " b/s (");
      format(base);
      System.out.println(")");

      performMeasurement(base * 1);
      performMeasurement(base * 2);
      performMeasurement(base * 3);
      performMeasurement(base * 4);
      performMeasurement(base * 5);
      performMeasurement(base * 6);
      performMeasurement(base * 7);
      performMeasurement(base * 8);
      performMeasurement(base * 9);
   }

   private static void performMeasurement(double bytesPerSecond) throws IOException {
      long bytes = calculateBytes(bytesPerSecond);

      final byte[] devzero = new byte[(int) Math.min(bytes, MAX_BYTES)];
      final byte[] devnull = new byte[(int) Math.min(bytesPerSecond, MAX_BYTES)];

      PipedInputStream pis = new PipedInputStream(devzero.length * 5);
      try (final PipedOutputStream pos = new PipedOutputStream(pis)) {

         Result result = RateCalculator.calculate((float) bytesPerSecond, 1 * 1000000);
         RealTimeTickSource t = new RealTimeTickSource(result.getTickNanosecondInterval(), true);
         RateLimitInputStream sut = Streams.limitRate(pis, t, result.getBytesPerTick(), result.getPrescale());

         final int currentSequence = ++sequence;

         Thread stuffer = new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  while (sequence == currentSequence)
                     pos.write(devzero);

               } catch (IOException ignore) {}
            }
         });
         stuffer.start();

         long t0 = System.currentTimeMillis();
         long done = 0;
         while (done < bytes) {
            int rd = sut.read(devnull);
            if (rd == -1)
               throw new Error("read -1");
            done += rd;
         }
         long t1 = System.currentTimeMillis();
         sequence++;
         sut.close();

         long tt = t1 - t0;
         double rate = 1000D * done / tt;

         System.out.printf("rate %16.2f B/s ", bytesPerSecond);
         System.out.printf("read %12d in %5d ", done, tt);
         System.out.printf("eff ");
         format(rate);
         System.out.printf(" error %5.2f %% ", 100D * rate / bytesPerSecond - 100);
         System.out.println();
      }
   }

   private static long calculateBytes(double bytesPerSecond) {
      return (long) ((long) calculateTargetTime(bytesPerSecond) * bytesPerSecond / 1000);
   }

   private static int calculateTargetTime(double bytesPerSecond) {
      if (bytesPerSecond < 50)
         return 5000;
      else if (bytesPerSecond < 500)
         return 2000;
      else
         return 500;
   }

   private static void format(double bytesPerSecond) {
      if (bytesPerSecond < 1100)
         System.out.printf("%5.0f B/s", bytesPerSecond);
      else if (bytesPerSecond < 1100000)
         System.out.printf("%5.2f kB/s", bytesPerSecond / 1000);
      else
         System.out.printf("%5.2f MB/s", bytesPerSecond / 1000000);
   }
}

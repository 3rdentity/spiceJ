package at.borkowski.spicej.rt;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.Random;

import at.borkowski.spicej.Streams;
import at.borkowski.spicej.impl.RealTimeTickSource;
import at.borkowski.spicej.impl.SleepWakeup;
import at.borkowski.spicej.proxy.DelayCalculator;
import at.borkowski.spicej.proxy.DelayCalculator.Result;
import at.borkowski.spicej.streams.DelayedInputStream;
import at.borkowski.spicej.streams.util.PipedInputStream;
import at.borkowski.spicej.streams.util.PipedOutputStream;

public class DelayInputStreamTests {

   private static final int BUFFER = 100 * 1024;

   static byte[][] blocks = { new byte[10], new byte[504], new byte[314], new byte[271], new byte[1337] }; //, new byte[10], new byte[504], new byte[314], new byte[271], new byte[1337] };
   static byte[][] read = new byte[blocks.length][];
   static boolean allRead;

   static Random random = new Random();

   static long[] timestamps_0 = new long[blocks.length];
   static long[] timestamps_1 = new long[blocks.length];

   static {
      for (int i = 0; i < blocks.length; i++)
         random.nextBytes(blocks[i]);
   }

   public static void main(String[] args) throws IOException {
      run();
   }

   public static void run() throws IOException {
      // scale: indicated the power of 10 (in ns) of orders of magnitude to test
      // eg. scale = 2 -> test in orders of magnitude of 10^2 ns

      for (int scale = 1; scale < 10; scale++)
         testScale(scale);
   }

   // 10^scale nanoseconds
   private static void testScale(int scale) throws IOException {
      long base = (long) Math.pow(10, scale);

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

   private static void performMeasurement(long nanoseconds) throws IOException {
      PipedInputStream pis = new PipedInputStream();
      PipedOutputStream pos = new PipedOutputStream(pis);

      Result result = DelayCalculator.calculate(nanoseconds);
      RealTimeTickSource t = new RealTimeTickSource(result.getTickNanosecondsInterval());
      final DelayedInputStream dis = Streams.addDelay(pis, t, result.getDelay(), BUFFER);

      Thread reader = new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               for (int i = 0; i < blocks.length; i++) {
                  read[i] = new byte[blocks[i].length];
                  int done = 0;
                  while (done < blocks[i].length) {
                     allRead = false;
                     int rd = dis.read(read[i], done, blocks[i].length - done);
                     done += rd;
                  }
                  timestamps_1[i] = System.nanoTime();
               }
               allRead = true;
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
         }
      });

      long sleepNs = nanoseconds * 2 / 3;
      long sleepMs = sleepNs / 1000000;
      int sleepNsInt = (int) (sleepNs % 1000000);

      allRead = true;
      reader.start();
      while (allRead)
         SleepWakeup.sleep(10);

      for (int i = 0; i < blocks.length; i++) {
         pos.write(blocks[i]);
         timestamps_0[i] = System.nanoTime();
         try {
            Thread.sleep(sleepMs, sleepNsInt);
         } catch (InterruptedException ignore) {}
      }

      while (!allRead)
         SleepWakeup.sleep(10);

      t.stop();

      long sum = 0;
      for (int i = 0; i < blocks.length; i++) {
         sum += (timestamps_1[i] - timestamps_0[i]);
         assertArrayEquals(blocks[i], read[i]);
      }
      long interval = sum / blocks.length;
      double error = ((double) interval / nanoseconds - 1) * 100;

      System.out.printf("delay ");
      format(nanoseconds);
      System.out.printf(" avg ");
      format((long) interval);
      System.out.printf(" err %6.2f %%", error);
      System.out.println();
   }

   private static void format(long nanoseconds) {
      if (nanoseconds < 999000)
         System.out.printf("%8.0f ns", (double) nanoseconds);
      else if (nanoseconds < 999000000)
         System.out.printf("%8.2f ms", (double) nanoseconds / 1000000);
      else
         System.out.printf("%8.2f s", (double) nanoseconds / 1000000000);
   }
}

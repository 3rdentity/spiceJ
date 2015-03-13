package at.borkowski.spicej.rt;

import java.text.DecimalFormat;
import java.util.Arrays;

import at.borkowski.spicej.impl.SleepWakeup;
import at.borkowski.spicej.ticks.TickListener;

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
 * {@link RealTimeTickSource}. Since these tests do not fulfill some crucial
 * criteria, they are not classified as regular (unit) tests in the classical
 * meaning, and are not executed automatically upon builds.
 * 
 * This class first determines the rough minimal interval value in nanoseconds
 * the running platform is able to generate. It then starts generating ticks
 * with intervals starting at this value. For each interval, a series of trials
 * is started, and the average error in percent is reported.
 * 
 * Finally, the lowest reliably generateable interval is reported. This can be
 * used as a measure for (real-time) system performance.
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
public class RealTimeTickSourceTests {

   public static void main(String[] args) {
      run();
   }

   /**
    * The maximal relative error permitted (0.03 = 3%)
    */
   public static final double EPSILON = 0.03;

   /**
    * How often a test is tried (with increasing n) before it is marked as
    * failed. This value has been proven useful by experimenting.
    */
   public static final int RETRIES = 20;

   /**
    * 1 ms in ns
    */
   public static final long MS = 1000000;

   /**
    * How many tick measurements to allow maximally
    */
   public static final int BUFFER_CAPACITY = 20000000;

   // unfortunately, this class is spaghetti code, that's why we have a lot of static variables :-(
   private static int tickCount;             // the amount of ticks we have acquired
   private static int deltaCount;            // the amount of deltas we have acquired
   private static long minBound = 0;         // we don't measure below this interval
   private static Long out_min = null;       // the lowest delta we've seen
   private static Long minReliable = null;   // the "result": this and all higher intervals could produce reliable results
   private static int reliability = 0;       // how many results were above minReliable that could produce reliable results    
   private static int sequence = 0;          // check variable to avoid concurrent modifications of buffer (= optimistic locking)
   private static boolean finished;          // whether the tick collector thread has finished

   private static long[] buffer = new long[BUFFER_CAPACITY];
   private static long[] deltas = new long[BUFFER_CAPACITY];

   private static void settle() {
      // let the JVM warm up and settle
      long deadline = System.currentTimeMillis() + 30;
      while (System.currentTimeMillis() < deadline)
         ;
      SleepWakeup.sleep(100);
   }

   public static void run() {
      performMinimumIntervalMeasurement();

      // scale: indicated the power of 10 (in nanoseconds) of orders of magnitude to test
      // eg. scale = 2 -> test in orders of magnitude of 10^2 ns

      for (int scale = 1; scale < 10; scale++)
         testScale(scale);

      if (minReliable != null)
         if (reliability > 3) {
            System.out.println("Lowest reliably generateable interval is " + minReliable + " ns");
         } else {
            System.out.println("Lowest reliably generateable interval is " + minReliable + " ns, but not enough consecutive measurements have been found (only " + reliability + ")");
         }
      else
         System.out.println("No reliable measurement was possible");
   }

   private static void performMinimumIntervalMeasurement() {
      System.out.println("Measuring minimum interval...");
      Long min = null;
      for (int i = 0; i < 5; i++) {
         performAndEvaluateMeasurement("[pre-test]", BUFFER_CAPACITY, 1);
         if (out_min != null && (min == null || min > out_min))
            min = out_min;
      }
      if (min == null) {
         System.out.println("Could not evaluate minium interval (no valid measurements)");
      } else {
         minBound = Math.min(500, min * 3 / 4);
         System.out.println("Minmal interval: " + min + " ns, selecting " + minBound + " ns");
      }

   }

   // 10^scale ns
   private static void testScale(int scale) {
      double base = Math.pow(10, scale);

      System.out.println("scale: 10E" + (scale) + " ns (" + format(base / 1000000) + " ms)");

      if (base < 10 * 1000000) { // up to 10ms
         performSeries(base * 1);
         performSeries(base * 2);
         performSeries(base * 3);
         performSeries(base * 4);
         performSeries(base * 5);
         performSeries(base * 6);
         performSeries(base * 7);
         performSeries(base * 8);
         performSeries(base * 9);
      } else if (base < 1000 * 1000000) { // up to 1s
         performSeries(base * 1);
         performSeries(base * 2);
         performSeries(base * 3);
         performSeries(base * 4);
         performSeries(base * 5);
         performSeries(base * 8);
      } else {
         performSeries(base * 1);
         performSeries(base * 3);
         performSeries(base * 5);
         performSeries(base * 9);
      }
   }

   private synchronized static void performSeries(double nanoSeconds) {
      if (nanoSeconds < minBound)
         return;

      boolean ok = false;
      for (int i = 0; i < RETRIES; i++)
         if (ok = performAndEvaluateMeasurement(i > 0 ? "[retry]" : "", calculateTickCount(nanoSeconds, i), nanoSeconds))
            break;

      if (!ok)
         minReliable = null;
      else if (minReliable == null)
         minReliable = (long) nanoSeconds;

      if (minReliable != null)
         reliability++;
      else
         reliability = 0;

      if (!ok)
         printLabel("[fail]", true);
   }

   private static int calculateTickCount(double nanoSeconds, int retry) {
      int waitForTickCount_;
      if (nanoSeconds < 1000)
         waitForTickCount_ = 100000;
      else if (nanoSeconds < 10000)
         waitForTickCount_ = 1000;
      else if (nanoSeconds < MS)
         waitForTickCount_ = 100;
      else
         waitForTickCount_ = 1;

      waitForTickCount_ *= (1 + retry * 10);

      return Math.min(buffer.length, waitForTickCount_ + 1); // we need n+1 measurements to have n deltas

   }

   private synchronized static boolean performAndEvaluateMeasurement(String label, int targetTickCount, double nanoSeconds) {
      long nanoSeconds_ = (long) nanoSeconds;

      settle();
      performMeasurement(nanoSeconds_, targetTickCount);

      printLabel(label, false);

      boolean ok = false;

      msns(nanoSeconds_);

      System.out.printf(" n %8d ", tickCount);

      if (deltaCount == 0) {
         System.out.print("no measured deltas");
      } else {
         Arrays.sort(deltas, 0, deltaCount);

         double average = average();

         boolean ticksOk = tickCount == targetTickCount;
         boolean ticksAlright = tickCount >= (targetTickCount * 95 / 100);

         System.out.printf("avg %13.2f ns ", average);

         double error = average / nanoSeconds - 1;
         boolean epsilonOk = Math.abs(error) <= EPSILON;

         System.out.printf("err %8.2f %% (", error * 100);
         msns((long) (average - nanoSeconds));
         System.out.printf(") ");
         if (!ticksOk)
            System.out.print("(INCOMPLETE MEASUREMENT " + format(100D * tickCount / targetTickCount) + " %) ");
         if (!epsilonOk)
            System.out.print("(ERROR TOO HIGH, > " + format(100D * EPSILON) + " %) ");
         ok = ticksAlright && epsilonOk;
      }

      System.out.println();
      return ok;
   }

   private static void msns(long nanoSeconds) {
      if (Math.abs(nanoSeconds) < 10000)
         System.out.printf("%7d ns", nanoSeconds);
      else
         System.out.printf("%7.2f ms", (double) nanoSeconds / MS);
   }

   private static void printLabel(String label, boolean newline) {
      System.out.printf("%11s ", label);
      if (newline)
         System.out.println();
   }

   private static void performMeasurement(long nanoSeconds_, final int targetTickCount) {
      RealTimeTickSource sut = new RealTimeTickSource(nanoSeconds_, false);

      tickCount = 0;
      finished = false;

      for (int i = 0; i < buffer.length; i++)
         buffer[i] = 0;

      for (int i = 0; i < deltas.length; i++)
         buffer[i] = 0;

      final int currentSequence = ++sequence;

      TickListener listener;
      sut.addListener(listener = new TickListener() {
         @Override
         public void tick(long tick) {
            long time = System.nanoTime();
            if (finished)
               return;
            if (sequence != currentSequence)
               return;
            buffer[tickCount++] = time;
            finished = tickCount >= targetTickCount;
         }
      });

      long netDurationMs = (long) ((double) targetTickCount / MS * nanoSeconds_);
      long deadline = System.currentTimeMillis() + Math.max(1000, netDurationMs * 4);
      sut.start();

      while (!finished && System.currentTimeMillis() < deadline)
         SleepWakeup.sleep(1);

      sut.removeListener(listener);
      sut.stop();
      finished = true;

      deltaCount = 0;
      long lastValidTimestamp = 0;
      for (int i = 0; i < targetTickCount; i++) {
         if (lastValidTimestamp != 0 && buffer[i] != 0)
            deltas[deltaCount++] = buffer[i] - lastValidTimestamp;
         if (buffer[i] != 0)
            lastValidTimestamp = buffer[i];
      }
   }

   private static double average() {
      out_min = null;
      double sum = 0;
      for (int i = 0; i < deltaCount; i++) {
         if (out_min != null)
            out_min = Math.min(out_min, deltas[i]);
         else
            out_min = deltas[i];
         sum += deltas[i];
      }
      return sum / deltaCount;
   }

   private static String format(double d) {
      return new DecimalFormat("0.00").format(d);
   }
}

package at.borkowski.spicej.proxy;

public class DelayCalculator {
   private DelayCalculator() {}

   public static final float DEFAULT_EPSILON = 0.05F;

   public static final long MAX_INTERVAL = 1000 * 1000000;

   public static Result calculate(long nanoseconds) {
      return calculate(nanoseconds, DEFAULT_EPSILON);
   }

   public static Result calculate(long nanoseconds, float epsilon) {
      if (epsilon <= 0 || epsilon >= 1)
         throw new IllegalArgumentException("0 < epsilon < 1 not satisfied");
      
      long max_divider = (int)(1D / epsilon);
      if (nanoseconds < max_divider)
         return new Result(nanoseconds, 1);

      long interval = Math.min(MAX_INTERVAL, nanoseconds / max_divider);
      long correction = nanoseconds / interval;

      return new Result(interval, correction);
   }

   public static class Result {
      private final long tickNanosecondsInterval;
      private final long delay;

      private Result(long tickNanosecondsInterval, long delay) {
         this.tickNanosecondsInterval = tickNanosecondsInterval;
         this.delay = delay;
      }

      public long getDelay() {
         return delay;
      }

      public long getTickNanosecondsInterval() {
         return tickNanosecondsInterval;
      }
   }
}

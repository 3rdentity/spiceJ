package at.borkowski.spicej.proxy;

public class DelayCalculator {
   private DelayCalculator() {}

   public static final long MAX_DIVIDER = 200;
   public static final long MAX_INTERVAL = 1000 * 1000000;

   public static Result calculate(long nanoseconds) {
      if (nanoseconds < MAX_DIVIDER)
         return new Result(nanoseconds, 1);

      long interval = Math.min(MAX_INTERVAL, nanoseconds / MAX_DIVIDER);
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

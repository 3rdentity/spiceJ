package at.borkowski.spicej.rt;

import at.borkowski.spicej.streams.DelayedInputStream;

/**
 * A utility class allowing easy calculation of parameters required for creating
 * a {@link DelayedInputStream} with real-time delay.
 * 
 * Two parameters are necessary for creating a delayed stream: The tick interval
 * in nanoseconds, and the delay in ticks. A low interval has the advantage of a
 * more precise delay, but the disadvantage of higher CPU load.
 * 
 * This class uses the notion of an <b>epsilon</b> value to specify the allowed
 * error in the result delay. The error results from a higher tick interval.
 * 
 * The default epsilon value is {@link #DEFAULT_EPSILON}.
 * 
 * Epsilon must be between 0 and 1, exclusively. The theoretical value of 0
 * means no tolerated error at all, and the value of 1 means that an error of
 * 100% is permitted. A value of 0.01 means a permitted error of 1%, and so on.
 * 
 * Note that the actual error may be (much) higher than epsilon, since binding
 * ticks to real time is a real-time application and as such prone to the
 * effects of preemptive computing. However, the theoretical (optimal) error
 * will not exceed epsilon (except for cases where the error is due to integer
 * rounding, this applies to very low delay values).
 */
public class DelayCalculator {
   private DelayCalculator() {}

   /**
    * The default epsilon value (see {@link DelayCalculator}).
    */
   public static final float DEFAULT_EPSILON = 0.05F;

   /**
    * The highest acceptable inverval.
    */
   public static final long MAX_INTERVAL = 1000 * 1000000;

   /**
    * Returns a result for the given delay in nanoseconds and the default
    * epsilon value ({@link #DEFAULT_EPSILON}).
    * 
    * @param nanoseconds
    *           The required delay in nanoseconds
    * @return The calculation result
    */
   public static Result calculate(long nanoseconds) {
      return calculate(nanoseconds, DEFAULT_EPSILON);
   }

   /**
    * Returns a result for the given delay and epsilon (error rate). Epsilon
    * must be between 0 and 1, exclusively. See {@link DelayCalculator} for
    * details.
    * 
    * @param nanoseconds
    *           The required delay in nanoseconds
    * @param epsilon
    *           The permitted error (0 &lt; epsilon &lt; 1)
    * @return The calculation result
    */
   public static Result calculate(long nanoseconds, float epsilon) {
      if (epsilon <= 0 || epsilon >= 1)
         throw new IllegalArgumentException("0 < epsilon < 1 not satisfied");

      long max_divider = (int) (1D / epsilon);
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

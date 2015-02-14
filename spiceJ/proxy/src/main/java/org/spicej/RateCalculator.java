package org.spicej;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RateCalculator {

   public RateCalculator() {}

   public static final float MIN_RATE = 0.000000001F;

   public static final int NS_PER_S = 1000000000;
   public static final int MS_PER_S = 1000000;

   public static final int MIN_INTERVAL_NS = 100 * MS_PER_S; // 100 ms = minimal interval
   public static final int MAX_INTERVAL_NS = NS_PER_S; // 1 second = maximal interval

   public static final long MAX_FRACTIONAL_RATE = NS_PER_S / MIN_INTERVAL_NS;

   public static final int SCALE = 60;

   public static Result calculate(float rateBytesPerSecond) {
      if (rateBytesPerSecond < 1) {
         BigDecimal interval_ = new BigDecimal(NS_PER_S).divide(new BigDecimal(rateBytesPerSecond), SCALE, RoundingMode.HALF_UP);

         while (interval_.compareTo(new BigDecimal(MAX_INTERVAL_NS)) > 0)
            interval_ = interval_.divide(new BigDecimal(2));

         // current rate: 1 [b] / interval [s]
         // prescale = current rate / rate
         //          = 1 [b] / interval / rate

         // interval = min(1 / rate, MAX_INTERVAL_NS)

         // proof that prescale will never be < 1:

         // condition for prescale < 1:
         // 1/(interval * rate) < 1     | * interval
         // 1 / rate < interval         | [A]

         // if LS of min: interval = 1 / rate, then 1 = interval * rate (-> prescale = 1)
         // if RS of min: interval = MAX_INTERVAL_NS
         //    then interval < 1 / rate -> contradiction to [A]

         // int prescale = (int) Math.round((double) 1D / interval / rateBytesPerSecond * NS_PER_S);         
         BigDecimal prescale = new BigDecimal(1);
         prescale = prescale.divide(interval_, SCALE, RoundingMode.HALF_UP);
         prescale = prescale.divide(new BigDecimal(rateBytesPerSecond), SCALE, RoundingMode.HALF_UP);
         prescale = prescale.multiply(new BigDecimal(NS_PER_S));

         if (prescale.compareTo(new BigDecimal(Integer.MAX_VALUE)) > 0)
            throw new IllegalArgumentException("rate too low (necessary prescale too big)");

         return new Result(1, prescale.intValue(), interval_.intValue());
      } else if (rateBytesPerSecond < MAX_FRACTIONAL_RATE) {
         // 1 <= rate < MAX_FRACTIONAL_RATE
         // interval: 1s / rate

         int interval = (int) (NS_PER_S / rateBytesPerSecond);
         return new Result(1, 1, interval);
      } else {
         int interval = MIN_INTERVAL_NS;
         BigDecimal bytespertick = new BigDecimal(rateBytesPerSecond);
         bytespertick = bytespertick.divide(new BigDecimal(NS_PER_S), SCALE, RoundingMode.HALF_UP);
         bytespertick = bytespertick.multiply(new BigDecimal(interval));
         return new Result(bytespertick.intValue(), 1, interval);
      }
   }

   public static class Result {
      private final int bytesPerTick;
      private final int prescale;
      private final int tickNanosecondInterval;

      public Result(int bytesPerTick, int prescale, int tickNanosecondInterval) {
         this.bytesPerTick = bytesPerTick;
         this.prescale = prescale;
         this.tickNanosecondInterval = tickNanosecondInterval;
      }

      public int getBytesPerTick() {
         return bytesPerTick;
      }

      public int getPrescale() {
         return prescale;
      }

      public int getTickNanosecondInterval() {
         return tickNanosecondInterval;
      }
   }
}

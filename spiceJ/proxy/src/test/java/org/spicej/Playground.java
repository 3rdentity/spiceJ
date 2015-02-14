package org.spicej;

import org.spicej.RateCalculator.Result;

public class Playground {
   public static void main(String[] args) {
      Result r = RateCalculator.calculate(1000);
      
      System.out.println(r.getBytesPerTick() + " bpt, " + r.getPrescale() + " ps, " + r.getTickNanosecondInterval() + "ns");
   }
}

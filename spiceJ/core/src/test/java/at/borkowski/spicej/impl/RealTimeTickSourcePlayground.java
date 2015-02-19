package at.borkowski.spicej.impl;

import at.borkowski.spicej.ticks.TickListener;

public class RealTimeTickSourcePlayground {
   static long x;

   public static void main(String[] args) throws InterruptedException {
      int ms = 750;
      RealTimeTickSource src = new RealTimeTickSource(ms * 1000000);
      src.addListener(new TickListener() {

         @Override
         public void tick(long tick) {
            long l = System.currentTimeMillis();
            if (x == 0)
               x = l;
            System.out.println("tick " + tick + " || " + l + " (delta since last: " + (l - x) + "ms )");
            x = l;
         }
      });

      src.start();
      Thread.sleep(7200);
      src.stop();
   }
}

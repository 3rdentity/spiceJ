package org.spicej.impl;

public class SimulationTickSource extends AbstractTimeSource {

   private long tick = 0;

   public void advance() {
      doTick(tick++);
   }

   public long getCurrentTick() {
      return tick;
   }

   @Override
   public void reset() {
      tick = 0;
   }

}

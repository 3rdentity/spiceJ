package org.spicej.impl;

/**
 * A tick source controlled by a simulation, with tick events sent out on
 * demand. Ticks start at 0 and are processed in single-threaded mode.
 */
public class SimulationTickSource extends AbstractTickSource {

   private long tick = 0;

   /**
    * Generates a tick and advances the tick counter by 1.
    */
   public void advance() {
      doTick(tick++);
   }

   /**
    * Returns the current tick.
    */
   public long getCurrentTick() {
      return tick;
   }

   @Override
   public void reset() {
      tick = 0;
   }

}

package org.spicej.ticks;

/**
 * An interface for tick sources, ie. objects generating tick events.
 */
public interface TickSource {
   /**
    * Resets the tick source to its initial state. The exact outcome is not
    * defined and may be either inside the first tick or in an uninitialized
    * tick state.
    */
   void reset();

   /**
    * Adds a listener to fire TICK events to.
    * 
    * @param listener
    */
   void addListener(TickListener listener);

   /**
    * Removes a TICK listener.
    * 
    * @param listener
    */
   void removeListener(TickListener listener);

   /**
    * Returns the current tick.
    */
   long getCurrentTick();
}

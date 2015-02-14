package org.spicej.ticks;

/**
 * An interface for tick sources, ie. objects generating tick events.
 */
public interface TickSource {
   /**
    * Resets the tick source to its initial state.
    */
   void reset();

   /**
    * Adds a listener to fire tick events to.
    * 
    * @param listener
    */
   void addListener(TickListener listener);

   /**
    * Removes a listener.
    * 
    * @param listener
    */
   void removeListener(TickListener listener);

   /**
    * Returns the current tick.
    */
   long getCurrentTick();
}

package at.borkowski.spicej.ticks;

/**
 * An interface for tick sources, ie. objects generating tick events.
 * 
 * Ticks sent to {@link TickListener} objects are guaranteed to not overlap each
 * other, ie. no two threads will enter the same object's
 * {@link TickListener#tick(long)} at the same time.
 */
public interface TickSource {
   /**
    * Resets the tick source to its initial state. The exact outcome is not
    * defined and may be either inside the first tick or in an uninitialized
    * tick state.
    */
   void reset();

   /**
    * Adds a listener to fire tick events to.
    * 
    * @param listener
    *           the listener to add
    */
   void addListener(TickListener listener);

   /**
    * Removes a tick listener.
    * 
    * @param listener
    *           the listener to remove
    */
   void removeListener(TickListener listener);

   /**
    * Returns the current tick.
    * 
    * @return the current tick
    */
   long getCurrentTick();
}

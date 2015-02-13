package org.spicej.ticks;

/**
 * An interface for listening to (receiving) tick events.
 */
public interface TickListener {

   /**
    * Called when a tick event is fired. The tick sequence is given in the
    * parameter and can be any numer - however, it is guaranteed that the number
    * increases by one between two ticks. Integer overflows are handled
    * naturally, ie. the tick {@link Long#MAX_VALUE} is followed by
    * {@link Long#MIN_VALUE}.
    * 
    * @param tick
    *           the tick number
    */
   public void tick(long tick);
}

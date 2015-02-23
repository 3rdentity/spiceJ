package at.borkowski.spicej.ticks;

/**
 * An interface for listening to (receiving) tick events.
 * 
 * Ticks sent to {@link TickListener} objects are guaranteed to not overlap each
 * other, ie. no two threads will enter the same object's
 * {@link TickListener#tick(long)} at the same time.
 */
public interface TickListener {

   /**
    * Called when a tick event is fired. The tick sequence is given in the
    * parameter and can be any numer - however, it is guaranteed that the number
    * increases by one between two ticks. Integer overflows are handled
    * naturally, ie. the tick {@link Long#MAX_VALUE} is followed by
    * {@link Long#MIN_VALUE}.
    * 
    * Furthermore, it is guaranteed that ticks do not overlap each other, ie. no
    * two threads will enter the same object's {@link TickListener#tick(long)}
    * at the same time.
    * 
    * @param tick
    *           the tick number
    */
   public void tick(long tick);
}

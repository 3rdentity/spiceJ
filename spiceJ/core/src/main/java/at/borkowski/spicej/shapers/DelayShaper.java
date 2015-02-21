package at.borkowski.spicej.shapers;

/**
 * An interface for delay shapers.
 */
public interface DelayShaper {

   /**
    * Sets a new delay in ticks. Note that bytes already waiting in queue also
    * obey to this new delay, ie. reducing the delay will transmit all bytes
    * waiting in queue which are due to transmission following the new delay.
    * 
    * @param delay
    *           the new delay in ticks
    */
   // TODO test this
   void setByteRate(long delay);
}

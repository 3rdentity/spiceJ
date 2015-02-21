package at.borkowski.spicej.shapers;

/**
 * An interface for byte rate shapers.
 */
public interface RateShaper {

   /**
    * Sets a new byte rate per tick (after prescaling).
    * 
    * @param byteRate the new byte rate per tick (after prescaling)
    */
   void setByteRate(int byteRate);
}

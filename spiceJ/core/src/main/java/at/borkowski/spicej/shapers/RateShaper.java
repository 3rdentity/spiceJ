package at.borkowski.spicej.shapers;

/**
 * An interface for byte rate shapers.
 */
public interface RateShaper {

   /**
    * Sets a new byte rate (after prescaling).
    * 
    * @param byteRate the new byte rate (after prescaling)
    */
   void setByteRate(int byteRate);
}

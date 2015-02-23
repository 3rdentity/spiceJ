package at.borkowski.spicej.shapers;

/**
 * An interface for byte rate shapers.
 */
public interface RateShaper {

   /**
    * Sets a new byte rate per tick (after prescaling) for this rate shaper.
    * 
    * @param byteRate
    *           the new byte rate per tick (after prescaling)
    */
   void setByteRate(int byteRate);

   /**
    * Returns the bytes per tick (after prescaling) for this rate shaper.
    * 
    * @return the current byte rate
    */
   int getByteRate();

   /**
    * Returns the current prescale value (ie. one of how many ticks is actually
    * counting towards the <i>things per tick</i> notion).
    * 
    * @return the current prescale value
    */
   int getPrescale();

   /**
    * Sets a new prescale value (ie. one of how many ticks is actually counting
    * towards the <i>things per tick</i> notion).
    * 
    * @param prescale
    *           the new prescale
    */
   void setPrescale(int prescale);
}

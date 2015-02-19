package at.borkowski.spicej.streams;

public class RateLimitInputStreamBlackboxPrescaler2Test extends RateLimitInputStreamBlackboxAbstractTest {
   @Override
   protected int getPrescaler() {
      return 2;
   }
}

package at.borkowski.spicej.streams;

public class RateLimitInputStreamBlackboxNoPrescalerTest extends RateLimitInputStreamBlackboxAbstractTest {
   @Override
   protected int getPrescale() {
      return 1;
   }
}

package at.borkowski.spicej.bytes;

public class RateLimitInputStreamBlackboxNoPrescalerTest extends RateLimitInputStreamBlackboxAbstractTest {
   @Override
   protected int getPrescaler() {
      return 1;
   }
}

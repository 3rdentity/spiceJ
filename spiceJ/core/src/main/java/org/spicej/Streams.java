package org.spicej;

import java.io.InputStream;
import java.io.OutputStream;

import org.spicej.bytes.RateLimitInputStream;
import org.spicej.bytes.RateLimitOutputStream;
import org.spicej.ticks.TickSource;

public class Streams {
   private Streams() {}

   public static RateLimitInputStream limitRate(InputStream base, TickSource tickSource, int bytesPerTick, int prescaler) {
      return new RateLimitInputStream(base, tickSource, bytesPerTick, prescaler);
   }

   public static RateLimitOutputStream limitRate(OutputStream base, TickSource tickSource, int bytesPerTick, int prescaler) {
      return new RateLimitOutputStream(base, tickSource, bytesPerTick, prescaler);
   }

}

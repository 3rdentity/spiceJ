package org.spicej;

import java.io.InputStream;
import java.io.OutputStream;

import org.spicej.bytes.RateLimitInputStream;
import org.spicej.bytes.RateLimitOutputStream;
import org.spicej.impl.RealTimeTickSource;
import org.spicej.ticks.TickSource;

public class Streams {
   private Streams() {}

   public static final int REAL_TIME_TICK_MS = 10;
   public static final int REAL_TIME_TICK_NS = REAL_TIME_TICK_MS * 1000000;

   public RateLimitInputStream limitRate(InputStream base, TickSource tickSource, int bytesPerTick) {
      return new RateLimitInputStream(base, tickSource, bytesPerTick);
   }

   public RateLimitOutputStream limitRate(OutputStream base, TickSource tickSource, int bytesPerTick) {
      return new RateLimitOutputStream(base, tickSource, bytesPerTick);
   }

   public RateLimitInputStream limitRate(InputStream base, int nanoSecondsPerTick, int bytesPerSecond) {
      return new RateLimitInputStream(base, new RealTimeTickSource(nanoSecondsPerTick), bytesPerSecond / nanoSecondsPerTick);
   }

   public RateLimitOutputStream limitRate(OutputStream base, int nanoSecondsPerTick, int bytesPerSecond) {
      return new RateLimitOutputStream(base, new RealTimeTickSource(nanoSecondsPerTick), bytesPerSecond / nanoSecondsPerTick);
   }

   public RateLimitInputStream limitRate(InputStream base, int bytesPerSecond) {
      return limitRate(base, REAL_TIME_TICK_NS, bytesPerSecond);
   }

   public RateLimitOutputStream limitRate(OutputStream base, int bytesPerSecond) {
      return limitRate(base, REAL_TIME_TICK_NS, bytesPerSecond);
   }

}

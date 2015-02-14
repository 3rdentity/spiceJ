package org.spicej;

import java.io.InputStream;
import java.io.OutputStream;

import org.spicej.bytes.RateLimitInputStream;
import org.spicej.bytes.RateLimitOutputStream;
import org.spicej.ticks.TickSource;

/**
 * Provides helper functions for easy shaping of streams.
 */
public class Streams {
   private Streams() {}

   /**
    * Creates a byte-rate-limited {@link InputStream}. See
    * {@link RateLimitInputStream#RateLimitInputStream(InputStream, TickSource, int, int)}
    * for detailed information.
    * 
    * @param base
    *           The raw (underlying) {@link InputStream}
    * @param tickSource
    *           The source of ticks
    * @param bytesPerTick
    *           (see
    *           {@link RateLimitInputStream#RateLimitInputStream(InputStream, TickSource, int, int)}
    *           )
    * @param prescale
    *           (see
    *           {@link RateLimitInputStream#RateLimitInputStream(InputStream, TickSource, int, int)}
    *           )
    * @return
    */
   public static RateLimitInputStream limitRate(InputStream base, TickSource tickSource, int bytesPerTick, int prescale) {
      return new RateLimitInputStream(base, tickSource, bytesPerTick, prescale);
   }

   /**
    * Creates a byte-rate-limited {@link OutputStream}. See
    * {@link RateLimitOutputStream#RateLimitOutputStream(OutputStream, TickSource, int, int)}
    * for detailed information.
    * 
    * @param base
    *           The raw (underlying) {@link OutputStream}
    * @param tickSource
    *           The source of ticks
    * @param bytesPerTick
    *           (see
    *           {@link RateLimitOutputStream#RateLimitOutputStream(OutputStream, TickSource, int, int)}
    *           )
    * @param prescaler
    *           (see
    *           {@link RateLimitOutputStream#RateLimitOutputStream(OutputStream, TickSource, int, int)}
    *           )
    * @return
    */
   public static RateLimitOutputStream limitRate(OutputStream base, TickSource tickSource, int bytesPerTick, int prescaler) {
      return new RateLimitOutputStream(base, tickSource, bytesPerTick, prescaler);
   }

}

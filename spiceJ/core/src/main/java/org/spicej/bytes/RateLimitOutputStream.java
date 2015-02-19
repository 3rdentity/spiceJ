package org.spicej.bytes;

import java.io.IOException;
import java.io.OutputStream;

import org.spicej.bytes.RateHelper.IdleNotify;
import org.spicej.shapers.RateShaper;
import org.spicej.ticks.TickSource;

/**
 * Provides an {@link OutputStream} with a limited rate of bytes.
 */
public class RateLimitOutputStream extends OutputStream implements RateShaper {
   private final OutputStream real;
   private int bytesPerTick;

   private final RateHelper rateHelper;

   /**
    * Constructs a byte-rate-limited {@link OutputStream}. See
    * {@link RateLimitInputStream} for a description of the rate parameters
    * (byteRate and prescale).
    */
   public RateLimitOutputStream(OutputStream real, TickSource tickSource, int byteRate, int prescale) {
      this.real = real;
      this.bytesPerTick = byteRate;

      this.rateHelper = new RateHelper(tickSource, byteRate, prescale);
   }

   @Override
   public void setByteRate(int bytesPerTick) {
      this.bytesPerTick = bytesPerTick;
   }

   @Override
   public void write(int b) throws IOException {
      rateHelper.takeOne();
      real.write(b);
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      int done = 0;
      while (done < len)
         done += realWrite(b, off + done, Math.min(len - done, bytesPerTick));
   }

   // len <= num
   private int realWrite(byte[] b, int off, int len) throws IOException {
      int lenToWrite = rateHelper.take(len);

      real.write(b, off, lenToWrite);
      return lenToWrite;
   }

   @Override
   public void write(byte[] b) throws IOException {
      write(b, 0, b.length);
   }

   @Override
   public void close() throws IOException {
      real.close();
      rateHelper.close();
   }

   @Override
   public void flush() throws IOException {
      real.flush();
   }

   public void setNonBlocking(boolean nonBlocking) {
      rateHelper.setNonBlocking(nonBlocking);
   }

   void test__SetIdleNotify(IdleNotify target) {
      rateHelper.test__SetIdleNotify(target);
   }

}

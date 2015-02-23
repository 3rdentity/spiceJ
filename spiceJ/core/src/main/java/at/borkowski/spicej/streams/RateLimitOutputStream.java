package at.borkowski.spicej.streams;

import java.io.IOException;
import java.io.OutputStream;

import at.borkowski.spicej.shapers.RateShaper;
import at.borkowski.spicej.streams.RateHelper.IdleNotify;
import at.borkowski.spicej.ticks.TickSource;

/**
 * Provides an {@link OutputStream} with a limited rate of bytes. For a
 * description of blocking mode, see {@link RateLimitInputStream}.
 */
public class RateLimitOutputStream extends OutputStream implements RateShaper {
   private final OutputStream real;

   private final RateHelper rateHelper;

   /**
    * Constructs a byte-rate-limited {@link OutputStream}. See
    * {@link RateLimitInputStream} for a description of the rate parameters
    * (byteRate and prescale).
    * 
    * @param real
    *           The raw {@link OutputStream} to send to
    * @param tickSource
    *           The tick source to use
    * @param byteRate
    *           The rate in bytes per tick (before prescaler) to use
    * @param prescale
    *           The prescaler to use (see {@link RateLimitInputStream})
    */
   public RateLimitOutputStream(OutputStream real, TickSource tickSource, int byteRate, int prescale) {
      this.real = real;

      this.rateHelper = new RateHelper(tickSource, byteRate, prescale);
   }

   // TODO test this
   @Override
   public void setByteRate(int bytesPerTick) {
      rateHelper.setThingsPerTick(bytesPerTick);
   }

   // TODO test this
   @Override
   public int getByteRate() {
      return rateHelper.getThingsPerTick();
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
         done += realWrite(b, off + done, Math.min(len - done, rateHelper.getThingsPerTick()));
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

   void test__SetIdleNotify(IdleNotify target) {
      rateHelper.test__SetIdleNotify(target);
   }

   /**
    * Sets the non-blocking flag.
    * 
    * @param nonBlocking
    *           whether the stream should be in non-blocking mode (see
    *           {@link RateLimitInputStream}).
    */
   public void setNonBlocking(boolean nonBlocking) {
      rateHelper.setNonBlocking(nonBlocking);
   }
   
   @Override
   public int getPrescale() {
      return rateHelper.getPrescale();
   }

   // TODO: test
   @Override
   public void setPrescale(int prescale) {
      rateHelper.setPrescale(prescale);
   }

   /**
    * Returns the underlying {@link OutputStream}.
    * 
    * @return the underlying stream
    */
   public OutputStream getBaseStream() {
      return real;
   }

   /**
    * Returns the {@link TickSource} this stream uses.
    * 
    * @return the used tick source
    */
   public TickSource getTickSource() {
      return rateHelper.getTickSource();
   }

}

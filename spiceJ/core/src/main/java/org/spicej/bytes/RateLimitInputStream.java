package org.spicej.bytes;

import java.io.IOException;
import java.io.InputStream;

import org.spicej.bytes.RateHelper.IdleNotify;
import org.spicej.shapers.RateShaper;
import org.spicej.ticks.TickSource;

/**
 * Provides an {@link InputStream} with a limited rate of bytes.
 * 
 * A stream has two modes: boring and non-boring. The boring flag modifies he
 * behavior of {@link #read(byte[], int, int)} and {@link #read(byte[])}.
 * 
 * According to {@link InputStream}, they method read between 1 and n bytes,
 * where n is the size of the effective buffer.
 * 
 * If the {@link RateLimitInputStream} is in boring mode, the methods block
 * until all actually available bytes have been read, even if many ticks must be
 * waited for the data to become available according to the set rate.
 * 
 * In non-boring mode, the methods block only for data which can be read
 * immediately.
 * 
 * Streams are non-boring by default.
 * 
 */
public class RateLimitInputStream extends InputStream implements RateShaper {
   private final InputStream real;

   private final RateHelper rateHelper;

   private boolean boring = false;

   /**
    * Constructs a byte-rate-limited {@link InputStream}.
    * 
    * The rate is given using two numbers: bytes per tick, and a prescale.
    * 
    * The prescale divides the incoming ticks by a constant number. A prescale
    * of 1 means that each tick coming from the tick source is counting towards
    * the rate, a prescale of 2 means every second, and so on. The prescale can
    * be used to achieve low byte rates, ie. rates below 1 byte per tick.
    * 
    * For example, a rate of 1 byte per tick and a prescale of 4 means that one
    * byte is read after 4 ticks coming from the tick source.
    * 
    * @param real
    *           The actual {@link InputStream} to read from
    * @param tickSource
    *           The tick source
    * @param byteRate
    *           The rate, ie. the limit of how many bytes per tick (after
    *           prescaling) should be readable from this stream
    * @param prescale
    *           The prescaling, ie. the frequency of ticks to actually consider
    *           for rating.
    */
   public RateLimitInputStream(InputStream real, TickSource tickSource, int byteRate, int prescale) {
      this.real = real;

      this.rateHelper = new RateHelper(tickSource, byteRate, prescale);
   }

   /**
    * Sets a new byte rate.
    */
   @Override
   public void setByteRate(int bytesPerTick) {
      rateHelper.setThingsPerTick(bytesPerTick);
   }

   @Override
   public int read() throws IOException {
      rateHelper.takeOne();

      return real.read();
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int done = 0;
      while (done < len && (done == 0 || available() > 0 || (boring ? real.available() > 0 : false)))
         done += realRead(b, off + done, Math.min(len - done, Math.max(1, rateHelper.getBytesPerTick())));
      return done;
   }

   // len <= num
   private int realRead(byte[] b, int off, int len) throws IOException {
      int lenToRead = rateHelper.take(len);

      int rd = real.read(b, off, lenToRead);
      if (rd != lenToRead)
         rateHelper.giveBack(lenToRead - rd);
      return rd;
   }

   @Override
   public int available() throws IOException {
      return Math.min(real.available(), rateHelper.getTimewiseAvailable());
   }

   @Override
   public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
   }

   @Override
   public void close() throws IOException {
      real.close();
      rateHelper.close();
   }

   @Override
   public synchronized void mark(int readlimit) {
      real.mark(readlimit);
   }

   @Override
   public boolean markSupported() {
      return real.markSupported();
   }

   @Override
   public synchronized void reset() throws IOException {
      real.reset();
   }

   /**
    * Testability function only. See {@link RateHelper}.
    */
   void testEnableFailOnHang() {
      rateHelper.testEnableFailOnHang();
   }

   /**
    * Testability function only. See {@link RateHelper}.
    */
   void testDisableFailOnHang() {
      rateHelper.testDisableFailOnHang();
   }

   /**
    * Testability function only. See {@link RateHelper}.
    */
   void testSetIdleNotify(IdleNotify target) {
      rateHelper.testSetIdleNotify(target);
   }

   /**
    * Sets the boring flag.
    * 
    * @param boring
    */
   public void setBoring(boolean boring) {
      this.boring = boring;
   }

}

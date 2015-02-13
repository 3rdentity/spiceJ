package org.spicej.bytes;

import java.io.IOException;
import java.io.InputStream;

import org.spicej.ticks.TickSource;

public class RateLimitInputStream extends InputStream {
   private final InputStream real;
   private final int bytesPerTick;

   private final RateHelper rateHelper;

   public RateLimitInputStream(InputStream real, TickSource tickSource, int bytesPerTick) {
      this.real = real;
      this.bytesPerTick = bytesPerTick;

      this.rateHelper = new RateHelper(tickSource, bytesPerTick);
   }

   @Override
   public int read() throws IOException {
      rateHelper.takeOne();

      return real.read();
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int done = 0;
      while (done < len && (done == 0 || real.available() > 0))
         done += realRead(b, off + done, Math.min(len - done, bytesPerTick));
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

   void testEnableFailOnHang() {
      rateHelper.testEnableFailOnHang();
   }

   void testDisableFailOnHang() {
      rateHelper.testDisableFailOnHang();
   }

   void testSetIdleNotify(IdleNotify target) {
      rateHelper.testSetIdleNotify(target);
   }

}

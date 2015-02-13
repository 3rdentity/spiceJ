package org.spicej.bytes;

import java.io.IOException;
import java.io.InputStream;

import org.spicej.bytes.RateHelper.IdleNotify;
import org.spicej.shapers.RateShaper;
import org.spicej.ticks.TickSource;

public class RateLimitInputStream extends InputStream implements RateShaper {
   private final InputStream real;

   private final RateHelper rateHelper;

   private boolean boring = false;

   public RateLimitInputStream(InputStream real, TickSource tickSource, int bytesPerTick, int prescaler) {
      this.real = real;

      this.rateHelper = new RateHelper(tickSource, bytesPerTick, prescaler);
   }

   @Override
   public void setBytesPerTick(int bytesPerTick) {
      rateHelper.setBytesPerTick(bytesPerTick);
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

   void testEnableFailOnHang() {
      rateHelper.testEnableFailOnHang();
   }

   void testDisableFailOnHang() {
      rateHelper.testDisableFailOnHang();
   }

   void testSetIdleNotify(IdleNotify target) {
      rateHelper.testSetIdleNotify(target);
   }

   public void setBoring(boolean boring) {
      this.boring = boring;
   }

}

package org.spicej.bytes;

import java.io.IOException;
import java.io.OutputStream;

import org.spicej.ticks.TickSource;

public class RateLimitOutputStream extends OutputStream {
   private final OutputStream real;
   private final int num;

   private final RateHelper rateHelper;

   public RateLimitOutputStream(OutputStream real, TickSource tickSource, int bytesPerTick) {
      this.real = real;
      this.num = bytesPerTick;

      this.rateHelper = new RateHelper(tickSource, bytesPerTick);
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
         done += realWrite(b, off + done, Math.min(len - done, num));
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
   }

   @Override
   public void flush() throws IOException {
      real.flush();
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

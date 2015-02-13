package org.spicej.bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.spicej.impl.SleepWakeup;
import org.spicej.ticks.TickListener;
import org.spicej.ticks.TickSource;

public class RateLimitOutputStream extends OutputStream {

   private final OutputStream real;
   private final int num;

   private boolean testFailOnHang;
   private IdleNotify testIdleNotify;

   private AtomicInteger spent = new AtomicInteger();

   private SleepWakeup sleep = new SleepWakeup();

   public RateLimitOutputStream(OutputStream real, TickSource tickSource, int bytesPerTick) {
      this.real = real;
      this.num = bytesPerTick;

      tickSource.addListener(new Listener());
   }

   @Override
   public void write(int b) throws IOException {
      while (true) {
         int stored = spent.get();
         if (stored >= num)
            sleep();
         if (spent.compareAndSet(stored, stored + 1))
            break;
      }

      real.write(b);
   }
   
   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      int done = 0;
      while (done < len)
         done += realWrite(b, off + done, Math.min(len - done, num));
   }

   private int realWrite(byte[] b, int off, int len) throws IOException {
      int lenToWrite;
      while (true) {
         int stored = spent.get();
         lenToWrite = Math.min(len, num - stored);
         if (stored >= num)
            sleep();
         else if (spent.compareAndSet(stored, stored + lenToWrite))
            break;
      }

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

   private void wakeup() {
      sleep.wakeup();
   }

   private void sleep() {
      if (testIdleNotify != null && testIdleNotify.idle())
         return;
      if (testFailOnHang)
         throw new AssertionError("should not hang");

      sleep.sleep();
   }

   private class Listener implements TickListener {

      @Override
      public void tick(long tick) {
         int stored;
         while (true) {
            stored = spent.get();
            if (stored > num && spent.compareAndSet(stored, stored - num))
               break;
            else if (spent.compareAndSet(stored, 0))
               break;
         }
         wakeup();
      }
   }

   void testEnableFailOnHang() {
      testFailOnHang = true;
   }

   void testDisableFailOnHang() {
      testFailOnHang = false;
   }

   void testSetIdleNotify(IdleNotify target) {
      this.testIdleNotify = target;
   }

}

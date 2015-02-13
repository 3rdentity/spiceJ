package org.spicej.bytes;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.spicej.ticks.TickListener;
import org.spicej.ticks.TickSource;

public class RateLimitInputStream extends InputStream {

   private final InputStream real;
   private final int num;

   private boolean testFailOnHang;
   private IdleNotify testIdleNotify;

   private AtomicInteger spent = new AtomicInteger();
   private int timewiseAvailable = 0;

   private Object lock = new Object();

   public RateLimitInputStream(InputStream real, TickSource tickSource, int bytesPerTick) {
      this.real = real;
      this.num = bytesPerTick;

      tickSource.addListener(new Listener());
   }

   @Override
   public int read() throws IOException {
      while (true) {
         int stored = spent.get();
         if (stored >= num)
            sleep();
         if (spent.compareAndSet(stored, stored + 1))
            break;
      }

      return real.read();
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int done = 0;
      while (done < len && (done == 0 || real.available() > 0))
         done += realRead(b, off + done, Math.min(len - done, num));
      return done;
   }

   // len <= num
   private int realRead(byte[] b, int off, int len) throws IOException {
      int lenToRead;
      while (true) {
         int stored = spent.get();
         lenToRead = Math.min(len, num - stored);
         if (stored >= num)
            sleep();
         else if (spent.compareAndSet(stored, stored + lenToRead))
            break;
      }

      int rd = real.read(b, off, lenToRead);
      if (rd != len) {
         spent.addAndGet(-rd);
         wakeup();
      }
      timewiseAvailable -= rd;
      return rd;
   }

   private void wakeup() {
      synchronized (lock) {
         lock.notifyAll();
      }
   }

   private void sleep() {
      if (testIdleNotify != null && testIdleNotify.idle())
         return;
      if (testFailOnHang)
         throw new AssertionError("should not hang");

      synchronized (lock) {
         try {
            lock.wait(5000);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }
   }

   @Override
   public int available() throws IOException {
      return Math.max(0, Math.min(real.available(), timewiseAvailable));
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
         timewiseAvailable = (int) (num - stored);
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

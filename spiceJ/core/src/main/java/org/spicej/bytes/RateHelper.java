package org.spicej.bytes;

import java.util.concurrent.atomic.AtomicInteger;

import org.spicej.impl.SleepWakeup;
import org.spicej.ticks.TickListener;
import org.spicej.ticks.TickSource;

class RateHelper {
   private final TickSource tickSource;
   private int bytesPerTick;
   private final int prescaler;
   
   private final Listener listener;

   private AtomicInteger spent = new AtomicInteger();
   private SleepWakeup sleep = new SleepWakeup();
   private int timewiseAvailable;

   private boolean testFailOnHang;
   private IdleNotify testIdleNotify;

   RateHelper(TickSource tickSource, int bytesPerTick, int prescaler) {
      this.tickSource = tickSource;
      this.bytesPerTick = bytesPerTick;
      this.prescaler = prescaler;

      timewiseAvailable = bytesPerTick;

      tickSource.addListener(listener = new Listener());
   }
   
   public void close() {
      tickSource.removeListener(listener);
   }

   public void setBytesPerTick(int bytesPerTick) {
      this.bytesPerTick = bytesPerTick;
      timewiseAvailable = (int) (bytesPerTick - spent.get());
   }

   public int getBytesPerTick() {
      return bytesPerTick;
   }

   public void takeOne() {
      while (true) {
         int stored = spent.get();
         if (stored >= bytesPerTick)
            sleep();
         if (spent.compareAndSet(stored, stored + 1))
            break;
      }
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

   public void testEnableFailOnHang() {
      testFailOnHang = true;
   }

   public void testDisableFailOnHang() {
      testFailOnHang = false;
   }

   public void testSetIdleNotify(IdleNotify target) {
      this.testIdleNotify = target;
   }

   private class Listener implements TickListener {
      @Override
      public void tick(long tick) {
         if (prescaler <= 1 || tick % prescaler == 0) {
            int value;
            while (true) {
               int stored = spent.get();
               if (stored > bytesPerTick && spent.compareAndSet(stored, value = (stored - bytesPerTick)))
                  break;
               else if (spent.compareAndSet(stored, (value = 0)))
                  break;
            }
            timewiseAvailable = (int) (bytesPerTick - value);
            wakeup();
         }
      }
   }

   public int getTimewiseAvailable() {
      return Math.max(0, timewiseAvailable);
   }

   public int take(int len) {
      int lenToTake;
      while (true) {
         int stored = spent.get();
         lenToTake = Math.min(len, bytesPerTick - stored);
         if (stored >= bytesPerTick)
            sleep();
         else if (spent.compareAndSet(stored, stored + lenToTake))
            break;
      }
      timewiseAvailable -= lenToTake;
      return lenToTake;
   }

   public void giveBack(int len) {
      spent.addAndGet(-len);
      timewiseAvailable += len;
      wakeup();
   }

   interface IdleNotify {
      boolean idle();
   }

}

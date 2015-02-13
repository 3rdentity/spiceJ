package org.spicej.bytes;

import java.util.concurrent.atomic.AtomicInteger;

import org.spicej.impl.SleepWakeup;
import org.spicej.ticks.TickListener;
import org.spicej.ticks.TickSource;

public class RateHelper {
   private final int bytesPerTick;

   private AtomicInteger spent = new AtomicInteger();
   private SleepWakeup sleep = new SleepWakeup();
   private int timewiseAvailable;

   private boolean testFailOnHang;
   private IdleNotify testIdleNotify;

   RateHelper(TickSource tickSource, int bytesPerTick) {
      this.bytesPerTick = bytesPerTick;

      timewiseAvailable = bytesPerTick;

      tickSource.addListener(new Listener());
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
         int stored;
         while (true) {
            stored = spent.get();
            if (stored > bytesPerTick && spent.compareAndSet(stored, stored - bytesPerTick))
               break;
            else if (spent.compareAndSet(stored, 0))
               break;
         }
         timewiseAvailable = (int) (bytesPerTick - stored);
         wakeup();
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
      return lenToTake;
   }

   public void giveBack(int len) {
      spent.addAndGet(-len);
      timewiseAvailable -= len;
      wakeup();
   }

}

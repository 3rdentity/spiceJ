package org.spicej.impl;

public class RealTimeTickSource extends AbstractTimeSource {
   private final int interval;

   private MyTimer timer;

   public RealTimeTickSource(int nanoSecondsPerTick) {
      this(nanoSecondsPerTick, true);
   }

   public RealTimeTickSource(int nanoSecondsPerTick, boolean start) {
      this.interval = nanoSecondsPerTick;
      if (start)
         start();
   }

   @Override
   public void reset() {
      stop();
   }

   public void start() {
      if (timer != null)
         throw new IllegalStateException("already running");

      timer = new MyTimer();
      Thread thread = new Thread(timer, "timer");
      thread.setDaemon(true);
      thread.start();
   }

   public void stop() {
      if (timer == null)
         return;

      timer.cancel();
      timer = null;
   }

   private class MyTimer implements Runnable {

      private final Object lock = new Object();

      private int tick = 0;
      private boolean cancel = false;

      @Override
      public void run() {
         long nextWakeup = System.nanoTime();

         while (!cancel) {
            if (System.nanoTime() >= nextWakeup) {
               RealTimeTickSource.super.doTick(tick++);
               nextWakeup = nextWakeup + interval;
            }

            synchronized (lock) {
               int period = Math.max(10, (int) ((nextWakeup - System.nanoTime()) / 4));
               try {
                  lock.wait(period / 1000000, period % 1000000);
               } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
               }
            }
         }
      }

      public void cancel() {
         this.cancel = true;
         synchronized (lock) {
            lock.notifyAll();
         }
      }

   }

}

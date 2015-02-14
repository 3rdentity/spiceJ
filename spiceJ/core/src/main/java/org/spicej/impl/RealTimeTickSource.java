package org.spicej.impl;

import org.spicej.ticks.TickListener;

/**
 * A tick source bound to real time. Any tick interval can be specified in
 * nanoseconds, but obviously the platform poses natural bounds for this
 * interval.
 * 
 * The lower bound is the resolution of {@link System#nanoTime()}, the upper
 * bound is {@link Integer#MAX_VALUE}.
 * 
 * Ticks are generated at a fixed tick-to-tick interval, regardless of the
 * duration of handling ticks. If the handling of ticks for all is not finished
 * until the next tick, one or more subsequent ticks will be fired immediately.
 * 
 * Ticks start at 0 and are processed in single-threaded mode.
 * 
 * If the last tick listener has been removed from this source, it shuts itself
 * down via {@link #stop()}, if it's not set to keepAlive mode using
 * {@link #setKeepAlive(boolean)}.
 */
public class RealTimeTickSource extends AbstractTickSource {
   private final int interval;
   private boolean keepAlive = false;

   private MyTimer timer;

   /**
    * Creates a running real time tick source with ticks generated at the given
    * interval.
    * 
    * @param nanoSecondsPerTick
    *           the interval of ticks in nanoseconds
    * 
    */
   public RealTimeTickSource(int nanoSecondsPerTick) {
      this(nanoSecondsPerTick, true);
   }

   /**
    * Creates a tick source with ticks generated at the given interval.
    * 
    * @param nanoSecondsPerTick
    *           the interval of ticks in nanoseconds
    * @param start
    *           whether the tick source should be started upon creation
    */
   public RealTimeTickSource(int nanoSecondsPerTick, boolean start) {
      this.interval = nanoSecondsPerTick;
      if (start)
         start();
   }

   /**
    * Sets the keepAlive mode of this tick source. If keepAlive is false
    * (default), the source shuts itself down via {@link #stop()} upon the
    * removal of the last tick listener. If keepAlive is true, the source stays
    * active.
    * 
    * @param keepAlive
    */
   public void setKeepAlive(boolean keepAlive) {
      this.keepAlive = keepAlive;
   }

   @Override
   public void reset() {
      stop();
   }

   /**
    * If the last tick listener has been removed from this source, it shuts
    * itself down via {@link #stop()}, if it's not set to keepAlive mode using
    * {@link #setKeepAlive(boolean)}.
    */
   @Override
   public void removeListener(TickListener listener) {
      super.removeListener(listener);

      if (listeners.isEmpty() && !keepAlive)
         stop();
   }

   /**
    * Starts generating tick events, starting from 0. This method must not be
    * called when the tick source is already running.
    */
   public void start() {
      if (timer != null)
         throw new IllegalStateException("already running");

      timer = new MyTimer();
      Thread thread = new Thread(timer, "timer");
      thread.setDaemon(true);
      thread.start();
   }

   /**
    * Stops generating tick events. It is not guaranteed how many tick events
    * are generated after the completion of this method, but best effort is made
    * to stop the ticks as soon as possible.
    */
   public void stop() {
      if (timer == null)
         return;

      timer.cancel();
      timer = null;
   }

   private class MyTimer implements Runnable {

      private final Object lock = new Object();

      private boolean cancel = false;

      @Override
      public void run() {
         long nextWakeup = System.nanoTime();

         while (!cancel) {
            if (System.nanoTime() >= nextWakeup) {
               RealTimeTickSource.super.doTick();
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

      void cancel() {
         this.cancel = true;
         synchronized (lock) {
            lock.notifyAll();
         }
      }

   }

}

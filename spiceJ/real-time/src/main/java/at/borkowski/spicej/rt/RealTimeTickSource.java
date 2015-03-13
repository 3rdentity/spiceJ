package at.borkowski.spicej.rt;

import at.borkowski.spicej.impl.AbstractTickSource;
import at.borkowski.spicej.ticks.TickListener;

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
   private final long interval;
   private boolean keepAlive = false;

   private MyTimer timer;

   /**
    * The threshold below which busy waiting is employed. Waiting via Java sleep
    * methods has the advantage of reduced CPU usage, however, a certain time
    * cost is encountered.
    * 
    * Therefore, intervals below {@link #BUSY_WAITING_THRESHOLD} are implemented
    * with busy waiting. Intervals above are implemented by synchronized
    * waiting.
    * 
    * On the current development machine, this threshold had to be at least a
    * value of 4.2 ms to ensure the real-time tests passing, which is why 15 ms
    * have been chosen as a margin. Above 10 ms of threshold, an constat error
    * rate of less than 1 % was measured.
    */
   public static final long BUSY_WAITING_THRESHOLD = 10 * 1000000;

   /**
    * The threshold above which a different implementation of the internal timer
    * is used, which is only considering milliseconds. This means that above
    * this threshold, the precision of this tick source is reduced to
    * milliseconds.
    */
   public static final long MILLISECOND_THRESHOLD = 50 * 1000000;

   /**
    * Creates a running real time tick source with ticks generated at the given
    * interval.
    * 
    * @param nanoSecondsPerTick
    *           the interval of ticks in nanoseconds
    * 
    */
   public RealTimeTickSource(long nanoSecondsPerTick) {
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
   public RealTimeTickSource(long nanoSecondsPerTick, boolean start) {
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
    *           whether the keepAlive mode should be on
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

      if (interval < BUSY_WAITING_THRESHOLD)
         timer = new MyBusyTimer();
      else if (interval < MILLISECOND_THRESHOLD)
         timer = new MyWaitingTimer();
      else
         timer = new MyMillisecondTimer();

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

   private abstract class MyTimer implements Runnable {

      protected boolean cancel = false;

      void cancel() {
         this.cancel = true;
      }
   }

   private class MyBusyTimer extends MyTimer {

      @Override
      public void run() {
         long nextWakeup = System.nanoTime();

         while (!cancel) {
            if (System.nanoTime() >= nextWakeup) {
               RealTimeTickSource.super.doTick();
               nextWakeup = nextWakeup + interval;
            }
         }
      }

   }

   private class MyWaitingTimer extends MyTimer {

      private final Object lock = new Object();

      @Override
      public void run() {
         long nextWakeup = System.nanoTime();

         while (!cancel) {
            if (System.nanoTime() >= nextWakeup) {
               RealTimeTickSource.super.doTick();
               nextWakeup = nextWakeup + interval;
            }

            long period = (long) ((nextWakeup - System.nanoTime()) / 4);

            /* 
             * don't sync-sleep below 1 ms per sleep slice, since method
             * call overhead will take loner than that (1 ms is very long
             * for a method call, this is a safety margin for slower systems)
             */
            if (period > 1 * 1000000) {
               period = Math.min(period, 10 * 1000000);
               try {
                  synchronized (lock) {
                     lock.wait(period / 1000000, (int) (period % 1000000));
                  }
               } catch (InterruptedException ignore) {}
            }
         }
      }

      void cancel() {
         super.cancel();
         synchronized (lock) {
            lock.notifyAll();
         }
      }

   }

   private class MyMillisecondTimer extends MyTimer {

      private final Object lock = new Object();

      @Override
      public void run() {
         long nextWakeup = System.currentTimeMillis();

         while (!cancel) {
            if (System.currentTimeMillis() >= nextWakeup) {
               RealTimeTickSource.super.doTick();
               nextWakeup = nextWakeup + interval / 1000000;
            }

            long period = (long) ((nextWakeup - System.currentTimeMillis()) / 4);

            /* 
             * don't sync-sleep below 1 ms per sleep slice, since method
             * call overhead will take loner than that (1 ms is very long
             * for a method call, this is a safety margin for slower systems)
             */
            if (period > 5) {
               period = Math.min(period, 100);
               try {
                  synchronized (lock) {
                     lock.wait(period);
                  }
               } catch (InterruptedException ignore) {}
            }
         }
      }

      void cancel() {
         super.cancel();
         synchronized (lock) {
            lock.notifyAll();
         }
      }

   }

}

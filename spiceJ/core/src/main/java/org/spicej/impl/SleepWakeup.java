package org.spicej.impl;

/**
 * Provides helper methods for sleeping and waking sleepers up.
 */
public class SleepWakeup {
   private final Object lock = new Object();

   /**
    * Wakes up all sleeping threads currently in {@link #sleep()}.
    */
   public void wakeup() {
      synchronized (lock) {
         lock.notifyAll();
      }
   }

   /**
    * Blocks the calling thread until another thread calls this object's
    * {@link #wakeup()}.
    */
   public void sleep() {
      synchronized (lock) {
         try {
            lock.wait(5000);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }
   }
}

package org.spicej.impl;

public class SleepWakeup {
   private final Object lock = new Object();

   public void wakeup() {
      synchronized (lock) {
         lock.notifyAll();
      }
   }

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

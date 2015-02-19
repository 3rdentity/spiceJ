package at.borkowski.spicej.impl;

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

   /**
    * Sleeps for the provided amount of milliseconds, ignoring thread
    * interrupts.
    * 
    * @param ms
    */
   public static void sleep(int ms) {
      long deadline = System.currentTimeMillis() + ms;
      while (System.currentTimeMillis() < deadline) {
         try {
            Thread.sleep(Math.max(1, (deadline - System.currentTimeMillis()) / 2));
         } catch (InterruptedException _) {}
      }
   }
}

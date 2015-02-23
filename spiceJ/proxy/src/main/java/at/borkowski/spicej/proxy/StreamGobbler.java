package at.borkowski.spicej.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A stream gobbler, copying data from an {@link InputStream} to an
 * {@link OutputStream}.
 */
public class StreamGobbler implements Runnable {
   private final InputStream is;
   private final OutputStream os;

   private Thread thread;
   private Throwable t = null;
   private boolean finished = false;
   private boolean cancel = false;

   StreamGobbler(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
   }

   public void run() {
      thread = Thread.currentThread();
      try {
         byte[] block = new byte[1024];
         while (!cancel) {
            int rd = is.read(block);
            if (rd == -1)
               break;
            os.write(block, 0, rd);
            os.flush();
         }
      } catch (Throwable t) {
         this.t = t;
      } finally {
         try {
            os.close();
         } catch (IOException ignore) {}
         try {
            is.close();
         } catch (IOException ignore) {}

         finished = true;
         synchronized (this) {
            this.notifyAll();
         }
      }
   }

   /**
    * Waits until the gobbler finished. If the gobbler encountered any
    * exception, it is thrown from this method.
    * 
    * @throws Throwable
    *            if the gobbler has thrown an exception
    */
   public void waitFor() throws Throwable {
      while (!finished) {
         try {
            synchronized (this) {
               this.wait(100);
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }

      if (t != null)
         throw t;
   }

   /**
    * Interrupts the gobbler and closes its streams.
    */
   public void close() {
      cancel = true;
      try {
         thread.interrupt();
         waitFor();
      } catch (Throwable ignore) {}

      try {
         is.close();
      } catch (Exception ignore) {}

      try {
         os.close();
      } catch (Exception ignore) {}
   }
}

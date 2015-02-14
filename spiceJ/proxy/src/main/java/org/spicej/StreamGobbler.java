package org.spicej;

import java.io.InputStream;
import java.io.OutputStream;

class StreamGobbler implements Runnable {
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

         os.close();
         is.close();
      } catch (Throwable t) {
         this.t = t;
      } finally {
         finished = true;
         synchronized (this) {
            this.notifyAll();
         }
      }
   }

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

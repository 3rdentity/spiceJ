package at.borkowski.spicej.testutil;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import at.borkowski.spicej.impl.SimulationTickSource;
import at.borkowski.spicej.impl.SleepWakeup;

public class InputStreamReaderRecorder {

   private final SimulationTickSource t;
   private long[] recording;
   private boolean running = true;

   private final SleepWakeup sleepFinish = new SleepWakeup();

   public InputStreamReaderRecorder(SimulationTickSource t) {
      this.t = t;
   }

   public void assertTimestamps(long... expected) {
      waitFor();
      if (!Arrays.equals(expected, recording)) {
         System.err.println("exp: " + Arrays.toString(expected));
         System.err.println("act: " + Arrays.toString(recording));
      }
      assertArrayEquals(expected, recording);
   }

   public void startRecording(InputStream sut, int target, int blockSize) {
      recording = new long[target];
      running = true;
      Thread t = new Thread(new Runner(sut, target, blockSize));
      t.setDaemon(true);
      t.start();
   }

   public void waitFor() {
      while (running)
         sleepFinish.sleep();
   }

   private class Runner implements Runnable {
      private InputStream suti;
      private long t0;
      private int target;
      private int blockSize;

      public Runner(InputStream sut, int target, int blockSize) {
         this.suti = sut;
         this.target = target;
         this.blockSize = blockSize;

         t0 = t.getCurrentTick();
      }

      @Override
      public void run() {
         try {
            int done = 0;
            byte[] block = new byte[blockSize];

            while (done < target) {
               try {
                  int result = blockSize == 0 ? suti.read() : suti.read(block, 0, Math.min(blockSize, target - done));

                  if (result == -1)
                     throw new AssertionError("not enough bytes");

                  int rd = blockSize == 0 ? 1 : result;

                  for (int i = done; i < done + rd; i++)
                     recording[i] = t.getCurrentTick() - t0;
                  done += rd;
               } catch (IOException e) {
                  throw new Error(e);
               }
            }
         } finally {
            running = false;
            sleepFinish.wakeup();
         }
      }
   }
}

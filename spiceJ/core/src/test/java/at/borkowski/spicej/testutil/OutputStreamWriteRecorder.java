package at.borkowski.spicej.testutil;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import at.borkowski.spicej.ticks.TickSource;

public class OutputStreamWriteRecorder extends OutputStream {

   private final TickSource t;
   
   private long t0;

   private long[] recording;
   private int count = 0;

   public OutputStreamWriteRecorder(TickSource t) {
      this.t = t;
   }

   private void write() {
      recording[count++] = t.getCurrentTick() - t0;
   }

   @Override
   public void write(int b) throws IOException {
      if (recording == null)
         return;
      write();
   }

   @Override
   public void write(byte[] b) throws IOException {
      if (recording == null)
         return;
      for (int i = 0; i < b.length; i++)
         write();
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      if (recording == null)
         return;
      for (int i = 0; i < len; i++)
         write();

   }

   public void assertTimestamps(long... expected) {
      if (!Arrays.equals(expected, recording)) {
         System.err.println("exp: " + Arrays.toString(expected));
         System.err.println("act: " + Arrays.toString(recording));
      }
      assertArrayEquals(expected, recording);
   }

   public void startRecording(int target) {
      t0 = t.getCurrentTick();
      recording = new long[target];
   }
}

package org.spicej.bytes;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.spicej.bytes.RateHelper.IdleNotify;
import org.spicej.impl.SimulationTickSource;
import org.spicej.testutil.InputStreamReaderRecorder;

public abstract class RateLimitInputStreamBlackboxAbstractTest {

   private PipedOutputStream pos;
   private RateLimitInputStream sut;
   private SimulationTickSource t;
   private InputStreamReaderRecorder recorder;

   @Before
   public void setUp() throws Exception {
      PipedInputStream pis = new PipedInputStream();
      pos = new PipedOutputStream(pis);

      t = new SimulationTickSource();
      sut = new RateLimitInputStream(pis, t, 10, getPrescaler());
      sut.testEnableFailOnHang();
      sut.testSetIdleNotify(new IdleNotify() {

         @Override
         public boolean idle() {
            t.advance();
            return true;
         }

      });
      
      t.advance(); // enter t = 0

      for (int i = 0; i < getPrescaler(); i++) {
         t.advance(); // enter t = 1 * prescaler
         t.advance(); // enter t = 2 * prescaler
         t.advance(); // enter t = 3 * prescaler
      }

      recorder = new InputStreamReaderRecorder(t);
   }

   protected abstract int getPrescaler();

   private void test(int number, int blockSize, long[] expected, boolean boring) throws IOException {
      for (int i = 0; i < expected.length; i++)
         expected[i] *= getPrescaler();

      sut.setBoring(boring);
      recorder.startRecording(sut, number, blockSize);
      pos.write(new byte[number]);
      pos.flush();
      recorder.assertTimestamps(expected);
   }

   @Test
   public void testOneTickBytewise() throws IOException {
      test(6, 0, new long[] { 0, 0, 0, 0, 0, 0 }, false);
   }

   @Test
   public void testOneTickBytewise_boring() throws IOException {
      test(6, 0, new long[] { 0, 0, 0, 0, 0, 0 }, true);
   }

   @Test
   public void testOneTickSmallBuffer() throws IOException {
      test(6, 4, new long[] { 0, 0, 0, 0, 0, 0 }, false);
   }

   @Test
   public void testOneTickSmallBuffer_boring() throws IOException {
      test(6, 4, new long[] { 0, 0, 0, 0, 0, 0 }, true);
   }

   @Test
   public void testOneTickExactBuffer() throws IOException {
      test(6, 6, new long[] { 0, 0, 0, 0, 0, 0 }, false);
   }

   @Test
   public void testOneTickExactBuffer_boring() throws IOException {
      test(6, 6, new long[] { 0, 0, 0, 0, 0, 0 }, true);
   }

   @Test
   public void testOneTickExcessBuffer() throws IOException {
      test(6, 8, new long[] { 0, 0, 0, 0, 0, 0 }, false);
   }

   @Test
   public void testOneTickExcessBuffer_boring() throws IOException {
      test(6, 8, new long[] { 0, 0, 0, 0, 0, 0 }, true);
   }

   @Test
   public void testTwoTicksBytewise() throws IOException {
      test(13, 0, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, false);
   }

   @Test
   public void testTwoTicksBytewise_boring() throws IOException {
      test(13, 0, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, true);
   }

   @Test
   public void testTwoTicksSmallBuffer() throws IOException {
      test(13, 4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, false);
   }

   @Test
   public void testTwoTicksSmallBuffer_boring() throws IOException {
      test(13, 4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1 }, true);
   }

   @Test
   public void testTwoTicksBlockBuffer() throws IOException {
      test(13, 10, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, false);
   }

   @Test
   public void testTwoTicksBlockBuffer_boring() throws IOException {
      test(13, 10, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, true);
   }

   @Test
   public void testTwoTicksOverBlockBuffer() throws IOException {
      test(13, 11, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, false);
   }

   @Test
   public void testTwoTicksOverBlockBuffer_boring() throws IOException {
      test(13, 11, new long[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, true);
   }

   @Test
   public void testTwoTicksExactBuffer() throws IOException {
      test(13, 13, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, false);
   }

   @Test
   public void testTwoTicksExactBuffer_boring() throws IOException {
      test(13, 13, new long[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, true);
   }

   @Test
   public void testTwoTicksExcessBuffer() throws IOException {
      test(13, 15, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 }, false);
   }

   @Test
   public void testTwoTicksExcessBuffer_boring() throws IOException {
      test(13, 15, new long[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, true);
   }

   @Test
   public void testThreeTicksBytewise() throws IOException {
      test(23, 0, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, false);
   }

   @Test
   public void testThreeTicksBytewise_boring() throws IOException {
      test(23, 0, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, true);
   }

   @Test
   public void testThreeTicksSmallBuffer() throws IOException {
      test(23, 4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, false);
   }

   @Test
   public void testThreeTicksSmallBuffer_boring() throws IOException {
      test(23, 4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, true);
   }

   @Test
   public void testThreeTicksBlockBuffer() throws IOException {
      test(23, 10, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, false);
   }

   @Test
   public void testThreeTicksBlockBuffer_boring() throws IOException {
      test(23, 10, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, true);
   }

   @Test
   public void testThreeTicksOverBlockBuffer() throws IOException {
      test(23, 11, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, false);
   }

   @Test
   public void testThreeTicksOverBlockBuffer_boring() throws IOException {
      test(23, 11, new long[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 }, true);
   }

   @Test
   public void testThreeTicksExactBuffer() throws IOException {
      test(23, 13, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, false);
   }

   @Test
   public void testThreeTicksExactBuffer_boring() throws IOException {
      test(23, 13, new long[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 }, true);
   }

   @Test
   public void testThreeTicksExcessBuffer() throws IOException {
      test(23, 25, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 }, false);
   }

   @Test
   public void testThreeTicksExcessBuffer_boring() throws IOException {
      test(23, 25, new long[] { 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 }, true);
   }
}

package org.spicej.bytes;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.spicej.bytes.RateHelper.IdleNotify;
import org.spicej.impl.SimulationTickSource;
import org.spicej.testutil.OutputStreamWriteRecorder;

public class RateLimitOutputStreamBlackboxTest {

   private RateLimitOutputStream sut;
   private SimulationTickSource t;
   private OutputStreamWriteRecorder ros;

   @Before
   public void setUp() throws Exception {
      t = new SimulationTickSource();
      sut = new RateLimitOutputStream(ros = new OutputStreamWriteRecorder(t), t, 10, 1);
      sut.test__EnableFailOnHang();
      sut.test__SetIdleNotify(new IdleNotify() {

         @Override
         public boolean idle() {
            t.advance();
            return true;
         }

      });

      t.advance();
      t.advance();
      t.advance();
   }

   private void test(int number, int blockSize, long[] expected) throws IOException {
      ros.startRecording(number);
      if (blockSize == 0) {
         for (int i = 0; i < number; i++) {
            sut.write(137);
            sut.flush();
         }
      } else {
         byte[] blk = new byte[blockSize];
         int done = 0;
         while (done < number) {
            int wr = Math.min(blockSize, number - done);
            sut.write(blk, 0, wr);
            sut.flush();
            done += wr;
         }
      }
      ros.assertTimestamps(expected);
   }

   @Test
   public void testOneTickBytewise() throws IOException {
      test(6, 0, new long[] { 0, 0, 0, 0, 0, 0 });
   }

   @Test
   public void testOneTickSmallBuffer() throws IOException {
      test(6, 4, new long[] { 0, 0, 0, 0, 0, 0 });
   }

   @Test
   public void testOneTickExactBuffer() throws IOException {
      test(6, 6, new long[] { 0, 0, 0, 0, 0, 0 });
   }

   @Test
   public void testOneTickExcessBuffer() throws IOException {
      test(6, 8, new long[] { 0, 0, 0, 0, 0, 0 });
   }

   @Test
   public void testTwoTicksBytewise() throws IOException {
      test(13, 0, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 });
   }

   @Test
   public void testTwoTicksSmallBuffer() throws IOException {
      test(13, 4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 });
   }

   @Test
   public void testTwoTicksBlockBuffer() throws IOException {
      test(13, 10, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 });
   }

   @Test
   public void testTwoTicksOverBlockBuffer() throws IOException {
      test(13, 11, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 });
   }

   @Test
   public void testTwoTicksExactBuffer() throws IOException {
      test(13, 13, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 });
   }

   @Test
   public void testTwoTicksExcessBuffer() throws IOException {
      test(13, 15, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1 });
   }

   @Test
   public void testThreeTicksBytewise() throws IOException {
      test(23, 0, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });
   }

   @Test
   public void testThreeTicksSmallBuffer() throws IOException {
      test(23, 4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });
   }

   @Test
   public void testThreeTicksBlockBuffer() throws IOException {
      test(23, 10, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });
   }

   @Test
   public void testThreeTicksOverBlockBuffer() throws IOException {
      test(23, 11, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });
   }

   @Test
   public void testThreeTicksExactBuffer() throws IOException {
      test(23, 13, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });
   }

   @Test
   public void testThreeTicksExcessBuffer() throws IOException {
      test(23, 15, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });
   }
}

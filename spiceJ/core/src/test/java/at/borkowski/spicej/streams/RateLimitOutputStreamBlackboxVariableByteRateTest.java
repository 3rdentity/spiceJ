package at.borkowski.spicej.streams;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import at.borkowski.spicej.impl.SimulationTickSource;
import at.borkowski.spicej.streams.RateHelper.IdleNotify;
import at.borkowski.spicej.testutil.OutputStreamWriteRecorder;

public class RateLimitOutputStreamBlackboxVariableByteRateTest {

   private RateLimitOutputStream sut;
   private SimulationTickSource t;
   private OutputStreamWriteRecorder ros;

   private int prescale = 4;

   @Before
   public void setUp() throws Exception {
      t = new SimulationTickSource();
      sut = new RateLimitOutputStream(ros = new OutputStreamWriteRecorder(t), t, 10, prescale);
      sut.setNonBlocking(true);
      sut.test__SetIdleNotify(new IdleNotify() {

         @Override
         public boolean idle() {
            t.advance();
            return true;
         }

      });

      advanceWithPrescale(3);
   }

   private void advanceWithPrescale(int ticks) {
      while (t.getCurrentTick() % prescale != 0)
         t.advance();
      for (int i = 0; i < prescale * ticks; i++)
         t.advance();
   }

   private void test(int blockSize, long[] expected) throws IOException {
      for (int i = 0; i < expected.length; i++)
         expected[i] *= prescale;
      testReal(blockSize, expected);
   }

   private void testReal(int blockSize, long[] expected) throws IOException {
      ros.startRecording(expected.length);
      if (blockSize == 0) {
         for (int i = 0; i < expected.length; i++) {
            sut.write(137);
            sut.flush();
         }
      } else {
         byte[] blk = new byte[blockSize];
         int done = 0;
         while (done < expected.length) {
            int wr = Math.min(blockSize, expected.length - done);
            sut.write(blk, 0, wr);
            sut.flush();
            done += wr;
         }
      }
      ros.assertTimestamps(expected);
   }

   @Test
   public void testChangeByteRateInIdle_1() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });

      sut.setByteRate(4);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3 });

      sut.setByteRate(12);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 });
   }

   @Test
   public void testChangeByteRateInIdle_2() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });

      advanceWithPrescale(3);
      sut.setByteRate(4);

      test(4, new long[] { 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3 });

      advanceWithPrescale(3);
      sut.setByteRate(12);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 });
   }

   @Test
   public void testChangeByteRateInTick() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });

      sut.setByteRate(4); // 3 consumed, 1 left

      test(4, new long[] { 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3 });

      sut.setByteRate(12); // 3 consumed, 9 left

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 });
   }

   @Test
   public void testChangePrescaleInIdle_1() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });

      sut.setPrescale(prescale = 2);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });

      sut.setPrescale(prescale = 12);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });
   }

   @Test
   public void testChangePrescaleInIdle_2() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });

      advanceWithPrescale(3);
      sut.setPrescale(prescale = 2);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2 });

      advanceWithPrescale(3);
      sut.setPrescale(prescale = 12);
      // prescale is not aligned and t is now at -6 (mod prescale)
      // hence the 6s after the 0s

      testReal(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 18, 18, 18 });
   }

   @Test
   public void testChangePrescaleInTick() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      sut.setPrescale(prescale = 1);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 });

      sut.setPrescale(prescale = 8);
      // 17 % 8 = 1, 1 tick after handling tick; 1 spent

      // the 0's here would have been available at t0 - 2 (so they're mathematically -2's)
      // that's why they are followed by 6's (-2 + 8 = 6)
      testReal(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 14 });
   }
}

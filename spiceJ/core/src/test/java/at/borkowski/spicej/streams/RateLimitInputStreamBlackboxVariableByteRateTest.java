package at.borkowski.spicej.streams;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import at.borkowski.spicej.impl.SimulationTickSource;
import at.borkowski.spicej.streams.RateHelper.IdleNotify;
import at.borkowski.spicej.streams.util.PipedInputStream;
import at.borkowski.spicej.streams.util.PipedOutputStream;
import at.borkowski.spicej.testutil.InputStreamReaderRecorder;

public class RateLimitInputStreamBlackboxVariableByteRateTest {

   private PipedOutputStream pos;
   private RateLimitInputStream sut;
   private SimulationTickSource t;
   private InputStreamReaderRecorder recorder;
   private int prescale = 4;

   @Before
   public void setUp() throws Exception {
      PipedInputStream pis = new PipedInputStream();
      pos = new PipedOutputStream(pis);

      t = new SimulationTickSource();
      sut = new RateLimitInputStream(pis, t, 10, prescale);
      sut.setNonBlocking(true);
      sut.setBoring(false);
      sut.test__SetIdleNotify(new IdleNotify() {

         @Override
         public boolean idle() {
            t.advance();
            return true;
         }

      });

      t.advance(); // enter t = 0
      advanceWithPrescale(3);

      recorder = new InputStreamReaderRecorder(t);
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
      testActualTimestamps(blockSize, expected);
   }

   private void testActualTimestamps(int blockSize, long[] expected) throws IOException {
      recorder.startRecording(sut, expected.length, blockSize);
      pos.write(new byte[expected.length]);
      pos.flush();
      recorder.assertTimestamps(expected);
   }

   @Test
   public void testChangeByteRateInIdle_1() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      advanceWithPrescale(3);
      sut.setByteRate(5);

      test(4, new long[] { 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2 });

      advanceWithPrescale(3);
      sut.setByteRate(15);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 });
   }

   @Test
   public void testChangeByteRateInIdle_2() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      sut.setByteRate(5);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2 });

      sut.setByteRate(15);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 });
   }

   @Test
   public void testChangeByteRateInTick() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      sut.setByteRate(5); // 4 spent in tick, after byterate change 1 left in tick

      test(4, new long[] { 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 3, 3 });

      sut.setByteRate(15); // 3 spent in tick, after byterate change 12 left in tick

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2 });
   }

   @Test
   public void testChangePrescaleInIdle_1() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      advanceWithPrescale(3);
      sut.setPrescale(prescale = 1);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      advanceWithPrescale(3);
      sut.setPrescale(prescale = 8);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });
   }

   @Test
   public void testChangePrescaleInIdle_2() throws IOException {
      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      sut.setPrescale(prescale = 1);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });

      sut.setPrescale(prescale = 8);
      advanceWithPrescale(3);

      test(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1 });
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
      testActualTimestamps(4, new long[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 14 });
   }
}

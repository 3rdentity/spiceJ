package org.spicej.bytes;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.spicej.bytes.RateHelper.IdleNotify;
import org.spicej.impl.SimulationTickSource;

public class RateHelperTest {

   private RateHelper sut;
   private SimulationTickSource t;
   private boolean autoAdvance = true;
   private long t0;

   @Before
   public void setUp() throws Exception {
      t = new SimulationTickSource();
      sut = new RateHelper(t, 10, 1);
      sut.setNonBlocking(true);
      sut.test__SetIdleNotify(new IdleNotify() {

         @Override
         public boolean idle() {
            if (autoAdvance)
               t.advance();
            return autoAdvance;
         }

      });

      t.advance();
      t.advance();
      t.advance();
      t0 = t.getCurrentTick();
   }

   @Test
   public void testTakeOne() throws IOException {
      sut.take(1);
      assertEquals(t0, t.getCurrentTick());
   }

   @Test
   public void testTakeOneTick() throws IOException {
      sut.take(9);
      assertEquals(t0, t.getCurrentTick());
      sut.takeOne();
      assertEquals(t0, t.getCurrentTick());
      sut.takeOne();
      assertEquals(t0 + 1, t.getCurrentTick());
   }

   @Test
   public void testSufficient() throws IOException {
      sut.take(3);
      assertEquals(t0, t.getCurrentTick());
   }

   @Test
   public void testInsufficient1extra() throws IOException {
      int rd = sut.take(13);
      assertEquals(10, rd);
      assertEquals(t0, t.getCurrentTick());
   }

   @Test
   public void testInsufficient3extra() throws IOException {
      int rd = sut.take(33);
      assertEquals(10, rd);
      assertEquals(t0, t.getCurrentTick());
   }

   private void singlewrite(int count) throws IOException {
      for (int i = 0; i < count; i++)
         sut.takeOne();
   }

   @Test
   public void testSingleSufficient() throws IOException {
      singlewrite(3);
      assertEquals(t0, t.getCurrentTick());
   }

   @Test
   public void testSingleInsufficient1extra() throws IOException {
      singlewrite(13);
      assertEquals(t0 + 1, t.getCurrentTick());
   }

   @Test
   public void testSingleInsufficient3extra() throws IOException {
      singlewrite(33);
      assertEquals(t0 + 3, t.getCurrentTick());
   }
}

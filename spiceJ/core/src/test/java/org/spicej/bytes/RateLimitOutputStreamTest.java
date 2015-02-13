package org.spicej.bytes;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.spicej.bytes.RateHelper.IdleNotify;
import org.spicej.impl.SimulationTickSource;

public class RateLimitOutputStreamTest {

   private RateLimitOutputStream sut;
   private SimulationTickSource t;
   private byte[] buffer = new byte[500];
   private boolean autoAdvance = true;
   private long t0;

   @Before
   public void setUp() throws Exception {
      t = new SimulationTickSource();
      sut = new RateLimitOutputStream(new ByteArrayOutputStream(250), t, 10, 1);
      sut.testEnableFailOnHang();
      sut.testSetIdleNotify(new IdleNotify() {

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
   public void testSufficient() throws IOException {
      sut.write(buffer, 0, 3);
      assertEquals(t0, t.getCurrentTick());
   }

   @Test
   public void testInsufficient1extra() throws IOException {
      sut.write(buffer, 0, 13);
      assertEquals(t0 + 1, t.getCurrentTick());
   }

   @Test
   public void testInsufficient3extra() throws IOException {
      sut.write(buffer, 0, 33);
      assertEquals(t0 + 3, t.getCurrentTick());
   }

   private void singlewrite(int count) throws IOException {
      for (int i = 0; i < count; i++)
         sut.write(0);
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

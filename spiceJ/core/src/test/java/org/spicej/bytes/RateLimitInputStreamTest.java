package org.spicej.bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.spicej.impl.SimulationTickSource;

public class RateLimitInputStreamTest {

   private PipedOutputStream pos;
   private RateLimitInputStream sut;
   private SimulationTickSource t;
   private byte[] buffer = new byte[500];
   private boolean autoAdvance = true;
   private long t0;

   @Before
   public void setUp() throws Exception {
      PipedInputStream pis = new PipedInputStream();
      pos = new PipedOutputStream(pis);

      t = new SimulationTickSource();
      sut = new RateLimitInputStream(pis, t, 10);
      sut.testEnableFailOnHang();
      sut.testSetIdleNotify(new IdleNotify() {

         @Override
         public boolean idle() {
            if (autoAdvance)
               t.advance();
            return autoAdvance;
         }

      });

      pos.write(new byte[250]);

      t.advance();
      t.advance();
      t.advance();
      t0 = t.getCurrentTick();
   }

   @Test
   public void testAvailable() throws IOException {
      autoAdvance = false;

      assertEquals(10, sut.available());
      sut.read(buffer, 0, 3);
      assertEquals(7, sut.available());
      sut.read(buffer, 0, 6);
      assertEquals(1, sut.available());
      sut.read(buffer, 0, 1);
      assertEquals(0, sut.available());

      t.advance();

      assertEquals(10, sut.available());
      sut.read(buffer, 0, 3);
      assertEquals(7, sut.available());

      t.advance();

      assertEquals(10, sut.available());

      t.advance();

      assertEquals(10, sut.available());

      assertEquals(3, sut.read(buffer, 0, 3));
      
      assertEquals(7, sut.available());
   }

   @Test
   public void testSufficient() throws IOException {
      int rd = sut.read(buffer, 0, 3);
      assertEquals(3, rd);
      assertEquals(t0, t.getCurrentTick());
   }

   @Test
   public void testInsufficient1extra() throws IOException {
      int rd = sut.read(buffer, 0, 13);
      assertEquals(t0 + 1, t.getCurrentTick());
      assertEquals(13, rd);
   }

   @Test
   public void testInsufficient3extra() throws IOException {
      int rd = sut.read(buffer, 0, 33);
      assertEquals(t0 + 3, t.getCurrentTick());
      assertEquals(33, rd);
   }

   @Test
   public void testUnderrun0() throws IOException {
      int rd = sut.read(buffer, 0, 260);
      assertEquals(t0 + 24, t.getCurrentTick());
      assertEquals(250, rd);
   }

   @Test
   public void testUnderrun1() throws IOException {
      int rd = sut.read(buffer, 0, 251);
      assertEquals(t0 + 24, t.getCurrentTick());
      assertEquals(250, rd);
   }

   @Test
   public void testEof() throws IOException {
      singleread(249);
      pos.close();

      int rd = sut.read(buffer, 0, 5);
      assertEquals(1, rd);
      rd = sut.read(buffer, 0, 5);
      assertEquals(-1, rd);
   }

   private void singleread(int count) throws IOException {
      for (int i = 0; i < count; i++)
         assertNotEquals(-1, sut.read());
   }

   @Test
   public void testSingleSufficient() throws IOException {
      singleread(3);
      assertEquals(t0, t.getCurrentTick());
   }

   @Test
   public void testSingleInsufficient1extra() throws IOException {
      singleread(13);
      assertEquals(t0 + 1, t.getCurrentTick());
   }

   @Test
   public void testSingleInsufficient3extra() throws IOException {
      singleread(33);
      assertEquals(t0 + 3, t.getCurrentTick());
   }

   @Test
   public void testSingleEof() throws IOException {
      singleread(249);
      pos.close();

      int rd = sut.read(buffer, 0, 5);
      assertEquals(1, rd);
      assertEquals(-1, sut.read());
   }

   @Test
   public void testVariableByteRate() throws IOException {
      autoAdvance = false;
      
      assertEquals(3, sut.read(buffer, 0, 3));
      assertEquals(7, sut.available());
      
      sut.setBytesPerTick(8);
      assertEquals(5, sut.available());
      
      sut.setBytesPerTick(5);
      assertEquals(2, sut.available());
      
      sut.setBytesPerTick(1); // -2
      assertEquals(0, sut.available());
      
      t.advance(); // -1
      assertEquals(0, sut.available());
      
      t.advance(); // 0
      assertEquals(0, sut.available());
      
      t.advance();
      assertEquals(1, sut.available());
      
      t.advance();
      assertEquals(1, sut.available());
      
      sut.setBytesPerTick(10);
      
      assertEquals(3, sut.read(buffer, 0, 3));
      assertEquals(7, sut.available());
      
      sut.setBytesPerTick(14);
      assertEquals(11, sut.available());
   }
}

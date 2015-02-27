package at.borkowski.spicej.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import at.borkowski.spicej.impl.SimulationTickSource;
import at.borkowski.spicej.streams.util.PipedInputStream;
import at.borkowski.spicej.streams.util.PipedOutputStream;

public class DelayedInputStreamVariableDelayTest {

   DelayedInputStream sut0;
   DelayedInputStream sut1;
   DelayedInputStream sutn;
   SimulationTickSource t;

   PipedOutputStream feed0, feed1, feedn;

   public static final int BUFFER = 50;
   public static final int DELAY = 20;

   @Before
   public void setUp() throws IOException {
      t = new SimulationTickSource();

      PipedInputStream pis0, pis1, pisn;

      feed0 = new PipedOutputStream(pis0 = new PipedInputStream());
      sut0 = new DelayedInputStream(t, pis0, 0, BUFFER);
      feed1 = new PipedOutputStream(pis1 = new PipedInputStream());
      sut1 = new DelayedInputStream(t, pis1, 1, BUFFER);
      feedn = new PipedOutputStream(pisn = new PipedInputStream());
      sutn = new DelayedInputStream(t, pisn, DELAY, BUFFER);

      t.advance();
      t.advance();
      t.advance();
      t.advance();
      t.advance();
   }

   @Test
   public void testChangeDelayInIdle() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };
      int delay = DELAY;

      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;

      assertEquals(0, sut.available());
      feed.write(blk);
      for (int i = 0; i < delay - 1; i++)
         t.advance();
      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += sut.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, sut.available());

      sut.setDelay(delay = 5);

      assertEquals(0, sut.available());
      feed.write(blk);
      for (int i = 0; i < delay - 1; i++)
         t.advance();
      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());
      rd = new byte[blk.length];
      done = 0;
      while (done < rd.length)
         done += sut.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, sut.available());

      sut.setDelay(delay = 35);

      assertEquals(0, sut.available());
      feed.write(blk);
      for (int i = 0; i < delay; i++)
         t.advance();
      assertEquals(blk.length, sut.available());
      rd = new byte[blk.length];
      done = 0;
      while (done < rd.length)
         done += sut.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, sut.available());
   }

   @Test
   public void testChangeDelayInBlockLowerResultsInLowerDelay() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };
      int delay = DELAY;

      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;

      assertEquals(0, sut.available());
      feed.write(blk);
      for (int i = 0; i < delay - 8; i++)
         t.advance();
      assertEquals(0, sut.available());

      sut.setDelay(delay = 5);

      // we only have to wait the new delay (5)
      // because remainder of original delay > new delay
      for (int i = 0; i < delay - 1; i++)
         t.advance();
      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += sut.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, sut.available());
   }

   @Test
   public void testChangeDelayInBlockLowerNoImpact() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };
      int delay = DELAY;

      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;

      assertEquals(0, sut.available());
      feed.write(blk);
      for (int i = 0; i < delay - 5; i++)
         t.advance();
      assertEquals(0, sut.available());

      sut.setDelay(delay = 8);

      // we still have to wait the original delay (where 4 ticks are left)
      // because remainder of original delay < new delay
      for (int i = 0; i < 4; i++)
         t.advance();
      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += sut.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, sut.available());
   }

   @Test
   public void testChangeDelayInBlockHigher() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };
      int delay = DELAY;

      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;

      assertEquals(0, sut.available());
      feed.write(blk);
      for (int i = 0; i < delay - 3; i++)
         t.advance();
      assertEquals(0, sut.available());

      sut.setDelay(delay = 15); // 3 left -> doesn't affect bytes already in queue
      feed.write(13);

      assertEquals(0, sut.available());
      for (int i = 0; i < 2; i++)
         t.advance();
      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += sut.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, sut.available());

      // 1 byte written 3 ticks ago with delay of 15 -> wait 12 

      for (int i = 0; i < 11; i++)
         t.advance();
      assertEquals(0, sut.available());
      t.advance();
      assertEquals(1, sut.available());
      assertEquals(13, sut.read());
   }

}

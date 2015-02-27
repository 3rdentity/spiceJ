package at.borkowski.spicej.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import at.borkowski.spicej.impl.SimulationTickSource;
import at.borkowski.spicej.streams.util.PipedInputStream;
import at.borkowski.spicej.streams.util.PipedOutputStream;

public class DelayedOutputStreamVariableDelayTest {

   DelayedOutputStream sut0;
   DelayedOutputStream sut1;
   DelayedOutputStream sutn;
   SimulationTickSource t;

   PipedInputStream feed0, feed1, feedn;

   public static final int BUFFER = 50;
   public static final int DELAY = 20;

   @Before
   public void setUp() throws IOException {
      t = new SimulationTickSource();

      PipedOutputStream pos0, pos1, posn;

      feed0 = new PipedInputStream(pos0 = new PipedOutputStream());
      sut0 = new DelayedOutputStream(t, pos0, 0, BUFFER);
      feed1 = new PipedInputStream(pos1 = new PipedOutputStream());
      sut1 = new DelayedOutputStream(t, pos1, 1, BUFFER);
      feedn = new PipedInputStream(posn = new PipedOutputStream());
      sutn = new DelayedOutputStream(t, posn, DELAY, BUFFER);

      t.advance();
      t.advance();
      t.advance();
      t.advance();
      t.advance();
   }

   @Test
   public void testChangeDelayInIdle() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };

      DelayedOutputStream sut = sutn;
      PipedInputStream feed = feedn;

      int delay = DELAY;

      assertEquals(0, feed.available());
      sut.write(blk);
      assertEquals(0, feed.available());
      for (int i = 0; i < delay - 1; i++)
         t.advance();
      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blk.length, feed.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += feed.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, feed.available());

      sut.setDelay(delay = 5);

      assertEquals(0, feed.available());
      sut.write(blk);
      assertEquals(0, feed.available());
      for (int i = 0; i < delay - 1; i++)
         t.advance();
      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blk.length, feed.available());
      rd = new byte[blk.length];
      done = 0;
      while (done < rd.length)
         done += feed.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, feed.available());

      sut.setDelay(delay = DELAY + 15);

      assertEquals(0, feed.available());
      sut.write(blk);
      assertEquals(0, feed.available());
      for (int i = 0; i < delay - 1; i++)
         t.advance();
      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blk.length, feed.available());
      rd = new byte[blk.length];
      done = 0;
      while (done < rd.length)
         done += feed.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, feed.available());
   }

   @Test
   public void testChangeDelayInBlockLowerResultsInLowerDelay() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };

      DelayedOutputStream sut = sutn;
      PipedInputStream feed = feedn;

      int delay = DELAY;

      assertEquals(0, feed.available());
      sut.write(blk);
      assertEquals(0, feed.available());
      for (int i = 0; i < delay - 8; i++)
         t.advance();
      assertEquals(0, feed.available());

      sut.setDelay(delay = 5);

      // we only have to wait the new delay (5)
      // because remainder of original delay > new delay
      for (int i = 0; i < delay - 1; i++)
         t.advance();
      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blk.length, feed.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += feed.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, feed.available());
   }

   @Test
   public void testChangeDelayInBlockLowerNoImpact() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };

      DelayedOutputStream sut = sutn;
      PipedInputStream feed = feedn;

      int delay = DELAY;

      assertEquals(0, feed.available());
      sut.write(blk);
      assertEquals(0, feed.available());
      for (int i = 0; i < delay - 5; i++)
         t.advance();
      assertEquals(0, feed.available());

      sut.setDelay(delay = 8);

      // we still have to wait the original delay (where 4 ticks are left)
      // because remainder of original delay < new delay
      for (int i = 0; i < 4; i++)
         t.advance();
      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blk.length, feed.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += feed.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, feed.available());
   }

   @Test
   public void testChangeDelayInBlockHigher() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };
      int delay = DELAY;

      DelayedOutputStream sut = sutn;
      PipedInputStream feed = feedn;

      assertEquals(0, feed.available());
      sut.write(blk);
      for (int i = 0; i < delay - 3; i++)
         t.advance();
      assertEquals(0, feed.available());

      sut.setDelay(delay = 15); // 3 left -> doesn't affect bytes already in queue
      sut.write(13);

      assertEquals(0, feed.available());
      for (int i = 0; i < 2; i++)
         t.advance();
      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blk.length, feed.available());
      byte[] rd = new byte[blk.length];
      int done = 0;
      while (done < rd.length)
         done += feed.read(rd, done, rd.length - done);
      assertArrayEquals(blk, rd);
      assertEquals(0, feed.available());

      // 1 byte written 3 ticks ago with delay of 15 -> wait 12 

      for (int i = 0; i < 11; i++)
         t.advance();
      assertEquals(0, feed.available());
      t.advance();
      assertEquals(1, feed.available());
      assertEquals(13, feed.read());
   }
}

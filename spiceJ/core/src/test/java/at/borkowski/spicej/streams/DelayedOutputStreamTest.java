package at.borkowski.spicej.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import at.borkowski.spicej.impl.SimulationTickSource;
import at.borkowski.spicej.streams.util.PipedInputStream;
import at.borkowski.spicej.streams.util.PipedOutputStream;

public class DelayedOutputStreamTest {

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
   public void testOneByteDelay0() throws IOException {
      DelayedOutputStream sut = sut0;
      PipedInputStream feed = feed0;

      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      sut.write(13);

      assertEquals(1, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      assertEquals(13, feed.read());

      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testOneByteDelay1() throws IOException {
      DelayedOutputStream sut = sut1;
      PipedInputStream feed = feed1;

      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      sut.write(13);

      assertEquals(0, feed.available());
      assertEquals(1, sut.bufferedBytes());
      assertEquals(BUFFER - 1, sut.freeBytes());

      t.advance();

      assertEquals(1, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      assertEquals(13, feed.read());
      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testOneByte() throws IOException {
      DelayedOutputStream sut = sutn;
      PipedInputStream feed = feedn;

      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      sut.write(12);

      assertEquals(0, feed.available());
      assertEquals(1, sut.bufferedBytes());
      assertEquals(BUFFER - 1, sut.freeBytes());

      t.advance();

      assertEquals(0, feed.available());
      assertEquals(1, sut.bufferedBytes());
      assertEquals(BUFFER - 1, sut.freeBytes());

      for (int i = 0; i < DELAY - 2; i++)
         t.advance();

      assertEquals(0, feed.available());
      t.advance();
      assertEquals(1, feed.available());

      assertEquals(12, feed.read());
      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testManyBytes() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };

      DelayedOutputStream sut = sutn;
      PipedInputStream feed = feedn;

      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      sut.write(blk);

      assertEquals(0, feed.available());
      assertEquals(blk.length, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length, sut.freeBytes());

      t.advance();

      assertEquals(0, feed.available());
      assertEquals(blk.length, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length, sut.freeBytes());

      for (int i = 0; i < DELAY - 2; i++)
         t.advance();

      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blk.length, feed.available());

      byte[] rd = new byte[blk.length];
      rd[0] = (byte) feed.read();
      assertEquals(blk.length - 1, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      rd[1] = (byte) feed.read();
      assertEquals(blk.length - 2, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      int done = 2;
      while (done < rd.length)
         done += feed.read(rd, done, rd.length - done);

      assertArrayEquals(blk, rd);

      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testManyBytesInterleaved() throws IOException {
      byte[] blkA = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };
      byte[] blkB = { 10, 11, 12, 13, 0, (byte) 0xCA, (byte) 0xFE, 0x77 };

      int interval = 7;

      DelayedOutputStream sut = sutn;
      PipedInputStream feed = feedn;

      assertEquals(0, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      sut.write(blkA);

      assertEquals(0, feed.available());
      assertEquals(blkA.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length, sut.freeBytes());

      t.advance();

      assertEquals(0, feed.available());
      assertEquals(blkA.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length, sut.freeBytes());

      for (int i = 0; i < interval - 1; i++)
         t.advance();

      assertEquals(0, feed.available());
      assertEquals(blkA.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length, sut.freeBytes());

      sut.write(blkB);

      assertEquals(0, feed.available());
      assertEquals(blkA.length + blkB.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length - blkB.length, sut.freeBytes());

      t.advance();

      assertEquals(0, feed.available());
      assertEquals(blkA.length + blkB.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length - blkB.length, sut.freeBytes());

      for (int i = 0; i < DELAY - interval - 2; i++)
         t.advance();

      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blkA.length, feed.available());

      byte[] rdA = new byte[blkA.length];
      rdA[0] = (byte) feed.read();
      assertEquals(blkA.length - 1, feed.available());
      assertEquals(blkB.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkB.length, sut.freeBytes());

      t.advance();
      assertEquals(blkA.length - 1, feed.available());
      assertEquals(blkB.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkB.length, sut.freeBytes());

      int done = 1;
      while (done < rdA.length)
         done += feed.read(rdA, done, rdA.length - done);

      assertArrayEquals(blkA, rdA);

      assertEquals(0, feed.available());
      assertEquals(blkB.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkB.length, sut.freeBytes());

      for (int i = 0; i < interval - 2; i++)
         t.advance();

      assertEquals(0, feed.available());
      t.advance();
      assertEquals(blkB.length, feed.available());

      byte[] rdB = new byte[blkB.length];
      rdB[0] = (byte) feed.read();
      assertEquals(blkB.length - 1, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();
      assertEquals(blkB.length - 1, feed.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      done = 1;
      while (done < rdB.length)
         done += feed.read(rdB, done, rdB.length - done);

      assertArrayEquals(blkB, rdB);
   }

   @Test
   public void testWrapAround() throws IOException {
      byte[] blkA = new byte[BUFFER - 5];
      Random random = new Random();

      DelayedOutputStream sut = sut1;
      PipedInputStream feed = feed1;

      for (int i = 0; i < 4; i++) {
         random.nextBytes(blkA);

         assertEquals(0, feed.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());

         sut.write(blkA);

         assertEquals(0, feed.available());
         assertEquals(blkA.length, sut.bufferedBytes());
         assertEquals(BUFFER - blkA.length, sut.freeBytes());

         t.advance();

         assertEquals(blkA.length, feed.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());

         byte[] rdA = new byte[blkA.length];
         rdA[0] = (byte) feed.read();
         assertEquals(blkA.length - 1, feed.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());

         t.advance();
         assertEquals(blkA.length - 1, feed.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());

         int done = 1;

         while (done < rdA.length)
            done += feed.read(rdA, done, rdA.length - done);

         assertArrayEquals(blkA, rdA);

         assertEquals(0, feed.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());
      }

   }
}

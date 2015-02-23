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

public class DelayedInputStreamTest {

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
   public void testOneByteDelay0() throws IOException {
      DelayedInputStream sut = sut0;
      PipedOutputStream feed = feed0;

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.write(13);

      assertEquals(1, sut.available());
      assertEquals(1, sut.bufferedBytes());
      assertEquals(BUFFER - 1, sut.freeBytes());

      assertEquals(13, sut.read());

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testOneByteDelay1() throws IOException {
      DelayedInputStream sut = sut1;
      PipedOutputStream feed = feed1;

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.write(13);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();

      assertEquals(1, sut.available());
      assertEquals(1, sut.bufferedBytes());
      assertEquals(BUFFER - 1, sut.freeBytes());

      assertEquals(13, sut.read());
      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testOneByte() throws IOException {
      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.write(12);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();

      assertEquals(0, sut.available());
      assertEquals(1, sut.bufferedBytes());
      assertEquals(BUFFER - 1, sut.freeBytes());

      for (int i = 0; i < DELAY - 2; i++)
         t.advance();

      assertEquals(0, sut.available());
      t.advance();
      assertEquals(1, sut.available());

      assertEquals(12, sut.read());
      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testManyBytes() throws IOException {
      byte[] blk = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };

      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.write(blk);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();

      assertEquals(0, sut.available());
      assertEquals(blk.length, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length, sut.freeBytes());

      for (int i = 0; i < DELAY - 2; i++)
         t.advance();

      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());

      byte[] rd = new byte[blk.length];
      rd[0] = (byte) sut.read();
      assertEquals(blk.length - 1, sut.available());
      assertEquals(blk.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length + 1, sut.freeBytes());

      rd[1] = (byte) sut.read();
      assertEquals(blk.length - 2, sut.available());
      assertEquals(blk.length - 2, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length + 2, sut.freeBytes());

      int done = 2;
      while (done < rd.length)
         done += sut.read(rd, done, rd.length - done);

      assertArrayEquals(blk, rd);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());
   }

   @Test
   public void testManyBytesInterleaved() throws IOException {
      byte[] blkA = { 3, 1, 4, 1, 2, 7, 1, 3, 1, 3, 3, 7 };
      byte[] blkB = { 10, 11, 12, 13, 0, (byte) 0xCA, (byte) 0xFE, 0x77 };

      int interval = 7;

      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.write(blkA);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();

      assertEquals(0, sut.available());
      assertEquals(blkA.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length, sut.freeBytes());

      for (int i = 0; i < interval - 1; i++)
         t.advance();

      assertEquals(0, sut.available());
      assertEquals(blkA.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length, sut.freeBytes());

      feed.write(blkB);

      assertEquals(0, sut.available());
      assertEquals(blkA.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length, sut.freeBytes());

      t.advance();

      assertEquals(0, sut.available());
      assertEquals(blkA.length + blkB.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length - blkB.length, sut.freeBytes());

      for (int i = 0; i < DELAY - interval - 2; i++)
         t.advance();

      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blkA.length, sut.available());

      byte[] rdA = new byte[blkA.length];
      rdA[0] = (byte) sut.read();
      assertEquals(blkA.length - 1, sut.available());
      assertEquals(blkA.length + blkB.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length - blkB.length + 1, sut.freeBytes());

      t.advance();
      assertEquals(blkA.length - 1, sut.available());
      assertEquals(blkA.length + blkB.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blkA.length - blkB.length + 1, sut.freeBytes());

      int done = 1;
      while (done < rdA.length)
         done += sut.read(rdA, done, rdA.length - done);

      assertArrayEquals(blkA, rdA);

      assertEquals(0, sut.available());
      assertEquals(blkB.length, sut.bufferedBytes());
      assertEquals(BUFFER - blkB.length, sut.freeBytes());

      for (int i = 0; i < interval - 2; i++)
         t.advance();

      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blkB.length, sut.available());

      byte[] rdB = new byte[blkB.length];
      rdB[0] = (byte) sut.read();
      assertEquals(blkB.length - 1, sut.available());
      assertEquals(blkB.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blkB.length + 1, sut.freeBytes());

      t.advance();
      assertEquals(blkB.length - 1, sut.available());
      assertEquals(blkB.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blkB.length + 1, sut.freeBytes());

      done = 1;
      while (done < rdB.length)
         done += sut.read(rdB, done, rdB.length - done);

      assertArrayEquals(blkB, rdB);
   }

   @Test
   public void testWrapAround() throws IOException {
      byte[] blkA = new byte[BUFFER - 5];
      Random random = new Random();

      DelayedInputStream sut = sut1;
      PipedOutputStream feed = feed1;

      for (int i = 0; i < 4; i++) {
         random.nextBytes(blkA);

         assertEquals(0, sut.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());

         feed.write(blkA);

         assertEquals(0, sut.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());

         t.advance();

         assertEquals(blkA.length, sut.available());
         assertEquals(blkA.length, sut.bufferedBytes());
         assertEquals(BUFFER - blkA.length, sut.freeBytes());

         byte[] rdA = new byte[blkA.length];
         rdA[0] = (byte) sut.read();
         assertEquals(blkA.length - 1, sut.available());
         assertEquals(blkA.length - 1, sut.bufferedBytes());
         assertEquals(BUFFER - blkA.length + 1, sut.freeBytes());

         t.advance();
         assertEquals(blkA.length - 1, sut.available());
         assertEquals(blkA.length - 1, sut.bufferedBytes());
         assertEquals(BUFFER - blkA.length + 1, sut.freeBytes());

         int done = 1;

         while (done < rdA.length)
            done += sut.read(rdA, done, rdA.length - done);

         assertArrayEquals(blkA, rdA);

         assertEquals(0, sut.available());
         assertEquals(0, sut.bufferedBytes());
         assertEquals(BUFFER, sut.freeBytes());
      }
   }

   @Test
   public void testClosing() throws IOException {
      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;
      byte[] blk = new byte[12];

      sut.setNonBlocking(true);
      sut.setEofDetection(true);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.write(blk);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();

      assertEquals(0, sut.available());
      assertEquals(blk.length, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length, sut.freeBytes());

      for (int i = 0; i < DELAY - 2; i++)
         t.advance();

      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());

      byte[] rdA = new byte[blk.length];
      rdA[0] = (byte) sut.read();
      assertEquals(blk.length - 1, sut.available());
      assertEquals(blk.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length + 1, sut.freeBytes());

      t.advance();

      assertEquals(blk.length - 1, sut.available());
      assertEquals(blk.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length + 1, sut.freeBytes());

      int done = 1;
      while (done < rdA.length)
         done += sut.read(rdA, done, rdA.length - done);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.close();

      sut.__waitForEofDetector();

      t.advance();
      assertEquals(-1, sut.read());
   }

   @Test
   public void testClosingInDelay() throws IOException {
      DelayedInputStream sut = sutn;
      PipedOutputStream feed = feedn;
      byte[] blk = new byte[12];

      sut.setNonBlocking(true);
      sut.setEofDetection(true);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      feed.write(blk);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();

      feed.close();

      sut.__waitForEofDetector();

      assertEquals(0, sut.available());
      assertEquals(blk.length, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length, sut.freeBytes());

      for (int i = 0; i < DELAY - 2; i++)
         t.advance();

      assertEquals(0, sut.available());
      t.advance();
      assertEquals(blk.length, sut.available());

      byte[] rdA = new byte[blk.length];
      rdA[0] = (byte) sut.read();
      assertEquals(blk.length - 1, sut.available());
      assertEquals(blk.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length + 1, sut.freeBytes());

      t.advance();

      assertEquals(blk.length - 1, sut.available());
      assertEquals(blk.length - 1, sut.bufferedBytes());
      assertEquals(BUFFER - blk.length + 1, sut.freeBytes());

      int done = 1;
      while (done < rdA.length)
         done += sut.read(rdA, done, rdA.length - done);

      assertEquals(0, sut.available());
      assertEquals(0, sut.bufferedBytes());
      assertEquals(BUFFER, sut.freeBytes());

      t.advance();
      assertEquals(-1, sut.read());
   }
}

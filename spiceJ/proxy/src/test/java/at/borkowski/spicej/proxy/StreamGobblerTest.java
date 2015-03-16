package at.borkowski.spicej.proxy;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import at.borkowski.spicej.impl.SleepWakeup;
import at.borkowski.spicej.streams.util.PipedInputStream;
import at.borkowski.spicej.streams.util.PipedOutputStream;

public class StreamGobblerTest {

   StreamGobbler sut;

   @Test
   public void testCopyWithEOF() throws IOException {
      byte[] blk = { 13, 37, 31, 41, 27, 10 };
      InputStream is = spy(new ByteArrayInputStream(blk));
      ByteArrayOutputStream os = spy(new ByteArrayOutputStream());
      sut = new StreamGobbler(is, os);
      sut.run();
      assertArrayEquals(blk, os.toByteArray());

      verify(is, atLeast(1)).close();
      verify(os, atLeast(1)).close();
   }

   @Test
   public void testCopyWithoutEOF() throws Throwable {
      byte[] blk = { 13, 37, 31, 41, 27, 10 };
      byte[] rd = new byte[blk.length];
      int done = 0;

      PipedInputStream pis = spy(new PipedInputStream());
      PipedOutputStream pos = spy(new PipedOutputStream(pis));
      pos.write(blk);
      sut = new StreamGobbler(pis, pos);
      Thread t = new Thread(sut);
      t.start();

      SleepWakeup.sleep(10);
      while (done < rd.length)
         done += pis.read(rd, done, rd.length - done);

      assertArrayEquals(rd, blk);

      verify(pis, never()).close();
      verify(pos, never()).close();

      pos.close();
      sut.waitFor();

      verify(pis, atLeast(1)).close();
      verify(pos, atLeast(1)).close();
   }

   @Test
   public void testInterruptCancel() throws IOException {
      byte[] blk = { 13, 37, 31, 41, 27, 10 };
      byte[] rd = new byte[blk.length];
      int done = 0;

      PipedInputStream pis = spy(new PipedInputStream());
      PipedOutputStream pos = spy(new PipedOutputStream(pis));
      pos.write(blk);
      sut = new StreamGobbler(pis, pos);
      Thread t = new Thread(sut);
      t.start();

      SleepWakeup.sleep(10);
      while (done < rd.length)
         done += pis.read(rd, done, rd.length - done);

      assertArrayEquals(rd, blk);

      verify(pis, never()).close();
      verify(pos, never()).close();

      sut.close();

      verify(pis, atLeast(1)).close();
      verify(pos, atLeast(1)).close();
   }

}

package at.borkowski.spicej.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import at.borkowski.spicej.WouldBlockException;
import at.borkowski.spicej.impl.SleepWakeup;
import at.borkowski.spicej.shapers.DelayShaper;
import at.borkowski.spicej.ticks.TickListener;
import at.borkowski.spicej.ticks.TickSource;

/**
 * Provides an {@link InputStream} instance with a delay in the transferred
 * bytes.
 * 
 * The stream uses a {@link TickSource} as a source of timing information and
 * delays the reception of data by a certain number of ticks.
 *
 */
// TODO implement output stream
public class DelayedInputStream extends InputStream implements TickListener, DelayShaper {

   private final InputStream in;
   private long delay;
   private final byte[] buffer;

   private boolean blocking = true;
   private long currentTick;

   private volatile int currentVirtualEnd = 0;
   private volatile int start = 0;
   private volatile int end = 0;
   private SortedSet<Long> tickMarks = new TreeSet<Long>();
   private Map<Long, Integer> tick_virtualEnd = new HashMap<>();

   private SleepWakeup sleep = new SleepWakeup();
   private Object lock = new Object();

   /**
    * Constructs a new {@link DelayedInputStream} with the given parameters.
    * 
    * @param t
    *           The tick source to use
    * @param in
    *           The underlying {@link InputStream} to read data from
    * @param delay
    *           The delay (in ticks) to introduce to data
    * @param bufferSize
    *           The buffer size to use. The implementation has to store read
    *           bytes in an intermediate buffer. The buffer must be large enough
    *           to store the data. Note that data which cannot be stored into
    *           the buffer because of its overflow will have a higher delay,
    *           which is why the buffer should be significantly higher than the
    *           expected data arrivel rate (times the expected interval of
    *           reading from this stream).
    */
   public DelayedInputStream(TickSource t, InputStream in, long delay, int bufferSize) {
      this.in = in;
      this.delay = delay;

      // +1 is necessary because we handle start == end as an empty
      // pipe and not, as it could be, as a full one (= we need at
      // least one empty byte to work). in order to fulfill the buffer
      // size, we increase the buffer size by one
      this.buffer = new byte[bufferSize + 1];

      t.addListener(this);
   }

   @Override
   public int read() throws IOException {
      if (delay == 0)
         handleNewData();

      waitForAvailable();

      synchronized (lock) {
         byte b = buffer[start++];
         if (start >= buffer.length)
            start -= buffer.length;
         return b & 0xFF;
      }

   }

   private void waitForAvailable() {
      while (bufferedBytes(currentVirtualEnd) == 0) {
         if (!blocking)
            throw new WouldBlockException();
         else
            sleep.sleep();
      }
   }

   @Override
   public void close() throws IOException {
      in.close();
      // TODO better handling of closed streams
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      if (delay == 0)
         handleNewData();

      waitForAvailable();
      int readable = bufferedBytes(currentVirtualEnd);

      int ret;
      int toRead = ret = Math.min(len, readable);
      if (start + toRead > buffer.length) {
         int chunk1 = buffer.length - start;
         System.arraycopy(buffer, start, b, off, chunk1);
         toRead -= chunk1;
         off += chunk1;
         start = 0;
      }
      System.arraycopy(buffer, start, b, off, toRead);
      synchronized (lock) {
         start += toRead;
         if (start >= buffer.length)
            start -= buffer.length;
      }

      return ret;
   }

   @Override
   public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
   }

   int bufferedBytes() {
      return bufferedBytes(end);
   }

   int bufferedBytes(int effectiveEnd) {
      if (start == effectiveEnd)
         return 0;
      else if (start < effectiveEnd)
         return effectiveEnd - start;
      else
         return effectiveEnd - start + buffer.length;
   }

   int freeBytes() {
      return buffer.length - bufferedBytes() - 1;
   }

   @Override
   public void tick(long tick) {
      currentTick = tick;
      handleNewData();
   }

   @Override
   public int available() throws IOException {
      if (delay == 0)
         handleNewData();
      return bufferedBytes(currentVirtualEnd);
   }

   private void handleNewData() {
      try {
         int previousEnd = end;

         if (in.available() > 0 && freeBytes() > 0) {
            int toRead = Math.min(freeBytes(), in.available());
            int rd;
            while (toRead > 0) {
               rd = in.read(buffer, end, Math.min(toRead, buffer.length - end));
               if (rd == -1)
                  break; // TODO: handle stream closing
               toRead -= rd;
               synchronized (lock) {
                  end += rd;
                  if (end >= buffer.length)
                     end -= buffer.length;
                  if (end != previousEnd && delay > 0) {
                     tick_virtualEnd.put(currentTick + delay - 1, end);
                     tickMarks.add(currentTick + delay - 1);
                  }
               }
            }
         }

         sleep.wakeup();
      } catch (IOException e) {
         // TODO handle exceptions
         throw new RuntimeException(e);
      }

      while (!tickMarks.isEmpty() && tickMarks.first() <= currentTick) {
         Long tick = tickMarks.first();
         tickMarks.remove(tick);
         currentVirtualEnd = tick_virtualEnd.remove(tick);
      }

      if (tickMarks.isEmpty())
         currentVirtualEnd = end;
   }

   @Override
   public void setByteRate(long delay) {
      this.delay = delay;
   }

   @Override
   public long getByteRate() {
      return delay;
   }

}

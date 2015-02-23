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
 * Provides an {@link InputStream} instance with a delay real the transferred
 * bytes.
 * 
 * The stream uses a {@link TickSource} as a source of timing information and
 * delays the reception of data by a certain number of ticks.
 *
 */
public class DelayedInputStream extends InputStream implements TickListener, DelayShaper {

   private final InputStream real;
   private final TickSource t;
   private long delay;
   private final byte[] buffer;

   private boolean blocking = true, eof = false, closed = false;
   private long currentTick;

   private volatile int currentAvailableEnd = 0;
   private volatile int start = 0;
   private volatile int end = 0;

   private SortedSet<Long> tickMarks = new TreeSet<Long>();
   private Map<Long, Integer> tick_virtualEnd = new HashMap<>();

   private SleepWakeup sleep = new SleepWakeup();

   /**
    * Constructs a new {@link DelayedInputStream} with the given parameters.
    * 
    * @param t
    *           The tick source to use
    * @param real
    *           The underlying {@link InputStream} to read data from
    * @param delay
    *           The delay (real ticks) to introduce to data
    * @param bufferSize
    *           The buffer size to use. The implementation has to store read
    *           bytes real an intermediate buffer. The buffer must be large
    *           enough to store the data. Note that data which cannot be stored
    *           into the buffer because of its overflow will have a higher
    *           delay, which is why the buffer should be significantly higher
    *           than the expected data arrivel rate (times the expected interval
    *           of reading from this stream).
    */
   public DelayedInputStream(TickSource t, InputStream real, long delay, int bufferSize) {
      this.real = real;
      this.t = t;
      this.delay = delay;

      // +1 is necessary because we handle start == end as an empty
      // pipe and not, as it could be, as a full one (= we need at
      // least one empty byte to work). real order to fulfill the buffer
      // size, we increase the buffer size by one
      this.buffer = new byte[bufferSize + 1];

      t.addListener(this);
   }

   @Override
   public int read() throws IOException {
      if (eof)
         return -1;

      checkNotClosed();

      if (delay == 0)
         handleNewData();

      waitForAvailable();

      byte b = buffer[start++];
      if (start >= buffer.length)
         start -= buffer.length;
      return b & 0xFF;

   }

   private void checkNotClosed() throws IOException {
      if (closed)
         throw new IOException("stream closed by reader");
   }

   private void waitForAvailable() {
      while (bufferedBytes(currentAvailableEnd) == 0) {
         if (!blocking)
            throw new WouldBlockException();
         else
            sleep.sleep();
      }
   }

   @Override
   public void close() throws IOException {
      real.close();
      closed = true;
      eof = true;
      t.removeListener(this);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      if (eof)
         return -1;

      checkNotClosed();

      if (delay == 0)
         handleNewData();

      waitForAvailable();
      int readable = bufferedBytes(currentAvailableEnd);

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
      start += toRead;
      if (start >= buffer.length)
         start -= buffer.length;

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
      checkNotClosed();
      if (eof)
         return 0;
      
      if (delay == 0)
         handleNewData();
      
      return bufferedBytes(currentAvailableEnd);
   }

   private void handleNewData() {
      try {
         int previousEnd = end;

         if (real.available() > 0 && freeBytes() > 0) {
            int toRead = Math.min(freeBytes(), real.available());
            int rd;
            while (toRead > 0) {
               rd = real.read(buffer, end, Math.min(toRead, buffer.length - end));
               if (rd == -1)
                  break; // TODO: handle stream closing
               toRead -= rd;
               end += rd;
               if (end >= buffer.length)
                  end -= buffer.length;
               if (end != previousEnd && delay > 0) {
                  // -1 is necessary because we read data one tick later than it actually arrived
                  // (we assume to receive the tick after the phase generating the data)
                  tick_virtualEnd.put(currentTick + delay - 1, end);
                  tickMarks.add(currentTick + delay - 1);
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
         currentAvailableEnd = tick_virtualEnd.remove(tick);
      }

      if (tickMarks.isEmpty())
         currentAvailableEnd = end;
   }

   @Override
   public void setDelay(long delay) {
      this.delay = delay;
   }

   @Override
   public long getDelay() {
      return delay;
   }

   /**
    * Returns the buffer size, real bytes
    * 
    * @return the buffer size
    */
   public int getBufferSize() {
      return buffer.length - 1; // see constructor on why -1
   }

   /**
    * Returns the underlying {@link InputStream}.
    * 
    * @return the underlying stream
    */
   public InputStream getBaseStream() {
      return real;
   }

   /**
    * Returns the {@link TickSource} this stream uses.
    * 
    * @return the used tick source
    */
   public TickSource getTickSource() {
      return t;
   }

   /**
    * Sets the non-blocking flag.
    * 
    * @param nonBlocking
    *           whether the stream should be in non-blocking mode (see
    *           {@link RateLimitInputStream}).
    */
   public void setNonBlocking(boolean nonBlocking) {
      blocking = !nonBlocking;
   }

}

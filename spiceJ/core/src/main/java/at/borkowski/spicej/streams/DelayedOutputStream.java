package at.borkowski.spicej.streams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import at.borkowski.spicej.WouldBlockException;
import at.borkowski.spicej.impl.SleepWakeup;
import at.borkowski.spicej.shapers.DelayShaper;
import at.borkowski.spicej.ticks.TickListener;
import at.borkowski.spicej.ticks.TickSource;

/**
 * Provides an {@link OutputStream} instance with a delay real the transferred
 * bytes.
 * 
 * The stream uses a {@link TickSource} as a source of timing information and
 * delays the sending of data by a certain number of ticks.
 *
 */
public class DelayedOutputStream extends OutputStream implements TickListener, DelayShaper {

   private final OutputStream real;
   private final TickSource t;
   private long delay;
   private final byte[] buffer;

   private boolean blocking = true;
   private long currentTick;

   private volatile int currentAvailableEnd = 0;
   private volatile int start = 0;
   private volatile int end = 0;

   private SortedSet<Long> tickMarks = new TreeSet<Long>();
   private Map<Long, Integer> tick_virtualEnd = new HashMap<>();

   private SleepWakeup sleep = new SleepWakeup();

   /**
    * Constructs a new {@link DelayedOutputStream} with the given parameters.
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
   public DelayedOutputStream(TickSource t, OutputStream real, long delay, int bufferSize) {
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
   public void write(int b) throws IOException {
      write(new byte[] { (byte) b }, 0, 1);
   }

   @Override
   public void write(byte[] b, int off, int len) throws IOException {
      while (freeBytes() < len)
         if (!blocking)
            throw new WouldBlockException();
         else
            sleep.sleep();

      if (end + len >= buffer.length) {
         int chunk1 = buffer.length - end;
         System.arraycopy(b, off, buffer, end, chunk1);
         end = 0;
         off += chunk1;
         len -= chunk1;
      }
      System.arraycopy(b, off, buffer, end, len);
      int previousEnd = end;

      end += len;
      if (end >= buffer.length)
         end -= buffer.length;
      if (end != previousEnd && delay > 0) {
         tick_virtualEnd.put(currentTick + delay, end);
         tickMarks.add(currentTick + delay);
      }

      if (delay == 0)
         handleWritableData();
   }

   @Override
   public void tick(long tick) {
      currentTick = tick;
      handleWritableData();
   }

   private void handleWritableData() {
      while (!tickMarks.isEmpty() && tickMarks.first() <= currentTick) {
         Long tick = tickMarks.first();
         tickMarks.remove(tick);
         currentAvailableEnd = tick_virtualEnd.remove(tick);
      }

      if (tickMarks.isEmpty())
         currentAvailableEnd = end;

      int writable = bufferedBytes(currentAvailableEnd);

      if (writable == 0)
         return;

      try {
         int todo = writable;
         if (start + todo > buffer.length) {
            int chunk1 = buffer.length - start;
            real.write(buffer, start, chunk1);
            todo -= chunk1;
            start = 0;
         }
         real.write(buffer, start, todo);
         start += todo;
         if (start >= buffer.length)
            start -= buffer.length;
      } catch (IOException ioEx) {
         throw new RuntimeException("delayed data transmission error", ioEx);
      }
   }

   public void write(byte[] b) throws IOException {
      write(b, 0, b.length);
   }

   @Override
   public void close() throws IOException {
      real.close();
      // TODO better handling of closed streams
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
   public void setDelay(long delay) {
      this.delay = delay;

      long longestAcceptableDeadline = currentTick + delay;
      List<Long> toDelete = new LinkedList<>();
      for (Long tick : tickMarks)
         if (tick > longestAcceptableDeadline)
            toDelete.add(tick);

      for (Long tick : toDelete) {
         int previousVirtualEnd = tick_virtualEnd.get(tick);
         tickMarks.remove(tick);
         tick_virtualEnd.remove(tick);

         // insert tick-end pair (longestAcceptableDeadline, previousVirtualEnd) if not
         // already a pair present (tick, virtualEnd) where currentDeadline > previousVirtualEnd

         long currentVirtualEnd = -1;
         if (tickMarks.contains(longestAcceptableDeadline))
            currentVirtualEnd = tick_virtualEnd.get(longestAcceptableDeadline);

         tickMarks.add(longestAcceptableDeadline);
         if (currentVirtualEnd <= previousVirtualEnd)
            tick_virtualEnd.put(longestAcceptableDeadline, previousVirtualEnd);
      }
      handleWritableData();
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
   public OutputStream getBaseStream() {
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

}

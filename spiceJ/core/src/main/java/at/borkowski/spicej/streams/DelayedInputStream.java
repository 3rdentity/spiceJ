package at.borkowski.spicej.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import at.borkowski.spicej.WouldBlockException;
import at.borkowski.spicej.impl.SleepWakeup;
import at.borkowski.spicej.ticks.TickListener;
import at.borkowski.spicej.ticks.TickSource;

public class DelayedInputStream extends InputStream implements TickListener {

   private final InputStream in;
   private final long delay;
   private final byte[] buffer;

   private boolean blocking = true;
   private long currentTick;

   private volatile int currentVirtualEnd = 0;
   private volatile int start = 0;
   private volatile int end = 0;
   private Queue<Long> tickMarks = new LinkedList<>();
   private Map<Long, Integer> tick_virtualEnd = new HashMap<>();

   private SleepWakeup sleep = new SleepWakeup();
   private Object lock = new Object();

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

   void handleNewData() {
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

      while (!tickMarks.isEmpty() && tickMarks.peek() <= currentTick)
         currentVirtualEnd = tick_virtualEnd.remove(tickMarks.poll());

      if (tickMarks.isEmpty())
         currentVirtualEnd = end;
   }

}

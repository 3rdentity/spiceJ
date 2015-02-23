package at.borkowski.spicej;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import at.borkowski.spicej.streams.DelayedInputStream;
import at.borkowski.spicej.streams.RateLimitInputStream;
import at.borkowski.spicej.streams.RateLimitOutputStream;
import at.borkowski.spicej.ticks.TickSource;

public class StreamsTest {

   @Test
   public void testLimitRateInputStream() {
      InputStream base = mock(InputStream.class);
      TickSource t = mock(TickSource.class);
      
      RateLimitInputStream result = Streams.limitRate(base, t, 123, 456);
      
      assertSame(base, result.getBaseStream());
      assertSame(t, result.getTickSource());
      assertEquals(123, result.getByteRate());
      assertEquals(456, result.getPrescale());
   }

   @Test
   public void testLimitRateOutputStream() {
      OutputStream base = mock(OutputStream.class);
      TickSource t = mock(TickSource.class);
      
      RateLimitOutputStream result = Streams.limitRate(base, t, 123, 456);
      
      assertSame(base, result.getBaseStream());
      assertSame(t, result.getTickSource());
      assertEquals(123, result.getByteRate());
      assertEquals(456, result.getPrescale());
   }

   @Test
   public void testAddDelayInputStream() {
      InputStream base = mock(InputStream.class);
      TickSource t = mock(TickSource.class);
      
      DelayedInputStream result = Streams.addDelay(base, t, 123, 456);
      
      assertSame(base, result.getBaseStream());
      assertSame(t, result.getTickSource());
      assertEquals(123, result.getDelay());
      assertEquals(456, result.getBufferSize());
   }

}

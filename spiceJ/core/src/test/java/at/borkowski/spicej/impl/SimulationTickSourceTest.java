package at.borkowski.spicej.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

import at.borkowski.spicej.ticks.TickListener;

public class SimulationTickSourceTest {

   private SimulationTickSource sut;

   @Before
   public void setUp() {
      sut = new SimulationTickSource();
      sut.advance();
   }

   @Test
   public void testAdvance() {
      assertEquals(0, sut.getCurrentTick());
      sut.advance();
      assertEquals(1, sut.getCurrentTick());
      sut.advance();
      sut.advance();
      assertEquals(3, sut.getCurrentTick());
   }

   @Test
   public void testListeners() {
      TickListener listenerA = mock(TickListener.class);
      TickListener listenerB = mock(TickListener.class);

      sut.addListener(listenerA);
      sut.advance();
      assertEquals(1, sut.getCurrentTick());

      verify(listenerA).tick(1);

      sut.advance();

      assertEquals(2, sut.getCurrentTick());
      verify(listenerA).tick(2);

      sut.addListener(listenerB);

      sut.advance();

      assertEquals(3, sut.getCurrentTick());
      verify(listenerA).tick(3);
      verify(listenerB).tick(3);
      
      sut.removeListener(listenerA);

      sut.advance();

      assertEquals(4, sut.getCurrentTick());
      verify(listenerA, never()).tick(4);
      verify(listenerB).tick(4);
   }

   @Test
   public void testReset() {
      assertEquals(0, sut.getCurrentTick());
      sut.advance();
      assertEquals(1, sut.getCurrentTick());

      sut.reset();

      sut.advance();
      assertEquals(0, sut.getCurrentTick());
   }

}

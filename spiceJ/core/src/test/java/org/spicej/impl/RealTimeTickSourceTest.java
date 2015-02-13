package org.spicej.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.spicej.ticks.TickListener;

public class RealTimeTickSourceTest {

   private SimulationTickSource sut;

   @Before
   public void setUp() {
      sut = new SimulationTickSource();
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

      verify(listenerA).tick(0);

      sut.advance();

      assertEquals(2, sut.getCurrentTick());
      verify(listenerA).tick(1);
      
      sut.addListener(listenerB);
      
      sut.advance();

      assertEquals(3, sut.getCurrentTick());
      verify(listenerA).tick(2);
      verify(listenerB).tick(2);
   }

}

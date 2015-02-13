package org.spicej.ticks;

public interface TickSource {
   void reset();

   void addListener(TickListener listener);

   void removeListener(TickListener listener);
}

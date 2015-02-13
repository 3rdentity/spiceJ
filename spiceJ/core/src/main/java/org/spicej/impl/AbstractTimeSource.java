package org.spicej.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.spicej.ticks.TickSource;
import org.spicej.ticks.TickListener;

public abstract class AbstractTimeSource implements TickSource {
   private final Queue<TickListener> listeners = new ConcurrentLinkedQueue<>();

   @Override
   public void addListener(TickListener listener) {
      listeners.add(listener);
   }

   @Override
   public void removeListener(TickListener listener) {
      listeners.remove(listener);
   }

   protected void doTick(long tick) {
      for (TickListener listener : listeners)
         listener.tick(tick);
   }
}

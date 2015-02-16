package org.spicej.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.spicej.ticks.TickSource;
import org.spicej.ticks.TickListener;

/**
 * An abstract tick source keeping track of its listeners. The tick source is in
 * a non-initialized state until the first call to {@link #doTick()}.
 */
public abstract class AbstractTickSource implements TickSource {
   private long tick = -1;

   protected final Queue<TickListener> listeners = new ConcurrentLinkedQueue<>();

   @Override
   public void addListener(TickListener listener) {
      listeners.add(listener);
   }

   @Override
   public void removeListener(TickListener listener) {
      listeners.remove(listener);
   }

   /**
    * Distributes a tick event to its listeners.
    */
   protected void doTick() {
      tick++;
      for (TickListener listener : listeners)
         listener.tick(tick);
   }

   @Override
   public long getCurrentTick() {
      return tick;
   }

   @Override
   public void reset() {
      tick = -1;
   }
}

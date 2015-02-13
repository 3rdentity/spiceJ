package org.spicej.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.spicej.ticks.TickSource;
import org.spicej.ticks.TickListener;

/**
 * An abstract tick source keeping track of its listeners.
 */
public abstract class AbstractTickSource implements TickSource {
   private final Queue<TickListener> listeners = new ConcurrentLinkedQueue<>();

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
    * 
    * @param tick
    */
   protected void doTick(long tick) {
      for (TickListener listener : listeners)
         listener.tick(tick);
   }
}

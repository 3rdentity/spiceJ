package org.spicej;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.spicej.RateCalculator.Result;
import org.spicej.impl.RealTimeTickSource;
import org.spicej.ticks.TickSource;

public class SocketProxy implements Runnable {
   private final int remotePort;
   private final String remoteHost;
   private final Float rateUp, rateDown;

   private ServerSocket listener;

   private static final int TICK_IN_NS = 1000000; // 1 ms
   private static final int S_IN_TICKS = 1000; // 1000 ticks = 1000 ms = 1 s

   private static final int PRESCALER_1 = 500;
   private static final int PRESCALER_2 = 1000;

   private static final int THRESHOLD = 10000;

   private static TickSource tickSource = new RealTimeTickSource(TICK_IN_NS, true);

   public SocketProxy(int localPort, String remoteHost, int remotePort, Float rateUp, Float rateDown) throws IOException {
      this.remoteHost = remoteHost;
      this.remotePort = remotePort;
      this.rateUp = rateUp;
      this.rateDown = rateDown;

      listener = new ServerSocket(localPort);
   }

   public void run() {
      while (true) {
         try {
            Socket client = listener.accept();
            try {
               Socket server = new Socket(remoteHost, remotePort);
               Thread connector = new Thread(new Connector(client, server));
               connector.setDaemon(true);
               connector.start();
            } catch (Exception ignore) {
               ignore.printStackTrace();
               try {
                  client.close();
               } catch (Exception ignore2) {}
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   class Connector implements Runnable {

      private final Socket client, server;

      public Connector(Socket client, Socket server) {
         this.client = client;
         this.server = server;
      }

      @Override
      public void run() {
         StreamGobbler a = null, b = null;
         try {
            a = new StreamGobbler(rate(client.getInputStream(), rateUp), server.getOutputStream());
            b = new StreamGobbler(rate(server.getInputStream(), rateDown), client.getOutputStream());

            Thread t = new Thread(a);
            t.setDaemon(true);
            t.start();

            t = new Thread(b);
            t.setDaemon(true);
            t.start();

            a.waitFor();
            b.waitFor();
         } catch (Throwable t) {
            t.printStackTrace();
         } finally {
            try {
               client.close();
            } catch (Exception ignore) {}
            try {
               server.close();
            } catch (Exception ignore) {}
         }
      }

   }

   private static InputStream rate(InputStream inputStream, Float rate) {
      if (rate == null)
         return inputStream;

      Result calculation = RateCalculator.calculate(rate);
      return Streams.limitRate(inputStream, new RealTimeTickSource(calculation.getTickNanosecondInterval(), true), calculation.getBytesPerTick(), calculation.getPrescale());
   }
}

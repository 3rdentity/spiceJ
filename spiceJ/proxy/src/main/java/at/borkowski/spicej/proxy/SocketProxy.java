package at.borkowski.spicej.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import at.borkowski.spicej.Streams;
import at.borkowski.spicej.impl.RealTimeTickSource;
import at.borkowski.spicej.proxy.RateCalculator.Result;

/**
 * A proxy provider for TCP connections allowing for upstream and downstream
 * rate limitation. This proxy listens for connections on a local port and
 * forwards data to and from a connection to a remote port upon local
 * connection. Upstream and downstream rate limitations are possible.
 */
public class SocketProxy implements Runnable {
   final int localPort, remotePort;
   final String remoteHost;
   final Float rateUp, rateDown;

   private ServerSocket listener;

   /**
    * Creates a proxy and binds to the local port.
    * 
    * @param localPort
    *           the local port to listen on
    * @param remoteHost
    *           the remote host to connect to
    * @param remotePort
    *           the remote port to connect to
    * @param rateUp
    *           the upstream rate limitation to establish, or <code>null</code>
    *           for no limit
    * @param rateDown
    *           the downstream rate limitation to establish, or
    *           <code>null</code> for no limit
    * @throws IOException
    */
   public SocketProxy(int localPort, String remoteHost, int remotePort, Float rateUp, Float rateDown) {
      this.localPort = localPort;
      this.remoteHost = remoteHost;
      this.remotePort = remotePort;
      this.rateUp = rateUp;
      this.rateDown = rateDown;
   }

   private static InputStream rate(InputStream inputStream, Float rate) {
      if (rate == null)
         return inputStream;

      Result calculation = RateCalculator.calculate(rate);
      return Streams.limitRate(inputStream, new RealTimeTickSource(calculation.getTickNanosecondInterval(), true), calculation.getBytesPerTick(), calculation.getPrescale());
   }

   public void initialize() throws IOException {
      if (listener != null)
         throw new IllegalStateException("already initialized");
      listener = new ServerSocket(localPort);
   }

   @Override
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
         } catch (SocketException ignore) {
            /*
             * unfortunately we can't distinguish between "socket closed" and other exceptions.
             * "socket closed" occurs because we have to close the socket while the other gobbler is
             * still running
             */
         } catch (Throwable t) {
            t.printStackTrace();
         } finally {
            a.close();
            b.close();
            try {
               client.close();
            } catch (Exception ignore) {}
            try {
               server.close();
            } catch (Exception ignore) {}
         }
      }

   }
}

package at.borkowski.spicej.proxy;

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

// TODO test this
public class ProxyMain {
   private static boolean printUsage = true;
   private static boolean printCommandLineErrors = true;

   public static void main(String[] args) throws IOException {
      SocketProxy proxy = processCommand(args);
      if (proxy != null) {
         try {
            proxy.initialize();
         } catch (IOException e) {
            System.err.println("Error in set-up: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
            return;
         }
         proxy.run();
      }
   }

   public static SocketProxy processCommand(String... args) {
      CommandLine commandLine = new CommandLine();
      JCommander commander = new JCommander(commandLine);
      try {
         commander.parse(args);
      } catch (ParameterException pEx) {
         err(pEx.getMessage());
         usage(commander);
         return null;
      }

      if (commandLine.help) {
         usage(commander);
         return null;
      }

      if (commandLine.udp) {
         err("UDP is not yet implemented");
         return null;
      }

      if (commandLine.rate != null) {
         if (commandLine.rateReceive != null) {
            err("You can't use --rate and --rate-receive at the same time");
            return null;
         }
         if (commandLine.rateSend != null) {
            err("You can't use --rate and --rate-send at the same time");
            return null;
         }
         commandLine.rateReceive = commandLine.rateSend = commandLine.rate;
      }

      if (commandLine.delay != null) {
         if (commandLine.delayReceive != null) {
            err("You can't use --delay and --delay-receive at the same time");
            return null;
         }
         if (commandLine.delaySend != null) {
            err("You can't use --delay and --delay-send at the same time");
            return null;
         }
         commandLine.delayReceive = commandLine.delaySend = commandLine.delay;
      }

      if (commandLine.proxyDescription.size() != 1) {
         err("Exactly one proxy description required in the form of localPort:[remoteHost:]remotePort");
         return null;
      }

      String[] split = commandLine.proxyDescription.get(0).split(":");
      if (split.length < 2 || split.length > 3) {
         err("Proxy description must be in the form of localPort:[remoteHost:]remotePort (is \"" + commandLine.proxyDescription.get(0) + "\"");
         return null;
      }
      int localPort, remotePort;
      String remoteHost = "localhost";

      localPort = Integer.parseInt(split[0]);
      remotePort = Integer.parseInt(split[split.length - 1]);
      if (split.length == 3)
         remoteHost = split[1];

      SocketProxy sp;
      sp = new SocketProxy(localPort, remoteHost, remotePort, commandLine.rateSend, commandLine.rateReceive, commandLine.delayReceive, commandLine.delaySend);

      return sp;
   }

   private static void err(String string) {
      if (printCommandLineErrors)
         System.err.println(string);
   }

   private static void usage(JCommander commander) {
      if (printUsage)
         commander.usage();
   }

   public static void setPrintUsage(boolean printUsage) {
      ProxyMain.printUsage = printUsage;
   }

   public static void setPrintCommandLineErrors(boolean printCommandLineErrors) {
      ProxyMain.printCommandLineErrors = printCommandLineErrors;
   }
}

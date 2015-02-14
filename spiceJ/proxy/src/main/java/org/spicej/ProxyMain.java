package org.spicej;

import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class ProxyMain {
   public static void main(String[] args) {
      CommandLine commandLine = new CommandLine();
      JCommander commander = new JCommander(commandLine);
      try {
         commander.parse(args);
      } catch (ParameterException pEx) {
         System.out.println(pEx.getMessage());
         commander.usage();
         System.exit(1);
         return;
      }

      if (commandLine.help) {
         commander.usage();
         return;
      }

      if (commandLine.udp) {
         System.err.println("UDP is not yet implemented");
         System.exit(1);
         return;
      }

      if (commandLine.rate != null) {
         if (commandLine.rateReceive != null) {
            System.err.println("You can't use --rate and --rate-receive at the same time");
            System.exit(1);
            return;
         }
         if (commandLine.rateSend != null) {
            System.err.println("You can't use --rate and --rate-send at the same time");
            System.exit(1);
            return;
         }
         commandLine.rateReceive = commandLine.rateSend = commandLine.rate;
      }

      SocketProxy sp;
      try {
         sp = new SocketProxy(commandLine.localPort, commandLine.remoteHost, commandLine.remotePort, commandLine.rateSend, commandLine.rateReceive);
      } catch (IOException e) {
         System.err.println("Error in set-up: " + e.getMessage());
         e.printStackTrace();
         System.exit(1);
         return;
      }

      sp.run();
   }
}

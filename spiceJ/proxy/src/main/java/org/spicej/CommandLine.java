package org.spicej;

import java.util.List;

import com.beust.jcommander.Parameter;

public class CommandLine {
   @Parameter(description = "localport:[remotehost:]remoteport", required = true)
   public List<String> proxyDescription;

   @Parameter(names = { "--rate-send", "-a" }, description = "The byte rate to allow towards the remote host")
   public Float rateSend;

   @Parameter(names = { "--rate-receive", "-b" }, description = "The byte rate to allow from the remote host")
   public Float rateReceive;

   @Parameter(names = { "--rate", "-r" }, description = "The byte rate to allow in both directions (can't be used with -a or -b)")
   public Float rate;

   @Parameter(names = { "--help", "-?" }, description = "Prints usage.")
   public boolean help;

   @Parameter(names = { "--udp", "-u" }, description = "Use UDP instead of TCP (not yet implemented)")
   public boolean udp = false;

   public static class ProxyDescription {
      public int localPort, remotePort;
      public String remoteHost = "localhost";
   }
}

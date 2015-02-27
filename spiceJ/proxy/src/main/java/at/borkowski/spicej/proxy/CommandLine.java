package at.borkowski.spicej.proxy;

import java.util.List;

import com.beust.jcommander.Parameter;

public class CommandLine {
   @Parameter(description = "localport:[remotehost:]remoteport", required = true)
   public List<String> proxyDescription;

   @Parameter(names = { "--rate-send", "-a" }, description = "The byte rate to allow towards the remote host, in bytes per second")
   public Float rateSend;

   @Parameter(names = { "--rate-receive", "-b" }, description = "The byte rate to allow from the remote host, in bytes per second")
   public Float rateReceive;

   @Parameter(names = { "--rate", "-r" }, description = "The byte rate to allow in both directions, in bytes per second (can't be used with -a/--rate-send or -b/--rate-receive)")
   public Float rate;
   
   @Parameter(names = { "--delay-send", "-A"}, description = "The delay to add to the stream towards the remote host, in milliseconds")
   public Float delayReceive;
   
   @Parameter(names = { "--delay-receive", "-B"}, description = "The delay to add to the stream from the remote hosts, in milliseconds")
   public Float delaySend;

   @Parameter(names = { "--delay", "-d" }, description = "The delay to add in both directions, in milliseconds (can't be used with -A/--delay-send or -B/--delay-receive)")
   public Float delay;

   @Parameter(names = { "--help", "-?" }, description = "Prints usage.")
   public boolean help;

   @Parameter(names = { "--udp", "-u" }, description = "Use UDP instead of TCP (not yet implemented)")
   public boolean udp = false;

   public static class ProxyDescription {
      public int localPort, remotePort;
      public String remoteHost = "localhost";
   }
}

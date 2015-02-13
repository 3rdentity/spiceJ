package org.spicej;

import com.beust.jcommander.Parameter;

public class CommandLine {
   @Parameter(names = { "--local-port", "-l" }, description = "The local port to listen on", required = true)
   public int localPort;

   @Parameter(names = { "--remote-host", "-h" }, description = "The remote host to connect to", required = false)
   public String remoteHost = "localhost";

   @Parameter(names = { "--remote-port", "-p" }, description = "The remote port to connect to", required = true)
   public int remotePort;

   @Parameter(names = { "--rate-send", "-a" }, description = "The byte rate to allow towards the remote host")
   public Integer rateSend;

   @Parameter(names = { "--rate-receive", "-b" }, description = "The byte rate to allow from the remote host")
   public Integer rateReceive;

   @Parameter(names = { "--rate", "-r" }, description = "The byte rate to allow in both directions (can't be used with -a or -b)")
   public Integer rate;
}

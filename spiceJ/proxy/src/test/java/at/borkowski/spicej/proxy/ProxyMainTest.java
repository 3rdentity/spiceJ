package at.borkowski.spicej.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class ProxyMainTest {

   public static float DELTA = 0.00001F;

   @Before
   public void setUp() {
      ProxyMain.setPrintCommandLineErrors(true);
      ProxyMain.setPrintUsage(false);
   }

   @Test
   public void testHelp() {
      ProxyMain.setPrintCommandLineErrors(false);

      assertNull(ProxyMain.processCommand("-?"));
      assertNull(ProxyMain.processCommand("--help"));
   }

   @Test
   public void testUnimplemented() {
      ProxyMain.setPrintCommandLineErrors(false);

      assertNull(ProxyMain.processCommand("-u"));
      assertNull(ProxyMain.processCommand("--udp"));
   }

   @Test
   public void testLocalSymmetric() {
      SocketProxy res = ProxyMain.processCommand("-r", "50000", "1234:1235");
      assertEquals(50000F, res.rateUp, DELTA);
      assertEquals(50000F, res.rateDown, DELTA);
      assertEquals(1234, res.localPort);
      assertEquals(1235, res.remotePort);
      assertEquals("localhost", res.remoteHost);

      res = ProxyMain.processCommand("--rate", "50005", "4321:5321");
      assertEquals(50005F, res.rateUp, DELTA);
      assertEquals(50005F, res.rateDown, DELTA);
      assertEquals(4321, res.localPort);
      assertEquals(5321, res.remotePort);
      assertEquals("localhost", res.remoteHost);
   }

   @Test
   public void testRemoteAsymmetric() {
      SocketProxy res = ProxyMain.processCommand("-a", "10", "-b", "1000", "1234:target:1235");
      assertEquals(10F, res.rateUp, DELTA);
      assertEquals(1000F, res.rateDown, DELTA);
      assertEquals(1234, res.localPort);
      assertEquals(1235, res.remotePort);
      assertEquals("target", res.remoteHost);

      res = ProxyMain.processCommand("--rate-send", "10", "-b", "1000", "1234:target:1235");
      assertEquals(10F, res.rateUp, DELTA);
      assertEquals(1000F, res.rateDown, DELTA);
      assertEquals(1234, res.localPort);
      assertEquals(1235, res.remotePort);
      assertEquals("target", res.remoteHost);
   }

   @Test
   public void testRemoteUpOnly() {
      SocketProxy res = ProxyMain.processCommand("-a", "10", "8080:target:1235");
      assertEquals(10F, res.rateUp, DELTA);
      assertEquals(null, res.rateDown);
      assertEquals(8080, res.localPort);
      assertEquals(1235, res.remotePort);
      assertEquals("target", res.remoteHost);
      
      res = ProxyMain.processCommand("--rate-send", "10", "8080:target:1235");
      assertEquals(10F, res.rateUp, DELTA);
      assertEquals(null, res.rateDown);
      assertEquals(8080, res.localPort);
      assertEquals(1235, res.remotePort);
      assertEquals("target", res.remoteHost);
   }

   @Test
   public void testLocalDownOnly() {
      SocketProxy res = ProxyMain.processCommand("-b", "10", "8080:target:1235");
      assertEquals(null, res.rateUp);
      assertEquals(10F, res.rateDown, DELTA);
      assertEquals(8080, res.localPort);
      assertEquals(1235, res.remotePort);
      assertEquals("target", res.remoteHost);
      
      res = ProxyMain.processCommand("--rate-receive", "13", "8080:target:1235");
      assertEquals(null, res.rateUp);
      assertEquals(13F, res.rateDown, DELTA);
      assertEquals(8080, res.localPort);
      assertEquals(1235, res.remotePort);
      assertEquals("target", res.remoteHost);
   }
}

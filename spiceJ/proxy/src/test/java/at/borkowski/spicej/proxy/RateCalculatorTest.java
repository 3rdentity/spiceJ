package at.borkowski.spicej.proxy;

import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.Before;
import org.junit.Test;

import at.borkowski.spicej.proxy.RateCalculator.Result;

public class RateCalculatorTest {

   public static final int SCALE = 60;

   private static final double MAX_ERROR = 0.01;

   @Before
   public void setUp() throws Exception {}

   @Test
   public void testMinimumByterate() {
      // one of the lowest byterates possible to generate with our data model
      test(RateCalculator.MIN_RATE);
   }

   @Test
   public void testMaximumByterate() {
      // one of the highest byterates possible to generate with our data model
      test(RateCalculator.MAX_RATE);
   }

   @Test
   public void testExtremelyLowByterates() {
      double[] rates = { 0.0001, 0.0005, 0.001, 0.002, 0.003, 0.004, 0.005, 0.007, 0.009, 0.01 };
      for (double r : rates)
         test((float) r);
   }

   @Test
   public void testVeryLowByterates() {
      double[] rates = { 0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.7, 0.9, 1 };
      for (double r : rates)
         test((float) r);
   }

   @Test
   public void testLowByterates() {
      double[] rates = { 1, 2, 3, 4, 5, 7, 10, 12, 15, 17, 19, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 125, 150, 175, 200, 300, 400, 500, 750, 900, 1000 };
      for (double r : rates)
         test((float) r);
   }

   @Test
   public void testMediumByterates1() {
      double[] ratesK = { 1, 1.1, 1.2, 1.3, 1.5, 1.75, 1.9, 2, 2.5, 3, 3.5, 4, 5, 6, 7, 8, 9, 10, 12, 15, 17, 19, 20, 25, 30, 35, 40, 50, 60, 70, 80, 90, 100 };
      for (double r : ratesK)
         test((float) r * 1000);
   }

   @Test
   public void testMediumByterates2() {
      double[] ratesK = { 100, 125, 150, 175, 200, 300, 500, 750, 1000, 1250, 1500, 1750, 2000, 3000, 4000, 5000, 7500, 8000, 10000 };
      for (double r : ratesK)
         test((float) r * 1000);
   }

   @Test
   public void testHighByterates() {
      double[] ratesM = { 10, 12, 15, 17, 19, 20, 23, 24, 26, 28, 30, 33, 39, 43, 49, 50, 51, 53, 58, 60, 65, 70, 75, 80, 85, 90, 95, 100 };
      for (double r : ratesM)
         test((float) r * 1000000);
   }

   @Test
   public void testVeryHighByterates() {
      double[] ratesM = { 125, 150, 175, 200, 250, 300, 350, 400, 450, 500, 550, 600, 700, 800, 900, 1000 };
      for (double r : ratesM)
         test((float) r * 1000000);
   }

   @Test
   public void testExtremelyHighByterates() {
      double[] ratesG = { 1, 1.2, 1.4, 1.6, 1.8, 2, 2.5, 3, 3.5, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 90, 100, 150, 200, 250, 300, 400, 500, 1000 };
      for (double r : ratesG)
         test((float) r * 1000000000);
   }

   private void test(float rate) {
      evaluate(rate, RateCalculator.calculate(rate));
      evaluate(rate, RateCalculator.calculate(rate, 50 * 1000000));
      evaluate(rate, RateCalculator.calculate(rate, 10 * 1000000));
      evaluate(rate, RateCalculator.calculate(rate, 1 * 1000000));
   }

   private void evaluate(float rate, Result r) {
      BigDecimal actualRate_ = new BigDecimal(RateCalculator.NS_PER_S);
      actualRate_ = actualRate_.multiply(new BigDecimal(r.getBytesPerTick()));
      actualRate_ = actualRate_.divide(new BigDecimal(r.getPrescale()), SCALE, RoundingMode.HALF_UP);
      actualRate_ = actualRate_.divide(new BigDecimal(r.getTickNanosecondInterval()), SCALE, RoundingMode.HALF_UP);

      if (actualRate_.compareTo(new BigDecimal(0)) == 0)
         fail("rate is 0");

      double actualRate = actualRate_.doubleValue();
      double error = actualRate / rate - 1;

      if (error > MAX_ERROR) {
         System.out.println(rate + " | " + actualRate + " | " + (double) Math.round(error * 10000) / 100 + "%");
         fail("error too high for byte rate " + rate + " (error: " + error + " > " + MAX_ERROR + ")");
      }
   }
}

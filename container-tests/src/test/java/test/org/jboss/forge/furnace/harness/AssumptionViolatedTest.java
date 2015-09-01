package test.org.jboss.forge.furnace.harness;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.furnace.Furnace;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;
import org.junit.runner.RunWith;

/**
 * Verify that {@link AssumptionViolatedException} is handled propertly in the {@link Furnace} test harness.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class AssumptionViolatedTest
{
   @Test
   public void testAssumptionShouldBeSkipped()
   {
      Assume.assumeTrue("If false, display this message", false);
      Assert.fail("This should not be executed");
   }

   @Test
   public void testAssumptionShouldBeSkippedNoMessage()
   {
      Assume.assumeTrue(false);
      Assert.fail("This should not be executed");
   }

   @Test
   public void testAssumptionShouldPass()
   {
      Assume.assumeTrue(true);
   }
}
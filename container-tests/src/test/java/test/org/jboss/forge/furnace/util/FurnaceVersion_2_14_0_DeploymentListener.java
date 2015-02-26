package test.org.jboss.forge.furnace.util;

import org.jboss.forge.arquillian.DeploymentListener;
import org.jboss.forge.furnace.Furnace;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Set the 'furnace.version.override' system property to 2.14.0.Final during test execution.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceVersion_2_14_0_DeploymentListener implements DeploymentListener
{
   @Override
   public void preDeploy(Furnace furnace, Archive<?> archive) throws Exception
   {
      System.setProperty("furnace.version.override", "2.14.0.Final");
   }

   @Override
   public void postDeploy(Furnace furnace, Archive<?> archive) throws Exception
   {
   }

   @Override
   public void preUndeploy(Furnace furnace, Archive<?> archive) throws Exception
   {
   }

   @Override
   public void postUndeploy(Furnace furnace, Archive<?> archive) throws Exception
   {
      System.clearProperty("furnace.version.override");
   }
}

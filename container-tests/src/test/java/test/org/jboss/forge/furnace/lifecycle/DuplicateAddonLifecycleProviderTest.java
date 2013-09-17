package test.org.jboss.forge.furnace.lifecycle;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.lifecycle.AddonLifecycleProvider;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class DuplicateAddonLifecycleProviderTest
{
   @Deployment
   @ShouldThrowException
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClass(MockAddonLifecycleProvider.class)
               .addClass(MockAddonLifecycleProvider2.class)
               .addAsServiceProvider(AddonLifecycleProvider.class, MockAddonLifecycleProvider.class,
                        MockAddonLifecycleProvider2.class);

      return archive;
   }

   @Test
   public void shouldNotRun() throws Exception
   {
      Assert.fail("Deployment should have failed.");
   }

}
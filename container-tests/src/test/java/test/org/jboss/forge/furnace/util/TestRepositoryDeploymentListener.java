package test.org.jboss.forge.furnace.util;

import java.io.FileNotFoundException;
import java.net.URL;

import org.jboss.forge.arquillian.DeploymentListener;
import org.jboss.forge.furnace.manager.maven.MavenContainer;

public class TestRepositoryDeploymentListener implements DeploymentListener
{
   private static String previousUserSettings;
   private static String previousLocalRepository;

   @Override
   public void preDeploy() throws Exception
   {
      previousUserSettings = System.setProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION,
               getAbsolutePath("profiles/settings.xml"));
      previousLocalRepository = System.setProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION,
               "target/the-other-repository");
   }

   @Override
   public void postDeploy() throws Exception
   {
      if (previousUserSettings == null)
      {
         System.clearProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION);
      }
      else
      {
         System.setProperty(MavenContainer.ALT_USER_SETTINGS_XML_LOCATION, previousUserSettings);
      }
      if (previousLocalRepository == null)
      {
         System.clearProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION);
      }
      else
      {
         System.setProperty(MavenContainer.ALT_LOCAL_REPOSITORY_LOCATION, previousUserSettings);
      }
   }

   @Override
   public void preUndeploy() throws Exception
   {
   }

   @Override
   public void postUndeploy() throws Exception
   {
   }

   private static String getAbsolutePath(String path) throws FileNotFoundException
   {
      URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
      if (resource == null)
         throw new FileNotFoundException(path);
      return resource.getFile();
   }

}

package org.jboss.forge.arquillian;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.util.Strings;

public class ForgeContainerConfiguration implements ContainerConfiguration
{
   private String classifier;

   public ForgeContainerConfiguration()
   {
      this.classifier = MavenAddonDependencyResolver.FORGE_ADDON_CLASSIFIER;
   }

   @Override
   public void validate() throws ConfigurationException
   {
      if (Strings.isNullOrEmpty(classifier))
      {
         throw new ConfigurationException("Classifier should not be null or empty");
      }
   }

   /**
    * @return the classifier
    */
   public String getClassifier()
   {
      return classifier;
   }

   /**
    * @param classifier the classifier to set
    */
   public void setClassifier(String classifier)
   {
      this.classifier = classifier;
   }

}

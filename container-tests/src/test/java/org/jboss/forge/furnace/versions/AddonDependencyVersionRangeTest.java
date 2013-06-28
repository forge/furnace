package org.jboss.forge.furnace.versions;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class AddonDependencyVersionRangeTest
{
   @Deployment(order = 1)
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("A", "[1,2]", true),
                        AddonDependencyEntry.create("B", "1", true),
                        AddonDependencyEntry.create("C", "2", true)
               );

      return archive;
   }

   @Deployment(name = "A,1", testable = false, order = 2)
   public static ForgeArchive getDeploymentA1()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @Deployment(name = "A,2", testable = false, order = 3)
   public static ForgeArchive getDeploymentA2()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @Deployment(name = "A,3", testable = false, order = 3)
   public static ForgeArchive getDeploymentA3()
   {
      return ShrinkWrap.create(ForgeArchive.class).addBeansXML();
   }

   @Deployment(name = "B,1", testable = false, order = 4)
   public static ForgeArchive getDeploymentB1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("A", "2,3", true)
               );

      return archive;
   }

   @Deployment(name = "C,1", testable = false, order = 4)
   public static ForgeArchive getDeploymentC1()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("A", "1", false)
               );

      return archive;
   }

   @Deployment(name = "C,2", testable = false, order = 4)
   public static ForgeArchive getDeploymentC2()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("A", "2", true)
               );

      return archive;
   }

   @Test
   public void testBuildGraphs() throws Exception
   {
   }
}
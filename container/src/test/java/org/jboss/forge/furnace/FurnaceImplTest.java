package org.jboss.forge.furnace;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.versions.Version;
import org.junit.Assert;
import org.junit.Test;

public class FurnaceImplTest
{
   @Test(expected = IllegalArgumentException.class)
   public void shouldValidateAddRepositoryArgumentMode() throws Exception
   {
      Furnace f = new FurnaceImpl();
      f.addRepository(null, new File("."));
   }

   @Test(expected = IllegalArgumentException.class)
   public void shouldValidateAddRepositoryArgumentDirectory() throws Exception
   {
      Furnace f = new FurnaceImpl();
      f.addRepository(AddonRepositoryMode.IMMUTABLE, null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void shouldValidateAddRepositoryArgumentRepository() throws Exception
   {
      Furnace f = new FurnaceImpl();
      f.addRepository((AddonRepository) null);
   }

   @Test
   public void testAddMultipleDiskRepositoriesWithSameRootDirectoryIsIdempotent() throws Exception
   {
      Furnace f = new FurnaceImpl();
      AddonRepository repo1 = f.addRepository(AddonRepositoryMode.IMMUTABLE, new File("target"));
      AddonRepository repo2 = f.addRepository(AddonRepositoryMode.IMMUTABLE, new File("target"));
      Assert.assertEquals(repo1, repo2);
   }

   @Test
   public void shouldNotAllowMultipleRepositoriesWithSameRootDirectory() throws Exception
   {
      Furnace f = new FurnaceImpl();
      AddonRepository repo1 = f.addRepository(AddonRepositoryMode.IMMUTABLE, new File("target"));
      AddonRepository repo2 = f.addRepository(new TestAddonRepository(new File("target")));
      Assert.assertEquals(repo1, repo2);
   }

   @Test
   public void shouldAllowToAddDiskRepository() throws Exception
   {
      Furnace f = new FurnaceImpl();
      f.addRepository(AddonRepositoryMode.IMMUTABLE, new File("target"));
      Assert.assertEquals(1, f.getRepositories().size());
   }

   @Test
   public void shouldAllowToAddCustomRepository() throws Exception
   {
      Furnace f = new FurnaceImpl();

      AddonRepository repository = new TestAddonRepository(new File("target"));
      f.addRepository(repository);

      Assert.assertEquals(1, f.getRepositories().size());
      Assert.assertEquals(repository, f.getRepositories().get(0));
   }

   private static class TestAddonRepository implements AddonRepository
   {
      private final Date modified;
      private final File rootDir;

      public TestAddonRepository(File rootDir)
      {
         this.modified = new Date();
         this.rootDir = rootDir;
      }

      @Override
      public File getAddonBaseDir(AddonId addon)
      {
         return null;
      }

      @Override
      public Set<AddonDependencyEntry> getAddonDependencies(AddonId addon)
      {
         return Collections.emptySet();
      }

      @Override
      public File getAddonDescriptor(AddonId addon)
      {
         return null;
      }

      @Override
      public List<File> getAddonResources(AddonId addon)
      {
         return Collections.emptyList();
      }

      @Override
      public File getRootDirectory()
      {
         return rootDir;
      }

      @Override
      public boolean isDeployed(AddonId addon)
      {
         return false;
      }

      @Override
      public boolean isEnabled(AddonId addon)
      {
         return false;
      }

      @Override
      public List<AddonId> listEnabled()
      {
         return Collections.emptyList();
      }

      @Override
      public List<AddonId> listEnabledCompatibleWithVersion(Version version)
      {
         return null;
      }

      @Override
      public Date getLastModified()
      {
         return modified;
      }

      @Override
      public int getVersion()
      {
         return 0;
      }
   }
}

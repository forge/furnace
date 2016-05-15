/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.repositories.*;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.furnace.versions.Versions;

/**
 * An immutable {@link MutableAddonRepository} implementation that delegates to a wrapped instances of
 * {@link MutableAddonStorageRepository} and {@link MutableAddonStateRepository}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public final class AddonRepositoryImpl implements MutableAddonRepository
{

   private static final String DEFAULT_ADDON_DIR = ".forge/addons";

   public static MutableAddonRepository forDirectory(Furnace furnace, File dir)
   {
      return new AddonRepositoryImpl(furnace, dir);
   }

   public static MutableAddonRepository forDefaultDirectory(Furnace furnace)
   {
      return new AddonRepositoryImpl(furnace, new File(OperatingSystemUtils.getUserHomePath(), DEFAULT_ADDON_DIR));
   }

   public static Version getRuntimeAPIVersion()
   {
      String versionOverride = System.getProperty("furnace.version.override");
      if (versionOverride != null)
      {
         return SingleVersion.valueOf(versionOverride);
      }
      return Versions.getImplementationVersionFor(AddonRepository.class);
   }

   public static boolean hasRuntimeAPIVersion()
   {
      return getRuntimeAPIVersion() != null;
   }

   public static boolean isApiCompatible(Version runtimeVersion, AddonId entry)
   {
      Assert.notNull(entry, "Addon entry must not be null.");

      return Versions.isApiCompatible(runtimeVersion, entry.getApiVersion());
   }

   private final MutableAddonStorageRepository storageRepository;

   private final MutableAddonStateRepository stateRepository;

   private final File addonDir;

   private AddonRepositoryImpl(Furnace furnace, File addonDir)
   {
      this(new AddonStorageRepositoryImpl(furnace.getLockManager(), addonDir),
              new AddonStateRepositoryImpl(furnace, addonDir), addonDir);
   }

   public AddonRepositoryImpl(MutableAddonStorageRepository storageRepository,
                              MutableAddonStateRepository stateRepository, File addonDir)
   {
      Assert.notNull(addonDir, "Addon directory must not be null.");

      this.storageRepository = storageRepository;
      this.stateRepository = stateRepository;
      this.addonDir = addonDir;
   }

   @Override
   public String toString()
   {
      return addonDir.getAbsolutePath();
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((addonDir == null) ? 0 : addonDir.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      AddonRepositoryImpl other = (AddonRepositoryImpl) obj;
      if (addonDir == null)
      {
         if (other.addonDir != null)
            return false;
      }
      else if (!addonDir.equals(other.addonDir))
         return false;
      return true;
   }

   @Override
   public boolean disable(AddonId addon)
   {
      return stateRepository.disable(addon);
   }

   @Override
   public boolean enable(AddonId addon)
   {
      return stateRepository.enable(addon);
   }

   @Override
   public boolean isEnabled(AddonId addon)
   {
      return stateRepository.isEnabled(addon);
   }

   @Override
   public List<AddonId> listAll()
   {
      return stateRepository.listAll();
   }

   @Override
   public List<AddonId> listEnabled()
   {
      return stateRepository.listEnabled();
   }

   @Override
   public List<AddonId> listEnabledCompatibleWithVersion(Version version)
   {
      return stateRepository.listEnabledCompatibleWithVersion(version);
   }

   @Override
   public int getVersion()
   {
      return stateRepository.getVersion();
   }

   @Override
   public boolean deploy(AddonId addon, Iterable<AddonDependencyEntry> dependencies, Iterable<File> resourceJars)
   {
      return storageRepository.deploy(addon, dependencies, resourceJars);
   }

   @Override
   public boolean undeploy(AddonId addonEntry)
   {
      return storageRepository.undeploy(addonEntry);
   }

   @Override
   public File getAddonBaseDir(AddonId addon)
   {
      return storageRepository.getAddonBaseDir(addon);
   }

   @Override
   public Set<AddonDependencyEntry> getAddonDependencies(AddonId addon)
   {
      return storageRepository.getAddonDependencies(addon);
   }

   @Override
   public File getAddonDescriptor(AddonId addon)
   {
      return storageRepository.getAddonDescriptor(addon);
   }

   @Override
   public List<File> getAddonResources(AddonId addon)
   {
      return storageRepository.getAddonResources(addon);
   }

   @Override
   public boolean isDeployed(AddonId addon)
   {
      return storageRepository.isDeployed(addon);
   }

   @Override
   public File getRootDirectory()
   {
      return addonDir;
   }

   @Override
   public Date getLastModified()
   {
      return new Date(addonDir.lastModified());
   }
}

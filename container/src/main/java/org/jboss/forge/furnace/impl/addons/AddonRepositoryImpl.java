/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonCompatibilityStrategy;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.MutableAddonRepository;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.util.Streams;
import org.jboss.forge.furnace.versions.SingleVersion;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.parser.xml.XMLParserException;

/**
 * Used to perform Addon installation/registration operations.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public final class AddonRepositoryImpl implements MutableAddonRepository
{
   /**
    * Setting this system property to <code>true</code> allows Furnace to deploy addons as symlinks
    */
   private static final String DEPLOY_AS_SYMLINK_SYSTEM_PROPERTY = "furnace.addon.deploy_as_symlink";

   private static final Logger logger = Logger.getLogger(AddonRepositoryImpl.class.getName());

   private static final String ATTR_API_VERSION = "api-version";
   private static final String ATTR_EXPORT = "export";
   private static final String ATTR_NAME = "name";
   private static final String ATTR_OPTIONAL = "optional";
   private static final String ATTR_VERSION = "version";

   private static final String DEFAULT_ADDON_DIR = ".forge/addons";
   private static final String REGISTRY_DESCRIPTOR_NAME = "installed.xml";
   private static final String ADDON_DESCRIPTOR_FILENAME = "addon.xml";

   private static final String DEPENDENCY_TAG_NAME = "dependency";
   private static final String DEPENDENCIES_TAG_NAME = "dependencies";

   private final LockManager lock;

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

   private final File addonDir;

   private int version = 1;

   private Furnace furnace;

   private AddonRepositoryImpl(Furnace furnace, File dir)
   {
      this.furnace = furnace;
      Assert.notNull(furnace, "Furnace must not be null.");
      Assert.notNull(dir, "Addon directory must not be null.");
      this.addonDir = dir;
      this.lock = furnace.getLockManager();
   }

   @Override
   public boolean deploy(final AddonId addon, final Iterable<AddonDependencyEntry> dependencies,
            final Iterable<File> resources)
   {
      return lock.performLocked(LockMode.WRITE, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            File addonSlotDir = getAddonBaseDir(addon);
            File descriptor = getAddonDescriptor(addon);
            try
            {
               if (resources != null)
               {
                  for (File resource : resources)
                  {
                     if (resource.isDirectory())
                     {
                        String child = addon.getName()
                                 + resource.getParentFile().getParentFile().getName();
                        child = OperatingSystemUtils.getSafeFilename(child);
                        File target = new File(addonSlotDir, child);
                        if (Boolean.getBoolean(DEPLOY_AS_SYMLINK_SYSTEM_PROPERTY))
                        {
                           logger.fine("Creating symlink from " + resource + " to " + target);
                           java.nio.file.Files.createSymbolicLink(target.toPath(), resource.toPath());
                        }
                        else
                        {
                           logger.fine("Copying " + resource + " to " + target);
                           Files.copyDirectory(resource, target);
                        }
                     }
                     else
                     {
                        if (Boolean.getBoolean(DEPLOY_AS_SYMLINK_SYSTEM_PROPERTY))
                        {
                           logger.fine("Creating symlink from " + resource + " to "
                                    + addonSlotDir.toPath().resolve(resource.getName()));
                           java.nio.file.Files.createSymbolicLink(addonSlotDir.toPath().resolve(resource.getName()),
                                    resource.toPath());
                        }
                        else
                        {
                           logger.fine("Copying " + resource + " to " + addonSlotDir);
                           Files.copyFileToDirectory(resource, addonSlotDir);
                        }
                     }
                  }
               }
               /*
                * Write out the addon module dependency configuration
                */
               Node addonXml = getXmlRoot(descriptor);
               Node dependenciesNode = addonXml.getOrCreate(DEPENDENCIES_TAG_NAME);

               if (dependencies != null)
               {
                  for (AddonDependencyEntry dependency : dependencies)
                  {
                     String name = dependency.getName();
                     Node dep = null;
                     for (Node node : dependenciesNode.get(DEPENDENCY_TAG_NAME))
                     {
                        if (name.equals(node.getAttribute(ATTR_NAME)))
                        {
                           dep = node;
                           break;
                        }
                     }
                     if (dep == null)
                     {
                        dep = dependenciesNode.createChild(DEPENDENCY_TAG_NAME);
                        dep.attribute(ATTR_NAME, name);
                     }
                     dep.attribute(ATTR_VERSION, dependency.getVersionRange());
                     dep.attribute(ATTR_EXPORT, dependency.isExported());
                     dep.attribute(ATTR_OPTIONAL, dependency.isOptional());
                  }
               }

               try (FileOutputStream fos = new FileOutputStream(descriptor))
               {
                  Streams.write(XMLParser.toXMLInputStream(addonXml), fos);
               }
               return true;
            }
            catch (IOException io)
            {
               logger.log(Level.SEVERE, "Error while deploying addon " + addon, io);
               return false;
            }
         }
      });
   }

   @Override
   public boolean disable(final AddonId addon)
   {
      return lock.performLocked(LockMode.WRITE, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            if (addon == null)
            {
               throw new RuntimeException("Addon must not be null");
            }

            File registryFile = getRepositoryRegistryFile();
            if (registryFile.exists())
            {
               try
               {
                  Node installed = getXmlRoot(registryFile);

                  Node child = installed.getSingle("addon@" + ATTR_NAME + "=" + addon.getName() + "&"
                           + ATTR_VERSION + "=" + addon.getVersion());
                  installed.removeChild(child);
                  saveRegistryFile(installed);
                  return true;
               }
               catch (IOException e)
               {
                  throw new RuntimeException("Could not modify [" + registryFile.getAbsolutePath() + "] - ", e);
               }
            }
            return false;
         }
      });
   }

   @Override
   public boolean enable(final AddonId addon)
   {
      return lock.performLocked(LockMode.WRITE, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            if (addon == null)
            {
               throw new RuntimeException("AddonId must not be null");
            }

            File registryFile = getRepositoryRegistryFile();
            try
            {
               Node installed = getXmlRoot(registryFile);

               installed.getOrCreate("addon@" + ATTR_NAME + "=" + (addon.getName() == null ? "" : addon.getName()) +
                        "&" + ATTR_VERSION + "=" + addon.getVersion())
                        .attribute(ATTR_API_VERSION, (addon.getApiVersion() == null ? "" : addon.getApiVersion()));

               saveRegistryFile(installed);
               return true;
            }
            catch (FileNotFoundException e)
            {
               throw new RuntimeException("Could not read [" + registryFile.getAbsolutePath() + "] - ", e);
            }
         }
      });
   }

   @Override
   public File getAddonBaseDir(final AddonId found)
   {
      Assert.notNull(found, "Addon must be specified.");
      Assert.notNull(found.getVersion(), "Addon version must be specified.");
      Assert.notNull(found.getName(), "Addon name must be specified.");

      return lock.performLocked(LockMode.READ, new Callable<File>()
      {
         @Override
         public File call() throws Exception
         {
            File addonDir = new File(getRootDirectory(), OperatingSystemUtils.getSafeFilename(found.toCoordinates()));
            return addonDir;
         }
      });
   }

   @Override
   public Set<AddonDependencyEntry> getAddonDependencies(final AddonId addon)
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<AddonDependencyEntry>>()
      {
         @Override
         public Set<AddonDependencyEntry> call() throws Exception
         {
            Set<AddonDependencyEntry> result = new LinkedHashSet<AddonDependencyEntry>();
            File descriptor = getAddonDescriptor(addon);

            try
            {
               Node installed = getXmlRoot(descriptor);

               List<Node> children = installed.get("dependencies/dependency");
               for (final Node child : children)
               {
                  if (child != null)
                  {
                     result.add(AddonDependencyEntry.create(
                              child.getAttribute(ATTR_NAME),
                              Versions.parseMultipleVersionRange(child.getAttribute(ATTR_VERSION)),
                              Boolean.valueOf(child.getAttribute(ATTR_EXPORT)),
                              Boolean.valueOf(child.getAttribute(ATTR_OPTIONAL))));
                  }
               }
            }
            catch (FileNotFoundException e)
            {
               // already removed
            }

            return result;
         }
      });
   }

   @Override
   public File getAddonDescriptor(final AddonId addon)
   {
      return lock.performLocked(LockMode.READ, new Callable<File>()
      {
         @Override
         public File call() throws Exception
         {
            File descriptorFile = getAddonDescriptorFile(addon);
            try
            {
               if (!descriptorFile.exists())
               {
                  descriptorFile.mkdirs();
                  descriptorFile.delete();
                  descriptorFile.createNewFile();

                  FileOutputStream stream = null;
                  try
                  {
                     stream = new FileOutputStream(descriptorFile);
                     Streams.write(XMLParser.toXMLInputStream(XMLParser.parse("<addon/>")), stream);
                  }
                  finally
                  {
                     Streams.closeQuietly(stream);
                  }
               }
               return descriptorFile;
            }
            catch (Exception e)
            {
               throw new RuntimeException("Error initializing addon descriptor file.", e);
            }
         }
      });
   }

   private File getAddonDescriptorFile(final AddonId addon)
   {
      return lock.performLocked(LockMode.READ, new Callable<File>()
      {

         @Override
         public File call() throws Exception
         {
            return new File(getAddonBaseDir(addon), ADDON_DESCRIPTOR_FILENAME);
         }
      });
   }

   @Override
   public List<File> getAddonResources(final AddonId found)
   {
      return lock.performLocked(LockMode.READ, new Callable<List<File>>()
      {
         @Override
         public List<File> call() throws Exception
         {
            File dir = getAddonBaseDir(found);
            if (dir.exists())
            {
               File[] files = dir.listFiles(new FileFilter()
               {
                  @Override
                  public boolean accept(File pathname)
                  {
                     return pathname.isDirectory() || pathname.getName().endsWith(".jar");
                  }
               });
               return Arrays.asList(files);
            }
            return Collections.emptyList();
         }
      });
   }

   @Override
   public File getRootDirectory()
   {
      if (!addonDir.exists() || !addonDir.isDirectory())
      {
         lock.performLocked(LockMode.READ, new Callable<File>()
         {
            @Override
            public File call() throws Exception
            {
               addonDir.delete();
               System.gc();
               if (!addonDir.mkdirs())
               {
                  throw new RuntimeException("Could not create Addon Directory [" + addonDir + "]");
               }
               return addonDir;
            }
         });
      }
      return addonDir;
   }

   private File getRepositoryRegistryFile()
   {
      return lock.performLocked(LockMode.READ, new Callable<File>()
      {
         @Override
         public File call() throws Exception
         {
            File registryFile = new File(getRootDirectory(), REGISTRY_DESCRIPTOR_NAME);
            try
            {
               if (!registryFile.exists())
               {
                  java.nio.file.Files.write(registryFile.toPath(), "<installed/>".getBytes(),
                           StandardOpenOption.CREATE_NEW);
               }
               return registryFile;
            }
            catch (Exception e)
            {
               throw new RuntimeException("Error initializing addon registry file [" + registryFile + "]", e);
            }
         }
      });
   }

   @Override
   public boolean isDeployed(final AddonId addon)
   {
      return lock.performLocked(LockMode.READ, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            File addonBaseDir = getAddonBaseDir(addon);
            File addonDescriptorFile = getAddonDescriptorFile(addon);

            return addonBaseDir.exists() && addonDescriptorFile.exists();
         }
      });
   }

   @Override
   public boolean isEnabled(final AddonId addon)
   {
      return lock.performLocked(LockMode.READ, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            return listEnabled().contains(addon);
         }
      });
   }

   @Override
   public List<AddonId> listEnabled()
   {
      final AddonCompatibilityStrategy strategy = furnace.getAddonCompatibilityStrategy();
      return lock.performLocked(LockMode.READ, new Callable<List<AddonId>>()
      {
         @Override
         public List<AddonId> call() throws Exception
         {
            List<AddonId> list = listAll();
            List<AddonId> result = new ArrayList<>();
            for (AddonId entry : list)
            {
               if (strategy.isCompatible(furnace, entry))
               {
                  result.add(entry);
               }
            }
            return result;
         }
      });

   }

   @Override
   public List<AddonId> listEnabledCompatibleWithVersion(final Version version)
   {
      return lock.performLocked(LockMode.READ, new Callable<List<AddonId>>()
      {
         @Override
         public List<AddonId> call() throws Exception
         {
            List<AddonId> list = listAll();
            List<AddonId> result = new ArrayList<>();
            for (AddonId entry : list)
            {
               if (version == null || entry.getApiVersion() == null
                        || Versions.isApiCompatible(version, entry.getApiVersion()))
               {
                  result.add(entry);
               }
            }
            return result;
         }
      });
   }

   @Override
   public List<AddonId> listAll()
   {
      return lock.performLocked(LockMode.READ, new Callable<List<AddonId>>()
      {
         @Override
         public List<AddonId> call() throws Exception
         {
            List<AddonId> result = new ArrayList<AddonId>();
            File registryFile = getRepositoryRegistryFile();
            try
            {
               Node installed = getXmlRoot(registryFile);
               if (installed == null)
               {
                  return Collections.emptyList();
               }
               List<Node> list = installed.get("addon");
               for (Node addon : list)
               {
                  AddonId entry = AddonId.from(addon.getAttribute(ATTR_NAME),
                           addon.getAttribute(ATTR_VERSION),
                           addon.getAttribute(ATTR_API_VERSION));
                  result.add(entry);
               }
            }
            catch (XMLParserException e)
            {
               throw new RuntimeException("Invalid syntax in [" + registryFile.getAbsolutePath()
                        + "] - Please delete this file and restart Furnace", e);
            }
            catch (FileNotFoundException e)
            {
               // this is OK, no addons installed
            }
            return result;
         }
      });
   }

   @Override
   public boolean undeploy(final AddonId addon)
   {
      return lock.performLocked(LockMode.WRITE, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            File dir = getAddonBaseDir(addon);
            disable(addon);
            return Files.delete(dir, true);
         }
      });
   }

   private Node getXmlRoot(File registryFile) throws FileNotFoundException, InterruptedException
   {
      Node installed = null;

      while (installed == null)
      {
         try
         {
            installed = XMLParser.parse(registryFile);
         }
         catch (XMLParserException e)
         {
            logger.log(Level.WARNING, "Error occurred while parsing [" + registryFile + "]", e);
         }
      }

      return installed;
   }

   @Override
   public Date getLastModified()
   {
      return lock.performLocked(LockMode.READ, new Callable<Date>()
      {
         @Override
         public Date call() throws Exception
         {
            return new Date(getRepositoryRegistryFile().lastModified());
         }
      });
   }

   @Override
   public int getVersion()
   {
      return version;
   }

   private void saveRegistryFile(Node installed) throws FileNotFoundException
   {
      FileOutputStream outStream = null;
      try
      {
         outStream = new FileOutputStream(getRepositoryRegistryFile());
         incrementVersion();
         Streams.write(XMLParser.toXMLInputStream(installed), outStream);
      }
      finally
      {
         Streams.closeQuietly(outStream);
      }
   }

   private void incrementVersion()
   {
      version++;
   }

   @Override
   public String toString()
   {
      return getRootDirectory().getAbsolutePath();
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
}

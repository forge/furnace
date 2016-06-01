/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.util.Files;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.forge.furnace.util.Streams;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.parser.xml.XMLParserException;

/**
 * Used to perform Addon installation operations.
 * 
 * @author <a href="bsideup@gmail.com">Sergei Egorov</a>
 * @see AddonRepositoryImpl
 */
public final class AddonRepositoryStorageStrategyImpl extends AbstractFileSystemAddonRepository
         implements MutableAddonRepositoryStorageStrategy
{
   /**
    * Setting this system property to <code>true</code> allows Furnace to deploy addons as symlinks
    */
   private static final String DEPLOY_AS_SYMLINK_SYSTEM_PROPERTY = "furnace.addon.deploy_as_symlink";

   private static final Logger logger = Logger.getLogger(AddonRepositoryStorageStrategyImpl.class.getName());

   private static final String ATTR_EXPORT = "export";
   private static final String ATTR_NAME = "name";
   private static final String ATTR_OPTIONAL = "optional";
   private static final String ATTR_VERSION = "version";

   private static final String ADDON_DESCRIPTOR_FILENAME = "addon.xml";

   private static final String DEPENDENCY_TAG_NAME = "dependency";
   private static final String DEPENDENCIES_TAG_NAME = "dependencies";

   public AddonRepositoryStorageStrategyImpl(LockManager lock, File addonDir)
   {
      super(lock, addonDir);
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
   public boolean undeploy(final AddonId addon)
   {
      return lock.performLocked(LockMode.WRITE, new Callable<Boolean>()
      {
         @Override
         public Boolean call() throws Exception
         {
            File dir = getAddonBaseDir(addon);
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
}

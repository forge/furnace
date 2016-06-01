/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonCompatibilityStrategy;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.MutableAddonRepositoryStateStrategy;
import org.jboss.forge.furnace.util.Streams;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.forge.furnace.versions.Versions;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.parser.xml.XMLParserException;

import java.io.*;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to perform Addon registration operations.
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public final class AddonRepositoryStateStrategyImpl extends AbstractFileSystemAddonRepository implements MutableAddonRepositoryStateStrategy
{

   private static final Logger logger = Logger.getLogger(AddonRepositoryStateStrategyImpl.class.getName());

   private static final String ATTR_API_VERSION = "api-version";
   private static final String ATTR_NAME = "name";
   private static final String ATTR_VERSION = "version";

   private static final String REGISTRY_DESCRIPTOR_NAME = "installed.xml";

   private final Furnace furnace;

   private int version = 1;

   public AddonRepositoryStateStrategyImpl(Furnace furnace, File addonDir)
   {
      super(furnace.getLockManager(), addonDir);
      this.furnace = furnace;
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
}

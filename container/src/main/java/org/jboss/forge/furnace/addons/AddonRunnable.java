/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.event.PostStartup;
import org.jboss.forge.furnace.event.PreShutdown;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.impl.AddonProducer;
import org.jboss.forge.furnace.impl.AddonRegistryProducer;
import org.jboss.forge.furnace.impl.AddonRepositoryProducer;
import org.jboss.forge.furnace.impl.ContainerServiceExtension;
import org.jboss.forge.furnace.impl.FurnaceProducer;
import org.jboss.forge.furnace.impl.ServiceRegistryImpl;
import org.jboss.forge.furnace.impl.ServiceRegistryProducer;
import org.jboss.forge.furnace.modules.AddonResourceLoader;
import org.jboss.forge.furnace.modules.ModularURLScanner;
import org.jboss.forge.furnace.modules.ModularWeld;
import org.jboss.forge.furnace.modules.ModuleScanResult;
import org.jboss.forge.furnace.services.ServiceRegistry;
import org.jboss.forge.furnace.util.Addons;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.BeanManagerUtils;
import org.jboss.forge.furnace.util.ClassLoaders;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.resources.spi.ResourceLoader;

import com.google.common.util.concurrent.Callables;

/**
 * Loads an {@link Addon}
 */
public final class AddonRunnable implements Runnable
{
   private static final Logger logger = Logger.getLogger(AddonRunnable.class.getName());

   boolean shutdownRequested = false;
   private Furnace furnace;
   private Addon addon;
   private AddonContainerStartup container;

   private Callable<Object> shutdownCallable = new Callable<Object>()
   {
      @Override
      public Object call() throws Exception
      {
         return null;
      }
   };

   private AddonLifecycleManager lifecycleManager;
   private AddonStateManager stateManager;

   public AddonRunnable(Furnace furnace,
            AddonLifecycleManager lifecycleManager,
            AddonStateManager stateManager,
            Addon addon)
   {
      this.lifecycleManager = lifecycleManager;
      this.stateManager = stateManager;
      this.furnace = furnace;
      this.addon = addon;
   }

   public void shutdown()
   {
      shutdownRequested = true;
      try
      {
         logger.info("< Stopping container [" + addon.getId() + "] [" + addon.getRepository().getRootDirectory()
                  + "]");
         long start = System.currentTimeMillis();
         ClassLoaders.executeIn(addon.getClassLoader(), shutdownCallable);
         logger.info("<< Stopped container [" + addon.getId() + "] - "
                  + (System.currentTimeMillis() - start) + "ms");
      }
      catch (RuntimeException e)
      {
         logger.log(Level.SEVERE, "Failed to shut down addon " + addon.getId(), e);
         throw e;
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Failed to shut down addon " + addon.getId(), e);
         throw new ContainerException("Failed to shut down addon " + addon.getId(), e);
      }
   }

   @Override
   public void run()
   {
      Thread currentThread = Thread.currentThread();
      String name = currentThread.getName();
      currentThread.setName(addon.getId().toCoordinates());
      try
      {
         logger.info("> Starting container [" + addon.getId() + "] [" + addon.getRepository().getRootDirectory()
                  + "]");
         long start = System.currentTimeMillis();
         container = new AddonContainerStartup();
         shutdownCallable = ClassLoaders.executeIn(addon.getClassLoader(), container);
         logger.info(">> Started container [" + addon.getId() + "] - "
                  + (System.currentTimeMillis() - start) + "ms");

         if (container.postStartupTask != null)
            ClassLoaders.executeIn(addon.getClassLoader(), container.postStartupTask);
      }
      catch (Throwable e)
      {
         logger.log(Level.SEVERE, "Failed to start addon [" + addon.getId() + "] with classloader ["
                  + stateManager.getClassLoaderOf(addon)
                  + "]", e);
         throw new RuntimeException(e);
      }
      finally
      {
         lifecycleManager.finishedStarting(addon);
         currentThread.setName(name);
      }
   }

   public Addon getAddon()
   {
      return addon;
   }

   public class AddonContainerStartup implements Callable<Callable<Object>>
   {
      private Callable<Void> postStartupTask;

      @Override
      public Callable<Object> call() throws Exception
      {
         try
         {
            ResourceLoader resourceLoader = new AddonResourceLoader(addon);
            ModularURLScanner scanner = new ModularURLScanner(resourceLoader, "META-INF/beans.xml");
            ModuleScanResult scanResult = scanner.scan();

            Callable<Object> shutdownCallback = null;

            if (scanResult.getDiscoveredResourceUrls().isEmpty())
            {
               /*
                * This is an import-only addon and does not require weld, nor provide remote services.
                */
               shutdownCallback = new Callable<Object>()
               {
                  @Override
                  public Object call() throws Exception
                  {
                     return null;
                  }
               };
            }
            else
            {
               final Weld weld = new ModularWeld(scanResult);
               WeldContainer container;
               container = weld.initialize();

               final BeanManager manager = container.getBeanManager();
               Assert.notNull(manager, "BeanManager was null");

               AddonRepositoryProducer repositoryProducer = BeanManagerUtils.getContextualInstance(manager,
                        AddonRepositoryProducer.class);
               repositoryProducer.setRepository(addon.getRepository());

               FurnaceProducer forgeProducer = BeanManagerUtils.getContextualInstance(manager, FurnaceProducer.class);
               forgeProducer.setForge(furnace);

               AddonProducer addonProducer = BeanManagerUtils.getContextualInstance(manager, AddonProducer.class);
               addonProducer.setAddon(addon);

               AddonRegistryProducer addonRegistryProducer = BeanManagerUtils.getContextualInstance(manager,
                        AddonRegistryProducer.class);
               addonRegistryProducer.setRegistry(furnace.getAddonRegistry());

               ContainerServiceExtension extension = BeanManagerUtils.getContextualInstance(manager,
                        ContainerServiceExtension.class);
               ServiceRegistryProducer serviceRegistryProducer = BeanManagerUtils.getContextualInstance(manager,
                        ServiceRegistryProducer.class);
               serviceRegistryProducer.setServiceRegistry(new ServiceRegistryImpl(furnace.getLockManager(), addon,
                        manager, extension));

               ServiceRegistry registry = BeanManagerUtils.getContextualInstance(manager, ServiceRegistry.class);
               Assert.notNull(registry, "Service registry was null.");
               stateManager.setServiceRegistry(addon, registry);

               logger.info("Services loaded from addon [" + addon.getId() + "] -  " + registry.getExportedTypes());

               shutdownCallback = new Callable<Object>()
               {
                  @Override
                  public Object call() throws Exception
                  {
                     try
                     {
                        manager.fireEvent(new PreShutdown());
                     }
                     catch (Exception e)
                     {
                        logger.log(Level.SEVERE, "Failed to execute pre-Shutdown event.", e);
                     }
                     finally
                     {
                     }

                     weld.shutdown();
                     return null;
                  }
               };

               postStartupTask = new Callable<Void>()
               {
                  @Override
                  public Void call() throws Exception
                  {
                     for (AddonDependency dependency : addon.getDependencies())
                     {
                        if (dependency.getDependency().getStatus().isLoaded())
                           Addons.waitUntilStarted(dependency.getDependency());
                     }

                     manager.fireEvent(new PostStartup());
                     return null;
                  }
               };
            }

            return shutdownCallback;
         }
         catch (Exception e)
         {
            addon.getFuture().cancel(false);
            if (!shutdownRequested)
               throw e;
            else
               return Callables.returning(null);
         }
      }
   }

   @Override
   public int hashCode()
   {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((addon == null) ? 0 : addon.hashCode());
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
      AddonRunnable other = (AddonRunnable) obj;
      if (addon == null)
      {
         if (other.addon != null)
            return false;
      }
      else if (!addon.equals(other.addon))
         return false;
      return true;
   }
}

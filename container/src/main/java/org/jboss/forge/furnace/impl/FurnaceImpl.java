/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.ContainerStatus;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.impl.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.impl.addons.AddonRegistryImpl;
import org.jboss.forge.furnace.impl.addons.AddonRepositoryImpl;
import org.jboss.forge.furnace.impl.addons.ImmutableAddonRepository;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.modules.Module;
import org.jboss.modules.log.StreamModuleLogger;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceImpl implements Furnace
{
   private static Logger logger = Logger.getLogger(FurnaceImpl.class.getName());

   private volatile boolean alive = false;
   private volatile ContainerStatus status = ContainerStatus.STOPPED;

   private final ExecutorService executor = Executors.newSingleThreadExecutor();

   private boolean serverMode = true;
   private AddonLifecycleManager manager;
   private final List<ContainerLifecycleListener> registeredListeners = new ArrayList<>();
   private final List<ListenerRegistration<ContainerLifecycleListener>> loadedListenerRegistrations = new ArrayList<>();

   private ClassLoader loader;

   private final List<AddonRepository> repositories = new ArrayList<>();
   private final Map<AddonRepository, Integer> lastRepoVersionSeen = new HashMap<>();

   private final LockManager lock = new LockManagerImpl();

   private String[] args;

   private int registryCount = 0;

   private WatchService watcher;

   public FurnaceImpl()
   {
      if (!AddonRepositoryImpl.hasRuntimeAPIVersion())
      {
         logger.warning("Could not detect Furnace runtime version - " +
                  "loading all addons, but failures may occur if versions are not compatible.");
      }

      if (!Boolean.getBoolean("furnace.logging.leak"))
      {
         /*
          * If enabled, allows the JDK java.util.logging.Level to leak ClassLoaders.
          */
         LoggingRepair.init();
      }

      if (Boolean.getBoolean("furnace.debug"))
      {
         /*
          * If enabled, prints a LOT of debug logging from JBoss Modules.
          */
         enableLogging();
      }

      try
      {
         watcher = FileSystems.getDefault().newWatchService();
      }
      catch (IOException e)
      {
         logger.log(Level.WARNING, "File monitoring could not be started.", e);
      }
   }

   @Override
   public LockManager getLockManager()
   {
      return lock;
   }

   @Override
   public ClassLoader getRuntimeClassLoader()
   {
      return loader;
   }

   public Furnace enableLogging()
   {
      assertNotAlive();
      Module.setModuleLogger(new StreamModuleLogger(System.err));
      return this;
   }

   @Override
   public Future<Furnace> startAsync()
   {
      return startAsync(FurnaceImpl.class.getClassLoader());
   }

   @Override
   public Future<Furnace> startAsync(final ClassLoader loader)
   {
      return executor.submit(new Callable<Furnace>()
      {
         @Override
         public Furnace call() throws Exception
         {
            Thread thread = new Thread()
            {
               @Override
               public void run()
               {
                  Thread.currentThread().setName("Furnace Container " + FurnaceImpl.this);
                  FurnaceImpl.this.start(loader);
               }
            };
            thread.start();

            while (!ContainerStatus.STARTED.equals(getStatus()))
               Thread.sleep(25);

            return FurnaceImpl.this;
         }
      });
   }

   @Override
   public void start()
   {
      start(FurnaceImpl.class.getClassLoader());
   }

   @Override
   public void start(ClassLoader loader)
   {
      logger.log(Level.INFO, "Furnace [" + AddonRepositoryImpl.getRuntimeAPIVersion() + "] starting.");
      assertNotAlive();
      alive = true;

      this.loader = loader;

      for (ContainerLifecycleListener listener : ServiceLoader.load(ContainerLifecycleListener.class, loader))
      {
         ListenerRegistration<ContainerLifecycleListener> registration = addContainerLifecycleListener(listener);
         loadedListenerRegistrations.add(registration);
      }

      fireBeforeContainerStartedEvent();

      try
      {
         getAddonRegistry();
         do
         {
            boolean dirty = false;
            if (!getLifecycleManager().isStartingAddons())
            {
               for (AddonRepository repository : repositories)
               {
                  int repoVersion = repository.getVersion();
                  if (repoVersion > lastRepoVersionSeen.get(repository))
                  {
                     logger.log(Level.INFO, "Detected changes in repository [" + repository + "].");
                     lastRepoVersionSeen.put(repository, repoVersion);
                     dirty = true;
                  }

               }

               WatchKey key = watcher.poll();
               while (key != null)
               {
                  List<WatchEvent<?>> events = key.pollEvents();
                  if (!events.isEmpty())
                  {
                     logger.log(Level.INFO, "Detected changes in repository [" + events.iterator().next().context()
                              + "].");
                     dirty = true;
                  }
                  key.reset();
                  key = watcher.poll();
               }

               if (dirty)
               {
                  try
                  {
                     fireBeforeConfigurationScanEvent();
                     getLifecycleManager().forceUpdate();
                     fireAfterConfigurationScanEvent();
                  }
                  catch (Exception e)
                  {
                     logger.log(Level.SEVERE, "Error occurred.", e);
                  }
               }
            }
            status = ContainerStatus.STARTED;

            Thread.sleep(100);
         }
         while (alive && serverMode);

         while (alive && getLifecycleManager().isStartingAddons())
         {
            Thread.sleep(100);
         }
      }
      catch (Exception e)
      {
         logger.log(Level.SEVERE, "Error occurred.", e);
      }
      finally
      {
         fireBeforeContainerStoppedEvent();
         status = ContainerStatus.STOPPED;
         getLifecycleManager().stopAll();
      }

      fireAfterContainerStoppedEvent();
      cleanup();
   }

   private void cleanup()
   {
      for (ListenerRegistration<ContainerLifecycleListener> registation : loadedListenerRegistrations)
      {
         registation.removeListener();
      }
      registeredListeners.clear();
      lastRepoVersionSeen.clear();
      loader = null;
      manager.dispose();
      manager = null;
      repositories.clear();
      executor.shutdownNow();
   }

   private void fireBeforeConfigurationScanEvent()
   {
      for (ContainerLifecycleListener listener : registeredListeners)
      {
         listener.beforeConfigurationScan(this);
      }
   }

   private void fireAfterConfigurationScanEvent()
   {
      for (ContainerLifecycleListener listener : registeredListeners)
      {
         listener.afterConfigurationScan(this);
      }
   }

   private void fireBeforeContainerStartedEvent()
   {
      for (ContainerLifecycleListener listener : registeredListeners)
      {
         listener.beforeStart(this);
      }
   }

   private void fireBeforeContainerStoppedEvent()
   {
      for (ContainerLifecycleListener listener : registeredListeners)
      {
         listener.beforeStop(this);
      }
   }

   private void fireAfterContainerStoppedEvent()
   {
      for (ContainerLifecycleListener listener : registeredListeners)
      {
         listener.afterStop(this);
      }
   }

   @Override
   public Furnace stop()
   {
      alive = false;
      return this;
   }

   @Override
   public void setArgs(String[] args)
   {
      assertNotAlive();
      this.args = args;
   }

   @Override
   public String[] getArgs()
   {
      return args;
   }

   @Override
   public boolean isServerMode()
   {
      return serverMode;
   }

   @Override
   public Furnace setServerMode(boolean server)
   {
      assertNotAlive();
      this.serverMode = server;
      return this;
   }

   @Override
   public AddonRegistry getAddonRegistry(AddonRepository... repositories)
   {
      assertIsAlive();

      AddonRegistry result = getLifecycleManager().findView(repositories);

      if (result == null)
      {
         if (repositories == null || repositories.length == 0)
         {
            result = new AddonRegistryImpl(lock, getLifecycleManager(), getRepositories(), "ROOT");
            getLifecycleManager().addView(result);
         }
         else
         {
            result = new AddonRegistryImpl(lock, getLifecycleManager(), Arrays.asList(repositories),
                     String.valueOf(registryCount++));
            getLifecycleManager().addView(result);
            getLifecycleManager().forceUpdate();
         }
      }

      return result;
   }

   public void disposeAddonView(AddonView view)
   {
      assertIsAlive();

      if (getAddonRegistry().equals(view))
         throw new IllegalArgumentException(
                  "Cannot dispose the root AddonRegistry. Call .stop() instead.");

      getLifecycleManager().removeView(view);
      getLifecycleManager().forceUpdate();
   }

   @Override
   public Version getVersion()
   {
      return AddonRepositoryImpl.getRuntimeAPIVersion();
   }

   @Override
   public ListenerRegistration<ContainerLifecycleListener> addContainerLifecycleListener(
            final ContainerLifecycleListener listener)
   {
      registeredListeners.add(listener);
      return new ListenerRegistration<ContainerLifecycleListener>()
      {
         @Override
         public ContainerLifecycleListener removeListener()
         {
            registeredListeners.remove(listener);
            return listener;
         }
      };
   }

   @Override
   public List<AddonRepository> getRepositories()
   {
      return Collections.unmodifiableList(repositories);
   }

   @Override
   public AddonRepository addRepository(AddonRepositoryMode mode, File directory)
   {
      Assert.notNull(mode, "Addon repository mode must not be null.");
      Assert.notNull(directory, "Addon repository directory must not be null.");

      AddonRepository repository = AddonRepositoryImpl.forDirectory(this, directory);

      if (mode.isImmutable())
         repository = new ImmutableAddonRepository(repository);
      else
      {
         try
         {
            if (watcher != null)
            {
               if ((directory.exists() && directory.isDirectory()) || directory.mkdirs())
               {
                  directory.toPath().register(watcher,
                           StandardWatchEventKinds.ENTRY_MODIFY,
                           StandardWatchEventKinds.ENTRY_CREATE,
                           StandardWatchEventKinds.ENTRY_DELETE,
                           StandardWatchEventKinds.OVERFLOW);
                  logger.log(Level.INFO, "Monitoring repository [" + directory.toString() + "] for file changes.");
               }
               else
               {
                  logger.log(Level.WARNING, "Cannot monitor repository [" + directory
                           + "] for changes because it is not a directory.");
               }
            }
         }
         catch (IOException e)
         {
            logger.log(Level.WARNING, "Could not monitor repository [" + directory.toString() + "] for file changes.",
                     e);
         }
      }
      return addRepository(repository);
   }

   @Override
   public AddonRepository addRepository(AddonRepository repository)
   {
      Assert.notNull(repository, "Addon repository must not be null.");

      for (AddonRepository registeredRepo : repositories)
      {
         if (registeredRepo.getRootDirectory().equals(repository.getRootDirectory()))
         {
            return registeredRepo;
         }
      }

      this.repositories.add(repository);
      lastRepoVersionSeen.put(repository, 0);

      return repository;
   }

   public void assertIsAlive()
   {
      if (!alive)
         throw new IllegalStateException(
                  "Cannot access this method until Furnace is running. Call .start() or .startAsync() first.");
   }

   public void assertNotAlive()
   {
      if (alive)
         throw new IllegalStateException("Cannot modify a running Furnace instance. Call .stop() first.");
   }

   @Override
   public ContainerStatus getStatus()
   {
      if (!alive)
         return ContainerStatus.STOPPED;

      boolean startingAddons = getLifecycleManager().isStartingAddons();
      return startingAddons ? ContainerStatus.STARTING : status;
   }

   public List<ContainerLifecycleListener> getRegisteredListeners()
   {
      return Collections.unmodifiableList(registeredListeners);
   }

   public AddonLifecycleManager getAddonLifecycleManager()
   {
      return getLifecycleManager();
   }

   @Override
   public String toString()
   {
      return getLifecycleManager().toString();
   }

   private AddonLifecycleManager getLifecycleManager()
   {
      if (manager == null)
         manager = new AddonLifecycleManager(this);
      return manager;
   }

   @Override
   public boolean isTestMode()
   {
      return Boolean.getBoolean("org.jboss.forge.furnace.test");
   }
}

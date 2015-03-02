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
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.ContainerStatus;
import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.AddonCompatibilityStrategy;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.addons.AddonView;
import org.jboss.forge.furnace.impl.addons.AddonLifecycleManager;
import org.jboss.forge.furnace.impl.addons.AddonRegistryImpl;
import org.jboss.forge.furnace.impl.addons.AddonRepositoryImpl;
import org.jboss.forge.furnace.impl.addons.ImmutableAddonRepository;
import org.jboss.forge.furnace.impl.lock.LockManagerImpl;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.repositories.AddonRepository;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.spi.ContainerLifecycleListener;
import org.jboss.forge.furnace.spi.ListenerRegistration;
import org.jboss.forge.furnace.util.AddonCompatibilityStrategies;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.Strings;
import org.jboss.forge.furnace.versions.Version;
import org.jboss.modules.Module;
import org.jboss.modules.log.StreamModuleLogger;

/**
 * Implementation for the {@link Furnace} interface
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceImpl implements Furnace
{
   public static final String FURNACE_ADDON_COMPATIBILITY_PROPERTY = "furnace.addons.compatibility";
   public static final String FURNACE_LOGGING_LEAK_CLASSLOADERS_PROPERTY = "furnace.logging.leak";
   public static final String FURNACE_DEBUG_PROPERTY = "furnace.debug";
   public static final String TEST_MODE_PROPERTY = "furnace.test.mode";

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
   boolean firedAfterStart = false;

   private WatchService watcher;
   private AddonCompatibilityStrategy addonCompatibilityStrategy = AddonCompatibilityStrategies.STRICT;

   public FurnaceImpl()
   {
      if (!AddonRepositoryImpl.hasRuntimeAPIVersion())
      {
         logger.warning("Could not detect Furnace runtime version - " +
                  "loading all addons, but failures may occur if versions are not compatible.");
      }

      String addonCompatibilityValue = System.getProperty(FURNACE_ADDON_COMPATIBILITY_PROPERTY);
      if (!Strings.isNullOrEmpty(addonCompatibilityValue))
      {
         AddonCompatibilityStrategy strategy = null;
         try
         {
            strategy = AddonCompatibilityStrategies.valueOf(addonCompatibilityValue);
         }
         catch (IllegalArgumentException iae)
         {
            // It's not an enum value, must be a fully qualified class name
            try
            {
               strategy = (AddonCompatibilityStrategy) Class.forName(addonCompatibilityValue).newInstance();
            }
            catch (Exception e)
            {
               logger.log(Level.SEVERE, "Error while loading class " + addonCompatibilityValue, e);
            }
         }
         if (strategy == null)
         {
            logger.warning("'" + addonCompatibilityValue + "' is not a valid value for the '"
                     + FURNACE_ADDON_COMPATIBILITY_PROPERTY + "' property. Possible values are: "
                     + Arrays.toString(AddonCompatibilityStrategies.values())
                     + " or a fully qualified class name. Assuming default value.");
         }
         else
         {
            setAddonCompatibilityStrategy(strategy);
         }
      }

      if (!Boolean.getBoolean(FURNACE_LOGGING_LEAK_CLASSLOADERS_PROPERTY))
      {
         /*
          * If enabled, allows the JDK java.util.logging.Level to leak ClassLoaders (memory leak).
          */
         LoggingRepair.init();
      }

      if (Boolean.getBoolean(FURNACE_DEBUG_PROPERTY))
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
      logger.log(Level.INFO, "Furnace [" + getVersion() + "] starting.");
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
            lock.performLocked(LockMode.WRITE, new Callable<Void>()
            {
               @Override
               public Void call() throws Exception
               {
                  boolean dirty = false;
                  if (!getLifecycleManager().isStartingAddons())
                  {
                     for (AddonRepository repository : repositories)
                     {
                        int repoVersion = repository.getVersion();
                        if (repoVersion > lastRepoVersionSeen.get(repository))
                        {
                           logger.log(Level.FINE, "Detected changes in repository [" + repository + "].");
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
                           logger.log(Level.FINE, "Detected changes in repository ["
                                    + events.iterator().next().context()
                                    + "].");
                           dirty = true;
                        }
                        key.reset();
                        key = watcher.poll();
                     }

                     if (dirty)
                     {
                        reloadConfiguration();
                     }
                  }

                  status = ContainerStatus.STARTED;
                  if (!firedAfterStart)
                  {
                     fireAfterContainerStartedEvent();
                     firedAfterStart = true;
                  }
                  return null;
               }
            });
            Thread.sleep(100);
         }
         while (isAlive() && serverMode);

         while (isAlive() && getLifecycleManager().isStartingAddons())
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
   public AddonRegistry getAddonRegistry(final AddonRepository... repositories)
   {
      assertIsAlive();

      AddonRegistry result = getLifecycleManager().findView(repositories);

      if (result == null)
      {
         result = lock.performLocked(LockMode.WRITE, new Callable<AddonRegistry>()
         {
            @Override
            public AddonRegistry call() throws Exception
            {
               AddonRegistry registry = getLifecycleManager().findView(repositories);
               if (registry == null)
               {
                  if (repositories == null || repositories.length == 0)
                  {
                     String name = "ROOT" + "_" + UUID.randomUUID().toString();
                     registry = new AddonRegistryImpl(lock, getLifecycleManager(), getRepositories(), name);
                  }
                  else
                  {
                     String name = String.valueOf(registryCount++ + "_" + UUID.randomUUID().toString());
                     registry = new AddonRegistryImpl(lock, getLifecycleManager(), Arrays.asList(repositories), name);
                  }
                  getLifecycleManager().addView(registry);
                  getLifecycleManager().forceUpdate();
               }
               return registry;
            }
         });
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
                  logger.log(Level.FINE, "Monitoring repository [" + directory.toString() + "] for file changes.");
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
   public AddonRepository addRepository(final AddonRepository repository)
   {
      Assert.notNull(repository, "Addon repository must not be null.");

      for (AddonRepository registeredRepo : repositories)
      {
         if (registeredRepo.getRootDirectory().equals(repository.getRootDirectory()))
         {
            return registeredRepo;
         }
      }

      lock.performLocked(LockMode.WRITE, new Callable<Void>()
      {
         @Override
         public Void call() throws Exception
         {
            /*
             * The existing ROOT view must be updated *before*
             */
            if (isAlive())
            {
               AddonRegistry registry = getAddonRegistry();
               ((AddonRegistryImpl) registry).addRepository(repository);
            }
            lastRepoVersionSeen.put(repository, 0);
            repositories.add(repository);
            return null;
         }
      });

      return repository;
   }

   public void assertIsAlive()
   {
      if (!isAlive())
         throw new IllegalStateException(
                  "Cannot access this method until Furnace is running. Call .start() or .startAsync() first.");
   }

   public void assertNotAlive()
   {
      if (isAlive())
         throw new IllegalStateException("Cannot modify a running Furnace instance. Call .stop() first.");
   }

   @Override
   public ContainerStatus getStatus()
   {
      return lock.performLocked(LockMode.READ, new Callable<ContainerStatus>()
      {
         @Override
         public ContainerStatus call() throws Exception
         {
            if (!isAlive())
               return ContainerStatus.STOPPED;

            boolean startingAddons = getLifecycleManager().isStartingAddons();
            return startingAddons ? ContainerStatus.STARTING : status;
         }
      });
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

   @Override
   public boolean isTestMode()
   {
      return Boolean.getBoolean(TEST_MODE_PROPERTY);
   }

   @Override
   public void setAddonCompatibilityStrategy(final AddonCompatibilityStrategy strategy)
   {
      Assert.notNull(strategy, "AddonCompatibilityStrategy cannot be null");
      if (isAlive())
      {
         lock.performLocked(LockMode.WRITE, new Callable<Void>()
         {
            @Override
            public Void call() throws Exception
            {
               FurnaceImpl.this.addonCompatibilityStrategy = strategy;
               reloadConfiguration();
               return null;
            }
         });
      }
      else
      {
         FurnaceImpl.this.addonCompatibilityStrategy = strategy;
      }
   }

   @Override
   public AddonCompatibilityStrategy getAddonCompatibilityStrategy()
   {
      return this.addonCompatibilityStrategy;
   }

   /*
    * Internal methods.
    */
   private AddonLifecycleManager getLifecycleManager()
   {
      if (manager == null)
         manager = new AddonLifecycleManager(this);
      return manager;
   }

   private boolean isAlive()
   {
      return alive;
   }

   private void reloadConfiguration()
   {
      if (status.isStarted())
         status = ContainerStatus.RELOADING;

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

      if (status.isReloading())
         status = ContainerStatus.STARTED;
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
      firedAfterStart = false;
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

   private void fireAfterContainerStartedEvent()
   {
      for (ContainerLifecycleListener listener : registeredListeners)
      {
         listener.afterStart(this);
      }
   }

   private void fireAfterContainerStoppedEvent()
   {
      for (ContainerLifecycleListener listener : registeredListeners)
      {
         listener.afterStop(this);
      }
   }
}

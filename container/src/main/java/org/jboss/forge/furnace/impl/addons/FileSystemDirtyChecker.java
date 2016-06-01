/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File system based dirty checker.
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
public class FileSystemDirtyChecker extends LazyDirtyChecker
{
    private static Logger logger = Logger.getLogger(FileSystemDirtyChecker.class.getName());

    private final File directory;

    private WatchService watcher;

    public FileSystemDirtyChecker(File directory)
    {
        this.directory = directory;
    }

    @Override
    protected void init()
    {
        try
        {
            watcher = FileSystems.getDefault().newWatchService();
        }
        catch (IOException e)
        {
            logger.log(Level.WARNING, "File monitoring could not be started.", e);
            return;
        }

        try
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
        catch (IOException e)
        {
            logger.log(Level.WARNING, "Could not monitor repository [" + directory.toString() + "] for file changes.",
                    e);
        }
    }

    @Override
    protected boolean isDirtyInternal()
    {
        if (watcher == null)
        {
            return false;
        }

        boolean dirty = false;
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
            key = watcher.poll();
        }

        return dirty;
    }

    @Override
    public void close() throws Exception
    {
        if (watcher != null)
        {
            watcher.close();
        }
    }
}

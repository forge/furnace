/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.util.Assert;

import java.io.File;
import java.util.concurrent.Callable;

/**
 *
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public abstract class AbstractFileSystemAddonRepository
{

    protected final LockManager lock;

    protected final File addonDir;

    public AbstractFileSystemAddonRepository(LockManager lock, File addonDir)
    {
        Assert.notNull(lock, "LockManager must not be null.");
        Assert.notNull(addonDir, "Addon directory must not be null.");
        this.lock = lock;
        this.addonDir = addonDir;
    }

    protected File getRootDirectory()
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
        AddonRepositoryStateStrategyImpl other = (AddonRepositoryStateStrategyImpl) obj;
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

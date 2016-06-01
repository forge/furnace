/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.function.Supplier;

/**
 * Version-based dirty checker.
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
public class VersionDirtyChecker implements DirtyChecker
{

    private final Supplier<Integer> versionSupplier;

    private int lastRepoVersionSeen = 0;

    public VersionDirtyChecker(Supplier<Integer> versionSupplier)
    {
        this.versionSupplier = versionSupplier;
    }

    @Override
    public boolean isDirty()
    {
        return versionSupplier.get() > lastRepoVersionSeen;
    }

    @Override
    public void resetDirtyStatus()
    {
        lastRepoVersionSeen = versionSupplier.get();
    }
}

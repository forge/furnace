/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

/**
 * Composite dirty checker to check other dirty checkers.
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
public class CompositeDirtyChecker implements DirtyChecker
{

    private final DirtyChecker[] dirtyCheckers;

    public CompositeDirtyChecker(DirtyChecker... dirtyCheckers)
    {
        this.dirtyCheckers = dirtyCheckers;
    }

    @Override
    public boolean isDirty()
    {
        for (DirtyChecker dirtyChecker : dirtyCheckers)
        {
            if (dirtyChecker.isDirty())
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public void resetDirtyStatus()
    {
        for (DirtyChecker dirtyChecker : dirtyCheckers)
        {
            dirtyChecker.resetDirtyStatus();
        }
    }

    @Override
    public void close() throws Exception
    {
        for (DirtyChecker dirtyChecker : dirtyCheckers)
        {
            dirtyChecker.close();
        }
    }
}

/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dirty checker with lazy initialization.
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
public abstract class LazyDirtyChecker implements DirtyChecker
{

    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    abstract protected void init();

    abstract protected boolean isDirtyInternal();

    @Override
    public final boolean isDirty()
    {
        if (initialized.compareAndSet(false, true))
            init();

        return isDirtyInternal();
    }
}

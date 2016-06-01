/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

/**
 * Used to check dirtiness.
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
public interface DirtyChecker extends AutoCloseable
{

    boolean isDirty();

    default void resetDirtyStatus()
    {
    }

    @Override
    default void close() throws Exception
    {
    }
}

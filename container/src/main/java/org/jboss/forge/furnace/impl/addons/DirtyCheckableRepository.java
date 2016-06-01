/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

/**
 * Used to allow dirty checks on repositories.
 *
 * @author <a href="mailto:bsideup@gmail.com">Sergei Egorov</a>
 */
public interface DirtyCheckableRepository
{
    DirtyChecker createDirtyChecker();
}
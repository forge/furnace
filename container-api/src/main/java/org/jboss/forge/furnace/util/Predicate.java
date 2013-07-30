/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.util;

/**
 * Determines a true or false value for a given input.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * @param <INPUTTYPE>
 */
public interface Predicate<INPUTTYPE>
{
   /**
    * Return <code>true</code> if the given value should be accepted; otherwise return <code>false</code>.
    */
   boolean accept(INPUTTYPE type);
}

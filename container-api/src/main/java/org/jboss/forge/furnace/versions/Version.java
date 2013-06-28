/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.versions;

import org.jboss.forge.furnace.addons.Addon;

/**
 * Represents an {@link Addon} version.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface Version extends Comparable<Version>
{
   /**
    * Get the major version component.
    * <p>
    * <code>Major.minor.micro-qualifier-buildnumber</code>
    */
   int getMajorVersion();

   /**
    * Get the minor version component.
    * <p>
    * <code>Major.minor.micro-qualifier-buildnumber</code>
    */
   int getMinorVersion();

   /**
    * Get the micro version component.
    * <p>
    * <code>Major.minor.micro-qualifier-buildnumber</code>
    */
   int getIncrementalVersion();

   /**
    * Get the build number version component.
    * <p>
    * <code>Major.minor.micro-qualifier-buildnumber</code>
    */
   int getBuildNumber();

   /**
    * Get the qualifier version component.
    * <p>
    * <code>Major.minor.micro-qualifier-buildnumber</code>
    */
   String getQualifier();
}

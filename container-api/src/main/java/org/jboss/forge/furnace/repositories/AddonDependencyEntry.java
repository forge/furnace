/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.repositories;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.versions.VersionRange;
import org.jboss.forge.furnace.versions.Versions;

/**
 * Represents an {@link Addon} dependency as specified in its originating {@link AddonRepository}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonDependencyEntry
{
   private String name;
   private VersionRange version;
   private boolean exported;
   private boolean optional;

   /**
    * Return <code>true</code> if this dependency is optional.
    */
   public boolean isOptional()
   {
      return optional;
   }

   /**
    * Return <code>true</code> if this dependency is exported.
    */
   public boolean isExported()
   {
      return exported;
   }

   /**
    * Get the dependency name.
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get the dependency {@link VersionRange}.
    */
   public VersionRange getVersionRange()
   {
      return version;
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, String versionRange)
   {
      return create(name, Versions.parseMultipleVersionRange(versionRange), false, false);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, VersionRange range)
   {
      return create(name, range, false, false);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, String versionRange, boolean exported)
   {
      return create(name, Versions.parseMultipleVersionRange(versionRange), exported, false);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, VersionRange range, boolean exported)
   {
      return create(name, range, exported, false);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, String versionRange, boolean exported, boolean optional)
   {
      return create(name, Versions.parseMultipleVersionRange(versionRange), exported, optional);
   }

   /**
    * Create a new {@link AddonDependencyEntry} with the given attributes.
    */
   public static AddonDependencyEntry create(String name, VersionRange range, boolean exported, boolean optional)
   {
      Assert.notNull(name, "Addon name must not be null.");
      Assert.notNull(range, "Addon version must not be null.");

      AddonDependencyEntry entry = new AddonDependencyEntry();
      entry.name = name;
      entry.version = range;
      entry.exported = exported;
      entry.optional = optional;
      return entry;
   }

   @Override
   public String toString()
   {
      return "name=" + name + ", version=" + version + ", exported=" + exported + ", optional="
               + optional;
   }

}

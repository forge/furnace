/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.addons;


/**
 * An edge in the registered {@link Addon} graph.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonDependency
{
   /**
    * Get the {@link Addon} dependency of the {@link Addon} returned by {@link #getDependent()}.
    */
   public Addon getDependency();

   /**
    * Return <code>true</code> if the {@link Addon} dependency returned by {@link #getDependency()} is exported by the
    * dependent {@link Addon}. If the dependency is not exported, return <code>false</code>.
    */
   public boolean isExported();

   /**
    * Return <code>true</code> if the {@link Addon} dependency returned by {@link #getDependency()} is an optional
    * dependency of the dependent {@link Addon}. If the dependency is not optional, return <code>false</code>.
    */
   public boolean isOptional();

}
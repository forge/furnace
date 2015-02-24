/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;

/**
 * Specifies an {@link Addon} that must be installed before this deployment may be performed. (Automatically adds this
 * dependency to the archive via
 * {@link AddonArchive#addAsAddonDependencies(org.jboss.forge.furnace.repositories.AddonDependencyEntry...)}).
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AddonDependency
{
   /**
    * Set the {@link AddonId} for this {@link Addon} deployment.
    */
   String name();

   /**
    * If version is empty, resolve to the version specified in the pom.xml of the project being tested
    */
   String version() default "";

   /**
    * Whether or not the specified {@link AddonId} should be imported as a dependency to the current {@link Deployment}.
    * (Default <code>true</code>.)
    */
   boolean imported() default true;

   /**
    * Whether or not the specified {@link AddonId} should be exported to dependencies of the current {@link Deployment}.
    * (Default <code>false</code>.)
    */
   boolean exported() default false;

   /**
    * Whether or not the specified {@link AddonId} should be marked as an optional dependency of the current
    * {@link Deployment}. (Default <code>false</code>.)
    */
   boolean optional() default false;

   /**
    * Set the {@link DeploymentListener} for this {@link AddonDependency}.
    */
   Class<? extends DeploymentListener> listener() default DeploymentListener.class;
}

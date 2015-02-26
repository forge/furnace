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

/**
 * Contains multiple {@link AddonDependency} annotations that must be deployed before this {@link Deployment} may be
 * performed. (Automatically adds this dependency to the archive via
 * {@link AddonArchive#addAsAddonDependencies(org.jboss.forge.furnace.repositories.AddonDependencyEntry...)}).
 * 
 * @deprecated Use {@link AddonDependencies} or {@link AddonDeployments} instead.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface Dependencies
{
   /**
    * The {@link AddonDependency} annotations.
    */
   AddonDependency[] value();
}

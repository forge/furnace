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
import org.jboss.forge.arquillian.spi.AddonServiceRegistrationStrategy;

/**
 * Deploy this addon as a service.
 * 
 * It basically declares a strategy for deploying this class as a service when no {@link Deployment} methods are
 * defined.
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeployUsing
{
   /**
    * The strategy to use. Valid values are <code>cdi</code>,<code>simple</code>,<code>local</code> or a fully-qualified
    * class name that implements the {@link AddonServiceRegistrationStrategy} interface
    */
   String value();
}

/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.arquillian.spi;

import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.forge.arquillian.archive.AddonArchive;

/**
 * Defines a strategy to register the {@link TestClass} in the specified {@link AddonArchive}
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface TestClassRegistrationStrategy
{
   /**
    * Returns <code>true</code> if the {@link TestClassRegistrationStrategy#register(TestClass, AddonArchive)} can be
    * invoked
    * 
    * @param testClass
    * @param addonArchive
    * @return <code>true</code> if this {@link TestClassRegistrationStrategy} can register this specific
    *         {@link TestClass} in the provided {@link AddonArchive}
    */
   boolean handles(TestClass testClass, AddonArchive addonArchive);

   /**
    * Registers the given {@link TestClass#getJavaClass()} as a service in the given {@link AddonArchive}
    * 
    * @param testClass the class under test
    * @param addonArchive the {@link AddonArchive} to be deployed
    */
   void register(TestClass testClass, AddonArchive addonArchive);
}

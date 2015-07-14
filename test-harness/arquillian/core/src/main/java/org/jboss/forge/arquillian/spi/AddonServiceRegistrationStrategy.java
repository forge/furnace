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
public interface AddonServiceRegistrationStrategy
{
   void registerAsService(TestClass testClass, AddonArchive addonArchive);

}

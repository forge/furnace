/**
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.arquillian.impl;

import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.spi.AddonServiceRegistrationStrategy;

/**
 * Existing strategies for service registration
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
enum AddonServiceRegistrationStrategies implements AddonServiceRegistrationStrategy
{
   CDI
   {
      @Override
      public void registerAsService(TestClass testClass, AddonArchive addonArchive)
      {
         addonArchive.addBeansXML();
      }
   },
   SIMPLE
   {
      @Override
      public void registerAsService(TestClass testClass, AddonArchive addonArchive)
      {
         addonArchive.addAsServiceProvider("org.jboss.forge.furnace.container.simple.Service",
                  testClass.getJavaClass().getName());
      }
   },
   LOCAL
   {
      @Override
      public void registerAsService(TestClass testClass, AddonArchive addonArchive)
      {
         addonArchive.addAsLocalServices(testClass.getJavaClass());
      }
   };

   /**
    * Creates an {@link AddonServiceRegistrationStrategy}
    * 
    * @throws IllegalArgumentException if the value is not a valid {@link AddonServiceRegistrationStrategy}
    */
   static AddonServiceRegistrationStrategy create(String deployStrategyName) throws IllegalArgumentException
   {
      AddonServiceRegistrationStrategy strategy;
      try
      {
         strategy = valueOf(deployStrategyName.toUpperCase());
      }
      catch (IllegalArgumentException iae)
      {
         try
         {
            strategy = (AddonServiceRegistrationStrategy) Class.forName(deployStrategyName).newInstance();
         }
         catch (Exception e)
         {
            throw new IllegalArgumentException("Illegal argument: " + deployStrategyName, e);
         }
      }
      return strategy;
   }
}

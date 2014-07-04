/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.collections;

import java.util.UUID;

public class ProfileFactory
{
   public Profile createProfile()
   {
      return new Profile(UUID.randomUUID().toString());
   }

   public ProfileManager createProfileManager()
   {
      return new ProfileManagerImpl();
   }
}

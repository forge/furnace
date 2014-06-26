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

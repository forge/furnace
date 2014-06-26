/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.forge.furnace.proxy.Proxies;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class ProfileManagerImpl implements ProfileManager
{
   @Override
   public Map<String, Profile> getProfiles()
   {
      Map<String, Profile> map = new HashMap<>();
      map.put("demo", new Profile("demo"));
      return map;
   }

   @Override
   public void setProfiles(Collection<Profile> profiles)
   {
      for (Profile profile : profiles)
      {
         if (profile.getName() == null)
            throw new RuntimeException("FAIL");
      }
   }

   @Override
   public void setProfileListCallGet(List<Profile> profiles)
   {
      Profile p = profiles.get(0);
      if (Proxies.isForgeProxy(p))
         throw new RuntimeException("[" + p + "] should not have been a proxy");
   }

}

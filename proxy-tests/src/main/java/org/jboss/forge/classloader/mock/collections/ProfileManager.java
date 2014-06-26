/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.classloader.mock.collections;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public interface ProfileManager
{
   Map<String, Profile> getProfiles();

   void setProfiles(Collection<Profile> profiles);

   void setProfileListCallGet(List<Profile> profiles);
}

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.forge.arquillian.archive.impl;

import org.jboss.forge.arquillian.DeploymentListener;
import org.jboss.forge.arquillian.NullDeploymentListener;
import org.jboss.forge.arquillian.archive.AddonDeploymentArchive;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.impl.base.container.ContainerBase;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class AddonDeploymentArchiveImpl extends ContainerBase<AddonDeploymentArchive> implements AddonDeploymentArchive
{
   private AddonId id;
   private String repository;
   private DeploymentListener listener;

   @Override
   public AddonId getAddonId()
   {
      return id;
   }

   @Override
   public AddonDeploymentArchive setAddonId(AddonId id)
   {
      this.id = id;
      return this;
   }

   @Override
   public String getAddonRepository()
   {
      return repository;
   }

   @Override
   public AddonDeploymentArchive setAddonRepository(String repository)
   {
      this.repository = repository;
      return this;
   }

   public AddonDeploymentArchiveImpl(Archive<?> archive)
   {
      super(AddonDeploymentArchive.class, archive);
   }

   @Override
   protected ArchivePath getClassesPath()
   {
      throw new UnsupportedOperationException("Placeholder Archive Type");
   }

   @Override
   protected ArchivePath getLibraryPath()
   {
      throw new UnsupportedOperationException("Placeholder Archive Type");
   }

   @Override
   protected ArchivePath getManifestPath()
   {
      throw new UnsupportedOperationException("Placeholder Archive Type");
   }

   @Override
   protected ArchivePath getResourcePath()
   {
      throw new UnsupportedOperationException("Placeholder Archive Type");
   }

   @Override
   public DeploymentListener getDeploymentListener()
   {
      if (listener == null)
      {
         return NullDeploymentListener.INSTANCE;
      }
      return listener;
   }

   @Override
   public void setDeploymentListener(DeploymentListener listener)
   {
      this.listener = listener;
   }
}

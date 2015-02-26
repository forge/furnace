/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.forge.arquillian.AddonDeployment;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.archive.AddonArchiveBase;
import org.jboss.forge.arquillian.archive.AddonDependencyAware;
import org.jboss.forge.arquillian.archive.AddonDeploymentArchive;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.arquillian.archive.RepositoryLocationAware;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;
import org.jboss.shrinkwrap.api.Node;

@SuppressWarnings("deprecation")
public class FurnaceDeploymentPackager implements DeploymentPackager
{

   private Pattern SHRINKWRAP_DESCRIPTOR_PATTERN = Pattern.compile("/org/jboss/shrinkwrap/descriptor/.*");

   @Override
   public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors)
   {
      Archive<?> applicationArchive = testDeployment.getApplicationArchive();

      if (applicationArchive instanceof AddonArchiveBase<?>)
      {
         AddonArchiveBase<?> deployment = AddonArchiveBase.class.cast(applicationArchive);

         Collection<Archive<?>> auxiliaryArchives = testDeployment.getAuxiliaryArchives();
         for (Archive<?> archive : auxiliaryArchives)
         {
            Map<ArchivePath, Node> shrinkwrapDescriptorsTypes = archive.getContent(new Filter<ArchivePath>()
            {
               @Override
               public boolean include(ArchivePath path)
               {
                  return SHRINKWRAP_DESCRIPTOR_PATTERN.matcher(path.get()).matches();
               }
            });

            /*
             * Remove the ShrinkWrap Descriptors types to avoid conflicts.
             */
            TreeSet<ArchivePath> toRemove = new TreeSet<ArchivePath>(Collections.reverseOrder());
            toRemove.addAll(shrinkwrapDescriptorsTypes.keySet());
            for (ArchivePath path : toRemove)
            {
               archive.delete(path);
            }

            deployment.addAsLibrary(archive);
         }

         /*
          * Ensure that all test harness classes are available in the addon under test.
          */
         deployment.addClasses(ForgeArchive.class, AddonArchive.class, AddonArchiveBase.class,
                  RepositoryLocationAware.class, AddonDependencyAware.class);
         return deployment;
      }
      else if (applicationArchive instanceof AddonDeploymentArchive)
      {
         return applicationArchive;
      }
      else
      {
         throw new IllegalArgumentException(
                  "Invalid Archive type. Ensure that your @Deployment method returns type 'AddonArchive'.");
      }
   }
}

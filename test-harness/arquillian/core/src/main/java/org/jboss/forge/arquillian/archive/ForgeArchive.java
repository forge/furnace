/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.archive;

import org.jboss.shrinkwrap.api.asset.Asset;

/**
 * Archive representing a Furnace AddonDependency deployment.
 * 
 * @deprecated As of Furnace 2.15.0.Final, please use {@link AddonArchive}.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@Deprecated
public interface ForgeArchive extends AddonArchiveBase<ForgeArchive>
{
   /**
    * Sets the current forge.xml descritor for this archive.
    */
   ForgeArchive setAsForgeXML(Asset resource) throws IllegalArgumentException;

   /**
    * Adds an empty beans.xml file in this archive
    */
   ForgeArchive addBeansXML();

   /**
    * Adds an beans.xml file in this archive with the specified content
    */
   ForgeArchive addBeansXML(Asset resource);

   /**
    * Add a basic service container, using the given service types as services in the deployment.
    * <p>
    * <b>WARNING: </b> Cannot be combined with other service containers.
    */
   ForgeArchive addAsLocalServices(Class<?>... serviceTypes);
}

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
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface AddonArchive extends AddonArchiveBase<AddonArchive>
{
   /**
    * Adds an empty beans.xml file in this archive (Requires http://github.com/forge/furnace-cdi).
    */
   AddonArchive addBeansXML();

   /**
    * Adds an beans.xml file in this archive with the specified content (Requires http://github.com/forge/furnace-cdi).
    */
   AddonArchive addBeansXML(Asset resource);

   /**
    * Add a basic service container, using the given service types as services in the deployment.
    * <p>
    * <b>WARNING: </b> Cannot be combined with other service containers.
    */
   AddonArchive addAsLocalServices(Class<?>... serviceTypes);
}

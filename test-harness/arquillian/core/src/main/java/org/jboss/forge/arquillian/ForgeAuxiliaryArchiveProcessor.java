/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;

public class ForgeAuxiliaryArchiveProcessor implements AuxiliaryArchiveProcessor
{
   @Override
   public void process(Archive<?> archive)
   {
      if ("arquillian-core.jar".equals(archive.getName()))
      {
         Node node = archive.get("org/jboss/shrinkwrap/descriptor");
         if (node != null)
            archive.delete(node.getPath());
      }
   }
}

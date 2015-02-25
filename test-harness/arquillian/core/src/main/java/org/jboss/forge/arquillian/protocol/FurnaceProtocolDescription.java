/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.protocol;

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class FurnaceProtocolDescription extends ProtocolDescription
{
   public FurnaceProtocolDescription()
   {
      super(FurnaceProtocol.NAME);
   }
}

/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.graph;

import org.jgrapht.ext.VertexNameProvider;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class AddonVertexNameProvider implements VertexNameProvider<AddonVertex>
{

   @Override
   public String getVertexName(AddonVertex vertex)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(vertex.toString().replaceAll("org.jboss.forge\\.?", ""));
      return builder.toString();
   }

}

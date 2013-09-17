/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.graph;

import org.jgrapht.ext.EdgeNameProvider;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class AddonDependencyEdgeNameProvider implements EdgeNameProvider<AddonDependencyEdge>
{

   @Override
   public String getEdgeName(AddonDependencyEdge edge)
   {
      StringBuilder builder = new StringBuilder();
      builder.append(edge.isOptional() ? "O" : "");
      builder.append(edge.isExported() ? "E" : "");
      return builder.toString();
   }

}

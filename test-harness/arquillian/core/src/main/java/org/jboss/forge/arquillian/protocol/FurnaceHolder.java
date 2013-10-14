/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.protocol;

import org.jboss.forge.arquillian.DeploymentStrategyType;
import org.jboss.forge.furnace.Furnace;

/**
 * Holds a {@link Furnace} instance so that the test executor may the current runtime.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class FurnaceHolder
{
   private Furnace furnace;
   private DeploymentStrategyType strategy;

   public Furnace getFurnace()
   {
      return furnace;
   }

   public void setFurnace(Furnace furnace)
   {
      this.furnace = furnace;
   }

   public DeploymentStrategyType getDeploymentStrategy()
   {
      return strategy;
   }

   public void setDeploymentStrategy(DeploymentStrategyType strategy)
   {
      this.strategy = strategy;
   }

}

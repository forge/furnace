package org.jboss.forge.arquillian.impl;

import org.jboss.forge.arquillian.DeploymentListener;

/**
 * No-op implementation of {@link DeploymentListener}.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public final class NullDeploymentListener implements DeploymentListener
{
   /**
    * Singleton instance of {@link NullDeploymentListener}.
    */
   public static final DeploymentListener INSTANCE = new NullDeploymentListener();

   private NullDeploymentListener()
   {
   }

   @Override
   public void preUndeploy() throws Exception
   {
   }

   @Override
   public void preDeploy() throws Exception
   {
   }

   @Override
   public void postUndeploy() throws Exception
   {
   }

   @Override
   public void postDeploy() throws Exception
   {
   }
}
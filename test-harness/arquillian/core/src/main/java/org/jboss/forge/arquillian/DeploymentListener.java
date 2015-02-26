package org.jboss.forge.arquillian;

import org.jboss.forge.furnace.Furnace;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Listener for the deployment lifecycle of an {@link AddonDeployment}.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
public interface DeploymentListener
{
   /**
    * Called immediately before the given {@link Archive} is deployed.
    */
   void preDeploy(Furnace furnace, Archive<?> archive) throws Exception;

   /**
    * Called immediately after the given {@link Archive} is deployed.
    */
   void postDeploy(Furnace furnace, Archive<?> archive) throws Exception;

   /**
    * Called immediately before the given {@link Archive} is undeployed.
    */
   void preUndeploy(Furnace furnace, Archive<?> archive) throws Exception;

   /**
    * Called immediately after the given {@link Archive} is undeployed.
    */
   void postUndeploy(Furnace furnace, Archive<?> archive) throws Exception;
}

package org.jboss.forge.arquillian;

/**
 * Listener for the deployment lifecycle of an {@link AddonDeployment}.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 *
 */
public interface DeploymentListener
{
   void preDeploy() throws Exception;

   void postDeploy() throws Exception;

   void preUndeploy() throws Exception;

   void postUndeploy() throws Exception;
}

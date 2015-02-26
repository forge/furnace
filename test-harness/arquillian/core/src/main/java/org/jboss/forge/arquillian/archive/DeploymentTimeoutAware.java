package org.jboss.forge.arquillian.archive;

import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.forge.arquillian.AddonDeployment;

/**
 * Configures the timeout period in {@link TimeUnit#MILLISECONDS} after which a {@link DeploymentException} should be
 * thrown if the current deployment has not yet finished deploying.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface DeploymentTimeoutAware<ORIGIN>
{
   /**
    * Set the timeout period in {@link TimeUnit#MILLISECONDS} after which a {@link DeploymentException} should be thrown
    * if this {@link AddonDeploymentArchive} has not yet finished deploying.
    */
   ORIGIN setDeploymentTimeoutQuantity(int quantity);

   /**
    * Get the timeout period in {@link TimeUnit#MILLISECONDS} after which a {@link DeploymentException} should be thrown
    * if this {@link AddonDeploymentArchive} has not yet finished deploying.
    */
   int getDeploymentTimeoutQuantity();

   /**
    * Set the {@link TimeUnit} after which a {@link DeploymentException} should be thrown if this
    * {@link AddonDeployment} has not yet finished deploying.
    */
   ORIGIN setDeploymentTimeoutUnit(TimeUnit unit);

   /**
    * Get the {@link TimeUnit} after which a {@link DeploymentException} should be thrown if this
    * {@link AddonDeployment} has not yet finished deploying.
    */
   TimeUnit getDeploymentTimeoutUnit();
}

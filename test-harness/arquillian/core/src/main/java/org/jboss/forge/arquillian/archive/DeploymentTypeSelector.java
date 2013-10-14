package org.jboss.forge.arquillian.archive;

import org.jboss.forge.arquillian.Strategy;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public interface DeploymentTypeSelector
{
   void setDeploymentStrategyType(Strategy strategy);

   Strategy getDeploymentStrategyType();
}

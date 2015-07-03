package org.jboss.forge.arquillian.spi;

import java.util.List;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.forge.arquillian.impl.FurnaceDeploymentScenarioGenerator;

/**
 * An interface used by the {@link FurnaceDeploymentScenarioGenerator} to alter the deployment of the test addons.
 *
 * @author <a href="mailto:mbriskar@gmail.com">Matej Briškár</a>
 */
public interface AddonDeploymentScenarioEnhancer
{
   /**
    * This method will be called for every single furnace test that has this enhancer implementation on classpath.
    * 
    * For example, it is possible to deploy a default addon for each furnace test.
    * 
    * @param testClass the {@link TestClass} with the calculated {@link DeploymentDescription} list
    * @param deployments the deployments before returned in
    *           {@link DeploymentScenarioGenerator#generate(org.jboss.arquillian.test.spi.TestClass)}
    * @return the deployments to be returned in
    *         {@link FurnaceDeploymentScenarioGenerator#generate(org.jboss.arquillian.test.spi.TestClass)}
    */
   List<DeploymentDescription> enhance(TestClass testClass, List<DeploymentDescription> deployments);
}

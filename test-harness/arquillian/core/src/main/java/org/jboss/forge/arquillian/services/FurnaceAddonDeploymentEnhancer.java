package org.jboss.forge.arquillian.services;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;

import java.util.List;

/**
 * An interface used by the {@link java.util.ServiceLoader} to alter the deployment of the test addons.
 * This method will be called for every single furnace test that has this enhancer implementation on classpath.
 * For example, it is possible to deploy default addon for each furnace test.
 *
 * @author <a href="mailto:mbriskar@gmail.com">Matej Briškár</a>
 */
public interface FurnaceAddonDeploymentEnhancer
{
    List<DeploymentDescription> enhanceDeployment(List<DeploymentDescription> deployments);
}

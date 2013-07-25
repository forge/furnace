package org.jboss.forge.arquillian;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.forge.arquillian.archive.ForgeRemoteAddon;
import org.jboss.forge.arquillian.archive.RepositoryForgeArchive;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.util.Annotations;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class ForgeDeploymentScenarioGenerator implements DeploymentScenarioGenerator
{
   @Override
   public List<DeploymentDescription> generate(TestClass testClass)
   {
      List<DeploymentDescription> deployments = new ArrayList<DeploymentDescription>();
      Method[] deploymentMethods = testClass.getMethods(Deployment.class);

      for (Method deploymentMethod : deploymentMethods)
      {
         validate(deploymentMethod);
         if (deploymentMethod.isAnnotationPresent(Dependencies.class))
            deployments.addAll(generateDependencyDeployments(deploymentMethod));
         deployments.add(generateDeployment(deploymentMethod));
      }

      return deployments;
   }

   private Collection<DeploymentDescription> generateDependencyDeployments(Method deploymentMethod)
   {
      Dependencies dependency = deploymentMethod.getAnnotation(Dependencies.class);
      Collection<DeploymentDescription> deployments = new ArrayList<DeploymentDescription>();

      if (dependency.value() != null)
         for (AddonDependency addon : dependency.value())
         {
            AddonId id = AddonId.from(addon.name(), addon.version());
            ForgeRemoteAddon remoteAddon = ShrinkWrap.create(ForgeRemoteAddon.class).setAddonId(id);

            if (Annotations.isAnnotationPresent(deploymentMethod, DeployToRepository.class))
               remoteAddon.setAddonRepository(Annotations.getAnnotation(deploymentMethod, DeployToRepository.class).value());

            DeploymentDescription deploymentDescription = new DeploymentDescription(id.toCoordinates(), remoteAddon);
            deploymentDescription.shouldBeTestable(false);
            deployments.add(deploymentDescription);
         }

      return deployments;
   }

   private void validate(Method deploymentMethod)
   {
      if (!Modifier.isStatic(deploymentMethod.getModifiers()))
      {
         throw new IllegalArgumentException("Method annotated with " + Deployment.class.getName() + " is not static. "
                  + deploymentMethod);
      }
      if (!Archive.class.isAssignableFrom(deploymentMethod.getReturnType())
               && !Descriptor.class.isAssignableFrom(deploymentMethod.getReturnType()))
      {
         throw new IllegalArgumentException(
                  "Method annotated with " + Deployment.class.getName() +
                           " must have return type " + Archive.class.getName() + " or " + Descriptor.class.getName()
                           + ". " + deploymentMethod);
      }
      if (deploymentMethod.getParameterTypes().length != 0)
      {
         throw new IllegalArgumentException("Method annotated with " + Deployment.class.getName()
                  + " can not accept parameters. " + deploymentMethod);
      }
   }

   /**
    * @param deploymentMethod
    * @return
    */
   private DeploymentDescription generateDeployment(Method deploymentMethod)
   {
      TargetDescription target = generateTarget(deploymentMethod);
      ProtocolDescription protocol = generateProtocol(deploymentMethod);

      Deployment deploymentAnnotation = deploymentMethod.getAnnotation(Deployment.class);
      DeploymentDescription description = null;
      if (Archive.class.isAssignableFrom(deploymentMethod.getReturnType()))
      {
         Archive<?> archive = invoke(Archive.class, deploymentMethod);
         if (archive instanceof RepositoryForgeArchive)
         {
            if (Annotations.isAnnotationPresent(deploymentMethod, DeployToRepository.class))
               ((RepositoryForgeArchive) archive).setAddonRepository(Annotations.getAnnotation(deploymentMethod,
                        DeployToRepository.class).value());
         }
         description = new DeploymentDescription(deploymentAnnotation.name(), archive);
         description.shouldBeTestable(deploymentAnnotation.testable());
      }
      else if (Descriptor.class.isAssignableFrom(deploymentMethod.getReturnType()))
      {
         description = new DeploymentDescription(deploymentAnnotation.name(),
                  invoke(Descriptor.class, deploymentMethod));
      }
      description.shouldBeManaged(deploymentAnnotation.managed());
      description.setOrder(deploymentAnnotation.order());

      if (target != null)
      {
         description.setTarget(target);
      }
      if (protocol != null)
      {
         description.setProtocol(protocol);
      }

      if (deploymentMethod.isAnnotationPresent(ShouldThrowException.class))
      {
         description.setExpectedException(deploymentMethod.getAnnotation(ShouldThrowException.class).value());
      }

      return description;
   }

   /**
    * @param deploymentMethod
    * @return
    */
   private TargetDescription generateTarget(Method deploymentMethod)
   {
      if (deploymentMethod.isAnnotationPresent(TargetsContainer.class))
      {
         return new TargetDescription(deploymentMethod.getAnnotation(TargetsContainer.class).value());
      }
      return TargetDescription.DEFAULT;
   }

   /**
    * @param deploymentMethod
    * @return
    */
   private ProtocolDescription generateProtocol(Method deploymentMethod)
   {
      if (deploymentMethod.isAnnotationPresent(OverProtocol.class))
      {
         return new ProtocolDescription(deploymentMethod.getAnnotation(OverProtocol.class).value());
      }
      return ProtocolDescription.DEFAULT;
   }

   /**
    * @param deploymentMethod
    * @return
    */
   private <T> T invoke(Class<T> type, Method deploymentMethod)
   {
      try
      {
         return type.cast(deploymentMethod.invoke(null));
      }
      catch (Exception e)
      {
         throw new RuntimeException("Could not invoke deployment method: " + deploymentMethod, e);
      }
   }
}

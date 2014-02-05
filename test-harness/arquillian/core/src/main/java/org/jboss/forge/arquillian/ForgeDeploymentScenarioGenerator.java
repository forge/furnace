package org.jboss.forge.arquillian;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
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
import org.jboss.forge.arquillian.maven.ProjectHelper;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.util.Annotations;
import org.jboss.forge.furnace.util.Strings;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class ForgeDeploymentScenarioGenerator implements DeploymentScenarioGenerator
{
   Map<String, String> dependencyMap;

   @Override
   public List<DeploymentDescription> generate(TestClass testClass)
   {
      List<DeploymentDescription> deployments = new ArrayList<DeploymentDescription>();
      Method[] deploymentMethods = testClass.getMethods(Deployment.class);
      for (Method deploymentMethod : deploymentMethods)
      {
         validate(deploymentMethod);
         if (deploymentMethod.isAnnotationPresent(Dependencies.class))
            deployments.addAll(generateDependencyDeployments(testClass.getJavaClass(), deploymentMethod));
         deployments.add(generateDeployment(deploymentMethod));
      }

      return deployments;
   }

   private Collection<DeploymentDescription> generateDependencyDeployments(Class<?> classUnderTest,
            Method deploymentMethod)
   {
      Dependencies dependency = deploymentMethod.getAnnotation(Dependencies.class);
      Collection<DeploymentDescription> deployments = new ArrayList<DeploymentDescription>();

      if (dependency.value() != null)
         for (AddonDependency addon : dependency.value())
         {
            String version;
            if (addon.version().isEmpty())
            {
               version = resolveVersionFromPOM(classUnderTest, addon.name());
               if (version == null)
               {
                  throw new IllegalStateException("Could not resolve the version for [" + addon.name()
                           + "]. Either specify the version for this @" + AddonDependency.class.getSimpleName()
                           + " in [" + classUnderTest.getName() + "] or add it to pom.xml located at ["
                           + getPomFileFor(classUnderTest) + "]");
               }
            }
            else
            {
               version = addon.version();
            }
            AddonId id = AddonId.from(addon.name(), version);
            ForgeRemoteAddon remoteAddon = ShrinkWrap.create(ForgeRemoteAddon.class).setAddonId(id);

            if (Annotations.isAnnotationPresent(deploymentMethod, DeployToRepository.class))
               remoteAddon.setAddonRepository(Annotations.getAnnotation(deploymentMethod, DeployToRepository.class)
                        .value());

            DeploymentDescription deploymentDescription = new DeploymentDescription(id.toCoordinates(), remoteAddon);
            deploymentDescription.shouldBeTestable(false);
            deployments.add(deploymentDescription);
         }

      return deployments;
   }

   /**
    * Read the pom.xml of the project containing the class under test
    * 
    * @param classUnderTest
    * @param name
    * @return
    */
   private String resolveVersionFromPOM(Class<?> classUnderTest, String name)
   {
      ProjectHelper projectHelper = new ProjectHelper();
      if (dependencyMap == null)
      {
         dependencyMap = new HashMap<String, String>();
         File pomFile = getPomFileFor(classUnderTest);
         try
         {
            // Needed for single-project addons
            Model model = projectHelper.loadPomFromFile(pomFile);
            String thisAddonName = (model.getGroupId() == null) ? model.getParent().getGroupId() : model.getGroupId()
                     + ":" + model.getArtifactId();
            String thisVersion = model.getVersion();
            dependencyMap.put(thisAddonName, thisVersion);
            List<Dependency> dependencies = projectHelper.resolveDependenciesFromPOM(pomFile);
            for (Dependency dependency : dependencies)
            {
               Artifact artifact = dependency.getArtifact();
               String addonName = artifact.getGroupId() + ":" + artifact.getArtifactId();
               String version = artifact.getBaseVersion();
               dependencyMap.put(addonName, version);
            }
         }
         catch (Exception e)
         {
            // TODO log this instead?
            e.printStackTrace();
         }
      }
      return dependencyMap.get(name);
   }

   private File getPomFileFor(Class<?> classUnderTest)
   {
      URL resource = classUnderTest.getClassLoader().getResource("");
      if (resource == null)
      {
         throw new IllegalStateException("Could not find the pom.xml for class " + classUnderTest.getName());
      }
      String directory = resource.getFile();
      File pomFile = findBuildDescriptor(directory);
      return pomFile;
   }

   private File findBuildDescriptor(String classLocation)
   {
      File pom = null;
      File dir = new File(classLocation);
      while (dir != null)
      {
         File testPom = new File(dir, "pom.xml");
         if (testPom.isFile())
         {
            pom = testPom;
            break;
         }
         dir = dir.getParentFile();
      }
      return pom;
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

      String name = deploymentMethod.getAnnotation(Deployment.class).name();
      try
      {
         if (!Strings.isNullOrEmpty(name) && !"_DEFAULT_".equals(name))
            AddonId.fromCoordinates(name);
      }
      catch (IllegalArgumentException e)
      {
         throw new IllegalArgumentException("@" + Deployment.class.getName()
                  + " requires name in the format \"name,version\", but was \"" + name + "\". ");
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

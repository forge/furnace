/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.arquillian.impl;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.client.deployment.TargetDescription;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OverProtocol;
import org.jboss.arquillian.container.test.api.ShouldThrowException;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.AddonDeployment;
import org.jboss.forge.arquillian.AddonDeployments;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.DeployToRepository;
import org.jboss.forge.arquillian.DeploymentListener;
import org.jboss.forge.arquillian.archive.AddonArchiveBase;
import org.jboss.forge.arquillian.archive.AddonDeploymentArchive;
import org.jboss.forge.arquillian.archive.RepositoryLocationAware;
import org.jboss.forge.arquillian.maven.ProjectHelper;
import org.jboss.forge.arquillian.protocol.FurnaceProtocolDescription;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.Annotations;
import org.jboss.forge.furnace.util.Strings;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * Creates {@link DeploymentDescription} instances from annotated test cases - handles {@link AddonDeployments} and
 * {@link AddonDependencies}.
 * 
 * @author <a href="lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@SuppressWarnings("deprecation")
public class FurnaceDeploymentScenarioGenerator implements DeploymentScenarioGenerator
{
   Map<String, String> dependencyMap;
   Set<AddonDependencyEntry> addonSet;

   @Override
   public List<DeploymentDescription> generate(TestClass testClass)
   {
      List<DeploymentDescription> deployments = new ArrayList<DeploymentDescription>();
      Method[] deploymentMethods = testClass.getMethods(Deployment.class);
      for (Method deploymentMethod : deploymentMethods)
      {
         validate(deploymentMethod);
         DeploymentDescription primaryDeployment = null;
         try
         {
            primaryDeployment = generateDeployment(deploymentMethod);

            if (deploymentMethod.isAnnotationPresent(AddonDeployments.class)
                     || deploymentMethod.isAnnotationPresent(AddonDependencies.class)
                     || deploymentMethod.isAnnotationPresent(Dependencies.class))
            {
               deployments.addAll(generateAnnotatedDeployments(primaryDeployment, testClass.getJavaClass(),
                        deploymentMethod));
            }
         }
         catch (Exception e)
         {
            throw new RuntimeException("Could not generate @Deployment for " + testClass.getName() + "."
                     + deploymentMethod.getName() + "()", e);
         }

         deployments.add(primaryDeployment);
      }

      return deployments;
   }

   private Collection<DeploymentDescription> generateAnnotatedDeployments(DeploymentDescription primaryDeployment,
            Class<?> classUnderTest, Method deploymentMethod)
   {
      Collection<DeploymentDescription> deployments = new ArrayList<DeploymentDescription>();

      Annotation[] annotations = deploymentMethod.getAnnotations();
      for (Annotation annotation : annotations)
      {
         if (annotation instanceof AddonDeployments)
         {
            AddonDeployments addonDeployments = (AddonDeployments) annotation;
            if (addonDeployments.value() != null)
            {
               for (AddonDeployment addonDeployment : addonDeployments.value())
               {
                  createAnnotatedDeployment(primaryDeployment,
                           classUnderTest,
                           deploymentMethod,
                           deployments,
                           addonDeployment.name(),
                           addonDeployment.version(),
                           AddonDeployment.class.getSimpleName(),
                           addonDeployment.imported(),
                           addonDeployment.exported(),
                           addonDeployment.optional(),
                           addonDeployment.listener(),
                           addonDeployment.timeout(),
                           addonDeployment.timeoutUnit(),
                           addonDeployment.shouldThrowException());
               }
            }
         }
         else if (annotation instanceof AddonDependencies)
         {
            AddonDependencies addonDependencies = (AddonDependencies) annotation;
            if (addonDependencies.value().length > 0)
            {
               for (AddonDependency addonDependency : addonDependencies.value())
               {
                  createAnnotatedDeployment(primaryDeployment,
                           classUnderTest,
                           deploymentMethod,
                           deployments,
                           addonDependency.name(),
                           addonDependency.version(),
                           AddonDependency.class.getSimpleName(),
                           addonDependency.imported(),
                           addonDependency.exported(),
                           addonDependency.optional(),
                           addonDependency.listener(),
                           addonDependency.timeout(),
                           addonDependency.timeoutUnit(),
                           addonDependency.shouldThrowException());
               }
            }
            else if (addonDependencies.automatic())
            {
               addAutomaticDependencies(primaryDeployment, classUnderTest, deploymentMethod, deployments);
            }
         }
         else if (annotation instanceof Dependencies)
         {
            Dependencies addonDependencies = (Dependencies) annotation;
            if (addonDependencies.value() != null)
            {
               for (AddonDependency addonDependency : addonDependencies.value())
               {
                  createAnnotatedDeployment(primaryDeployment,
                           classUnderTest,
                           deploymentMethod,
                           deployments,
                           addonDependency.name(),
                           addonDependency.version(),
                           AddonDependency.class.getSimpleName(),
                           addonDependency.imported(),
                           addonDependency.exported(),
                           addonDependency.optional(),
                           addonDependency.listener(),
                           addonDependency.timeout(),
                           addonDependency.timeoutUnit(),
                           addonDependency.shouldThrowException());
               }
            }
         }
      }

      return deployments;
   }

   @SuppressWarnings("unchecked")
   private void addAutomaticDependencies(DeploymentDescription primaryDeployment, Class<?> classUnderTest,
            Method deploymentMethod, Collection<DeploymentDescription> deployments)
   {
      for (AddonDependencyEntry dependency : getAddonSet(classUnderTest))
      {
         createAnnotatedDeployment(primaryDeployment,
                  classUnderTest,
                  deploymentMethod,
                  deployments,
                  dependency.getName(),
                  dependency.getVersionRange().toString(),
                  AddonDependency.class.getSimpleName(),
                  true,
                  dependency.isExported(),
                  dependency.isOptional(),
                  new Class[0],
                  10000,
                  TimeUnit.MILLISECONDS,
                  NullException.class);
      }
   }

   private Set<AddonDependencyEntry> getAddonSet(Class<?> classUnderTest)
   {
      if (addonSet == null)
         buildDependencyMaps(classUnderTest);

      return addonSet;
   }

   private void createAnnotatedDeployment(DeploymentDescription primaryDeployment, Class<?> classUnderTest,
            Method deploymentMethod, Collection<DeploymentDescription> deployments, String addonName,
            String addonVersion, String annotationSimpleName, boolean imported, boolean exported, boolean optional,
            Class<? extends DeploymentListener>[] listenerClasses, int timeoutQuantity, TimeUnit timeoutUnit,
            Class<? extends Exception> expectedException)
   {
      /*
       * Resolve version of annotated deployment (if possible)f
       */
      String version;
      if (addonVersion.isEmpty())
      {
         version = resolveVersionFromPOM(classUnderTest, addonName);
         if (version == null)
         {
            throw new IllegalStateException("Could not resolve the version for [" + addonName
                     + "]. Either specify the version for this @" + annotationSimpleName
                     + " in [" + classUnderTest.getName() + "] or add it to pom.xml located at ["
                     + getPomFileFor(classUnderTest) + "]");
         }
      }
      else
      {
         version = addonVersion;
      }
      AddonId id = AddonId.from(addonName, version);
      AddonDeploymentArchive archive = ShrinkWrap.create(AddonDeploymentArchive.class).setAddonId(id);

      /*
       * Configure deploymenet timeout
       */
      archive.setDeploymentTimeoutUnit(timeoutUnit);
      archive.setDeploymentTimeoutQuantity(timeoutQuantity);

      /*
       * Configure target repository
       */
      if (Annotations.isAnnotationPresent(deploymentMethod, DeployToRepository.class))
      {
         archive.setAddonRepository(Annotations.getAnnotation(deploymentMethod, DeployToRepository.class)
                  .value());
      }

      /*
       * Configure automatic dependency registration to parent Archive
       */
      if (imported)
      {
         AddonDependencyEntry dependency =
                  AddonDependencyEntry.create(addonName, addonVersion, exported, optional);
         ((AddonArchiveBase<?>) primaryDeployment.getArchive()).addAsAddonDependencies(dependency);
      }

      /*
       * Configure deployment listeners
       */
      for (Class<? extends DeploymentListener> listenerClass : listenerClasses)
      {
         if (DeploymentListener.class.equals(listenerClass))
            continue; // do nothing for the default

         try
         {
            archive.addDeploymentListener(listenerClass.newInstance());
         }
         catch (Exception e)
         {
            throw new RuntimeException("Could not instantiate " + DeploymentListener.class.getSimpleName()
                     + " of type " + listenerClass.getName(), e);
         }
      }

      DeploymentDescription deploymentDescription = new DeploymentDescription(id.toCoordinates(), archive);

      /*
       * Don't package supporting test classes in annotation deployments
       */
      deploymentDescription.shouldBeTestable(false);

      /*
       * Configure expected deployment exception
       */
      if (!NullException.class.isAssignableFrom(expectedException))
         deploymentDescription.setExpectedException(expectedException);

      deployments.add(deploymentDescription);
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

   private DeploymentDescription generateDeployment(Method deploymentMethod)
   {
      TargetDescription target = generateTarget(deploymentMethod);
      ProtocolDescription protocol = generateProtocol(deploymentMethod);

      Deployment deploymentAnnotation = deploymentMethod.getAnnotation(Deployment.class);
      DeploymentDescription description = null;
      if (Archive.class.isAssignableFrom(deploymentMethod.getReturnType()))
      {
         Archive<?> archive = invoke(Archive.class, deploymentMethod);
         if (archive instanceof RepositoryLocationAware)
         {
            if (Annotations.isAnnotationPresent(deploymentMethod, DeployToRepository.class))
               ((RepositoryLocationAware<?>) archive).setAddonRepository(Annotations.getAnnotation(deploymentMethod,
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

   private TargetDescription generateTarget(Method deploymentMethod)
   {
      if (deploymentMethod.isAnnotationPresent(TargetsContainer.class))
      {
         return new TargetDescription(deploymentMethod.getAnnotation(TargetsContainer.class).value());
      }
      return TargetDescription.DEFAULT;
   }

   private ProtocolDescription generateProtocol(Method deploymentMethod)
   {
      if (deploymentMethod.isAnnotationPresent(OverProtocol.class))
      {
         return new ProtocolDescription(deploymentMethod.getAnnotation(OverProtocol.class).value());
      }
      return new FurnaceProtocolDescription();
   }

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

   /**
    * Read the pom.xml of the project containing the class under test
    */
   private String resolveVersionFromPOM(Class<?> classUnderTest, String name)
   {
      if (dependencyMap == null)
      {
         buildDependencyMaps(classUnderTest);
      }
      return dependencyMap.get(name);
   }

   private void buildDependencyMaps(Class<?> classUnderTest)
   {
      ProjectHelper projectHelper = new ProjectHelper();
      addonSet = new LinkedHashSet<>();
      dependencyMap = new HashMap<String, String>();
      File pomFile = getPomFileFor(classUnderTest);
      try
      {
         // Needed for single-project addons
         Model model = projectHelper.loadPomFromFile(pomFile);
         String thisAddonName = (model.getGroupId() == null) ? model.getParent().getGroupId() : model.getGroupId()
                  + ":" + model.getArtifactId();
         String thisVersion = model.getVersion();
         if (projectHelper.isAddon(model))
         {
            addonSet.add(AddonDependencyEntry.create(thisAddonName, thisVersion, true, false));
         }
         dependencyMap.put(thisAddonName, thisVersion);

         Map<AddonDependencyEntry, String> containerScope = new HashMap<>();
         List<Dependency> dependencies = projectHelper.resolveDependenciesFromPOM(pomFile);
         for (Dependency dependency : dependencies)
         {
            Artifact artifact = dependency.getArtifact();
            String addonName = artifact.getGroupId() + ":" + artifact.getArtifactId();
            String version = artifact.getBaseVersion();
            String scope = dependency.getScope();
            boolean optional = dependency.isOptional();
            dependencyMap.put(addonName, version);

            if (MavenAddonDependencyResolver.FORGE_ADDON_CLASSIFIER.equals(artifact.getClassifier()))
            {
               AddonDependencyEntry entry = AddonDependencyEntry.create(addonName, version,
                        MavenAddonDependencyResolver.isExported(scope), optional);
               if (MavenAddonDependencyResolver.isFurnaceContainer(artifact))
               {
                  containerScope.put(entry, scope);
               }
               addonSet.add(entry);
            }
         }
         // If there are more than one container set and at least one container set in test scope
         if (containerScope.size() > 1 && containerScope.containsValue(JavaScopes.TEST))
         {
            // Remove non-test scoped containers
            for (Entry<AddonDependencyEntry, String> entry : containerScope.entrySet())
            {
               if (!JavaScopes.TEST.equals(entry.getValue()))
               {
                  addonSet.remove(entry.getKey());
               }
            }
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
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
}

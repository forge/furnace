/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.furnace.maven.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.impl.graph.AddonDependencyEdge;
import org.jboss.forge.furnace.impl.graph.AddonDependencyEdgeNameProvider;
import org.jboss.forge.furnace.impl.graph.AddonVertex;
import org.jboss.forge.furnace.impl.graph.AddonVertexNameProvider;
import org.jboss.forge.furnace.manager.maven.addon.MavenAddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonDependencyResolver;
import org.jboss.forge.furnace.manager.spi.AddonInfo;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;

/**
 * Generate a DOT file from the graph
 */
@Mojo(defaultPhase = LifecyclePhase.PREPARE_PACKAGE, name = "generate-dot", threadSafe = true)
public class GenerateDOTMojo extends AbstractMojo
{
   /**
    * Output directoy
    */
   @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/resources")
   private String outputDirectory;

   /**
    * The output filename. Default will be the addonId name (without the groupId)
    */
   @Parameter
   private String outputFileName;

   /**
    * Include transitive addons
    */
   @Parameter
   private boolean includeTransitiveAddons;

   /**
    * Addon IDs to install
    */
   @Parameter
   private String[] addonIds;

   /**
    * Should the produced artifact be attached to the project?
    */
   @Parameter
   private boolean attach;

   /**
    * Skip this execution ?
    */
   @Parameter(property = "furnace.dot.skip")
   private boolean skip;

   /**
    * Classifier used for addon resolution (default is forge-addon)
    */
   @Parameter(defaultValue = "forge-addon")
   private String classifier;

   /**
    * The current maven project
    */
   @Component
   private MavenProject mavenProject;

   /**
    * Maven Project Helper
    */
   @Component
   private MavenProjectHelper projectHelper;

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      if (skip)
      {
         getLog().info("Execution skipped.");
         return;
      }
      AddonDependencyResolver addonResolver = new MavenAddonDependencyResolver(classifier);
      if (addonIds == null || addonIds.length == 0)
      {
         // XXX
         // if (!"forge-addon".equals(mavenProject.getArtifact().getClassifier()))
         // {
         // getLog().warn("No <addonIds> informed and current project is not a forge-addon. Skipping");
         // return;
         // }
         AddonId id = AddonId.from(mavenProject.getGroupId() + ":" + mavenProject.getArtifactId(),
                  mavenProject.getVersion());
         String fileName = outputFileName == null ? id.getName().substring(id.getName().indexOf(':') + 1) + "-"
                  + id.getVersion() + ".dot" : outputFileName;
         File file = generateDOTFile(addonResolver, id, fileName);
         if (attach && file.isFile())
         {
            projectHelper.attachArtifact(mavenProject, "dot", file);
         }
      }
      else
      {
         for (String addonId : addonIds)
         {
            AddonId id = AddonId.fromCoordinates(addonId);
            String fileName = id.getName().substring(id.getName().indexOf(':') + 1) + "-"
                     + id.getVersion() + ".dot";
            generateDOTFile(addonResolver, id, fileName);
         }
      }
   }

   /**
    * Generates the DOT file for a given addonId
    * 
    * @param addonResolver
    * @param id
    * @return generated file
    */
   private File generateDOTFile(AddonDependencyResolver addonResolver, AddonId id, String fileName)
   {
      File parent = new File(outputDirectory);
      parent.mkdirs();
      File file = new File(parent, fileName);
      getLog().info("Generating " + file);
      AddonInfo addonInfo = addonResolver.resolveAddonDependencyHierarchy(id);
      toDOT(file, toGraph(addonInfo));
      return file;
   }

   DirectedGraph<AddonVertex, AddonDependencyEdge> toGraph(AddonInfo info)
   {
      DirectedGraph<AddonVertex, AddonDependencyEdge> graph = new DefaultDirectedGraph<AddonVertex, AddonDependencyEdge>(
               AddonDependencyEdge.class);
      AddonId addon = info.getAddon();
      AddonVertex rootVertex = new AddonVertex(addon.getName(), addon.getVersion());
      graph.addVertex(rootVertex);
      for (AddonDependencyEntry entry : info.getDependencyEntries())
      {
         AddonVertex depVertex = new AddonVertex(entry.getName(), entry.getVersionRange().getMax());
         graph.addVertex(depVertex);
         graph.addEdge(rootVertex, depVertex,
                  new AddonDependencyEdge(entry.getVersionRange(), entry.isExported()));
      }
      if (includeTransitiveAddons)
      {
         // TODO
      }
      return graph;
   }

   void toDOT(File file, DirectedGraph<AddonVertex, AddonDependencyEdge> graph)
   {
      FileWriter fw = null;
      try
      {
         DOTExporter<AddonVertex, AddonDependencyEdge> exporter = new DOTExporter<AddonVertex, AddonDependencyEdge>(
                  new IntegerNameProvider<AddonVertex>(),
                  new AddonVertexNameProvider(),
                  new AddonDependencyEdgeNameProvider());

         fw = new FileWriter(file);
         exporter.export(fw, graph);
         fw.flush();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      finally
      {
         if (fw != null)
            try
            {
               fw.close();
            }
            catch (IOException ignored)
            {
            }
      }
   }
}
Furnace
=======

Lightweight Modular Service Container - Based on JBoss Modules and Maven. It's easier than OSGi :)

[![Build Status](https://travis-ci.org/forge/furnace.svg?branch=master)](https://travis-ci.org/forge/furnace)
[![License](http://img.shields.io/:license-EPL-blue.svg)](https://www.eclipse.org/legal/epl-v10.html)
[![Maven central](https://maven-badges.herokuapp.com/maven-central/org.jboss.forge.furnace/furnace-se/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jboss.forge.furnace/furnace-se)

Usage
=====

First, include the Furnace dependency in your project:

    <dependency>
       <groupId>org.jboss.forge.furnace</groupId>
       <artifactId>furnace-se</artifactId>
       <version>${version.furnace}</version>
    </dependency>

Then configure the container and start it:

    Furnace furnace = FurnaceFactory.getFurnace()
    furnace.addRepository(AddonRepositoryMode.MUTABLE, new File(OperatingSystemUtils.getUserForgeDir(), "addons"));
    
To install addons, you will need to add the following dependencies to your project:

 
    <dependency>
       <groupId>org.jboss.forge.furnace</groupId>
       <artifactId>furnace-manager</artifactId>
       <version>${version.furnace}</version>
    </dependency>

    <dependency>
       <groupId>org.jboss.forge.furnace</groupId>
       <artifactId>furnace-manager-resolver-maven</artifactId>
       <version>${version.furnace}</version>
    </dependency>
    
Then create a new AddonManager:

    AddonManager manager = new AddonManagerImpl(furnace, new MavenAddonDependencyResolver())
    
Once you have an `AddonManager` instance, you can begin to install addons (You can do this while Furnace is running):
    
    InstallRequest request = manager.install(AddonId.from("org.example:my-addon", "1.0.0.Final"));
    request.perform();
    
Don't forget to start Furnace:

    Future<Furnace> future = furnace.startAsync();
    future.get(); // wait for Furnace to start, before continuing.
    
Once this is done, you'll now be able to request services from Furnace's `AddonRegistry`, and utilize the functionality of the addons you've installed:
    
    MyServiceType instance = furnace.getAddonRegistry().getServices(MyServiceType.class).get();

Of course, addons can be pre-bundled into a project using the Furnace Maven Plugin, making it much simpler (and faster) to run your application:

    <plugin>
       <groupId>org.jboss.forge.furnace</groupId>
       <artifactId>furnace-maven-plugin</artifactId>
       <version>${version.furnace}</version>
       <executions>
          <execution>
             <id>deploy-addons</id>
             <phase>prepare-package</phase>
             <goals>
                <goal>addon-install</goal>
             </goals>
             <inherited>false</inherited>
             <configuration>
                <addonRepository>${basedir}/addon-repository</addonRepository>
                <addonIds>
                   <addonId>org.example:my-addon,1.0.0.Final</addonId>
                </addonIds>
             </configuration>
          </execution>
       </executions>
    </plugin>
    
To learn more about writing addons, see the full documentation here: https://github.com/forge/core#developing-an-addon

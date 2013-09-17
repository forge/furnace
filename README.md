furnace
=======

Lightweight Modular Service Container - Based Maven and JBoss Modules. It's easier than OSGi :)

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
    
    InstallRequest request = manager.install(AddonId.from("test:no_dep", "1.0.0.Final"));
    request.perform();
    
Don't forget to start Furnace:

    furnace.startAsync();
    
    

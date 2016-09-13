package org.jboss.forge.furnace.proxy.classloader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.arquillian.services.LocalServices;
import org.jboss.forge.classloader.mock.JavaUtilLoggingFactory;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterBuilder;
import org.jboss.forge.furnace.proxy.Proxies;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.logging.LogRecord;

/**
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 */
@RunWith(Arquillian.class)
public class ClassLoaderAdapterJavaUtilLoggingTest {
    @Deployment(order = 3)
    public static AddonArchive getDeployment()
    {
        AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
                .addBeansXML()
                .addClasses(JavaUtilLoggingFactory.class)
                .addAsLocalServices(ClassLoaderAdapterJavaUtilLoggingTest.class);

        return archive;
    }

    @Deployment(name = "dep,1", testable = false, order = 2)
    public static AddonArchive getDeploymentDep1()
    {
        AddonArchive archive = ShrinkWrap.create(AddonArchive.class)
                .addClasses(JavaUtilLoggingFactory.class)
                .addBeansXML();

        return archive;
    }

    @Test
    public void testLogRecordProxy() throws Exception
    {
        AddonRegistry registry = LocalServices.getFurnace(getClass().getClassLoader())
                .getAddonRegistry();
        ClassLoader thisLoader = ClassLoaderAdapterJavaUtilLoggingTest.class.getClassLoader();
        ClassLoader dep1Loader = registry.getAddon(AddonId.from("dep", "1")).getClassLoader();

        Class<?> foreignType = dep1Loader.loadClass(JavaUtilLoggingFactory.class.getName());
        LogRecord logRecord = (LogRecord) foreignType.getMethod("getLogRecord")
                .invoke(foreignType.newInstance());

        Assert.assertNotNull(logRecord);
        Assert.assertTrue(logRecord.getClass().equals(LogRecord.class));

        Object delegate = foreignType.newInstance();
        JavaUtilLoggingFactory enhancedFactory = (JavaUtilLoggingFactory) ClassLoaderAdapterBuilder.callingLoader(thisLoader)
                .delegateLoader(dep1Loader).enhance(delegate);

        Assert.assertTrue(Proxies.isForgeProxy(enhancedFactory));
        LogRecord result = enhancedFactory.getLogRecord();
        Assert.assertFalse(Proxies.isForgeProxy(result));
    }
}

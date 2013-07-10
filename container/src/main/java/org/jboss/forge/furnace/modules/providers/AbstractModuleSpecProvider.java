package org.jboss.forge.furnace.modules.providers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.forge.furnace.modules.ModuleSpecProvider;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.filter.PathFilters;

/**
 * This class is the base class for any {@link ModuleSpecProvider} implementation inside Furnace
 */
public abstract class AbstractModuleSpecProvider implements ModuleSpecProvider
{
   @Override
   public ModuleSpec get(ModuleLoader loader, ModuleIdentifier id)
   {
      if (getId().equals(id))
      {
         Builder builder = ModuleSpec.build(id);
         builder.addDependency(DependencySpec.createClassLoaderDependencySpec(PathFilters.acceptAll(),
                  PathFilters.acceptAll(), ClassLoader.getSystemClassLoader(), getPaths()));

         configure(loader, builder);

         return builder.create();
      }
      return null;
   }

   protected void configure(ModuleLoader loader, Builder builder)
   {
   }

   protected abstract ModuleIdentifier getId();

   protected abstract Set<String> getPaths();

   static protected Set<String> systemPaths = new HashSet<String>();

   private static Set<String> getPathsFrom(String root, File file)
   {
      Set<String> result = new HashSet<String>();
      String[] children = file.list();
      for (String name : children)
      {
         File child = new File(file, name);
         if (child.isDirectory())
         {
            result.addAll(getPathsFrom(root, child));
            String path = child.getAbsolutePath().substring(root.length() + 1);
            result.add(path);
         }
      }
      return result;
   }

   protected static Set<String> getLoaderPaths()
   {
      Set<String> result = new HashSet<String>();
      ClassLoader loader = ClassLoader.getSystemClassLoader();
      URL[] urls = ((URLClassLoader) loader).getURLs();

      for (URL url : urls)
      {
         try
         {
            File file = new File(url.toURI());
            if (file.isDirectory())
            {
               result.addAll(getPathsFrom(file.getAbsolutePath(), file));
            }
            else if (!file.isDirectory())
            {
               JarFile jar = new JarFile(file);
               Enumeration<JarEntry> entries = jar.entries();
               while (entries.hasMoreElements())
               {
                  JarEntry entry = entries.nextElement();
                  String name = entry.getName();
                  if (name.indexOf('/') != -1)
                     result.add(name.substring(0, name.lastIndexOf('/')));
               }
            }
         }
         catch (IOException e)
         {
            System.out.println("Failed loading paths from: [" + url.toString() + "]. Attempting folder discovery");
         }
         catch (URISyntaxException e)
         {
            throw new RuntimeException(e);
         }
      }
      return result;
   }

   static
   {
      systemPaths.add("META-INF");
      systemPaths.add("META-INF/services");
      systemPaths.add("__redirected");
      systemPaths.add("java/awt");
      systemPaths.add("java/awt/color");
      systemPaths.add("java/awt/datatransfer");
      systemPaths.add("java/awt/dnd");
      systemPaths.add("java/awt/dnd/peer");
      systemPaths.add("java/awt/event");
      systemPaths.add("java/awt/font");
      systemPaths.add("java/awt/geom");
      systemPaths.add("java/awt/im");
      systemPaths.add("java/awt/im/spi");
      systemPaths.add("java/awt/image");
      systemPaths.add("java/awt/image/renderable");
      systemPaths.add("java/awt/peer");
      systemPaths.add("java/awt/print");
      systemPaths.add("java/beans");
      systemPaths.add("java/beans/beancontext");
      systemPaths.add("java/io");
      systemPaths.add("java/lang");
      systemPaths.add("java/lang/annotation");
      systemPaths.add("java/lang/instrument");
      systemPaths.add("java/lang/management");
      systemPaths.add("java/lang/ref");
      systemPaths.add("java/lang/reflect");
      systemPaths.add("java/math");
      systemPaths.add("java/net");
      systemPaths.add("java/nio");
      systemPaths.add("java/nio/channels");
      systemPaths.add("java/nio/channels/spi");
      systemPaths.add("java/nio/charset");
      systemPaths.add("java/nio/charset/spi");
      systemPaths.add("java/rmi");
      systemPaths.add("java/rmi/activation");
      systemPaths.add("java/rmi/dgc");
      systemPaths.add("java/rmi/registry");
      systemPaths.add("java/rmi/server");
      systemPaths.add("java/security");
      systemPaths.add("java/security/acl");
      systemPaths.add("java/security/cert");
      systemPaths.add("java/security/interfaces");
      systemPaths.add("java/security/spec");
      systemPaths.add("java/sql");
      systemPaths.add("java/text");
      systemPaths.add("java/text/spi");
      systemPaths.add("java/util");
      systemPaths.add("java/util/concurrent");
      systemPaths.add("java/util/concurrent/atomic");
      systemPaths.add("java/util/concurrent/locks");
      systemPaths.add("java/util/jar");
      systemPaths.add("java/util/logging");
      systemPaths.add("java/util/prefs");
      systemPaths.add("java/util/regex");
      systemPaths.add("java/util/spi");
      systemPaths.add("java/util/zip");
      systemPaths.add("javax");
      systemPaths.add("javax/accessibility");
      systemPaths.add("javax/activation");
      systemPaths.add("javax/activity");
      systemPaths.add("javax/crypto");
      systemPaths.add("javax/crypto/interfaces");
      systemPaths.add("javax/crypto/spec");
      systemPaths.add("javax/imageio");
      systemPaths.add("javax/imageio/event");
      systemPaths.add("javax/imageio/metadata");
      systemPaths.add("javax/imageio/plugins/bmp");
      systemPaths.add("javax/imageio/plugins/jpeg");
      systemPaths.add("javax/imageio/spi");
      systemPaths.add("javax/imageio/stream");
      systemPaths.add("javax/jws");
      systemPaths.add("javax/jws/soap");
      systemPaths.add("javax/lang/model");
      systemPaths.add("javax/lang/model/element");
      systemPaths.add("javax/lang/model/type");
      systemPaths.add("javax/lang/model/util");
      systemPaths.add("javax/management");
      systemPaths.add("javax/management/loading");
      systemPaths.add("javax/management/modelmbean");
      systemPaths.add("javax/management/monitor");
      systemPaths.add("javax/management/openmbean");
      systemPaths.add("javax/management/relation");
      systemPaths.add("javax/management/remote");
      systemPaths.add("javax/management/remote/rmi");
      systemPaths.add("javax/management/timer");
      systemPaths.add("javax/media/j3d");
      systemPaths.add("javax/media/jai");
      systemPaths.add("javax/media/jai/iterator");
      systemPaths.add("javax/media/jai/operator");
      systemPaths.add("javax/media/jai/registry");
      systemPaths.add("javax/media/jai/remote");
      systemPaths.add("javax/media/jai/tilecodec");
      systemPaths.add("javax/media/jai/util");
      systemPaths.add("javax/media/jai/widget");
      systemPaths.add("javax/naming");
      systemPaths.add("javax/naming/directory");
      systemPaths.add("javax/naming/event");
      systemPaths.add("javax/naming/ldap");
      systemPaths.add("javax/naming/spi");
      systemPaths.add("javax/net");
      systemPaths.add("javax/net/ssl");
      systemPaths.add("javax/print");
      systemPaths.add("javax/print/attribute");
      systemPaths.add("javax/print/attribute/standard");
      systemPaths.add("javax/print/event");
      systemPaths.add("javax/rmi");
      systemPaths.add("javax/rmi/CORBA");
      systemPaths.add("javax/rmi/ssl");
      systemPaths.add("javax/script");
      systemPaths.add("javax/security/auth");
      systemPaths.add("javax/security/auth/callback");
      systemPaths.add("javax/security/auth/kerberos");
      systemPaths.add("javax/security/auth/login");
      systemPaths.add("javax/security/auth/spi");
      systemPaths.add("javax/security/auth/x500");
      systemPaths.add("javax/security/cert");
      systemPaths.add("javax/security/sasl");
      systemPaths.add("javax/servlet");
      systemPaths.add("javax/servlet/annotation");
      systemPaths.add("javax/servlet/descriptor");
      systemPaths.add("javax/servlet/http");
      systemPaths.add("javax/smartcardio");
      systemPaths.add("javax/sound/midi");
      systemPaths.add("javax/sound/midi/spi");
      systemPaths.add("javax/sound/sampled");
      systemPaths.add("javax/sound/sampled/spi");
      systemPaths.add("javax/sql");
      systemPaths.add("javax/sql/rowset");
      systemPaths.add("javax/sql/rowset/serial");
      systemPaths.add("javax/sql/rowset/spi");
      systemPaths.add("javax/swing");
      systemPaths.add("javax/swing/border");
      systemPaths.add("javax/swing/colorchooser");
      systemPaths.add("javax/swing/event");
      systemPaths.add("javax/swing/filechooser");
      systemPaths.add("javax/swing/plaf");
      systemPaths.add("javax/swing/plaf/basic");
      systemPaths.add("javax/swing/plaf/metal");
      systemPaths.add("javax/swing/plaf/multi");
      systemPaths.add("javax/swing/plaf/synth");
      systemPaths.add("javax/swing/table");
      systemPaths.add("javax/swing/text");
      systemPaths.add("javax/swing/text/html");
      systemPaths.add("javax/swing/text/html/parser");
      systemPaths.add("javax/swing/text/rtf");
      systemPaths.add("javax/swing/tree");
      systemPaths.add("javax/swing/undo");
      systemPaths.add("javax/tools");
      systemPaths.add("javax/transaction");
      systemPaths.add("javax/transaction/xa");
      systemPaths.add("javax/vecmath");
      systemPaths.add("javax/xml");
      systemPaths.add("javax/xml/bind");
      systemPaths.add("javax/xml/bind/annotation");
      systemPaths.add("javax/xml/bind/annotation/adapters");
      systemPaths.add("javax/xml/bind/attachment");
      systemPaths.add("javax/xml/bind/helpers");
      systemPaths.add("javax/xml/bind/util");
      systemPaths.add("javax/xml/crypto");
      systemPaths.add("javax/xml/crypto/dom");
      systemPaths.add("javax/xml/crypto/dsig");
      systemPaths.add("javax/xml/crypto/dsig/dom");
      systemPaths.add("javax/xml/crypto/dsig/keyinfo");
      systemPaths.add("javax/xml/crypto/dsig/spec");
      systemPaths.add("javax/xml/datatype");
      systemPaths.add("javax/xml/namespace");
      systemPaths.add("javax/xml/parsers");
      systemPaths.add("javax/xml/soap");
      systemPaths.add("javax/xml/stream");
      systemPaths.add("javax/xml/stream/events");
      systemPaths.add("javax/xml/stream/util");
      systemPaths.add("javax/xml/transform");
      systemPaths.add("javax/xml/transform/dom");
      systemPaths.add("javax/xml/transform/sax");
      systemPaths.add("javax/xml/transform/stax");
      systemPaths.add("javax/xml/transform/stream");
      systemPaths.add("javax/xml/validation");
      systemPaths.add("javax/xml/ws");
      systemPaths.add("javax/xml/ws/handler");
      systemPaths.add("javax/xml/ws/handler/soap");
      systemPaths.add("javax/xml/ws/http");
      systemPaths.add("javax/xml/ws/soap");
      systemPaths.add("javax/xml/ws/spi");
      systemPaths.add("javax/xml/ws/wsaddressing");
      systemPaths.add("javax/xml/xpath");
      systemPaths.add("org/ietf/jgss");
      systemPaths.add("org/jcp/xml/dsig/internal");
      systemPaths.add("org/jcp/xml/dsig/internal/dom");
      systemPaths.add("org/omg/CORBA");
      systemPaths.add("org/omg/CORBA/DynAnyPackage");
      systemPaths.add("org/omg/CORBA/ORBPackage");
      systemPaths.add("org/omg/CORBA/TypeCodePackage");
      systemPaths.add("org/omg/CORBA/portable");
      systemPaths.add("org/omg/CORBA_2_3");
      systemPaths.add("org/omg/CORBA_2_3/portable");
      systemPaths.add("org/omg/CosNaming");
      systemPaths.add("org/omg/CosNaming/NamingContextExtPackage");
      systemPaths.add("org/omg/CosNaming/NamingContextPackage");
      systemPaths.add("org/omg/Dynamic");
      systemPaths.add("org/omg/DynamicAny");
      systemPaths.add("org/omg/DynamicAny/DynAnyFactoryPackage");
      systemPaths.add("org/omg/DynamicAny/DynAnyPackage");
      systemPaths.add("org/omg/IOP");
      systemPaths.add("org/omg/IOP/CodecFactoryPackage");
      systemPaths.add("org/omg/IOP/CodecPackage");
      systemPaths.add("org/omg/Messaging");
      systemPaths.add("org/omg/PortableInterceptor");
      systemPaths.add("org/omg/PortableInterceptor/ORBInitInfoPackage");
      systemPaths.add("org/omg/PortableServer");
      systemPaths.add("org/omg/PortableServer/CurrentPackage");
      systemPaths.add("org/omg/PortableServer/POAManagerPackage");
      systemPaths.add("org/omg/PortableServer/POAPackage");
      systemPaths.add("org/omg/PortableServer/ServantLocatorPackage");
      systemPaths.add("org/omg/PortableServer/portable");
      systemPaths.add("org/omg/SendingContext");
      systemPaths.add("org/omg/stub/java/rmi");
      systemPaths.add("org/omg/stub/javax/management/remote/rmi");
      systemPaths.add("org/relaxng/datatype");
      systemPaths.add("org/relaxng/datatype/helpers");
      systemPaths.add("org/w3c/dom");
      systemPaths.add("org/w3c/dom/bootstrap");
      systemPaths.add("org/w3c/dom/css");
      systemPaths.add("org/w3c/dom/events");
      systemPaths.add("org/w3c/dom/html");
      systemPaths.add("org/w3c/dom/ls");
      systemPaths.add("org/w3c/dom/ranges");
      systemPaths.add("org/w3c/dom/stylesheets");
      systemPaths.add("org/w3c/dom/traversal");
      systemPaths.add("org/w3c/dom/views");
      systemPaths.add("org/w3c/dom/xpath");
      systemPaths.add("org/xml/sax");
      systemPaths.add("org/xml/sax/ext");
      systemPaths.add("org/xml/sax/helpers");
   }
}

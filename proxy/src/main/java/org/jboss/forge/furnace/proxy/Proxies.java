/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.forge.furnace.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Proxies
{
   private static final Pattern JAVA_PACKAGE_REGEXP = Pattern.compile("^(java\\.).*");
   private static final Pattern JAVA_LANG_PACKAGE_REGEXP = Pattern.compile("^(java\\.lang).*");
   private static final Pattern JAVA_UTIL_LOGGING_PACKAGE_REGEXP = Pattern.compile("^(java\\.util\\.logging).*");
   private static final Pattern JAVA_IO_PACKAGE_REGEXP = Pattern.compile("^((sun|java)\\.n?io).*");
   private static final Pattern JAVA_NET_PACKAGE_REGEXP = Pattern.compile("^(java\\.net).*");

   private static final Pattern CGLIB_CLASSNAME_REGEXP = Pattern.compile("^(.*)\\$\\$EnhancerByCGLIB\\$\\$.*");
   private static final Pattern JAVASSIST_CLASSNAME_REGEXP = Pattern.compile("^(.*)_\\$\\$_jvst.*");

   private static MethodFilter filter = new ForgeProxyMethodFilter();

   private static Map<String, Map<String, WeakReference<Class<?>>>> classCache = new ConcurrentHashMap<>();

   /**
    * Create a proxy for the given {@link Class} type, {@link Object} instance, and {@link ForgeProxy} handler. If
    * instance is <code>null</code>, this will return <code>null</code>.
    */
   @SuppressWarnings("unchecked")
   public static <T> T enhance(final ClassLoader loader, Object instance, ForgeProxy handler)
   {
      Assert.notNull(loader, "ClassLoader must not be null");
      Assert.notNull(handler, "ForgeProxy handler must not be null");

      if (instance == null)
         return null;

      Class<?> type = Proxies.unwrapProxyTypes(instance.getClass(), loader);

      Object result = null;

      Class<?> proxyType = getCachedProxyType(loader, type);
      if (proxyType == null)
      {
         Class<?>[] hierarchy = null;
         Class<?> superclass = null;

         hierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(loader, type);
         if (hierarchy == null || hierarchy.length == 0)
            throw new IllegalArgumentException("Must specify at least one non-final type to enhance for Object: "
                     + instance + " of type " + instance.getClass());

         Class<?> first = hierarchy[0];
         if (!first.isInterface() && !isProxyType(first))
         {
            superclass = Proxies.unwrapProxyTypes(first, loader);
            hierarchy = Arrays.shiftLeft(hierarchy, new Class<?>[hierarchy.length - 1]);
         }
         else if (isProxyType(first))
            hierarchy = Arrays.shiftLeft(hierarchy, new Class<?>[hierarchy.length - 1]);

         int index = Arrays.indexOf(hierarchy, ProxyObject.class);
         if (index >= 0)
         {
            hierarchy = Arrays.removeElementAtIndex(hierarchy, index);
         }

         if (!Proxies.isProxyType(first) && !Arrays.contains(hierarchy, ForgeProxy.class))
            hierarchy = Arrays.append(hierarchy, ForgeProxy.class);

         ProxyFactory f = new ProxyFactory()
         {
            @Override
            protected ClassLoader getClassLoader()
            {
               return loader;
            }
         };

         f.setInterfaces(hierarchy);
         f.setSuperclass(superclass);
         f.setFilter(filter);

         proxyType = f.createClass();

         setCachedProxyType(loader, type, proxyType);
      }

      try
      {
         result = proxyType.newInstance();
      }
      catch (InstantiationException e)
      {
         throw new IllegalStateException(
                  "Could not instantiate proxy for object [" + instance + "] of type [" + type
                           + "]. For optimal proxy compatibility, ensure " +
                           "that this type is an interface, or a class with a default constructor.",
                  e);
      }
      catch (IllegalAccessException e)
      {
         throw new IllegalStateException(e);
      }

      if (result instanceof Proxy)
         ((Proxy) result).setHandler(handler);
      else if (result instanceof ProxyObject)
         ((ProxyObject) result).setHandler(handler);
      else
         throw new IllegalStateException("Could not set proxy handler [" + handler + "] for proxy object ["
                  + result + "] for instance object [" + instance + "] of type [" + instance.getClass() + "]");

      return (T) result;
   }

   private static Class<?> getCachedProxyType(ClassLoader loader, Class<?> type)
   {
      Class<?> proxyType = null;
      Map<String, WeakReference<Class<?>>> cache = classCache.get(loader.toString());
      if (cache != null)
      {
         WeakReference<Class<?>> ref = cache.get(type.getName());
         if (ref != null)
         {
            proxyType = ref.get();
         }
      }
      return proxyType;
   }

   private static void setCachedProxyType(ClassLoader classLoader, Class<?> type, Class<?> proxyType)
   {
      Map<String, WeakReference<Class<?>>> cache = classCache.get(classLoader.toString());
      if (cache == null)
      {
         cache = new ConcurrentHashMap<>();
         classCache.put(classLoader.toString(), cache);
      }
      cache.put(type.getName(), new WeakReference<Class<?>>(proxyType));
   }

   /**
    * Create a proxy for the given {@link Class} type and {@link ForgeProxy} handler.
    */
   @SuppressWarnings("unchecked")
   public static <T> T enhance(Class<T> type, ForgeProxy handler)
   {
      Assert.notNull(type, "Class type to proxy must not be null");
      Assert.notNull(handler, "ForgeProxy handler must not be null");

      Object result = null;

      Class<?> proxyType = getCachedProxyType(type.getClassLoader(), type);
      if (proxyType == null)
      {
         Class<?>[] hierarchy = null;
         Class<?> superclass = null;

         if (type.isInterface() && !ForgeProxy.class.isAssignableFrom(type))
            hierarchy = new Class<?>[] { type, ForgeProxy.class };
         else if (type.isInterface())
            hierarchy = new Class<?>[] { type };
         else
         {
            if (Proxies.isProxyType(type))
               superclass = unwrapProxyTypes(type);
            else
            {
               superclass = type;
               hierarchy = new Class<?>[] { ForgeProxy.class };
            }
         }

         ProxyFactory f = new ProxyFactory();

         f.setFilter(filter);
         f.setInterfaces(hierarchy);
         f.setSuperclass(superclass);

         proxyType = f.createClass();
         setCachedProxyType(type.getClassLoader(), type, proxyType);
      }

      try
      {
         result = proxyType.newInstance();
      }
      catch (InstantiationException e)
      {
         throw new IllegalStateException(
                  "Could not instantiate proxy for type [" + type
                           + "]. For optimal proxy compatibility, ensure " +
                           "that this type is an interface, or a class with a default constructor.",
                  e);
      }
      catch (IllegalAccessException e)
      {
         throw new IllegalStateException(e);
      }

      if (result instanceof Proxy)
         ((Proxy) result).setHandler(handler);
      else if (result instanceof ProxyObject)
         ((ProxyObject) result).setHandler(handler);
      else
         throw new IllegalStateException("Could not set proxy handler [" + handler + "] for proxy object ["
                  + result + "] for proxy of type [" + type + "]");

      return (T) result;
   }

   public static boolean isProxyType(Class<?> type)
   {
      if (type != null)
      {
         if (type.getName().contains("$$EnhancerByCGLIB$$")
                  || type.getName().contains("_jvst")
                  || type.getName().contains("$Proxy$_$$_WeldClientProxy")
                  || Proxy.class.isAssignableFrom(type)
                  || ProxyObject.class.isAssignableFrom(type))
         {
            return true;
         }
      }
      return false;
   }

   /**
    * Returns the delegate object, if the given object was created via {@link Proxies}, otherwise it returns the given
    * object, unchanged.
    */
   @SuppressWarnings("unchecked")
   public static <T> T unwrap(Object object)
   {
      T result = (T) object;

      if (object != null)
      {
         while (isForgeProxy(result))
         {
            try
            {
               Method method = result.getClass().getMethod("getDelegate");
               method.setAccessible(true);
               result = (T) method.invoke(result);
            }
            catch (Exception e)
            {
               break;
            }
         }

         if (result == null)
            result = (T) object;
      }
      return result;
   }

   @SuppressWarnings("unchecked")
   public static <T> T unwrapOnce(Object object)
   {
      T result = (T) object;

      if (object != null)
      {
         if (isForgeProxy(result))
         {
            try
            {
               Method method = result.getClass().getMethod("getDelegate");
               method.setAccessible(true);
               result = (T) method.invoke(result);
            }
            catch (Exception e)
            {
            }
         }

         if (result == null)
            result = (T) object;
      }
      return result;
   }

   /**
    * Returns true if the given object was created via {@link Proxies}.
    */
   public static boolean isForgeProxy(Object object)
   {
      if (object != null)
      {
         Class<?>[] interfaces = object.getClass().getInterfaces();
         if (interfaces != null)
         {
            for (Class<?> iface : interfaces)
            {
               if (iface.getName().equals(ForgeProxy.class.getName()))
               {
                  return true;
               }
            }
         }
      }
      return false;
   }

   public static Class<?> unwrapProxyTypes(Class<?> type, ClassLoader... loaders)
   {
      Class<?> result = type;

      if (isProxyType(result))
      {
         Class<?> superclass = result.getSuperclass();
         while (superclass != null && !superclass.getName().equals(Object.class.getName()) && isProxyType(superclass))
         {
            superclass = superclass.getSuperclass();
         }

         if (superclass != null && !superclass.getName().equals(Object.class.getName()))
            result = superclass;

         String typeName = unwrapProxyClassName(result);
         if (loaders != null)
         {
            for (ClassLoader loader : loaders)
            {
               try
               {
                  result = loader.loadClass(typeName);
                  break;
               }
               catch (ClassNotFoundException e)
               {
               }
            }
         }
      }
      return result;
   }

   /**
    * Unwraps the proxy type if javassist or CGLib is used
    *
    * @param type the class type
    * @return the unproxied class name
    */
   public static String unwrapProxyClassName(Class<?> type)
   {
      String typeName = null;
      if (type != null)
      {
         if (type.getName().contains("$$EnhancerByCGLIB$$"))
         {
            typeName = CGLIB_CLASSNAME_REGEXP.matcher(type.getName()).replaceAll("$1");
         }
         else if (type.getName().contains("_jvst"))
         {
            typeName = JAVASSIST_CLASSNAME_REGEXP.matcher(type.getName()).replaceAll("$1");
         }
         else
         {
            typeName = type.getName();
         }
      }
      return typeName;
   }

   /**
    * This method tests if two proxied objects are equivalent.
    *
    * It does so by comparing the class names and the hashCode, since they may be loaded from different classloaders.
    */
   public static boolean areEquivalent(Object proxiedObj, Object anotherProxiedObj)
   {
      if (proxiedObj == null && anotherProxiedObj == null)
      {
         return true;
      }
      else if (proxiedObj == null || anotherProxiedObj == null)
      {
         return false;
      }
      else
      {
         Object unproxiedObj = unwrap(proxiedObj);
         Object anotherUnproxiedObj = unwrap(anotherProxiedObj);

         boolean sameClassName = unwrapProxyClassName(unproxiedObj.getClass()).equals(
                  unwrapProxyClassName(anotherUnproxiedObj.getClass()));
         if (sameClassName)
         {
            if (unproxiedObj.getClass().isEnum())
            {
               // Enum hashCode is different if loaded from different classloaders and cannot be overriden.
               Enum<?> enumLeft = Enum.class.cast(unproxiedObj);
               Enum<?> enumRight = Enum.class.cast(anotherUnproxiedObj);
               return (enumLeft.name().equals(enumRight.name())) && (enumLeft.ordinal() == enumRight.ordinal());
            }
            else
            {
               return (unproxiedObj.hashCode() == anotherUnproxiedObj.hashCode());
            }
         }
         else
         {
            return false;
         }
      }
   }

   /**
    * Checks if a proxied object is an instance of the specified {@link Class}
    */
   public static boolean isInstance(Class<?> type, Object proxiedObject)
   {
      return type.isInstance(unwrap(proxiedObject));
   }

   /**
    * Determine whether or not a given {@link Class} type is instantiable.
    */
   public static boolean isInstantiable(Class<?> type)
   {
      if (type != null)
      {
         try
         {
            if (type.isInterface())
               return true;
            type.getConstructor();
            return true;
         }
         catch (SecurityException e)
         {
            return false;
         }
         catch (NoSuchMethodException e)
         {
            return false;
         }
      }
      return false;
   }

   /**
    * Determine if the given {@link Class} type does not require {@link ClassLoader} proxying.
    */
   public static boolean isPassthroughType(Class<?> type)
   {
      Assert.notNull(type, "Type to inspect must not be null.");

      boolean result = type.isArray()
               || JAVA_LANG_PACKAGE_REGEXP.matcher(type.getName()).matches()
               || JAVA_IO_PACKAGE_REGEXP.matcher(type.getName()).matches()
               || JAVA_NET_PACKAGE_REGEXP.matcher(type.getName()).matches()
               || JAVA_UTIL_LOGGING_PACKAGE_REGEXP.matcher(type.getName()).matches()
               || type.isPrimitive();

      result = result && !(Iterable.class.getName().equals(type.getName()));

      return result;
   }

   public static boolean isLanguageType(Class<?> type)
   {
      Assert.notNull(type, "Type to inspect must not be null.");

      boolean result = type.isArray()
               || JAVA_PACKAGE_REGEXP.matcher(type.getName()).matches()
               || type.isPrimitive();

      return result;
   }

   public static boolean isCollectionType(Object instance)
   {
      Assert.notNull(instance, "Object to inspect must not be null.");

      boolean result = instance instanceof Collection
               || instance instanceof Iterable
               || instance.getClass().isArray();

      return result;
   }

   public static Object getForgeProxyHandler(Object result)
   {
      try
      {
         if (isForgeProxy(result))
            return result.getClass().getMethod("getHandler").invoke(result);
      }
      catch (Exception e)
      {
      }
      return null;
   }
}

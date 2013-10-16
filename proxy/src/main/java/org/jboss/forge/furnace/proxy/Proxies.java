/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class Proxies
{
   private static Map<Integer, Class<?>> cache = new WeakHashMap<Integer, Class<?>>();

   private static MethodFilter filter = new MethodFilter()
   {
      @Override
      public boolean isHandled(Method method)
      {
         String name = method.getName();
         Class<?>[] parameterTypes = method.getParameterTypes();
         if (!method.getDeclaringClass().getName().contains("java.lang")
                  || ("clone".equals(name) && parameterTypes.length == 0)
                  || ("equals".equals(name) && parameterTypes.length == 1)
                  || ("hashCode".equals(name) && parameterTypes.length == 0)
                  || ("toString".equals(name) && parameterTypes.length == 0))
            return true;
         return false;
      }
   };

   /**
    * Create a proxy for the given {@link Class} type.
    */
   @SuppressWarnings("unchecked")
   public static <T> T enhance(final ClassLoader loader, Object instance, ForgeProxy handler)
   {
      Class<?> type = Proxies.unwrapProxyTypes(instance.getClass(), loader);

      Object result = null;
      Class<?> proxyType = cache.get(type.hashCode());
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

         cache.put(type.hashCode(), proxyType);
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
                           "that this type is an interface, or a class with a default constructor.", e);
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

   /**
    * Create a proxy for the given {@link Class} type.
    */
   @SuppressWarnings("unchecked")
   public static <T> T enhance(Class<T> type, ForgeProxy handler)
   {
      Object result = null;

      Class<?> proxyType = cache.get(type.hashCode());
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

         cache.put(type.hashCode(), proxyType);
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
                           "that this type is an interface, or a class with a default constructor.", e);
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
      if (type.getName().contains("$$EnhancerByCGLIB$$")
               || type.getName().contains("_javassist_")
               || type.getName().contains("$Proxy$_$$_WeldClientProxy")
               || Proxy.class.isAssignableFrom(type)
               || ProxyObject.class.isAssignableFrom(type))
      {
         return true;
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
            return superclass;

         String typeName = unwrapProxyClassName(result);
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
      String typeName;
      if (type.getName().contains("$$EnhancerByCGLIB$$"))
      {
         typeName = type.getName().replaceAll("^(.*)\\$\\$EnhancerByCGLIB\\$\\$.*", "$1");
      }
      else if (type.getName().contains("_javassist_"))
      {
         typeName = type.getName().replaceAll("^(.*)_\\$\\$_javassist_.*", "$1");
      }
      else
      {
         typeName = type.getName();
      }
      return typeName;
   }

   /**
    * This method tests if two proxied objects are equivalent.
    * 
    * It does so by comparing the class names and the hashCode, since they may be loaded from different classloaders.
    * 
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
      boolean result = type.isArray()
               || type.getName().matches("^(java\\.lang).*")
               || type.getName().matches("^(java\\.io).*")
               || type.getName().matches("^(java\\.net).*")
               || type.isPrimitive();

      result = result && !(Iterable.class.getName().equals(type.getName()));

      return result;
   }

   public static boolean isLanguageType(Class<?> type)
   {
      boolean result = type.isArray()
               || type.getName().matches("^(java\\.).*")
               || type.isPrimitive();

      return result;
   }

   public static boolean isCollectionType(Object instance)
   {
      boolean result = instance instanceof Collection
               || instance instanceof Iterable
               || instance.getClass().isArray();

      return result;
   }
}

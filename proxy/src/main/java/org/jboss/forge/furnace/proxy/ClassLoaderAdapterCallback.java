/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.proxy;

import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.forge.furnace.exception.ContainerException;
import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.Proxy;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.forge.furnace.util.Assert;
import org.jboss.forge.furnace.util.ClassLoaders;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ClassLoaderAdapterCallback implements MethodHandler, ForgeProxy
{
   private static final Logger log = Logger.getLogger(ClassLoaderAdapterCallback.class.getName());
   private static final ClassLoader JAVASSIST_LOADER = ProxyObject.class.getClassLoader();
   private static final Map<String, Map<String, WeakReference<Class<?>>>> classCache = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> returnTypeNeedsEnhancementCache = new ConcurrentHashMap<>();
   private static final Map<String, List<WeakReference<Class<?>>>> resultHierarchyCache = new ConcurrentHashMap<>();
   private static final Map<String, List<WeakReference<Class<?>>>> returnTypeHierarchyCache = new ConcurrentHashMap<>();

   private final Object delegate;

   private final ClassLoader initialCallingLoader;
   private final ClassLoader delegateLoader;
   private final Callable<Set<ClassLoader>> whitelist;
   private final ClassLoader nullClassLoader = new ClassLoader()
   {
   };

   private ClassLoader getCallingLoader()
   {
      ClassLoader callingLoader = initialCallingLoader;
      if (callingLoader == null)
         callingLoader = ClassLoaderInterceptor.getCurrentloader();
      return callingLoader;
   }

   public ClassLoaderAdapterCallback(Callable<Set<ClassLoader>> whitelist, ClassLoader callingLoader,
            ClassLoader delegateLoader, Object delegate)
   {
      Assert.notNull(whitelist, "ClassLoader whitelist must not be null");
      Assert.notNull(callingLoader, "Calling loader must not be null.");
      Assert.notNull(delegateLoader, "Delegate loader must not be null.");
      Assert.notNull(delegate, "Delegate must not be null.");

      this.whitelist = whitelist;
      this.initialCallingLoader = callingLoader;

      if (delegateLoader == callingLoader)
      {
         // If we don't do this, class enhancement fails because types are always resolved
         this.delegateLoader = nullClassLoader;
      }
      else
      {
         this.delegateLoader = delegateLoader;
      }

      this.delegate = delegate;
   }

   @Override
   public Object invoke(final Object obj, final Method thisMethod, final Method proceed, final Object[] args)
            throws Throwable
   {
      if (Thread.currentThread().isInterrupted())
      {
         throw new ContainerException("Thread.interrupt() requested.");
      }

      Object result = ClassLoaders.executeIn(delegateLoader, new Callable<Object>()
      {
         @Override
         public Object call() throws Exception
         {
            try
            {
               if (thisMethod.getDeclaringClass().getName().equals(ForgeProxy.class.getName()))
               {
                  if (thisMethod.getName().equals("getDelegate"))
                     return ClassLoaderAdapterCallback.this.getDelegate();
                  if (thisMethod.getName().equals("getHandler"))
                     return ClassLoaderAdapterCallback.this.getHandler();
               }
            }
            catch (final Exception e)
            {
            }

            final Method delegateMethod = getDelegateMethod(thisMethod);

            final List<Object> parameterValues = enhanceParameterValues(args, delegateMethod);

            AccessibleObject.setAccessible(new AccessibleObject[] { delegateMethod }, true);
            try
            {
               final Object[] parameterValueArray = parameterValues.toArray();
               final Object result = delegateMethod.invoke(delegate, parameterValueArray);
               return enhanceResult(thisMethod, result);
            }
            catch (final InvocationTargetException e)
            {
               if (e.getCause() instanceof Exception)
                  throw enhanceException(delegateMethod, (Exception) e.getCause());
               throw enhanceException(delegateMethod, e);
            }

         }

         private Method getDelegateMethod(final Method proxy) throws ClassNotFoundException, NoSuchMethodException
         {

            Method delegateMethod = null;
            try
            {
               final List<Class<?>> parameterTypes = translateParameterTypes(proxy);
               delegateMethod = delegate.getClass().getMethod(proxy.getName(),
                        parameterTypes.toArray(new Class<?>[parameterTypes.size()]));
            }
            catch (final ClassNotFoundException e)
            {
               method: for (final Method m : delegate.getClass().getMethods())
               {
                  final String methodName = proxy.getName();
                  final String delegateMethodName = m.getName();
                  if (methodName.equals(delegateMethodName))
                  {
                     final Class<?>[] methodParameterTypes = proxy.getParameterTypes();
                     final Class<?>[] delegateParameterTypes = m.getParameterTypes();

                     if (methodParameterTypes.length == delegateParameterTypes.length)
                     {
                        for (int i = 0; i < methodParameterTypes.length; i++)
                        {
                           final Class<?> methodType = methodParameterTypes[i];
                           final Class<?> delegateType = delegateParameterTypes[i];

                           if (!methodType.getName().equals(delegateType.getName()))
                           {
                              continue method;
                           }
                        }

                        delegateMethod = m;
                        break;
                     }
                  }
               }
               if (delegateMethod == null)
                  throw e;
            }

            return delegateMethod;
         }
      });

      if (Thread.currentThread().isInterrupted())
      {
         throw new ContainerException("Thread.interrupt() requested.");
      }

      return result;
   }

   private Object enhanceResult(final Method method, Object result) throws Exception
   {
      if (result != null)
      {
         final Class<?> unwrappedResultType = Proxies.unwrap(result).getClass();
         ClassLoader callingLoader = getCallingLoader();
         if (getCallingLoader().equals(delegateLoader))
            callingLoader = getInitialCallingLoader();

         ClassLoader resultInstanceLoader = delegateLoader;
         if (!ClassLoaders.containsClass(delegateLoader, unwrappedResultType))
         {
            resultInstanceLoader = Proxies.unwrapProxyTypes(unwrappedResultType, getCallingLoader(), delegateLoader,
                     unwrappedResultType.getClassLoader()).getClassLoader();
            // FORGE-928: java.util.ArrayList.class.getClassLoader() returns null
            if (resultInstanceLoader == null)
            {
               resultInstanceLoader = getClass().getClassLoader();
            }
         }

         final Class<?> returnType = method.getReturnType();
         if (Class.class.equals(returnType))
         {
            final Class<?> resultClassValue = (Class<?>) result;
            try
            {
               result = callingLoader.loadClass(Proxies.unwrapProxyClassName(resultClassValue));
            }
            catch (final ClassNotFoundException e)
            {
               try
               {
                  // If all else fails, try the whitelist loaders.
                  result = loadClassFromWhitelist(Proxies.unwrapProxyClassName(resultClassValue));
               }
               catch (final ClassNotFoundException e3)
               {
                  // Oh well.
               }

               if (result == null)
               {
                  /*
                   * No way, here is the original class and god bless you :) Also unwrap any proxy types since we don't
                   * know about this object, there is no reason to pass a proxied class type.
                   */
                  result = Proxies.unwrapProxyTypes(resultClassValue);
               }
            }
         }
         else if (returnTypeNeedsEnhancement(method, returnType, unwrappedResultType))
         {
            result = stripClassLoaderAdapters(result);

            if (!Modifier.isFinal(returnType.getModifiers()))
            {
               Class<?>[] resultHierarchy = calculateResultHierarchy(result.getClass(), unwrappedResultType,
                        callingLoader);
               if (Object.class.equals(returnType) && !Object.class.equals(result))
               {
                  result = enhance(whitelist, callingLoader, resultInstanceLoader, method, result, resultHierarchy);
               }
               else
               {
                  Class<?>[] returnTypeHierarchy = calculateReturnTypeHierarchy(callingLoader, returnType);
                  if (returnTypeHierarchy.length == 0)
                  {
                     returnTypeHierarchy = new Class[] { returnType };
                  }

                  Object delegateObject = result;
                  if (result instanceof ForgeProxy)
                  {
                     Object forgeProxy = Proxies.getForgeProxyHandler(result);
                     if (forgeProxy instanceof ClassLoaderAdapterCallback)
                     {
                        final ClassLoaderAdapterCallback handler = (ClassLoaderAdapterCallback) forgeProxy;
                        if (handler.getCallingLoader().equals(getCallingLoader())
                                 && handler.getDelegateLoader().equals(getDelegateLoader()))
                        {
                           delegateObject = stripClassLoaderAdapters(result);
                        }
                     }
                  }

                  result = enhance(whitelist, callingLoader, resultInstanceLoader, method, delegateObject,
                           mergeHierarchies(returnTypeHierarchy, resultHierarchy));
               }
            }
            else
            {
               if (result.getClass().isEnum())
               {
                  result = enhanceEnum(callingLoader, result);
               }
               else
               {
                  // FURNACE-104
                  if (result instanceof Optional)
                  {
                     Optional<?> optionalResult = ((Optional<?>) result);
                     if (optionalResult.isPresent())
                     {
                        Object nestedResult = optionalResult.get();
                        Class<?> resultClass = nestedResult.getClass();
                        if (!Proxies.isPassthroughType(resultClass) && !Proxies.isLanguageType(resultClass))
                        {
                           final Class<?>[] resultHierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(
                                    getCallingLoader(),
                                    Proxies.unwrapProxyTypes(resultClass, getCallingLoader(), delegateLoader,
                                             resultInstanceLoader));
                           result = Optional
                                    .of(enhance(whitelist, getCallingLoader(), resultInstanceLoader, method,
                                             nestedResult,
                                             resultHierarchy));
                        }
                     }
                  }
                  else
                  {
                     Class<?>[] returnTypeHierarchy = calculateReturnTypeHierarchy(callingLoader, returnType);
                     if (returnTypeHierarchy.length > 0)
                     {
                        result = enhance(whitelist, callingLoader, resultInstanceLoader, method, returnTypeHierarchy);
                     }
                  }
               }
            }
         }
         else
         {
            result = stripClassLoaderAdapters(result);
         }
      }
      return result;
   }

   private Class<?>[] calculateReturnTypeHierarchy(ClassLoader callingLoader, final Class<?> returnType)
   {
      String key = getReturnTypeHierarchyCacheKey(callingLoader, returnType);

      Class<?>[] returnTypeHierarchy = getCachedReturnTypeHierarchy(key);
      if (returnTypeHierarchy == null)
      {
         returnTypeHierarchy = removeProxyTypes(ProxyTypeInspector.getCompatibleClassHierarchy(
                  callingLoader, returnType));

         setCachedReturnTypeHierarchy(key, returnTypeHierarchy);
      }
      return returnTypeHierarchy;
   }

   private void setCachedReturnTypeHierarchy(String key, Class<?>[] returnTypeHierarchy)
   {
      List<WeakReference<Class<?>>> list = new ArrayList<>();
      for (Class<?> type : returnTypeHierarchy)
      {
         list.add(new WeakReference<Class<?>>(type));
      }
      returnTypeHierarchyCache.put(key, list);
   }

   private Class<?>[] getCachedReturnTypeHierarchy(String key)
   {
      List<Class<?>> result = null;
      List<WeakReference<Class<?>>> list = returnTypeHierarchyCache.get(key);
      if (list != null)
      {
         result = new ArrayList<>();
         for (WeakReference<Class<?>> ref : list)
         {
            Class<?> type = ref.get();
            if (type == null)
               return null;
            result.add(type);
         }
         return result.toArray(new Class<?>[result.size()]);
      }
      return null;
   }

   private String getReturnTypeHierarchyCacheKey(ClassLoader callingLoader, Class<?> returnType)
   {
      return callingLoader + "" + returnType.getName();
   }

   private Class<?>[] calculateResultHierarchy(Class<?> resultType, final Class<?> unwrappedResultType,
            ClassLoader callingLoader)
   {
      String key = getResultHierarchyCacheKey(callingLoader, unwrappedResultType, resultType);

      Class<?>[] resultHierarchy = getCachedResultHierarchy(key);
      if (resultHierarchy == null)
      {
         Class<?>[] resultTypeHierarchy = removeProxyTypes(ProxyTypeInspector.getCompatibleClassHierarchy(
                  callingLoader, resultType));
         Class<?>[] unwrappedResultHierarchy = calculateReturnTypeHierarchy(callingLoader, unwrappedResultType);

         resultHierarchy = mergeHierarchies(resultTypeHierarchy, unwrappedResultHierarchy);
         setCachedResultHierarchy(key, resultHierarchy);
      }
      return resultHierarchy;
   }

   private void setCachedResultHierarchy(String key, Class<?>[] resultHierarchy)
   {
      List<WeakReference<Class<?>>> list = new ArrayList<>();
      for (Class<?> type : resultHierarchy)
      {
         list.add(new WeakReference<Class<?>>(type));
      }
      resultHierarchyCache.put(key, list);
   }

   private Class<?>[] getCachedResultHierarchy(String key)
   {
      List<Class<?>> result = null;
      List<WeakReference<Class<?>>> list = resultHierarchyCache.get(key);
      if (list != null)
      {
         result = new ArrayList<>();
         for (WeakReference<Class<?>> ref : list)
         {
            Class<?> type = ref.get();
            if (type == null)
               return null;
            result.add(type);
         }
         return result.toArray(new Class<?>[result.size()]);
      }
      return null;
   }

   private String getResultHierarchyCacheKey(ClassLoader callingLoader, Class<?> unwrappedResultType,
            Class<?> resultType)
   {
      return callingLoader + "" + unwrappedResultType.getClassLoader() + "" + unwrappedResultType.getName()
               + resultType.getClassLoader() + resultType.getName();
   }

   private Class<?>[] removeProxyTypes(Class<?>[] types)
   {
      final List<Class<?>> result = new ArrayList<>();
      if (types != null)
      {
         for (final Class<?> type : types)
         {
            if (!Proxies.isProxyType(type))
            {
               result.add(type);
            }
         }
      }
      return result.toArray(new Class<?>[result.size()]);
   }

   private static Object stripClassLoaderAdapters(Object value)
   {
      while (Proxies.isForgeProxy(value))
      {
         final Object handler = Proxies.getForgeProxyHandler(value);
         if (handler != null && handler.getClass().getName().equals(ClassLoaderAdapterCallback.class.getName()))
            value = Proxies.unwrapOnce(value);
         else
            break;
      }

      return value;
   }

   private static Object stripClassLoaderInterceptors(Object value)
   {
      while (Proxies.isForgeProxy(value))
      {
         final Object handler = Proxies.getForgeProxyHandler(value);
         if (handler != null && handler.getClass().getName().equals(ClassLoaderInterceptor.class.getName()))
            value = Proxies.unwrapOnce(value);
         else
            break;
      }

      return value;
   }

   private Exception enhanceException(final Method method, final Exception exception)
   {
      Exception result = exception;
      try
      {
         if (exception != null)
         {
            final Class<?> unwrappedExceptionType = Proxies.unwrap(exception).getClass();

            ClassLoader exceptionLoader = delegateLoader;
            if (!ClassLoaders.containsClass(delegateLoader, unwrappedExceptionType))
            {
               exceptionLoader = Proxies.unwrapProxyTypes(unwrappedExceptionType, getCallingLoader(), delegateLoader,
                        unwrappedExceptionType.getClassLoader()).getClassLoader();
               if (exceptionLoader == null)
               {
                  exceptionLoader = getClass().getClassLoader();
               }
            }

            if (exceptionNeedsEnhancement(exception))
            {
               final Class<?>[] exceptionHierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(getCallingLoader(),
                        Proxies.unwrapProxyTypes(exception.getClass(), getCallingLoader(), delegateLoader,
                                 exceptionLoader));

               if (!Modifier.isFinal(unwrappedExceptionType.getModifiers()))
               {
                  result = enhance(whitelist, getCallingLoader(), exceptionLoader, method, exception,
                           exceptionHierarchy);
               }
            }
         }
      }
      catch (final Exception e)
      {
         log.log(Level.WARNING,
                  "Could not enhance exception for passing through ClassLoader boundary. Exception type ["
                           + exception.getClass().getName() + "], Caller [" + getCallingLoader() + "], Delegate ["
                           + delegateLoader + "]");
         return exception;
      }
      return result;
   }

   @SuppressWarnings({ "unchecked", "rawtypes" })
   private Object enhanceEnum(ClassLoader loader, Object instance)
   {
      try
      {
         final Class<Enum> callingType = (Class<Enum>) loader.loadClass(instance.getClass().getName());
         return Enum.valueOf(callingType, ((Enum) instance).name());
      }
      catch (final ClassNotFoundException e)
      {
         throw new ContainerException(
                  "Could not enhance instance [" + instance + "] of type [" + instance.getClass() + "]", e);
      }
   }

   private Class<?>[] mergeHierarchies(Class<?>[] left, Class<?>[] right)
   {
      for (final Class<?> type : right)
      {
         boolean found = false;
         for (final Class<?> existing : left)
         {
            if (type.equals(existing))
            {
               found = true;
               break;
            }
         }

         if (!found)
         {
            if (type.isInterface())
               left = Arrays.append(left, type);
            else if (left.length == 0 || left[0].isInterface())
               left = Arrays.prepend(left, type);
         }
      }
      return left;
   }

   @SuppressWarnings("unchecked")
   private boolean exceptionNeedsEnhancement(Exception exception)
   {
      final Class<? extends Exception> exceptionType = exception.getClass();
      final Class<? extends Exception> unwrappedExceptionType = (Class<? extends Exception>) Proxies.unwrap(exception)
               .getClass();

      if (Proxies.isPassthroughType(unwrappedExceptionType))
      {
         return false;
      }

      if (unwrappedExceptionType.getClassLoader() != null
               && !exceptionType.getClassLoader().equals(getCallingLoader()))
      {
         if (ClassLoaders.containsClass(getCallingLoader(), exceptionType))
         {
            return false;
         }
      }
      return true;
   }

   private boolean returnTypeNeedsEnhancement(Method method, Class<?> methodReturnType,
            Class<?> unwrappedReturnValueType)
   {

      String key = getReturnTypeNeedsEnhancementCacheKey(methodReturnType, unwrappedReturnValueType);
      Boolean result = returnTypeNeedsEnhancementCache.get(key);

      if (result == null)
      {
         result = true;
         if (Proxies.isPassthroughType(unwrappedReturnValueType))
         {
            result = false;
         }
         else if (!Object.class.equals(methodReturnType) && Proxies.isPassthroughType(methodReturnType))
         {
            result = false;
         }
         else if (unwrappedReturnValueType.getClassLoader() != null
                  && !unwrappedReturnValueType.getClassLoader().equals(getCallingLoader()))
         {
            if (ClassLoaders.containsClass(getCallingLoader(), unwrappedReturnValueType)
                     && ClassLoaders.containsClass(getCallingLoader(), methodReturnType))
            {
               result = false;
            }
         }
         returnTypeNeedsEnhancementCache.put(key, result);
      }

      return result;

   }

   private String getReturnTypeNeedsEnhancementCacheKey(Class<?> methodReturnType, Class<?> unwrappedReturnValueType)
   {
      return getCallingLoader() + "-" + methodReturnType.getClassLoader() + "-" + methodReturnType.getName()
               + unwrappedReturnValueType.getClassLoader() + "" + unwrappedReturnValueType.getName();
   }

   private static boolean whitelistContainsAll(Callable<Set<ClassLoader>> whitelist, ClassLoader... classLoaders)
   {
      try
      {
         final Set<ClassLoader> set = whitelist.call();
         for (final ClassLoader classLoader : classLoaders)
         {
            if (!set.contains(classLoader))
               return false;
         }
         return true;
      }
      catch (final Exception e)
      {
         throw new RuntimeException("Could not retrieve ClassLoader whitelist from callback [" + whitelist + "].", e);
      }
   }

   private Class<?> loadClassFromWhitelist(String typeName) throws ClassNotFoundException
   {
      Class<?> result;

      Set<ClassLoader> loaders;
      try
      {
         loaders = whitelist.call();
      }
      catch (final Exception e)
      {
         throw new RuntimeException("Could not retrieve ClassLoader whitelist from callback [" + whitelist + "].", e);
      }

      for (final ClassLoader loader : loaders)
      {
         try
         {
            result = loader.loadClass(typeName);
            return result;
         }
         catch (final Exception e)
         {
            // next!
         }
      }

      throw new ClassNotFoundException(typeName);
   }

   private List<Object> enhanceParameterValues(final Object[] args, Method delegateMethod) throws Exception
   {
      final List<Object> parameterValues = new ArrayList<>();
      for (int i = 0; i < delegateMethod.getParameterTypes().length; i++)
      {
         final Class<?> delegateParameterType = delegateMethod.getParameterTypes()[i];
         final Object parameterValue = args[i];

         final Object enhancedParameterValue = enhanceSingleParameterValue(delegateMethod, delegateParameterType,
                  stripClassLoaderAdapters(parameterValue));
         parameterValues.add(enhancedParameterValue);
      }
      return parameterValues;
   }

   private Object enhanceSingleParameterValue(final Method delegateMethod, final Class<?> delegateParameterType,
            final Object parameterValue) throws Exception
   {
      if (parameterValue != null)
      {
         if (parameterValue instanceof Class<?>)
         {
            final Class<?> paramClassValue = (Class<?>) parameterValue;
            Class<?> loadedClass = null;
            try
            {
               loadedClass = delegateLoader.loadClass(Proxies.unwrapProxyClassName(paramClassValue));
            }
            catch (final ClassNotFoundException e)
            {
               try
               {
                  // If all else fails, try the whitelist loaders.
                  loadedClass = loadClassFromWhitelist(Proxies.unwrapProxyClassName(paramClassValue));
               }
               catch (final ClassNotFoundException e3)
               {
                  // Oh well.
               }

               if (loadedClass == null)
               {
                  /*
                   * No way, here is the original class and god bless you :) Also unwrap any proxy types since we don't
                   * know about this object, there is no reason to pass a proxied class type.
                   */
                  loadedClass = Proxies.unwrapProxyTypes(paramClassValue);
               }
            }
            return loadedClass;
         }
         else
         {
            final Object unwrappedValue = stripClassLoaderAdapters(parameterValue);
            if (delegateParameterType.isAssignableFrom(unwrappedValue.getClass())
                     && !Proxies.isLanguageType(unwrappedValue.getClass())
                     && (!isEquals(delegateMethod) || (isEquals(delegateMethod) && ClassLoaders.containsClass(
                              delegateLoader, unwrappedValue.getClass()))))
            {
               // https://issues.jboss.org/browse/FORGE-939
               return unwrappedValue;
            }
            else
            {
               final Class<?> unwrappedValueType = Proxies.unwrapProxyTypes(unwrappedValue.getClass(), delegateMethod
                        .getDeclaringClass().getClassLoader(), getCallingLoader(),
                        delegateLoader, unwrappedValue.getClass()
                                 .getClassLoader());

               ClassLoader valueDelegateLoader = delegateLoader;
               final ClassLoader methodLoader = delegateMethod.getDeclaringClass().getClassLoader();
               if (methodLoader != null && ClassLoaders.containsClass(methodLoader, unwrappedValueType))
               {
                  valueDelegateLoader = methodLoader;
               }

               ClassLoader valueCallingLoader = getCallingLoader();
               final ClassLoader unwrappedValueLoader = unwrappedValueType.getClassLoader();
               if (unwrappedValueLoader != null && !ClassLoaders.containsClass(getCallingLoader(), unwrappedValueType))
               {
                  valueCallingLoader = unwrappedValueLoader;
               }

               // If it is a class, use the delegateLoader loaded version

               if (delegateParameterType.isPrimitive())
               {
                  return parameterValue;
               }
               else if (delegateParameterType.isEnum())
               {
                  return enhanceEnum(methodLoader, parameterValue);
               }
               else if (delegateParameterType.isArray())
               {
                  final Object[] array = (Object[]) unwrappedValue;
                  final Object[] delegateArray = (Object[]) Array.newInstance(delegateParameterType.getComponentType(),
                           array.length);
                  for (int j = 0; j < array.length; j++)
                  {
                     delegateArray[j] = enhanceSingleParameterValue(delegateMethod,
                              delegateParameterType.getComponentType(), stripClassLoaderAdapters(array[j]));
                  }
                  return delegateArray;
               }
               else if (delegateParameterType == Optional.class)
               {
                  Optional<?> optionalResult = ((Optional<?>) parameterValue);
                  if (optionalResult.isPresent())
                  {
                     Object nestedResult = optionalResult.get();
                     Class<?> parameterClass = nestedResult.getClass();
                     if (!Proxies.isPassthroughType(parameterClass) && !Proxies.isLanguageType(parameterClass))
                     {
                        Class<?>[] compatibleClassHierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(
                                 delegateLoader,
                                 Proxies.unwrapProxyTypes(parameterClass, getCallingLoader(), delegateLoader));
                        nestedResult = enhance(whitelist, valueDelegateLoader, valueCallingLoader,
                                 nestedResult,
                                 compatibleClassHierarchy);
                     }
                     return Optional.of(nestedResult);
                  }
                  else
                  {
                     return Optional.empty();
                  }

               }
               else
               {
                  final Class<?> parameterType = parameterValue.getClass();
                  if ((!Proxies.isPassthroughType(delegateParameterType)
                           && Proxies.isLanguageType(delegateParameterType))
                           || !delegateParameterType.isAssignableFrom(parameterType)
                           || isEquals(delegateMethod))
                  {
                     Class<?>[] compatibleClassHierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(
                              valueDelegateLoader, unwrappedValueType);

                     if (compatibleClassHierarchy.length == 0)
                     {
                        compatibleClassHierarchy = new Class[] { delegateParameterType };
                     }

                     Object delegateObject = parameterValue;
                     if (parameterValue instanceof ForgeProxy)
                     {
                        Object forgeProxy = Proxies.getForgeProxyHandler(parameterValue);
                        if (forgeProxy instanceof ClassLoaderAdapterCallback)
                        {
                           final ClassLoaderAdapterCallback handler = (ClassLoaderAdapterCallback) forgeProxy;
                           if (handler.getCallingLoader().equals(getCallingLoader())
                                    && handler.getDelegateLoader().equals(getDelegateLoader())
                                    && delegateParameterType.isAssignableFrom(unwrappedValue.getClass()))
                           {
                              delegateObject = unwrappedValue;
                           }
                        }
                     }

                     final Object delegateParameterValue = enhance(whitelist, valueDelegateLoader, valueCallingLoader,
                              delegateObject,
                              compatibleClassHierarchy);

                     return delegateParameterValue;
                  }
                  else
                  {
                     return unwrappedValue;
                  }
               }
            }
         }
      }
      return null;
   }

   private static boolean isEquals(Method method)
   {
      if (boolean.class.equals(method.getReturnType())
               && "equals".equals(method.getName())
               && method.getParameterTypes().length == 1
               && Object.class.equals(method.getParameterTypes()[0]))
         return true;
      return false;
   }

   private static boolean isHashCode(Method method)
   {
      if (int.class.equals(method.getReturnType())
               && "hashCode".equals(method.getName())
               && method.getParameterTypes().length == 0)
         return true;
      return false;
   }

   private static boolean isAutoCloseableClose(Method method)
   {
      if (void.class.equals(method.getReturnType())
               && "close".equals(method.getName())
               && method.getParameterTypes().length == 0)
         return true;
      return false;
   }

   private List<Class<?>> translateParameterTypes(final Method method) throws ClassNotFoundException
   {
      final List<Class<?>> parameterTypes = new ArrayList<>();
      for (int i = 0; i < method.getParameterTypes().length; i++)
      {
         final Class<?> parameterType = method.getParameterTypes()[i];

         if (parameterType.isPrimitive())
         {
            parameterTypes.add(parameterType);
         }
         else
         {
            final Class<?> delegateParameterType = delegateLoader.loadClass(parameterType.getName());
            parameterTypes.add(delegateParameterType);
         }
      }
      return parameterTypes;
   }

   private static Class<?> getCachedProxyType(ClassLoader callingLoader, ClassLoader delegateLoader, Class<?> type)
   {
      Class<?> proxyType = null;
      Map<String, WeakReference<Class<?>>> cache = classCache.get(getClassCacheKey(callingLoader, delegateLoader));
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

   private static void setCachedProxyType(ClassLoader callingLoader, ClassLoader delegateLoader, Class<?> type,
            Class<?> proxyType)
   {
      String key = getClassCacheKey(callingLoader, delegateLoader);
      Map<String, WeakReference<Class<?>>> cache = classCache.get(key);
      if (cache == null)
      {
         cache = new ConcurrentHashMap<>();

         classCache.put(key, cache);
      }
      cache.put(type.getName(), new WeakReference<Class<?>>(proxyType));
   }

   private static String getClassCacheKey(ClassLoader callingLoader, ClassLoader delegateLoader)
   {
      return callingLoader.toString() + delegateLoader.toString();
   }

   static <T> T enhance(Callable<Set<ClassLoader>> whitelist, final ClassLoader callingLoader,
            final ClassLoader delegateLoader,
            final Object delegate,
            final Class<?>... types)
   {
      return enhance(whitelist, callingLoader, delegateLoader, null, delegate, types);
   }

   @SuppressWarnings("unchecked")
   private static <T> T enhance(
            final Callable<Set<ClassLoader>> whitelist,
            final ClassLoader callingLoader,
            final ClassLoader delegateLoader,
            final Method sourceMethod,
            final Object delegate, final Class<?>... types)
   {
      if (whitelistContainsAll(whitelist, callingLoader, delegateLoader))
         return (T) delegate;

      // TODO consider removing option to set type hierarchy here. Instead it might just be
      // best to use type inspection of the given initialCallingLoader ClassLoader to figure out the proper type.
      final Class<?> delegateType = stripClassLoaderInterceptors(delegate).getClass();

      try
      {
         return ClassLoaders.executeIn(JAVASSIST_LOADER, new Callable<T>()
         {
            @Override
            public T call() throws Exception
            {
               try
               {
                  Class<?> proxyType = getCachedProxyType(callingLoader, delegateLoader, delegateType);
                  if (proxyType == null)
                  {
                     Class<?>[] hierarchy = null;
                     if (types == null || types.length == 0)
                     {
                        hierarchy = ProxyTypeInspector.getCompatibleClassHierarchy(callingLoader, delegateType);
                        if (hierarchy == null || hierarchy.length == 0)
                        {
                           Logger.getLogger(getClass().getName()).fine(
                                    "Must specify at least one non-final type to enhance for Object: "
                                             + delegate + " of type " + delegate.getClass());

                           return (T) delegate;
                        }
                     }
                     else
                        hierarchy = Arrays.copy(types, new Class<?>[types.length]);

                     final ProxyFactory f = new ProxyFactory()
                     {
                        @Override
                        protected ClassLoader getClassLoader0()
                        {
                           ClassLoader result = callingLoader;
                           if (!ClassLoaders.containsClass(result, ProxyObject.class))
                              result = super.getClassLoader0();
                           return result;
                        };
                     };

                     f.setUseCache(true);

                     final Class<?> first = hierarchy[0];
                     if (!first.isInterface())
                     {
                        f.setSuperclass(Proxies.unwrapProxyTypes(first, callingLoader, delegateLoader));
                        hierarchy = Arrays.shiftLeft(hierarchy, new Class<?>[hierarchy.length - 1]);
                     }

                     final int index = Arrays.indexOf(hierarchy, ProxyObject.class);
                     if (index >= 0)
                     {
                        hierarchy = Arrays.removeElementAtIndex(hierarchy, index);
                     }

                     if (!Proxies.isProxyType(first) && !Arrays.contains(hierarchy, ForgeProxy.class))
                        hierarchy = Arrays.append(hierarchy, ForgeProxy.class);

                     if (hierarchy.length > 0)
                        f.setInterfaces(hierarchy);

                     final Class<?>[] finalHierarchy = hierarchy;
                     final MethodFilter filter = new MethodFilter()
                     {
                        @Override
                        public boolean isHandled(Method method)
                        {
                           Class<?> declaringClass = method.getDeclaringClass();

                           if (!declaringClass.getName().contains("java.lang")
                                    || !Proxies.isPassthroughType(declaringClass)
                                    || isToString(method)
                                    || isEquals(method)
                                    || isHashCode(method)
                                    || isAutoCloseableClose(method)
                                    || Arrays.contains(finalHierarchy, declaringClass)
                                    || ((!Object.class.equals(declaringClass)) && ProxyTypeInspector
                                             .superclassHierarchyContains(f.getSuperclass(), declaringClass)))
                              return true;
                           return false;
                        }

                        private boolean isToString(Method method)
                        {
                           return "toString".equals(method.getName()) && method.getParameterTypes().length == 0;
                        }
                     };

                     f.setFilter(filter);
                     proxyType = f.createClass();

                     setCachedProxyType(callingLoader, delegateLoader, delegateType, proxyType);
                  }

                  Object enhancedResult = proxyType.newInstance();

                  try
                  {
                     ((ProxyObject) enhancedResult)
                              .setHandler(new ClassLoaderAdapterCallback(whitelist, callingLoader, delegateLoader,
                                       delegate));
                  }
                  catch (final ClassCastException e)
                  {
                     final Class<?>[] interfaces = enhancedResult.getClass().getInterfaces();
                     for (final Class<?> javassistType : interfaces)
                     {
                        if (ProxyObject.class.getName().equals(javassistType.getName())
                                 || Proxy.class.getName().equals(javassistType.getName()))
                        {
                           final String callbackClassName = ClassLoaderAdapterCallback.class.getName();
                           final ClassLoader javassistLoader = javassistType.getClassLoader();
                           final Constructor<?> callbackConstructor = javassistLoader.loadClass(callbackClassName)
                                    .getConstructors()[0];

                           final Class<?> typeArgument = javassistLoader.loadClass(MethodHandler.class.getName());
                           final Method setHandlerMethod = javassistType.getMethod("setHandler", typeArgument);
                           setHandlerMethod.invoke(enhancedResult,
                                    callbackConstructor.newInstance(whitelist, callingLoader, delegateLoader,
                                             delegate));
                        }
                     }
                  }

                  return (T) enhancedResult;
               }
               catch (final Exception e)
               {
                  // Added try/catch for debug breakpoint purposes only.
                  throw e;
               }
            }

         });
      }
      catch (final Exception e)
      {
         throw new ContainerException("Failed to create proxy for type [" + delegateType + "]", e);
      }
   }

   @Override
   public Object getDelegate() throws Exception
   {
      return delegate;
   }

   @Override
   public Object getHandler() throws Exception
   {
      return this;
   }

   public ClassLoader getDelegateLoader()
   {
      return delegateLoader;
   }

   public ClassLoader getInitialCallingLoader()
   {
      return initialCallingLoader;
   }
}

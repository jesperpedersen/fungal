/*
 * The Fungal kernel project
 * Copyright (C) 2010
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.github.fungal.impl;

import com.github.fungal.deployment.BeanType;
import com.github.fungal.deployment.ConstructorType;
import com.github.fungal.deployment.DependsType;
import com.github.fungal.deployment.EntryType;
import com.github.fungal.deployment.IncallbackType;
import com.github.fungal.deployment.InjectType;
import com.github.fungal.deployment.InstallType;
import com.github.fungal.deployment.ListType;
import com.github.fungal.deployment.MapType;
import com.github.fungal.deployment.NullType;
import com.github.fungal.deployment.ParameterType;
import com.github.fungal.deployment.PropertyType;
import com.github.fungal.deployment.SetType;
import com.github.fungal.deployment.ThisType;
import com.github.fungal.deployment.UncallbackType;
import com.github.fungal.deployment.UninstallType;
import com.github.fungal.deployment.Unmarshaller;
import com.github.fungal.deployment.ValueType;
import com.github.fungal.spi.deployers.CloneableDeployer;
import com.github.fungal.spi.deployers.DeployException;
import com.github.fungal.spi.deployers.Deployer;
import com.github.fungal.spi.deployers.DeployerPhases;
import com.github.fungal.spi.deployers.Deployment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The deployment deployer (deploys .xml files)
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public final class DeploymentDeployer implements CloneableDeployer
{
   /** The logger */
   private Logger log = Logger.getLogger(DeploymentDeployer.class.getName());

   /** Trace logging enabled */
   private boolean trace = log.isLoggable(Level.FINEST);

   /** The kernel */
   private KernelImpl kernel;

   /**
    * Constructor
    * @param kernel The kernel
    */
   public DeploymentDeployer(KernelImpl kernel)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Kernel is null");

      this.kernel = kernel;
   }

   /**
    * Deploy
    * @param url The URL
    * @param parent The parent classloader
    * @return The deployment; or null if no deployment was made
    * @exception DeployException Thrown if an error occurs during deployment
    */
   public Deployment deploy(URL url, ClassLoader parent) throws DeployException
   {
      if (url == null || !url.toString().endsWith(".xml"))
         return null;

      DeployException deployException = null;
      try
      {
         Unmarshaller deploymentU = new Unmarshaller();
         com.github.fungal.deployment.Deployment deployment = 
            deploymentU.unmarshal(url);

         if (deployment != null && deployment.getBean().size() > 0)
         {
            for (BeanType bt : deployment.getBean())
            {
               kernel.setBeanStatus(bt.getName(), ServiceLifecycle.NOT_STARTED);
            }

            kernel.beansRegistered();

            List<BeanDeployer> deployers = new ArrayList<BeanDeployer>(deployment.getBean().size());
            List<String> beans = Collections.synchronizedList(new ArrayList<String>(deployment.getBean().size()));
            Map<String, List<Method>> uninstall = 
               new ConcurrentHashMap<String, List<Method>>(deployment.getBean().size());
            Set<String> ignoreStops = Collections.synchronizedSet(new HashSet<String>(deployment.getBean().size()));
            Set<String> ignoreDestroys = Collections.synchronizedSet(new HashSet<String>(deployment.getBean().size()));

            final CountDownLatch beansLatch = new CountDownLatch(deployment.getBean().size());

            for (BeanType bt : deployment.getBean())
            {
               BeanDeployer deployer = new BeanDeployer(bt, beans, uninstall, ignoreStops, ignoreDestroys, kernel,
                                                        beansLatch, parent, log);
               deployers.add(deployer);

               kernel.getExecutorService().submit(deployer);
            }

            beansLatch.await();

            Iterator<BeanDeployer> it = deployers.iterator();
            while (deployException == null && it.hasNext())
            {
               BeanDeployer deployer = it.next();
               if (deployer.getDeployException() != null)
                  deployException = deployer.getDeployException();
            }

            if (deployException == null)
               return new BeanDeployment(url, beans, uninstall, ignoreStops, ignoreDestroys, kernel);
         }
      }
      catch (Throwable t)
      {
         log.log(Level.SEVERE, t.getMessage(), t);
         throw new DeployException("Deployment " + url + " failed", t);
      }

      if (deployException != null)
         throw new DeployException("Deployment " + url + " failed", deployException);

      return null;
   }

   /**
    * Bean deployer
    */
   static class BeanDeployer implements Runnable
   {
      /** Supported types by parameters/properties */
      private static final Set<Class<?>> SUPPORTED_TYPES = new HashSet<Class<?>>(19);

      /** The bean */
      private BeanType bt;

      /** The bean names */
      private List<String> beans;

      /** Uninstall methods */
      private Map<String, List<Method>> uninstall;

      /** Ignore stop */
      private Set<String> ignoreStops;

      /** Ignore destroy */
      private Set<String> ignoreDestroys;

      /** The kernel */
      private KernelImpl kernel;

      /** The bean latch */
      private CountDownLatch beansLatch;

      /** The classloader */
      private ClassLoader classLoader;

      /** The log */
      private Logger log;

      /** DeployException */
      private DeployException deployException;

      static
      {
         SUPPORTED_TYPES.add(String.class);
         SUPPORTED_TYPES.add(byte.class);
         SUPPORTED_TYPES.add(Byte.class);
         SUPPORTED_TYPES.add(short.class);
         SUPPORTED_TYPES.add(Short.class);
         SUPPORTED_TYPES.add(int.class);
         SUPPORTED_TYPES.add(Integer.class);
         SUPPORTED_TYPES.add(long.class);
         SUPPORTED_TYPES.add(Long.class);
         SUPPORTED_TYPES.add(float.class);
         SUPPORTED_TYPES.add(Float.class);
         SUPPORTED_TYPES.add(double.class);
         SUPPORTED_TYPES.add(Double.class);
         SUPPORTED_TYPES.add(boolean.class);
         SUPPORTED_TYPES.add(Boolean.class);
         SUPPORTED_TYPES.add(char.class);
         SUPPORTED_TYPES.add(Character.class);
         SUPPORTED_TYPES.add(InetAddress.class);
         SUPPORTED_TYPES.add(Class.class);
      }

      /**
       * Constructor
       * @param bt The bean
       * @param beans The list of bean names
       * @param uninstall Uninstall methods for beans
       * @param ignoreStops Ignore stop methods for beans
       * @param ignoreDestroys Ignore destroy methods for beans
       * @param kernel The kernel
       * @param beansLatch The beans latch
       * @param classLoader The class loader
       * @param log The logger
       */
      public BeanDeployer(BeanType bt, 
                          List<String> beans,
                          Map<String, List<Method>> uninstall,
                          Set<String> ignoreStops,
                          Set<String> ignoreDestroys,
                          KernelImpl kernel,
                          CountDownLatch beansLatch,
                          ClassLoader classLoader,
                          Logger log)
      {
         this.bt = bt;
         this.beans = beans;
         this.uninstall = uninstall;
         this.ignoreStops = ignoreStops;
         this.ignoreDestroys = ignoreDestroys;
         this.kernel = kernel;
         this.beansLatch = beansLatch;
         this.classLoader = classLoader;
         this.log = log;
         this.deployException = null;
      }

      /**
       * Run
       */
      public void run()
      {
         SecurityActions.setThreadContextClassLoader(classLoader);

         String beanName = bt.getName();
         try
         {
            if (kernel.getBean(beanName) == null)
            {
               CountDownLatch dependencies = getDependencies(bt);

               try
               {
                  if (dependencies != null)
                     dependencies.await();
               }
               catch (InterruptedException ie)
               {
                  Thread.interrupted();
               }

               kernel.setBeanStatus(beanName, ServiceLifecycle.STARTING);

               Object bean = createBean(bt, classLoader);

               kernel.addBean(beanName, bean); 
               beans.add(beanName);

               kernel.setBeanStatus(beanName, ServiceLifecycle.STARTED);
            }
            else
            {
               log.warning("Warning: A service with name " + beanName + " already exists");
            }
         }
         catch (Throwable t)
         {
            deployException = new DeployException("Installing bean " + beanName, t);
            kernel.setBeanStatus(beanName, ServiceLifecycle.ERROR);
            log.log(Level.SEVERE, "Installing bean " + beanName, t);
         }

         beansLatch.countDown();
      }

      /**
       * Get deploy exception
       * @return null if no error; otherwise the exception
       */
      public DeployException getDeployException()
      {
         return deployException;
      }

      /**
       * Get the dependencies for a bean
       * @paran bt The bean type
       * @return The count down latch for the dependencies; <code>null</code> if no dependencies
       * @exception DeployException Thrown if an error occurs
       */
      private CountDownLatch getDependencies(BeanType bt) throws DeployException
      {
         Set<String> deps = null;

         List<DependsType> dts = bt.getDepends();
         if (dts.size() > 0)
         {
            deps = new HashSet<String>(dts.size());
            for (DependsType dt : dts)
            {
               deps.add(dt.getValue());
            }
         }

         List<PropertyType> pts = bt.getProperty();
         if (pts.size() > 0)
         {
            for (PropertyType pt : pts)
            {
               Object element = pt.getContent().get(0);

               if (element != null && element instanceof InjectType)
               {
                  if (deps == null)
                     deps = new HashSet<String>(1);

                  InjectType it = (InjectType)element;
                  deps.add(it.getBean());
               }
            }
         }

         ConstructorType ct = bt.getConstructor();
         if (ct != null)
         {
            if (ct.getFactory() != null)
            {
               if (deps == null)
                  deps = new HashSet<String>(1);

               deps.add(ct.getFactory().getBean());
            }
            
            if (ct.getParameter() != null && ct.getParameter().size() > 0)
            {
               for (ParameterType pt : ct.getParameter())
               {
                  Object v = pt.getContent().get(0);
                  if (v instanceof InjectType)
                  {
                     if (deps == null)
                        deps = new HashSet<String>(1);

                     InjectType it = (InjectType)v;
                     deps.add(it.getBean());
                  }
               }
            }
         }

         if (deps != null && deps.size() > 0)
         {
            CountDownLatch cdl = new CountDownLatch(deps.size());

            for (String dependency : deps)
            {
               ServiceLifecycle dependencyStatus = kernel.getBeanStatus(dependency);
               
               if (dependencyStatus == null && kernel.isAllBeansRegistered())
                  throw new DeployException("Unknown dependency: " + dependency);

               kernel.addBeanDependants(bt.getName(), dependency, cdl);
            }

            return cdl;
         }

         return null;
      }

      /**
       * Create a bean
       * @param bt The bean type definition
       * @param cl The classloader
       * @return The new bean
       * @exception Throwable Thrown if an error occurs
       */
      @SuppressWarnings("unchecked") 
      private Object createBean(BeanType bt, ClassLoader cl) throws Throwable
      {
         Class<?> clz = null;
         Object instance = null;

         if (bt.getClazz() != null && bt.getConstructor() == null)
         {
            clz = Class.forName(bt.getClazz(), true, cl);

            Constructor<?> con = findConstructor(clz, null, cl);
            con.setAccessible(true);

            instance = con.newInstance();
         }
         else
         {
            ConstructorType ct = bt.getConstructor();

            Object factoryObject = null;
            Class<?> factoryClass = null;

            if (ct.getFactory() != null)
            {
               factoryObject = kernel.getBean(ct.getFactory().getBean());
               factoryClass = factoryObject.getClass();
            }
            else
            {
               String fcs = ct.getFactoryClass();

               if (fcs == null)
                  fcs = bt.getClazz();

               factoryClass = Class.forName(fcs, true, cl);
            }

            if (ct.getFactoryMethod() == null)
            {
               if (ct.getParameter() == null || ct.getParameter().size() == 0)
               {
                  Constructor<?> con = findConstructor(factoryClass, null, cl);
                  con.setAccessible(true);

                  instance = con.newInstance();
                  clz = instance.getClass();
               }
               else
               {
                  Constructor<?> factoryConstructor = findConstructor(factoryClass, ct.getParameter(), cl);
                  Object[] args = getArguments(ct.getParameter(), factoryConstructor.getParameterTypes(), cl);

                  factoryConstructor.setAccessible(true);
                  instance = factoryConstructor.newInstance(args);
                  clz = instance.getClass();
               }
            }
            else
            {
               Method factoryMethod = findMethod(factoryClass, ct.getFactoryMethod(), ct.getParameter(), cl);
               factoryMethod.setAccessible(true);

               if (ct.getParameter() == null || ct.getParameter().size() == 0)
               {
                  instance = factoryMethod.invoke(factoryObject);
                  clz = instance.getClass();
               }
               else
               {
                  Object[] args = getArguments(ct.getParameter(), factoryMethod.getParameterTypes(), cl);
                  instance = factoryMethod.invoke(factoryObject, args);
                  clz = instance.getClass();
               }
            }
         }

         // Bean properties
         if (bt.getProperty() != null)
         {
            for (PropertyType pt : bt.getProperty())
            {
               setBeanProperty(instance, pt, cl);
            }
         }

         if (bt.getIgnoreCreate() == null)
         {
            try
            {
               Method createMethod = clz.getMethod("create", (Class[])null);
               createMethod.setAccessible(true);
               createMethod.invoke(instance);
            }
            catch (NoSuchMethodException nsme)
            {
               // No create method
            }
            catch (InvocationTargetException ite)
            {
               throw ite.getTargetException();
            }
         }

         if (bt.getIgnoreStart() == null)
         {
            try
            {
               Method startMethod = clz.getMethod("start", (Class[])null);
               startMethod.setAccessible(true);
               startMethod.invoke(instance);
            }
            catch (NoSuchMethodException nsme)
            {
               // No start method
            }
            catch (InvocationTargetException ite)
            {
               throw ite.getTargetException();
            }
         }

         if (bt.getIgnoreStop() != null)
            ignoreStops.add(bt.getName());

         if (bt.getIgnoreDestroy() != null)
            ignoreDestroys.add(bt.getName());

         // Invoke install methods
         if (bt.getInstall() != null && bt.getInstall().size() > 0)
         {
            for (InstallType it : bt.getInstall())
            {
               try
               {
                  Method method = clz.getMethod(it.getMethod(), (Class[])null);
                  method.setAccessible(true);
                  method.invoke(instance);
               }
               catch (InvocationTargetException ite)
               {
                  throw ite.getTargetException();
               }
            }
         }

         // Register uninstall methods
         if (bt.getUninstall() != null && bt.getUninstall().size() > 0)
         {
            List<Method> methods = new ArrayList<Method>(bt.getUninstall().size());
            for (UninstallType ut : bt.getUninstall())
            {
               try
               {
                  Method method = clz.getMethod(ut.getMethod(), (Class[])null);
                  method.setAccessible(true);
                  methods.add(method);
               }
               catch (NoSuchMethodException nsme)
               {
                  throw new Exception("Unknown uninstall method:" + ut.getMethod());
               }
            }
            uninstall.put(bt.getName(), methods);
         }

         // Register incallback methods
         if (bt.getIncallback() != null && bt.getIncallback().size() > 0)
         {
            for (IncallbackType it : bt.getIncallback())
            {
               List<Method> candidates = new ArrayList<Method>(1);
               Method[] methods = clz.getMethods();

               for (Method m : methods)
               {
                  if (m.getName().equals(it.getMethod()) && m.getParameterTypes().length == 1)
                     candidates.add(m);
               }

               if (candidates.size() > 0)
               {
                  Method method = candidates.get(0);
                  method.setAccessible(true);

                  Class<?> parameter = method.getParameterTypes()[0];

                  Callback cb = new Callback(parameter, method, instance);

                  kernel.registerIncallback(cb);
               }
            }
         }

         // Register uncallback methods
         if (bt.getUncallback() != null && bt.getUncallback().size() > 0)
         {
            for (UncallbackType ut : bt.getUncallback())
            {
               List<Method> candidates = new ArrayList<Method>(1);
               Method[] methods = clz.getMethods();

               for (Method m : methods)
               {
                  if (m.getName().equals(ut.getMethod()) && m.getParameterTypes().length == 1)
                     candidates.add(m);
               }

               if (candidates.size() > 0)
               {
                  Method method = candidates.get(0);
                  method.setAccessible(true);

                  Class<?> parameter = method.getParameterTypes()[0];

                  Callback cb = new Callback(parameter, method, instance);

                  kernel.registerUncallback(cb);
               }
            }
         }

         // Register deployer
         if (instance instanceof Deployer)
         {
            ((MainDeployerImpl)kernel.getMainDeployer()).addDeployer((Deployer)instance);
         }

         // Register deployer phases
         if (instance instanceof DeployerPhases)
         {
            kernel.addDeployerPhasesBean(bt.getName());
         }

         return instance;
      }

      /**
       * Find constructor
       * @param clz The class
       * @param parameters The list of parameters
       * @param cl The class loader
       * @return The constructor
       * @exception Throwable Thrown if a constructor cannot be found
       */
      @SuppressWarnings("unchecked") 
      private Constructor<?> findConstructor(Class<?> clz, List<ParameterType> parameters, ClassLoader cl)
         throws Throwable
      {
         if (parameters == null || parameters.size() == 0)
         {
            Class<?> constructorClass = clz;

            while (constructorClass != null)
            {
               Constructor[] constructors = constructorClass.getDeclaredConstructors();

               if (constructors != null)
               {
                  for (int i = 0; i < constructors.length; i++)
                  {
                     Constructor<?> con = constructors[i];
                     if (con.getParameterTypes().length == 0)
                        return con;
                  }
               }

               constructorClass = constructorClass.getSuperclass();
            }
         }
         else
         {
            Class<?> constructorClass = clz;

            while (constructorClass != null)
            {
               Constructor[] constructors = constructorClass.getDeclaredConstructors();

               for (Constructor<?> c : constructors)
               {
                  if (parameters.size() == c.getParameterTypes().length)
                  {
                     boolean include = true;

                     for (int i = 0; include && i < parameters.size(); i++)
                     {
                        ParameterType pt = parameters.get(i);
                        Class<?> parameterClass = c.getParameterTypes()[i];

                        if (pt.getClazz() == null)
                        {
                           if ((!(pt.getContent().get(0) instanceof InjectType)) &&
                               (!(pt.getContent().get(0) instanceof NullType)))
                              if (!SUPPORTED_TYPES.contains(parameterClass))
                                 include = false;
                        }
                        else
                        {
                           Class<?> pClz = Class.forName(pt.getClazz(), true, cl);

                           if (!parameterClass.equals(pClz))
                              include = false;
                        }
                     }

                     if (include)
                        return c;
                  }
               }

               constructorClass = constructorClass.getSuperclass();
            }
         }

         throw new Exception("Unable to find constructor for " + clz.getName());
      }

      /**
       * Find method
       * @param clz The class
       * @param name The method name
       * @param parameters The list of parameters
       * @param cl The class loader
       * @return The constructor
       * @exception Throwable Thrown if a constructor cannot be found
       */
      @SuppressWarnings("unchecked") 
      private Method findMethod(Class<?> clz, String name, List<ParameterType> parameters, ClassLoader cl)
         throws Throwable
      {
         if (parameters == null || parameters.size() == 0)
         {
            Class<?> methodClass = clz;

            while (methodClass != null)
            {
               Method[] methods = methodClass.getDeclaredMethods();

               if (methods != null)
               {
                  for (int i = 0; i < methods.length; i++)
                  {
                     Method method = methods[i];
                     if (name.equals(method.getName()) && method.getParameterTypes().length == 0)
                        return method;
                  }
               }

               methodClass = methodClass.getSuperclass();
            }
         }
         else
         {
            Class<?> methodClass = clz;

            while (methodClass != null)
            {
               Method[] methods = methodClass.getDeclaredMethods();

               for (Method m : methods)
               {
                  if (m.getName().equals(name))
                  {
                     if (parameters.size() == m.getParameterTypes().length)
                     {
                        boolean include = true;

                        for (int i = 0; include && i < parameters.size(); i++)
                        {
                           ParameterType pt = parameters.get(i);
                           Class<?> parameterClass = m.getParameterTypes()[i];

                           if (pt.getClazz() == null)
                           {
                              if ((!(pt.getContent().get(0) instanceof InjectType)) &&
                                  (!(pt.getContent().get(0) instanceof NullType)))
                                 if (!SUPPORTED_TYPES.contains(parameterClass))
                                    include = false;
                           }
                           else
                           {
                              Class<?> pClz = Class.forName(pt.getClazz(), true, cl);
                           
                              if (!parameterClass.equals(pClz))
                                 include = false;
                           }
                        }

                        if (include)
                           return m;
                     }
                  }
               }

               methodClass = methodClass.getSuperclass();
            }
         }

         throw new Exception("Unable to find method (" + name + "[" + parameters + "]) in " + clz.getName());
      }

      /**
       * Get the argument values
       * @param definitions The argument definitions
       * @param types The argument types
       * @param cl The class loader
       * @return The values
       * @exception Throwable Thrown if an error occurs
       */
      private Object[] getArguments(List<ParameterType> definitions, Class<?>[] types, ClassLoader cl)
         throws Throwable
      {
         if (definitions == null || definitions.size() == 0)
            return null;

         Object[] args = new Object[types.length];

         Injection injection = new Injection();
         for (int i = 0; i < definitions.size(); i++)
         {
            ParameterType parameter = definitions.get(i);

            Object v = parameter.getContent().get(0);

            if (v instanceof InjectType)
            {
               args[i] = getInjectValue((InjectType)v);
            }
            else if (v instanceof NullType)
            {
               args[i] = null;
            }
            else
            {
               args[i] = injection.getValue(parameter.toString(), types[i], (String)v, cl);
            }
         }

         return args;
      }

      /**
       * Get inject value
       * @param it The inject type
       * @return The value
       * @exception Exception If the injection bean cannot be resolved or if an error occurs
       */
      private Object getInjectValue(InjectType it) throws Exception
      {
         Object injectionObject = kernel.getBean(it.getBean());

         if (injectionObject == null)
            throw new Exception("Required dependency " + it.getBean() + " not found");

         if (it.getProperty() != null)
         {
            Method method = null;
            Field field = null;

            String baseName = it.getProperty().substring(0, 1).toUpperCase(Locale.US);

            if (it.getProperty().length() > 1)
               baseName += it.getProperty().substring(1);

            try
            {
               String getMethodName = "get" + baseName;
               method = injectionObject.getClass().getMethod(getMethodName, (Class[])null);
            }
            catch (NoSuchMethodException nsme)
            {
               try
               {
                  String isMethodName = "is" + baseName;
                  method = injectionObject.getClass().getMethod(isMethodName, (Class[])null);
               }
               catch (NoSuchMethodException insme)
               {
                  field = injectionObject.getClass().getField(it.getProperty());
               }
            }

            if (method != null)
            {
               method.setAccessible(true);
               return method.invoke(injectionObject);
            }
            else
            {
               field.setAccessible(true);
               return field.get(injectionObject);
            }
         }
         else
         {
            return injectionObject;
         }
      }

      /**
       * Set a property on an object instance
       * @param instance The object instance
       * @param pt The property type definition
       * @param cl The classloader
       * @exception Exception Thrown if an error occurs
       */
      @SuppressWarnings("unchecked") 
      private void setBeanProperty(Object instance, PropertyType pt, ClassLoader cl) throws Exception
      {
         Injection injection = new Injection();

         String name = "set" + pt.getName().substring(0, 1).toUpperCase(Locale.US);
         if (pt.getName().length() > 1)
            name += pt.getName().substring(1);

         Method m = injection.findMethod(instance.getClass(), name, pt.getClazz());
      
         if (m == null)
            throw new Exception("Property " + pt.getName() + " not found on " + instance.getClass().getName());

         m.setAccessible(true);

         Class<?> parameterClass = m.getParameterTypes()[0];
      
         Object parameterValue = null;
         Object element = pt.getContent().get(0);

         if (element == null)
            element = "";

         if (element instanceof InjectType)
         {
            parameterValue = getInjectValue((InjectType)element);
         }
         else if (element instanceof MapType)
         {
            MapType mt = (MapType)element;

            Map<Object, Object> map = null;
            
            if (mt.getClazz() == null)
            {
               map = new HashMap<Object, Object>(mt.getEntry().size());
            }
            else
            {
               Class<?> mapClass = Class.forName(mt.getClazz(), true, cl);

               if (mt.getClazz().equals("java.util.HashMap") ||
                   mt.getClazz().equals("java.util.Hashtable") ||
                   mt.getClazz().equals("java.util.LinkedHashMap") ||
                   mt.getClazz().equals("java.util.WeakHashMap"))
               {
                  Constructor<?> con = mapClass.getConstructor(int.class);
                  con.setAccessible(true);
                  map = (Map)con.newInstance(mt.getEntry().size());
               }
               else
               {
                  map = (Map)mapClass.newInstance();
               }
            }

            Class<?> keyClass = Class.forName(mt.getKeyClass(), true, cl);
            Class<?> valueClass = Class.forName(mt.getValueClass(), true, cl);

            for (EntryType et : mt.getEntry())
            {
               Object key = injection.getValue(et.toString(), keyClass, et.getKey().getValue(), cl);
               Object value = injection.getValue(et.toString(), valueClass, et.getValue().getValue(), cl);

               map.put(key, value);
            }

            parameterValue = map;
         }
         else if (element instanceof ListType)
         {
            ListType lt = (ListType)element;

            List<Object> list = null;
            
            if (lt.getClazz() == null)
            {
               list = new ArrayList<Object>(lt.getValue().size());
            }
            else
            {
               Class<?> listClass = Class.forName(lt.getClazz(), true, cl);

               if (lt.getClazz().equals("java.util.ArrayList") ||
                   lt.getClazz().equals("java.util.Vector"))
               {
                  Constructor<?> con = listClass.getConstructor(int.class);
                  con.setAccessible(true);
                  list = (List)con.newInstance(lt.getValue().size());
               }
               else
               {
                  list = (List)listClass.newInstance();
               }
            }

            Class<?> elementClass = Class.forName(lt.getElementClass(), true, cl);

            for (ValueType vt : lt.getValue())
            {
               Object value = injection.getValue(vt.toString(), elementClass, vt.getValue(), cl);
               list.add(value);
            }

            parameterValue = list;
         }
         else if (element instanceof SetType)
         {
            SetType st = (SetType)element;

            Set<Object> set = null;
            
            if (st.getClazz() == null)
            {
               set = new HashSet<Object>(st.getValue().size());
            }
            else
            {
               Class<?> setClass = Class.forName(st.getClazz(), true, cl);

               if (st.getClazz().equals("java.util.HashSet"))
               {
                  Constructor<?> con = setClass.getConstructor(int.class);
                  con.setAccessible(true);
                  set = (Set)con.newInstance(st.getValue().size());
               }
               else
               {
                  set = (Set)setClass.newInstance();
               }
            }

            Class<?> elementClass = Class.forName(st.getElementClass(), true, cl);

            for (ValueType vt : st.getValue())
            {
               Object value = injection.getValue(vt.toString(), elementClass, vt.getValue(), cl);
               set.add(value);
            }

            parameterValue = set;
         }
         else if (element instanceof NullType)
         {
            parameterValue = null;
         }
         else if (element instanceof ThisType)
         {
            parameterValue = instance;
         }
         else if (element instanceof ValueType)
         {
            parameterValue = injection.getValue(pt.getName(), parameterClass, ((ValueType)element).getValue(), cl);
         }
         else
         {
            parameterValue = injection.getValue(pt.getName(), parameterClass, (String)element, cl);
         }

         m.invoke(instance, parameterValue);
      }
   }

   /**
    * Clone
    * @return The copy of the object
    * @exception CloneNotSupportedException Thrown if a copy can't be created
    */
   public Deployer clone() throws CloneNotSupportedException
   {
      DeploymentDeployer dd = (DeploymentDeployer)super.clone();
      dd.kernel = kernel;

      return dd;
   }
}

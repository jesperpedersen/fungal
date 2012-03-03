/*
 * The Fungal kernel project
 * Copyright (C) 2012
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

import com.github.fungal.api.deployment.Bean;
import com.github.fungal.api.deployment.Depends;
import com.github.fungal.api.deployment.Entry;
import com.github.fungal.api.deployment.Incallback;
import com.github.fungal.api.deployment.Inject;
import com.github.fungal.api.deployment.Install;
import com.github.fungal.api.deployment.Null;
import com.github.fungal.api.deployment.Parameter;
import com.github.fungal.api.deployment.Property;
import com.github.fungal.api.deployment.This;
import com.github.fungal.api.deployment.Uncallback;
import com.github.fungal.api.deployment.Uninstall;
import com.github.fungal.api.deployment.Value;
import com.github.fungal.spi.deployers.DeployException;
import com.github.fungal.spi.deployers.Deployer;
import com.github.fungal.spi.deployers.DeployerPhases;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * Bean deployer
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
class BeanDeployer implements Runnable
{
   /** Supported types by parameters/properties */
   private static final java.util.Set<Class<?>> SUPPORTED_TYPES = new HashSet<Class<?>>(19);
   
   /** The bean */
   private Bean bt;
   
   /** The bean names */
   private java.util.List<String> beans;
   
   /** Uninstall methods */
   private java.util.Map<String, java.util.List<Method>> uninstall;
   
   /** Stop */
   private java.util.Map<String, String> stops;
   
   /** Destroy */
   private java.util.Map<String, String> destroys;
   
   /** Ignore stop */
   private java.util.Set<String> ignoreStops;
   
   /** Ignore destroy */
   private java.util.Set<String> ignoreDestroys;
   
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
    * @param stops Stop methods for beans
    * @param destroys Destroy methods for beans
    * @param ignoreStops Ignore stop methods for beans
    * @param ignoreDestroys Ignore destroy methods for beans
    * @param kernel The kernel
    * @param beansLatch The beans latch
    * @param classLoader The class loader
    * @param log The logger
    */
   public BeanDeployer(Bean bt, 
                       java.util.List<String> beans,
                       java.util.Map<String, java.util.List<Method>> uninstall,
                       java.util.Map<String, String> stops,
                       java.util.Map<String, String> destroys,
                       java.util.Set<String> ignoreStops,
                       java.util.Set<String> ignoreDestroys,
                       KernelImpl kernel,
                       CountDownLatch beansLatch,
                       ClassLoader classLoader,
                       Logger log)
   {
      this.bt = bt;
      this.beans = beans;
      this.uninstall = uninstall;
      this.stops = stops;
      this.destroys = destroys;
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
   private CountDownLatch getDependencies(Bean bt) throws DeployException
   {
      java.util.Set<String> deps = null;
      
      java.util.List<Depends> dts = bt.getDepends();
      if (dts.size() > 0)
      {
         deps = new HashSet<String>(dts.size());
         for (Depends dt : dts)
         {
            deps.add(dt.getValue());
         }
      }

      java.util.List<Property> pts = bt.getProperty();
      if (pts.size() > 0)
      {
         for (Property pt : pts)
         {
            Object element = pt.getContent().get(0);
            
            if (element != null && element instanceof Inject)
            {
               if (deps == null)
                  deps = new HashSet<String>(1);

               Inject it = (Inject)element;
               deps.add(it.getBean());
            }
         }
      }

      com.github.fungal.api.deployment.Constructor ct = bt.getConstructor();
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
            for (Parameter pt : ct.getParameter())
            {
               Object v = pt.getContent().get(0);
               if (v instanceof Inject)
               {
                  if (deps == null)
                     deps = new HashSet<String>(1);
                  
                  Inject it = (Inject)v;
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
   private Object createBean(Bean bt, ClassLoader cl) throws Throwable
   {
      Class<?> clz = null;
      Object instance = null;
      
      if (bt.getClazz() != null && bt.getConstructor() == null)
      {
         clz = Class.forName(bt.getClazz(), true, cl);
         
         java.lang.reflect.Constructor<?> con = findConstructor(clz, null, cl);
         con.setAccessible(true);
         
         instance = con.newInstance();
      }
      else
      {
         com.github.fungal.api.deployment.Constructor ct = bt.getConstructor();
         
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
               java.lang.reflect.Constructor<?> con = findConstructor(factoryClass, null, cl);
               con.setAccessible(true);

               instance = con.newInstance();
               clz = instance.getClass();
            }
            else
            {
               java.lang.reflect.Constructor<?> factoryConstructor =
                  findConstructor(factoryClass, ct.getParameter(), cl);
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
         for (Property pt : bt.getProperty())
         {
            setBeanProperty(instance, pt, cl);
         }
      }

      if (!bt.isIgnoreCreate())
      {
         try
         {
            String methodName = "create";
            if (bt.getCreate() != null && bt.getCreate().getMethod() != null)
               methodName = bt.getCreate().getMethod();

            Method createMethod = clz.getMethod(methodName, (Class[])null);
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

      if (!bt.isIgnoreStart())
      {
         try
         {
            String methodName = "start";
            if (bt.getStart() != null && bt.getStart().getMethod() != null)
               methodName = bt.getStart().getMethod();

            Method startMethod = clz.getMethod(methodName, (Class[])null);
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

      if (bt.getStop() != null && bt.getStop().getMethod() != null)
         stops.put(bt.getName(), bt.getStop().getMethod());

      if (bt.getDestroy() != null && bt.getDestroy().getMethod() != null)
         destroys.put(bt.getName(), bt.getDestroy().getMethod());

      if (bt.isIgnoreStop())
         ignoreStops.add(bt.getName());

      if (bt.isIgnoreDestroy())
         ignoreDestroys.add(bt.getName());

      // Invoke install methods
      if (bt.getInstall() != null && bt.getInstall().size() > 0)
      {
         for (Install it : bt.getInstall())
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
         java.util.List<Method> methods = new ArrayList<Method>(bt.getUninstall().size());
         for (Uninstall ut : bt.getUninstall())
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
         for (Incallback it : bt.getIncallback())
         {
            java.util.List<Method> candidates = new ArrayList<Method>(1);
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
         for (Uncallback ut : bt.getUncallback())
         {
            java.util.List<Method> candidates = new ArrayList<Method>(1);
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
   private java.lang.reflect.Constructor<?> findConstructor(Class<?> clz, java.util.List<Parameter> parameters,
                                                            ClassLoader cl) throws Throwable
   {
      if (parameters == null || parameters.size() == 0)
      {
         Class<?> constructorClass = clz;
         
         while (constructorClass != null)
         {
            java.lang.reflect.Constructor[] constructors = constructorClass.getDeclaredConstructors();

            if (constructors != null)
            {
               for (int i = 0; i < constructors.length; i++)
               {
                  java.lang.reflect.Constructor<?> con = constructors[i];
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
            java.lang.reflect.Constructor[] constructors = constructorClass.getDeclaredConstructors();

            for (java.lang.reflect.Constructor<?> c : constructors)
            {
               if (parameters.size() == c.getParameterTypes().length)
               {
                  boolean include = true;

                  for (int i = 0; include && i < parameters.size(); i++)
                  {
                     Parameter pt = parameters.get(i);
                     Class<?> parameterClass = c.getParameterTypes()[i];

                     if (pt.getClazz() == null)
                     {
                        if ((!(pt.getContent().get(0) instanceof Inject)) &&
                            (!(pt.getContent().get(0) instanceof Null)))
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
   private Method findMethod(Class<?> clz, String name, java.util.List<Parameter> parameters, ClassLoader cl)
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
                        Parameter pt = parameters.get(i);
                        Class<?> parameterClass = m.getParameterTypes()[i];

                        if (pt.getClazz() == null)
                        {
                           if ((!(pt.getContent().get(0) instanceof Inject)) &&
                               (!(pt.getContent().get(0) instanceof Null)))
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
   private Object[] getArguments(java.util.List<Parameter> definitions, Class<?>[] types, ClassLoader cl)
      throws Throwable
   {
      if (definitions == null || definitions.size() == 0)
         return null;

      Object[] args = new Object[types.length];

      Injection injection = new Injection();
      for (int i = 0; i < definitions.size(); i++)
      {
         Parameter parameter = definitions.get(i);

         Object v = parameter.getContent().get(0);
         
         if (v instanceof Inject)
         {
            args[i] = getInjectValue((Inject)v);
         }
         else if (v instanceof Null)
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
   private Object getInjectValue(Inject it) throws Exception
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
   private void setBeanProperty(Object instance, Property pt, ClassLoader cl) throws Exception
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

      if (element instanceof Inject)
      {
         parameterValue = getInjectValue((Inject)element);
      }
      else if (element instanceof com.github.fungal.api.deployment.Map)
      {
         com.github.fungal.api.deployment.Map mt = (com.github.fungal.api.deployment.Map)element;
         
         java.util.Map<Object, Object> map = null;
         
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
               java.lang.reflect.Constructor<?> con = mapClass.getConstructor(int.class);
               con.setAccessible(true);
               map = (java.util.Map)con.newInstance(mt.getEntry().size());
            }
            else
            {
               map = (java.util.Map)mapClass.newInstance();
            }
         }

         Class<?> keyClass = Class.forName(mt.getKeyClass(), true, cl);
         Class<?> valueClass = Class.forName(mt.getValueClass(), true, cl);

         for (Entry et : mt.getEntry())
         {
            Object key = injection.getValue(et.toString(), keyClass, et.getKey().getValue(), cl);
            Object value = injection.getValue(et.toString(), valueClass, et.getValue().getValue(), cl);
            
            map.put(key, value);
         }

         parameterValue = map;
      }
      else if (element instanceof com.github.fungal.api.deployment.List)
      {
         com.github.fungal.api.deployment.List lt = (com.github.fungal.api.deployment.List)element;

         java.util.List<Object> list = null;
            
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
               java.lang.reflect.Constructor<?> con = listClass.getConstructor(int.class);
               con.setAccessible(true);
               list = (java.util.List)con.newInstance(lt.getValue().size());
            }
            else
            {
               list = (java.util.List)listClass.newInstance();
            }
         }

         Class<?> elementClass = Class.forName(lt.getElementClass(), true, cl);

         for (Value vt : lt.getValue())
         {
            Object value = injection.getValue(vt.toString(), elementClass, vt.getValue(), cl);
            list.add(value);
         }

         parameterValue = list;
      }
      else if (element instanceof com.github.fungal.api.deployment.Set)
      {
         com.github.fungal.api.deployment.Set st = (com.github.fungal.api.deployment.Set)element;

         java.util.Set<Object> set = null;
         
         if (st.getClazz() == null)
         {
            set = new HashSet<Object>(st.getValue().size());
         }
         else
         {
            Class<?> setClass = Class.forName(st.getClazz(), true, cl);

            if (st.getClazz().equals("java.util.HashSet"))
            {
               java.lang.reflect.Constructor<?> con = setClass.getConstructor(int.class);
               con.setAccessible(true);
               set = (java.util.Set)con.newInstance(st.getValue().size());
            }
            else
            {
               set = (java.util.Set)setClass.newInstance();
            }
         }

         Class<?> elementClass = Class.forName(st.getElementClass(), true, cl);
         
         for (Value vt : st.getValue())
         {
            Object value = injection.getValue(vt.toString(), elementClass, vt.getValue(), cl);
            set.add(value);
         }

         parameterValue = set;
      }
      else if (element instanceof Null)
      {
         parameterValue = null;
      }
      else if (element instanceof This)
      {
         parameterValue = instance;
      }
      else if (element instanceof Value)
      {
         parameterValue = injection.getValue(pt.getName(), parameterClass, ((Value)element).getValue(), cl);
      }
      else
      {
         parameterValue = injection.getValue(pt.getName(), parameterClass, (String)element, cl);
      }

      m.invoke(instance, parameterValue);
   }
}

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

import com.github.fungal.api.Kernel;
import com.github.fungal.api.classloading.ClassLoaderFactory;
import com.github.fungal.api.classloading.KernelClassLoader;
import com.github.fungal.api.configuration.KernelConfiguration;
import com.github.fungal.api.deployer.MainDeployer;
import com.github.fungal.api.events.Event;
import com.github.fungal.api.events.EventListener;
import com.github.fungal.api.remote.Command;
import com.github.fungal.bootstrap.Bootstrap;
import com.github.fungal.impl.netboot.Netboot;
import com.github.fungal.impl.remote.CommunicationServer;
import com.github.fungal.impl.remote.commands.Deploy;
import com.github.fungal.impl.remote.commands.GetCommand;
import com.github.fungal.impl.remote.commands.Help;
import com.github.fungal.impl.remote.commands.Undeploy;
import com.github.fungal.spi.deployers.DeployerPhases;
import com.github.fungal.spi.deployers.Deployment;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * The kernel implementation for Fungal
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class KernelImpl implements Kernel
{
   /** The logger */
   private static Logger log = null;

   /** Trace logging enabled */
   private static boolean trace = false;

   /** Version information */
   private static final String VERSION = "Fungal 0.9.0.Beta3";

   /** Kernel configuration */
   private KernelConfiguration kernelConfiguration;

   /** Deployments */
   private List<Deployment> deployments = Collections.synchronizedList(new ArrayList<Deployment>(1));

   /** Beans */
   private ConcurrentMap<String, Object> beans = new ConcurrentHashMap<String, Object>(1);

   /** Bean status */
   private ConcurrentMap<String, ServiceLifecycle> beanStatus = new ConcurrentHashMap<String, ServiceLifecycle>(1);

   /** Bean dependants */
   private ConcurrentMap<String, Set<String>> beanDependants = new ConcurrentHashMap<String, Set<String>>(1);

   /** Bean deployments */
   private AtomicInteger beanDeployments;

   /** Kernel thread pool */
   private ThreadPoolExecutor threadPoolExecutor;

   /** The old class loader */
   private ClassLoader oldClassLoader;

   /** Kernel class loader */
   private KernelClassLoader kernelClassLoader;

   /** Main deployer */
   private MainDeployerImpl mainDeployer;

   /** MBeanServer */
   private MBeanServer mbeanServer;

   /** Communition server */
   private CommunicationServer remote;

   /** Temporary environment */
   private boolean temporaryEnvironment;

   /** Incallbacks */
   private ConcurrentMap<Class<?>, List<Callback>> incallbacks = new ConcurrentHashMap<Class<?>, List<Callback>>(1);

   /** Uncallbacks */
   private ConcurrentMap<Class<?>, List<Callback>> uncallbacks = new ConcurrentHashMap<Class<?>, List<Callback>>(1);

   /** Callback beans */
   private ConcurrentMap<Object, List<Callback>> callbackBeans = new ConcurrentHashMap<Object, List<Callback>>(1);

   /** DeployerPhases beans */
   private Set<String> deployerPhasesBeans = Collections.synchronizedSet(new HashSet<String>(1));

   /** New DeployerPhases beans */
   private Set<String> newDeployerPhasesBeans = Collections.synchronizedSet(new HashSet<String>(1));

   /** Hot deployer */
   private HotDeployer hotDeployer;

   /**
    * Constructor
    * @param kc The kernel configuration
    */
   public KernelImpl(KernelConfiguration kc)
   {
      this.kernelConfiguration = kc;
      this.beanDeployments = new AtomicInteger(0);
      this.temporaryEnvironment = false;
      this.mainDeployer = null;
      this.kernelClassLoader = null;
      this.mbeanServer = null;
      this.hotDeployer = null;
   }

   /**
    * Get the MBeanServer for the kernel
    * @return The MBeanServer instance
    */
   public MBeanServer getMBeanServer()
   {
      return mbeanServer;
   }

   /**
    * Get a deployment unit
    * @param url The unique URL for a deployment
    * @return The deployment unit; <code>null</code> if no unit is found
    */
   public Deployment getDeployment(URL url)
   {
      if (deployments != null)
      {
         for (Deployment deployment : deployments)
         {
            if (deployment.getURL().toString().equals(url.toString()))
               return deployment;
         }
      }

      return null;
   }

   /**
    * Startup
    * @exception Throwable Thrown if an error occurs
    */
   public void startup() throws Throwable
   {
      SecurityActions.setSystemProperty("javax.xml.stream.XMLInputFactory", 
                                        "com.sun.xml.internal.stream.XMLInputFactoryImpl");

      List<EventListener> els = kernelConfiguration.getEventListeners();
      // STARTING
      if (els != null && els.size() > 0)
      {
         for (EventListener el : els)
         {
            el.event(this, Event.STARTING);
         }
      }

      ThreadGroup tg = kernelConfiguration.getThreadGroup();
      if (tg == null)
         tg = new ThreadGroup("fungal");

      BlockingQueue<Runnable> threadPoolQueue = new SynchronousQueue<Runnable>(true);
      ThreadFactory tf = new FungalThreadFactory(tg);

      threadPoolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Integer.MAX_VALUE,
                                                  60, TimeUnit.SECONDS,
                                                  threadPoolQueue,
                                                  tf);

      threadPoolExecutor.allowCoreThreadTimeOut(true);
      threadPoolExecutor.prestartAllCoreThreads();

      File root = null;

      if (kernelConfiguration.getHome() != null)
      {
         root = new File(kernelConfiguration.getHome().toURI());
         SecurityActions.setSystemProperty(kernelConfiguration.getName() + ".home", root.getAbsolutePath());
      }
      else
      {
         File tmp = new File(SecurityActions.getSystemProperty("java.io.tmpdir"));
         root = new File(tmp, kernelConfiguration.getName());

         if (root.exists())
         {
            recursiveDelete(root);
         }

         if (!root.mkdirs())
            throw new IOException("Could not create directory " + root.getAbsolutePath());

         SecurityActions.setSystemProperty(kernelConfiguration.getName() + ".home", root.getAbsolutePath());
         
         temporaryEnvironment = true;
      }

      File libDirectory = null;
      File configDirectory = null;
      File repositoryDirectory = null;
      File systemDirectory = null;
      File deployDirectory = null;

      if (root != null && root.exists())
      {
         if (kernelConfiguration.getLibrary() != null)
            libDirectory = new File(root, File.separator + kernelConfiguration.getLibrary() + File.separator);

         if (kernelConfiguration.getConfiguration() != null)
            configDirectory = new File(root, File.separator + kernelConfiguration.getConfiguration() + File.separator);

         if (kernelConfiguration.getRepository() != null)
            repositoryDirectory = new File(root, File.separator + kernelConfiguration.getRepository() + File.separator);

         if (kernelConfiguration.getSystem() != null)
            systemDirectory = new File(root, File.separator + kernelConfiguration.getSystem() + File.separator);

         if (kernelConfiguration.getDeploy() != null)
            deployDirectory = new File(root, File.separator + kernelConfiguration.getDeploy() + File.separator);
      }

      // PRE_CLASSLOADER
      if (els != null && els.size() > 0)
      {
         for (EventListener el : els)
         {
            el.event(this, Event.PRE_CLASSLOADER);
         }
      }

      oldClassLoader = SecurityActions.getThreadContextClassLoader();

      // Setup bootstrap
      Bootstrap bootstrap = null;
      if (configDirectory != null && configDirectory.exists() && configDirectory.isDirectory())
      {
         File bootstrapXml = new File(configDirectory, "bootstrap.xml");

         if (bootstrapXml != null && bootstrapXml.exists())
         {
            com.github.fungal.bootstrap.Unmarshaller bootstrapU = 
               new com.github.fungal.bootstrap.Unmarshaller();
            bootstrap = bootstrapU.unmarshal(bootstrapXml.toURI().toURL());
         }
      }

      // Setup kernel classloader
      URL[] libUrls = getUrls(libDirectory);
      URL[] confUrls = getUrls(configDirectory);

      URL[] urls = mergeUrls(libUrls, confUrls);

      kernelClassLoader = ClassLoaderFactory.create(kernelConfiguration.getClassLoader(), urls, oldClassLoader);
      SecurityActions.setThreadContextClassLoader(kernelClassLoader);

      initKernelLogging();

      // Netboot
      boolean netbooted = false;
      if (bootstrap != null)
         netbooted = Netboot.resolve(getExecutorService(), bootstrap, repositoryDirectory, root);

      if (netbooted)
      {
         libUrls = getUrls(libDirectory);
         confUrls = getUrls(configDirectory);

         urls = mergeUrls(libUrls, confUrls);

         kernelClassLoader = ClassLoaderFactory.create(kernelConfiguration.getClassLoader(), urls, kernelClassLoader);
         SecurityActions.setThreadContextClassLoader(kernelClassLoader);
      }

      // POST_CLASSLOADER
      if (els != null && els.size() > 0)
      {
         for (EventListener el : els)
         {
            el.event(this, Event.POST_CLASSLOADER);
         }
      }

      if (kernelConfiguration.getBindAddress() != null)
      {
         SecurityActions.setSystemProperty(kernelConfiguration.getName() + ".bindaddress", 
                                           kernelConfiguration.getBindAddress().trim());
      }

      // Create MBeanServer
      mbeanServer = MBeanServerFactory.createMBeanServer(kernelConfiguration.getName());

      // Main deployer
      mainDeployer = new MainDeployerImpl(this);
      ObjectName mainDeployerObjectName = new ObjectName(kernelConfiguration.getName() + ":name=MainDeployer");
      mbeanServer.registerMBean(mainDeployer, mainDeployerObjectName);

      // Add the deployment deployer
      mainDeployer.addDeployer(new DeploymentDeployer(this));

      // Add the kernel bean reference
      addBean("Kernel", this);
      setBeanStatus("Kernel", ServiceLifecycle.STARTED);

      // Log version information
      log.info(VERSION + " started");

      if (log.isLoggable(Level.FINE))
      {
         StringBuilder vmArgs = new StringBuilder();
         RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
         List<String> inputArguments = runtime.getInputArguments();
         Iterator<String> it = inputArguments.iterator();
         while (it.hasNext()) 
         {
            String arg = it.next();

            vmArgs = vmArgs.append(arg);
            
            if (it.hasNext())
               vmArgs = vmArgs.append(" ");
         }

         log.fine("Java version: " + 
                  SecurityActions.getSystemProperty("java.version") + "," +
                  SecurityActions.getSystemProperty("java.vendor"));
         
         log.fine("Java Runtime: " + 
                  SecurityActions.getSystemProperty("java.runtime.name") + " (build " + 
                  SecurityActions.getSystemProperty("java.runtime.version") + ")");      
         
         log.fine("Java VM: " + 
                  SecurityActions.getSystemProperty("java.vm.name") + " " + 
                  SecurityActions.getSystemProperty("java.vm.version") + "," +
                  SecurityActions.getSystemProperty("java.vm.vendor"));
         
         log.fine("OS-System: " + 
                  SecurityActions.getSystemProperty("os.name") + " " + 
                  SecurityActions.getSystemProperty("os.version") + "," + 
                  SecurityActions.getSystemProperty("os.arch"));
         
         log.fine("VM arguments: " + vmArgs.toString());
      }

      // Start all URLs defined in bootstrap.xml
      if (bootstrap != null)
      {
         beanDeployments = new AtomicInteger(bootstrap.getUrl().size());

         List<URL> bootstrapUrls = new ArrayList<URL>(bootstrap.getUrl().size());

         for (String url : bootstrap.getUrl())
         {
            URL fullPath = new URL(configDirectory.toURI().toURL().toExternalForm() + url);
            bootstrapUrls.add(fullPath);
         }

         deployUrls(bootstrapUrls.toArray(new URL[bootstrapUrls.size()]));
      }
      incallback();

      // PreDeploy
      preDeploy(false);

      // Deploy all files in system/
      if (systemDirectory != null && systemDirectory.exists() && systemDirectory.isDirectory())
      {
         File[] files = systemDirectory.listFiles();

         if (files != null)
         {
            List<URL> l = new ArrayList<URL>(files.length);
            int counter = 0;

            for (File f : files)
            {
               URL u = f.toURI().toURL();
               
               l.add(u);

               if (u.toString().endsWith(".xml"))
                  counter++;
            }

            Collections.sort(l, kernelConfiguration.getDeploymentOrder());

            beanDeployments = new AtomicInteger(counter);

            if (kernelConfiguration.isParallelDeploy())
            {
               deployUrls(l.toArray(new URL[l.size()]));
            }
            else
            {
               for (URL u : l)
               {
                  deployUrls(new URL[] {u});
               }
            }

            if (counter > 0)
               incallback();
         }
      }

      // Deploy all files in deploy/
      if (deployDirectory != null && deployDirectory.exists() && deployDirectory.isDirectory())
      {
         // Hot deployer
         if (kernelConfiguration.isHotDeployment())
         {
            hotDeployer = new HotDeployer(kernelConfiguration.getHotDeploymentInterval(),
                                          deployDirectory,
                                          this);

            ObjectName hotDeployerObjectName = new ObjectName(kernelConfiguration.getName() + ":name=HotDeployer");
            mbeanServer.registerMBean(hotDeployer, hotDeployerObjectName);
         }

         File[] files = deployDirectory.listFiles();

         if (files != null)
         {
            List<URL> l = new ArrayList<URL>(files.length);
            int counter = 0;

            for (File f : files)
            {
               URL u = f.toURI().toURL();
               
               l.add(u);

               if (u.toString().endsWith(".xml"))
                  counter++;

               if (hotDeployer != null)
                  hotDeployer.register(u);
            }

            Collections.sort(l, kernelConfiguration.getDeploymentOrder());

            beanDeployments = new AtomicInteger(counter);

            if (kernelConfiguration.isParallelDeploy())
            {
               deployUrls(l.toArray(new URL[l.size()]));
            }
            else
            {
               for (URL u : l)
               {
                  deployUrls(new URL[] {u});
               }
            }

            if (counter > 0)
               incallback();
         }

         if (hotDeployer != null)
            hotDeployer.start();
      }

      // PostDeploy
      postDeploy(false);

      // Remote access
      if (kernelConfiguration.isRemoteAccess())
      {
         remote = new CommunicationServer(this,
                                          kernelConfiguration.getBindAddress(),
                                          kernelConfiguration.getRemotePort());

         remote.registerCommand(new Help(remote));
         remote.registerCommand(new GetCommand(remote));
         remote.registerCommand(new Deploy(getMainDeployer(), getHotDeployer()));
         remote.registerCommand(new Undeploy(getMainDeployer(), getHotDeployer()));

         List<Command> commands = kernelConfiguration.getCommands();
         if (commands != null && commands.size() > 0)
         {
            for (Command command : commands)
            {
               remote.registerCommand(command);
            }
         }

         remote.start();

         threadPoolExecutor.submit(remote);
      }

      // Memory information
      if (log.isLoggable(Level.FINE))
      {
         MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
         log.fine("Heap memory: " + memoryBean.getHeapMemoryUsage().toString());
         log.fine("NonHeap memory: " + memoryBean.getNonHeapMemoryUsage().toString());
      }

      // STARTED
      if (els != null && els.size() > 0)
      {
         for (EventListener el : els)
         {
            el.event(this, Event.STARTED);
         }
      }
   }

   /**
    * Init kernel logging
    */
   private void initKernelLogging()
   {
      log = Logger.getLogger("com.github.fungal.Fungal");
      trace = log.isLoggable(Level.FINEST);
   }

   /**
    * Deploy URLs
    * @param urls The URLs
    */
   private void deployUrls(URL[] urls)
   {
      if (urls != null && urls.length > 0)
      {
         try
         {
            List<UnitDeployer> unitDeployers = new ArrayList<UnitDeployer>(urls.length);

            final CountDownLatch unitLatch = new CountDownLatch(urls.length);

            for (URL url : urls)
            {
               try
               {
                  if (log.isLoggable(Level.FINE))
                     log.fine("URL=" + url.toString());

                  MainDeployerImpl deployer = (MainDeployerImpl)mainDeployer.clone();
                  UnitDeployer unitDeployer = new UnitDeployer(url, deployer, kernelClassLoader, unitLatch);
                  unitDeployers.add(unitDeployer);
                  
                  getExecutorService().execute(unitDeployer);
               }
               catch (Throwable deployThrowable)
               {
                  log.log(Level.SEVERE, deployThrowable.getMessage(), deployThrowable);
               }
            }

            unitLatch.await();

            Iterator<UnitDeployer> it = unitDeployers.iterator();
            while (it.hasNext())
            {
               UnitDeployer deployer = it.next();
               if (deployer.getThrowable() != null)
               {
                  Throwable t = deployer.getThrowable();
                  log.log(Level.SEVERE, t.getMessage(), t);
               }
            }
         }
         catch (Throwable t)
         {
            log.log(Level.SEVERE, t.getMessage(), t);
         }
      }
   }

   /**
    * Shutdown
    * @exception Throwable Thrown if an error occurs
    */
   public void shutdown() throws Throwable
   {
      List<EventListener> els = kernelConfiguration.getEventListeners();
      // STOPPING
      if (els != null && els.size() > 0)
      {
         for (EventListener el : els)
         {
            el.event(this, Event.STOPPING);
         }
      }

      SecurityActions.setThreadContextClassLoader(kernelClassLoader);

      // Stop hot deployer
      if (hotDeployer != null)
      {
         hotDeployer.stop();
      }

      // Stop the remote connector
      if (remote != null)
      {
         remote.stop();
      }

      // PreUndeploy
      preUndeploy(false);      

      // Shutdown all deployments
      if (deployments.size() > 0)
      {
         List<Deployment> shutdownDeployments = new LinkedList<Deployment>(deployments);
         Collections.reverse(shutdownDeployments);

         for (Deployment deployment : shutdownDeployments)
         {
            if (hotDeployer != null)
               hotDeployer.unregister(deployment.getURL());

            shutdownDeployment(deployment);
         }
      }

      // PostUndeploy
      // We are removing all deployments including all DeployerPhases beans => no postUndeploy call

      // Remove kernel bean
      removeBean("Kernel");

      // Check for additional beans
      if (beans.size() > 0)
      {
         List<String> beanNames = new LinkedList<String>(beans.keySet());
         for (String beanName : beanNames)
         {
            removeBean(beanName);
         }
      }

      // Release MBeanServer
      if (mbeanServer != null)
         MBeanServerFactory.releaseMBeanServer(mbeanServer);

      // Shutdown thread pool
      if (threadPoolExecutor != null)
         threadPoolExecutor.shutdown();

      // Cleanup temporary environment
      if (temporaryEnvironment)
      {
         File tmp = new File(SecurityActions.getSystemProperty("java.io.tmpdir"));
         File root = new File(tmp, kernelConfiguration.getName());

         recursiveDelete(root);
      }

      // Log shutdown
      if (log != null)
      {
         log.info(VERSION + " stopped");
      }
      else
      {
         System.out.println(VERSION + " stopped");
      }

      // Shutdown kernel class loader
      if (kernelClassLoader != null)
      {
         try
         {
            kernelClassLoader.shutdown();
         }
         catch (IOException ioe)
         {
            // Swallow
         }

         kernelClassLoader = null;
      }

      // Reset to the old class loader
      SecurityActions.setThreadContextClassLoader(oldClassLoader);

      // STOPPED
      if (els != null && els.size() > 0)
      {
         for (EventListener el : els)
         {
            el.event(this, Event.STOPPED);
         }
      }
   }

   /**
    * Shutdown a deployment unit
    * @param deployment The deployment unit
    * @exception Throwable If an error occurs
    */
   @SuppressWarnings("unchecked") 
   void shutdownDeployment(Deployment deployment) throws Throwable
   {
      SecurityActions.setThreadContextClassLoader(kernelClassLoader);

      try
      {
         Method stopMethod = deployment.getClass().getMethod("stop", (Class[])null);
         stopMethod.invoke(deployment, (Object[])null);
      }
      catch (NoSuchMethodException nsme)
      {
         // No stop method
      }
      catch (InvocationTargetException ite)
      {
         throw ite.getCause();
      }

      try
      {
         Method destroyMethod = deployment.getClass().getMethod("destroy", (Class[])null);
         destroyMethod.invoke(deployment, (Object[])null);
      }
      catch (NoSuchMethodException nsme)
      {
         // No destroy method
      }
      catch (InvocationTargetException ite)
      {
         throw ite.getCause();
      }

      deployments.remove(deployment);
   }

   /**
    * Get the kernel class loader
    * @return The class loader
    */
   public KernelClassLoader getKernelClassLoader()
   {
      return kernelClassLoader;
   }

   /** 
    * Get the executor service
    * @return The executor service
    */
   public ExecutorService getExecutorService()
   {
      return threadPoolExecutor;
   }

   /**
    * Get the kernel configuration
    * @return The configuration
    */
   KernelConfiguration getKernelConfiguration()
   {
      return kernelConfiguration;
   }

   /**
    * Get the bean status
    * @param name The bean name
    * @return The status
    */
   ServiceLifecycle getBeanStatus(String name)
   {
      return beanStatus.get(name);
   }

   /**
    * Set the bean status
    * @param name The bean name
    * @param status The status
    */
   void setBeanStatus(String name, ServiceLifecycle status)
   {
      beanStatus.put(name, status);
   }

   /**
    * Add a bean
    * @param name The name of the bean
    * @param bean The bean
    */
   void addBean(String name, Object bean)
   {
      if (!beans.containsKey(name))
      {
         beans.put(name, bean);
      }
      else
      {
         log.log(Level.SEVERE, "Bean [" + name + "] already exists (Existing=" + 
                 beans.get(name) + ", New=" + bean + ")");
      }
   }

   /**
    * Remove a bean
    * @param name The name of the bean
    */
   void removeBean(String name)
   {
      if (uncallbacks.size() > 0)
      {
         Object bean = beans.get(name);

         if (bean != null)
         {
            if (callbackBeans.containsKey(bean))
            {
               Iterator<Map.Entry<Class<?>, List<Callback>>> cit = uncallbacks.entrySet().iterator();
               while (cit.hasNext())
               {
                  Map.Entry<Class<?>, List<Callback>> entry = cit.next();

                  Class<?> type = entry.getKey();
                  List<Callback> callbacks = entry.getValue();
            
                  if (type.isInstance(bean))
                  {
                     for (Callback cb : callbacks)
                     {
                        try
                        {
                           Method m = cb.getMethod();
                           Object instance = cb.getInstance();
                           
                           m.invoke(instance, new Object[] {bean});
                        }
                        catch (Throwable t)
                        {
                           if (log.isLoggable(Level.FINE))
                              log.fine(cb.toString());
                        }
                     }
                  }
               }

               callbackBeans.remove(bean);
            }
         }
      }

      deployerPhasesBeans.remove(name);
      beans.remove(name);
      beanStatus.remove(name);
   }

   /**
    * Get a bean
    * @param name The bean name
    * @param expectedType The expected type for the bean
    * @return The bean instance
    * @exception Throwable If an error occurs
    */
   public <T> T getBean(String name, Class<T> expectedType) throws Throwable
   {
      if (name == null)
         throw new IllegalArgumentException("Name is null");

      if (expectedType == null)
         throw new IllegalArgumentException("ExpectedType is null");

      return expectedType.cast(getBean(name));
   }

   /**
    * Get a bean
    * @param name The name of the bean
    * @return The bean
    */
   public Object getBean(String name)
   {
      return beans.get(name);
   }

   /**
    * Get the set of dependants for a bean
    * @param name The name of the bean
    * @return The set of dependants; <code>null</code> if there are no dependants
    */
   Set<String> getBeanDependants(String name)
   {
      return beanDependants.get(name);
   }

   /**
    * Add a bean to the dependants map
    * @param from The name of the from bean
    * @param to The name of the to bean
    */
   void addBeanDependants(String from, String to)
   {
      Set<String> dependants = beanDependants.get(from);
      if (dependants == null)
      {
         Set<String> newDependants = new HashSet<String>(1);
         dependants = beanDependants.putIfAbsent(from, newDependants);
         if (dependants == null)
         {
            dependants = newDependants;
         }
      }
      
      dependants.add(to);
   }

   /**
    * Register deployment
    * @param deployment The deployment
    */
   void registerDeployment(Deployment deployment)
   {
      deployments.add(deployment);
   }

   /**
    * Beans registered
    */
   void beansRegistered()
   {
      beanDeployments.decrementAndGet();
   }

   /**
    * Is all beans registered
    * @return True if all beans have been registered; otherwise false
    */
   boolean isAllBeansRegistered()
   {
      return beanDeployments.get() <= 0;
   }

   /**
    * Get the main deployer
    * @return The main deployer
    */
   public MainDeployer getMainDeployer()
   {
      try
      {
         return (MainDeployer)mainDeployer.clone();
      }
      catch (CloneNotSupportedException cnse)
      {
         return mainDeployer;
      }
   }

   /**
    * Get the hot deployer
    * @return The hot deployer
    */
   public HotDeployer getHotDeployer()
   {
      return hotDeployer;
   }

   /**
    * Register an incallback method with the kernel
    * @param cb The callback structure
    */
   void registerIncallback(Callback cb)
   {
      List<Callback> callbacks = incallbacks.get(cb.getType());
      if (callbacks == null)
      {
         List<Callback> newCallbacks = new ArrayList<Callback>(1);
         callbacks = incallbacks.putIfAbsent(cb.getType(), newCallbacks);
         if (callbacks == null)
         {
            callbacks = newCallbacks;
         }
      }
      
      callbacks.add(cb);
   }

   /**
    * Register an uncallback method with the kernel
    * @param cb The callback structure
    */
   void registerUncallback(Callback cb)
   {
      List<Callback> callbacks = uncallbacks.get(cb.getType());
      if (callbacks == null)
      {
         List<Callback> newCallbacks = new ArrayList<Callback>(1);
         callbacks = uncallbacks.putIfAbsent(cb.getType(), newCallbacks);
         if (callbacks == null)
         {
            callbacks = newCallbacks;
         }
      }
      
      callbacks.add(cb);
   }

   /**
    * Handle incallback
    */
   private void incallback()
   {
      if (incallbacks.size() > 0)
      {
         Iterator<Map.Entry<Class<?>, List<Callback>>> cit = incallbacks.entrySet().iterator();
         while (cit.hasNext())
         {
            Map.Entry<Class<?>, List<Callback>> entry = cit.next();

            Class<?> type = entry.getKey();
            List<Callback> callbacks = entry.getValue();
            
            Iterator<Object> bit = beans.values().iterator();
            while (bit.hasNext())
            {
               Object bean = bit.next();

               if (type.isInstance(bean))
               {
                  for (Callback cb : callbacks)
                  {
                     List<Callback> registeredCallbacks = callbackBeans.get(bean);
                     if (registeredCallbacks == null || !registeredCallbacks.contains(bean))
                     {
                        if (registeredCallbacks == null)
                           registeredCallbacks = new ArrayList<Callback>(1);

                        try
                        {
                           Method m = cb.getMethod();
                           Object instance = cb.getInstance();
                           
                           m.invoke(instance, new Object[] {bean});

                           registeredCallbacks.add(cb);
                           callbackBeans.put(bean, registeredCallbacks);
                        }
                        catch (Throwable t)
                        {
                           if (log.isLoggable(Level.FINE))
                              log.fine(cb.toString());
                        }
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * Get the URLs for the directory and all libraries located in the directory
    * @param directrory The directory
    * @return The URLs
    * @exception MalformedURLException MalformedURLException
    * @exception IOException IOException
    */
   private URL[] getUrls(File directory) throws MalformedURLException, IOException
   {
      if (directory != null && directory.exists() && directory.isDirectory())
      {
         List<URL> result = scanUrls(directory);
         return result.toArray(new URL[result.size()]);      
      }

      return new URL[0];
   }

   /**
    * Get the URLs for the directory and all libraries located in the directory and sub-directories
    * @param f The file handler
    * @return The URLs
    * @exception MalformedURLException MalformedURLException
    * @exception IOException IOException
    */
   private List<URL> scanUrls(File f) throws MalformedURLException, IOException
   {
      List<URL> result = new LinkedList<URL>();

      if (f != null && f.exists())
      {
         if (f.isDirectory())
         {
            // Add directory
            result.add(f.toURI().toURL());

            // Add the contents of the directory too
            File[] jars = f.listFiles();
            
            if (jars != null)
            {
               for (int j = 0; j < jars.length; j++)
               {
                  List<URL> deeper = scanUrls(jars[j]);
                  result.addAll(deeper);
               }
            }
         }
         else
         {
            if (f.getName().endsWith(".jar"))
               result.add(f.getCanonicalFile().toURI().toURL());
         }
      }

      return result;
   }

   /**
    * Merge URLs into a single array
    * @param urls The URLs
    * @return The combined list
    */
   private URL[] mergeUrls(URL[]... urls)
   {
      if (urls != null)
      {
         List<URL> list = new LinkedList<URL>();

         for (URL[] u : urls)
         {
            if (u != null)
            {
               for (URL url : u)
               {
                  list.add(url);
               }
            }
         }

         return list.toArray(new URL[list.size()]);      
      }

      return new URL[0];
   }

   /**
    * Recursive delete
    * @param f The file handler
    * @exception IOException Thrown if a file could not be deleted
    */
   private void recursiveDelete(File f) throws IOException
   {
      if (f != null && f.exists())
      {
         File[] files = f.listFiles();
         if (files != null)
         {
            for (int i = 0; i < files.length; i++)
            {
               if (files[i].isDirectory())
               {
                  recursiveDelete(files[i]);
               } 
               else
               {
                  if (!files[i].delete())
                     throw new IOException("Could not delete " + files[i]);
               }
            }
         }
         if (!f.delete())
            throw new IOException("Could not delete " + f);
      }
   }

   /**
    * Add a bean as a DeployerPhases
    * @param bean The bean name
    */
   void addDeployerPhasesBean(String bean)
   {
      newDeployerPhasesBeans.add(bean);
   }

   /**
    * Pre deploy
    * @param delegate True if any exception should be delegated
    * @exception Throwable Thrown if there is an error and delegate is true
    */
   void preDeploy(boolean delegate) throws Throwable
   {
      if (newDeployerPhasesBeans.size() > 0)
      {
         deployerPhasesBeans.addAll(newDeployerPhasesBeans);
         newDeployerPhasesBeans.clear();
      }

      for (String beanName : deployerPhasesBeans)
      {
         DeployerPhases bean = (DeployerPhases)getBean(beanName);

         if (bean != null && getBeanStatus(beanName) == ServiceLifecycle.STARTED)
         {
            if (delegate)
            {
               bean.preDeploy();
            }
            else
            {
               try
               {
                  bean.preDeploy();
               }
               catch (Throwable t)
               {
                  log.log(Level.WARNING, t.getMessage(), t);
               }
            }
         }
      }
   }

   /**
    * Post deploy
    * @param delegate True if any exception should be delegated
    * @exception Throwable Thrown if there is an error and delegate is true
    */
   void postDeploy(boolean delegate) throws Throwable
   {
      if (newDeployerPhasesBeans.size() > 0)
      {
         deployerPhasesBeans.addAll(newDeployerPhasesBeans);
         newDeployerPhasesBeans.clear();
      }

      for (String beanName : deployerPhasesBeans)
      {
         DeployerPhases bean = (DeployerPhases)getBean(beanName);

         if (bean != null && getBeanStatus(beanName) == ServiceLifecycle.STARTED)
         {
            if (delegate)
            {
               bean.postDeploy();
            }
            else
            {
               try
               {
                  bean.postDeploy();
               }
               catch (Throwable t)
               {
                  log.log(Level.WARNING, t.getMessage(), t);
               }
            }
         }
      }
   }

   /**
    * Pre undeploy
    * @param delegate True if any exception should be delegated
    * @exception Throwable Thrown if there is an error and delegate is true
    */
   void preUndeploy(boolean delegate) throws Throwable
   {
      for (String beanName : deployerPhasesBeans)
      {
         DeployerPhases bean = (DeployerPhases)getBean(beanName);

         if (bean != null && getBeanStatus(beanName) == ServiceLifecycle.STARTED)
         {
            if (delegate)
            {
               bean.preUndeploy();
            }
            else
            {
               try
               {
                  bean.preUndeploy();
               }
               catch (Throwable t)
               {
                  log.log(Level.WARNING, t.getMessage(), t);
               }
            }
         }
      }
   }

   /**
    * Post undeploy
    * @param delegate True if any exception should be delegated
    * @exception Throwable Thrown if there is an error and delegate is true
    */
   void postUndeploy(boolean delegate) throws Throwable
   {
      for (String beanName : deployerPhasesBeans)
      {
         DeployerPhases bean = (DeployerPhases)getBean(beanName);

         if (bean != null && getBeanStatus(beanName) == ServiceLifecycle.STARTED)
         {
            if (delegate)
            {
               bean.postUndeploy();
            }
            else
            {
               try
               {
                  bean.postUndeploy();
               }
               catch (Throwable t)
               {
                  log.log(Level.WARNING, t.getMessage(), t);
               }
            }
         }
      }
   }

   /**
    * Unit deployer
    */
   static class UnitDeployer implements Runnable
   {
      /** Unit URL */
      private URL url;

      /** Main deployer */
      private MainDeployerImpl deployer;

      /** Class loader */
      private ClassLoader classLoader;

      /** Unit latch */
      private CountDownLatch unitLatch;

      /** Throwable */
      private Throwable throwable;

      /**
       * Constructor
       * @param url The deployment url
       * @param deployer The main deployer
       * @param classLoader The class loader
       * @param unitLatch The unit latch
       */
      public UnitDeployer(final URL url,
                          final MainDeployerImpl deployer,
                          final ClassLoader classLoader,
                          final CountDownLatch unitLatch)
      {
         this.url = url;
         this.deployer = deployer;
         this.classLoader = classLoader;
         this.unitLatch = unitLatch;
         this.throwable = null;
      }

      /**
       * Run
       */
      public void run()
      {
         SecurityActions.setThreadContextClassLoader(classLoader);

         try
         {
            deployer.deploy(url, false, classLoader);
         }
         catch (Throwable t)
         {
            throwable = t;
         }

         unitLatch.countDown();
      }

      /**
       * Get deploy exception
       * @return null if no error; otherwise the exception
       */
      public Throwable getThrowable()
      {
         return throwable;
      }
   }
}

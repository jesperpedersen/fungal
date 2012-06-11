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

import com.github.fungal.api.Kernel;
import com.github.fungal.api.classloading.ClassLoaderFactory;
import com.github.fungal.api.classloading.KernelClassLoader;
import com.github.fungal.api.configuration.KernelConfiguration;
import com.github.fungal.api.deployer.MainDeployer;
import com.github.fungal.api.deployment.Bean;
import com.github.fungal.api.deployment.BeanDeployment;
import com.github.fungal.api.events.Event;
import com.github.fungal.api.events.EventListener;
import com.github.fungal.api.remote.Command;
import com.github.fungal.api.util.JMX;
import com.github.fungal.bootstrap.Bootstrap;
import com.github.fungal.impl.netboot.Netboot;
import com.github.fungal.impl.remote.CommunicationServer;
import com.github.fungal.impl.remote.commands.Deploy;
import com.github.fungal.impl.remote.commands.GetCommand;
import com.github.fungal.impl.remote.commands.Help;
import com.github.fungal.impl.remote.commands.Undeploy;
import com.github.fungal.spi.deployers.DeployException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
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
public class KernelImpl implements Kernel, KernelImplMBean
{
   /** Version information */
   private static final String VERSION = "Fungal 0.11.0.Beta10";

   /** Kernel configuration */
   private KernelConfiguration kernelConfiguration;

   /** The logger */
   private Logger log = null;

   /** Trace logging enabled */
   private boolean trace = false;

   /** Deployments */
   private List<Deployment> deployments = Collections.synchronizedList(new ArrayList<Deployment>(1));

   /** Beans */
   private ConcurrentMap<String, Object> beans = new ConcurrentHashMap<String, Object>(1);

   /** Bean status */
   private ConcurrentMap<String, ServiceLifecycle> beanStatus = new ConcurrentHashMap<String, ServiceLifecycle>(1);

   /** Bean dependants */
   private ConcurrentMap<String, Set<String>> beanDependants = new ConcurrentHashMap<String, Set<String>>(1);

   /** Bean latches */
   private Map<String, List<CountDownLatch>> beanLatches =
      Collections.synchronizedMap(new HashMap<String, List<CountDownLatch>>(1));

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

   /** Remote JMX access */
   private JmxRemote jmxRemote;

   /** Started */
   private boolean started;

   /**
    * Constructor
    * @param kc The kernel configuration
    */
   public KernelImpl(KernelConfiguration kc)
   {
      this.kernelConfiguration = kc;

      initialize();
   }

   /**
    * Initialize
    */
   private void initialize()
   {
      this.log = null;
      this.trace = false;
      this.deployments.clear();
      this.beans.clear();
      this.beanStatus.clear();
      this.beanDependants.clear();
      this.beanLatches.clear();
      this.beanDeployments = new AtomicInteger(0);

      setExecutorService(null);

      this.oldClassLoader = null;
      this.kernelClassLoader = null;
      this.mainDeployer = null;
      this.mbeanServer = null;
      this.remote = null;
      this.temporaryEnvironment = false;
      this.incallbacks.clear();
      this.uncallbacks.clear();
      this.callbackBeans.clear();
      this.deployerPhasesBeans.clear();
      this.newDeployerPhasesBeans.clear();
      this.hotDeployer = null;
      this.jmxRemote = null;
      this.started = false;
   }

   /**
    * {@inheritDoc}
    */
   public String getName()
   {
      if (kernelConfiguration == null)
         return null;

      return kernelConfiguration.getName();
   }

   /**
    * {@inheritDoc}
    */
   public String getVersion()
   {
      return VERSION;
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
    * {@inheritDoc}
    */
   public Collection<Deployment> getDeployments()
   {
      Collection<Deployment> result = null;

      if (deployments != null)
      {
         result = new ArrayList<Deployment>(deployments.size());
         result.addAll(deployments);
      }

      if (result == null)
         return null;

      return Collections.unmodifiableCollection(result);
   }

   /**
    * {@inheritDoc}
    */
   public List<Deployment> getDeployments(URL url)
   {
      return getDeployments(url, true);
   }

   /**
    * Get a deployment unit
    * @param url The unique URL for a deployment
    * @param readonly Should the result be read only
    * @return The deployment unit; <code>null</code> if no unit is found
    */
   List<Deployment> getDeployments(URL url, boolean readonly)
   {
      List<Deployment> result = null;

      if (deployments != null)
      {
         if (url == null)
         {
            for (Deployment deployment : deployments)
            {
               if (deployment.getURL() == null)
               {
                  if (result == null)
                     result = new ArrayList<Deployment>(1);

                  result.add(deployment);
               }
            }
         }
         else
         {
            for (Deployment deployment : deployments)
            {
               if (deployment.getURL() != null && deployment.getURL().toString().equals(url.toString()))
               {
                  if (result == null)
                     result = new ArrayList<Deployment>(1);

                  result.add(deployment);
               }
            }
         }
      }

      if (result == null)
         return null;

      if (readonly)
         return Collections.unmodifiableList(result);

      return result;
   }

   /**
    * Startup
    * @exception Throwable Thrown if an error occurs
    */
   public void startup() throws Throwable
   {
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

      ThreadPoolExecutor tpe = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Integer.MAX_VALUE,
                                                      60, TimeUnit.SECONDS,
                                                      threadPoolQueue,
                                                      tf);

      tpe.allowCoreThreadTimeOut(true);
      tpe.prestartAllCoreThreads();

      setExecutorService(tpe);

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

      if (kernelConfiguration.isManagement())
      {
         // Create MBeanServer
         if (!kernelConfiguration.isUsePlatformMBeanServer())
         {
            mbeanServer = MBeanServerFactory.createMBeanServer(kernelConfiguration.getName());
         }
         else
         {
            mbeanServer = ManagementFactory.getPlatformMBeanServer();
         }
      }

      // Main deployer
      mainDeployer = new MainDeployerImpl(this, new Deployers());
      if (kernelConfiguration.isManagement())
      {
         ObjectName mainDeployerObjectName = new ObjectName(kernelConfiguration.getName() + ":name=MainDeployer");
         mbeanServer.registerMBean(mainDeployer, mainDeployerObjectName);
      }

      // Add the deployment deployer
      mainDeployer.addDeployer(new DeploymentDeployer(this));

      // Add the kernel bean reference
      addBean("Kernel", this, false);
      setBeanStatus("Kernel", ServiceLifecycle.STARTED);

      if (kernelConfiguration.isManagement())
      {
         ObjectName kernelObjectName = new ObjectName(kernelConfiguration.getName() + ":name=Kernel");
         mbeanServer.registerMBean(this, kernelObjectName);
      }

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

            log.log(Level.FINE, "System deployments: " + l);

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

            if (kernelConfiguration.isManagement())
            {
               ObjectName hotDeployerObjectName = new ObjectName(kernelConfiguration.getName() + ":name=HotDeployer");
               mbeanServer.registerMBean(hotDeployer, hotDeployerObjectName);
            }
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

            log.log(Level.FINE, "Deploy deployments: " + l);

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

         getExecutorService().submit(remote);
      }

      // JMX Remote
      if (kernelConfiguration.isManagement() &&
          kernelConfiguration.isRemoteJmxAccess() &&
          kernelConfiguration.getBindAddress() != null)
      {
         jmxRemote = new JmxRemote(mbeanServer,
                                   kernelConfiguration.getBindAddress(),
                                   kernelConfiguration.getRmiRegistryPort(),
                                   kernelConfiguration.getRmiServerPort());

         jmxRemote.start();
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

      started = true;
   }

   /**
    * Init kernel logging
    */
   private void initKernelLogging()
   {
      log = Logger.getLogger("com.github.fungal.Fungal");
      trace = log.isLoggable(Level.FINER);
   }

   /**
    * Deploy URLs
    * @param urls The URLs
    */
   private void deployUrls(URL[] urls)
   {
      if (urls != null && urls.length > 0)
      {
         SortedMap<Integer, List<URL>> sm = new TreeMap<Integer, List<URL>>();

         for (URL url : urls)
         {
            Integer index = Integer.valueOf(kernelConfiguration.getDeploymentOrder().getOrderIndex(url));
            List<URL> l = sm.get(index);

            if (l == null)
               l = new ArrayList<URL>(1);

            l.add(url);
            sm.put(index, l);
         }

         for (List<URL> l : sm.values())
         {
            try
            {
               List<UnitDeployer> unitDeployers = new ArrayList<UnitDeployer>(l.size());

               final CountDownLatch unitLatch = new CountDownLatch(l.size());

               for (URL url : l)
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
   }

   /**
    * Shutdown
    * @exception Throwable Thrown if an error occurs
    */
   public void shutdown() throws Throwable
   {
      Throwable throwable = null;

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

      if (jmxRemote != null)
      {
         jmxRemote.stop();
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

            try
            {
               shutdownDeployment(deployment);
            }
            catch (Throwable t)
            {
               if (throwable == null)
                  throwable = t;
            }
         }
      }

      // PostUndeploy
      // We are removing all deployments including all DeployerPhases beans => no postUndeploy call

      // Remove kernel bean
      removeBean("Kernel", false);

      // Check for additional beans
      if (beans.size() > 0)
      {
         List<String> beanNames = new LinkedList<String>(beans.keySet());
         for (String beanName : beanNames)
         {
            removeBean(beanName);
         }
      }

      // Unregister MBeans
      if (mbeanServer != null)
      {
         ObjectName mainDeployerObjectName = new ObjectName(kernelConfiguration.getName() + ":name=MainDeployer");
         if (mbeanServer.isRegistered(mainDeployerObjectName))
            mbeanServer.unregisterMBean(mainDeployerObjectName);

         ObjectName hotDeployerObjectName = new ObjectName(kernelConfiguration.getName() + ":name=HotDeployer");
         if (mbeanServer.isRegistered(hotDeployerObjectName))
            mbeanServer.unregisterMBean(hotDeployerObjectName);

         ObjectName kernelObjectName = new ObjectName(kernelConfiguration.getName() + ":name=Kernel");
         if (mbeanServer.isRegistered(kernelObjectName))
            mbeanServer.unregisterMBean(kernelObjectName);

         // Release MBeanServer
         if (!kernelConfiguration.isUsePlatformMBeanServer())
            MBeanServerFactory.releaseMBeanServer(mbeanServer);
      }

      // Shutdown thread pool
      if (getExecutorService() != null)
         getExecutorService().shutdown();

      // Cleanup temporary environment
      if (temporaryEnvironment)
      {
         File tmp = new File(SecurityActions.getSystemProperty("java.io.tmpdir"));
         File root = new File(tmp, kernelConfiguration.getName());

         recursiveDelete(root);
      }

      SecurityActions.setSystemProperty(kernelConfiguration.getName() + ".home", "");
      SecurityActions.setSystemProperty(kernelConfiguration.getName() + ".bindaddress", "");

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

      initialize();

      if (throwable != null)
         throw throwable;
   }

   /**
    * Shutdown a deployment unit
    * @param deployment The deployment unit
    * @exception Throwable If an error occurs
    */
   @SuppressWarnings("unchecked") 
   void shutdownDeployment(Deployment deployment) throws Throwable
   {
      ClassLoader currentCL = SecurityActions.getThreadContextClassLoader();
      SecurityActions.setThreadContextClassLoader(deployment.getClassLoader());

      Throwable throwable = null;
      try
      {
         Method stopMethod = deployment.getClass().getMethod("stop", (Class[])null);
         stopMethod.setAccessible(true);
         stopMethod.invoke(deployment, (Object[])null);
      }
      catch (NoSuchMethodException nsme)
      {
         // No stop method
      }
      catch (InvocationTargetException ite)
      {
         throwable = ite.getCause();
      }

      try
      {
         Method destroyMethod = deployment.getClass().getMethod("destroy", (Class[])null);
         destroyMethod.setAccessible(true);
         destroyMethod.invoke(deployment, (Object[])null);
      }
      catch (NoSuchMethodException nsme)
      {
         // No destroy method
      }
      catch (InvocationTargetException ite)
      {
         if (throwable == null)
            throwable = ite.getCause();
      }

      SecurityActions.setThreadContextClassLoader(currentCL);

      deployments.remove(deployment);

      if (throwable != null)
         throw throwable;
   }

   /**
    * Get the kernel class loader
    * @return The class loader
    */
   public KernelClassLoader getKernelClassLoader()
   {
      if (kernelClassLoader == null)
         throw new IllegalStateException("Kernel not started");

      return kernelClassLoader;
   }

   /**
    * Set the executor service
    * @param v The value
    */
   private void setExecutorService(ThreadPoolExecutor v)
   {
      this.threadPoolExecutor = v;
   }

   /** 
    * Get the executor service
    * @return The executor service
    */
   public ExecutorService getExecutorService()
   {
      if (threadPoolExecutor == null)
         throw new IllegalStateException("Thread pool is null");

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

      if (trace)
         log.log(Level.FINER, "Bean: " + name + ", Status: " + status);

      if (status == ServiceLifecycle.NOT_STARTED)
      {
         List<CountDownLatch> l = beanLatches.get(name);
         if (l == null)
         {
            beanLatches.put(name, Collections.synchronizedList(new ArrayList<CountDownLatch>(1)));
         }
      }
      else if (status == ServiceLifecycle.STARTED || status == ServiceLifecycle.ERROR)
      {
         synchronized (beanLatches)
         {
            List<CountDownLatch> l = beanLatches.get(name);
            if (l != null)
            {
               for (CountDownLatch cdl : l)
               {
                  if (cdl.getCount() > 0)
                     cdl.countDown();
               }
            }
         }
      }
   }

   /**
    * Add a bean
    * @param name The name of the bean
    * @param bean The bean
    */
   void addBean(String name, Object bean)
   {
      addBean(name, bean, true);
   }

   /**
    * Add a bean
    * @param name The name of the bean
    * @param bean The bean
    * @param mgt Add management
    */
   private void addBean(String name, Object bean, boolean mgt)
   {
      if (!beans.containsKey(name))
      {
         beans.put(name, bean);

         if (mgt && kernelConfiguration.isManagement() && kernelConfiguration.isBeanManagement())
         {
            try
            {
               ObjectName on = new ObjectName(kernelConfiguration.getName() + ":name=" + name + ",type=Bean");
               mbeanServer.registerMBean(JMX.createMBean(bean), on);
            }
            catch (Throwable t)
            {
               log.log(Level.WARNING, "Error during management registering of bean [" + name + "]", t);
            }
         }
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
      removeBean(name, true);
   }

   /**
    * Remove a bean
    * @param name The name of the bean
    * @param mgt Remove management
    */
   void removeBean(String name, boolean mgt)
   {
      if (trace)
         log.log(Level.FINER, "Removing bean: " + name);

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

                           m.setAccessible(true);
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
      beanLatches.remove(name);

      if (mgt && kernelConfiguration.isManagement() && kernelConfiguration.isBeanManagement())
      {
         try
         {
            ObjectName on = new ObjectName(kernelConfiguration.getName() + ":name=" + name + ",type=Bean");
            if (mbeanServer.isRegistered(on))
               mbeanServer.unregisterMBean(on);
         }
         catch (Throwable t)
         {
            log.log(Level.FINER, "Error during management unregistering of bean [" + name + "]", t);
         }
      }
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

      if (!beans.containsKey(name))
         throw new IllegalArgumentException("Bean '" + name + "' doesn't exist");

      return expectedType.cast(getBean(name));
   }

   /**
    * {@inheritDoc}
    */
   public BeanDeployment install(Bean... beans) throws Throwable
   {
      DeployException deployException = null;
      try
      {
         if (beans != null)
         {
            for (Bean bt : beans)
            {
               setBeanStatus(bt.getName(), ServiceLifecycle.NOT_STARTED);
            }

            beansRegistered();

            List<BeanDeployer> deployers = new ArrayList<BeanDeployer>(beans.length);
            List<String> beanNames = Collections.synchronizedList(new ArrayList<String>(beans.length));
            Map<String, List<Method>> uninstall = 
               new ConcurrentHashMap<String, List<Method>>(beans.length);
            Map<String, String> stops =
               Collections.synchronizedMap(new HashMap<String, String>(beans.length));
            Map<String, String> destroys =
               Collections.synchronizedMap(new HashMap<String, String>(beans.length));
            Set<String> ignoreStops = Collections.synchronizedSet(new HashSet<String>(beans.length));
            Set<String> ignoreDestroys = Collections.synchronizedSet(new HashSet<String>(beans.length));

            final CountDownLatch beansLatch = new CountDownLatch(beans.length);

            for (Bean bt : beans)
            {
               BeanDeployer deployer = new BeanDeployer(bt, beanNames, uninstall,
                                                        stops, destroys, ignoreStops, ignoreDestroys,
                                                        this, beansLatch, kernelClassLoader, log);
               deployers.add(deployer);

               getExecutorService().submit(deployer);
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
            {
               BeanDeployment deployment = new BeanDeploymentImpl(null, beanNames, uninstall, 
                                                                  stops, destroys, ignoreStops, ignoreDestroys, this);
               registerDeployment(deployment);
               return deployment;
            }
         }
      }
      catch (Throwable t)
      {
         log.log(Level.SEVERE, t.getMessage(), t);
         throw new DeployException("Unable to deploy: " + beans, t);
      }

      if (deployException != null)
         throw new DeployException("Unable to deploy: " + beans, deployException);

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void uninstall(BeanDeployment beans) throws Throwable
   {
      if (beans != null)
      {
         preUndeploy(true);

         shutdownDeployment(beans);

         postUndeploy(true);
      }
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
    * @param cdl The count down latch that should be notified
    */
   void addBeanDependants(String from, String to, CountDownLatch cdl)
   {
      // Register the 'from' -> 'to' binding
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

      // Register the count down latch if the 'to' hasn't started
      ServiceLifecycle slc = getBeanStatus(to);
      boolean doCountDown = true;

      if (slc != ServiceLifecycle.STARTED && slc != ServiceLifecycle.ERROR)
      {
         synchronized (beanLatches)
         {
            slc = getBeanStatus(to);
            if (slc != ServiceLifecycle.STARTED && slc != ServiceLifecycle.ERROR)
            {
               List<CountDownLatch> l = beanLatches.get(to);
               if (l == null)
               {
                  l = Collections.synchronizedList(new ArrayList<CountDownLatch>(1));
                  beanLatches.put(to, l);
               }
         
               l.add(cdl);
               doCountDown = false;
            }
         }
      }

      if (doCountDown)
         cdl.countDown();
   }

   /**
    * Register deployment
    * @param deployment The deployment
    */
   void registerDeployment(Deployment deployment)
   {
      deployments.add(deployment);
      
      if (started && deployment instanceof BeanDeployment)
         incallback();
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
      if (mainDeployer == null)
         throw new IllegalStateException("Kernel not started");

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
    * {@inheritDoc}
    */
   public String dump()
   {
      StringBuilder sb = new StringBuilder();

      Iterator<String> it = new TreeSet<String>(beans.keySet()).iterator();
      while (it.hasNext())
      {
         dumpBean(sb, it.next());

         if (it.hasNext())
            sb.append("\n");
      }

      return sb.toString();
   }

   /**
    * {@inheritDoc}
    */
   public String dump(String name)
   {
      StringBuilder sb = new StringBuilder();

      dumpBean(sb, name);

      return sb.toString();
   }

   /**
    * Dump a bean
    * @param sb The string builder
    * @param name The name
    */
   private void dumpBean(StringBuilder sb, String name)
   {
      if (beans.containsKey(name))
      {
         sb.append("Bean \"").append(name).append("\" (").append(beanStatus.get(name)).append(")\n");
         sb.append("  Class: ").append((beans.get(name)).getClass().getName()).append("\n");

         if (beanDependants.containsKey(name))
         {
            sb.append("  DependsOn: ");
            Iterator<String> it = beanDependants.get(name).iterator();
            while (it.hasNext())
            {
               sb.append(it.next());
               if (it.hasNext())
                  sb.append(", ");
            }
         }
         else
         {
            sb.append("  DependsOn: None");
         }
         sb.append("\n");
         
      }
      else
      {
         sb.append("Bean \"").append(name).append("\" not found");
      }
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
         List<Callback> newCallbacks = Collections.synchronizedList(new ArrayList<Callback>(1));
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
         List<Callback> newCallbacks = Collections.synchronizedList(new ArrayList<Callback>(1));
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
                     if (registeredCallbacks == null || !registeredCallbacks.contains(cb))
                     {
                        if (registeredCallbacks == null)
                           registeredCallbacks = new ArrayList<Callback>(1);

                        try
                        {
                           Method m = cb.getMethod();
                           Object instance = cb.getInstance();
                           
                           m.setAccessible(true);
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
      beanLatches.clear();

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

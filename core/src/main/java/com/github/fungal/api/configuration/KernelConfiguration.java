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

package com.github.fungal.api.configuration;

import com.github.fungal.api.classloading.ClassLoaderFactory;
import com.github.fungal.api.events.EventListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Kernel configuration implementation
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 * @see com.github.fungal.api.classloading.ClassLoaderFactory
 * @see com.github.fungal.api.events.EventListener
 */
public class KernelConfiguration
{
   /** Name */
   private String name;

   /** Home */
   private URL home;

   /** Kernel class loader */
   private int classLoader;

   /** Library */
   private String library;

   /** Configuration */
   private String configuration;

   /** System */
   private String system;

   /** Deploy */
   private String deploy;

   /** Do parallel deployment in deploy */
   private boolean parallelDeploy;

   /** Bind address */
   private String bindAddress;

   /** Thread group */
   private ThreadGroup threadGroup;

   /** Remote access */
   private boolean remoteAccess;

   /** Remote port */
   private int remotePort;

   /** Hot deployment */
   private boolean hotDeployment;

   /** Hot deployment internal in seconds */
   private int hotDeploymentInterval;

   /** Event listeners */
   private List<EventListener> eventListeners;

   /**
    * Constructor
    */
   public KernelConfiguration()
   {
      name = "fungal";
      home = null;
      classLoader = ClassLoaderFactory.TYPE_EXPORT;
      library = "lib";
      configuration = "config";
      system = "system";
      deploy = "deploy";
      parallelDeploy = true;
      bindAddress = null;
      threadGroup = null;
      remoteAccess = true;
      remotePort = 1202;
      hotDeployment = true;
      hotDeploymentInterval = 5;
      eventListeners = null;
   }

   /**
    * Set the name; default <code>fungal</code>
    * @param n The name
    * @return The configuration
    */
   public KernelConfiguration name(String n)
   {
      this.name = n;

      return this;
   }

   /**
    * Get the name
    * @return The name
    */
   public String getName()
   {
      return name;
   }

   /**
    * Set the home; default <code>null</code>
    * @param h The home
    * @return The configuration
    */
   public KernelConfiguration home(URL h)
   {
      this.home = h;

      return this;
   }

   /**
    * Get the home
    * @return The home
    */
   public URL getHome()
   {
      return home;
   }

   /**
    * Set the kernel class loader type; default <code>ClassLoaderFactory.TYPE_EXPORT</code>
    * @param type The type
    * @return The configuration
    */
   public KernelConfiguration classLoader(int type)
   {
      this.classLoader = type;

      return this;
   }

   /**
    * Get the kernel class loader type
    * @return The type
    */
   public int getClassLoader()
   {
      return classLoader;
   }

   /**
    * Set the library directory; default <code>lib</code>
    * @param value The value
    * @return The configuration
    */
   public KernelConfiguration library(String value)
   {
      this.library = value;

      return this;
   }

   /**
    * Get the library directory
    * @return The value
    */
   public String getLibrary()
   {
      return library;
   }

   /**
    * Set the configuration directory; default <code>config</code>
    * @param value The value
    * @return The configuration
    */
   public KernelConfiguration configuration(String value)
   {
      this.configuration = value;

      return this;
   }

   /**
    * Get the configuration directory
    * @return The value
    */
   public String getConfiguration()
   {
      return configuration;
   }

   /**
    * Set the system directory; default <code>system</code>
    * @param value The value
    * @return The configuration
    */
   public KernelConfiguration system(String value)
   {
      this.system = value;

      return this;
   }

   /**
    * Get the system directory
    * @return The value
    */
   public String getSystem()
   {
      return system;
   }

   /**
    * Set the deploy directory; default <code>deploy</code>
    * @param value The value
    * @return The configuration
    */
   public KernelConfiguration deploy(String value)
   {
      this.deploy = value;

      return this;
   }

   /**
    * Get the deploy directory
    * @return The value
    */
   public String getDeploy()
   {
      return deploy;
   }

   /**
    * Set if the files in the system and deploy directories should deployed
    * in parallel; default <code>true</code>
    * @param value The value
    * @return The configuration
    */
   public KernelConfiguration parallelDeploy(boolean value)
   {
      this.parallelDeploy = value;

      return this;
   }

   /**
    * Get if the files in the system and deploy directories should be deployed
    * in parallel
    * @return The value
    */
   public boolean isParallelDeploy()
   {
      return parallelDeploy;
   }

   /**
    * Set the bind address; default <code>null</code>
    * @param ba The value
    * @return The configuration
    */
   public KernelConfiguration bindAddress(String ba)
   {
      this.bindAddress = ba;

      return this;
   }

   /**
    * Get the bind address
    * @return The value
    */
   public String getBindAddress()
   {
      return bindAddress;
   }

   /**
    * Set the thread group; default <code>null</code>
    * @param tg The value
    * @return The configuration
    */
   public KernelConfiguration threadGroup(ThreadGroup tg)
   {
      this.threadGroup = tg;

      return this;
   }

   /**
    * Get the thread group
    * @return The value
    */
   public ThreadGroup getThreadGroup()
   {
      return threadGroup;
   }

   /**
    * Set the remote access; default <code>true</code>
    * @param v The value
    * @return The configuration
    */
   public KernelConfiguration remoteAccess(boolean v)
   {
      this.remoteAccess = v;

      return this;
   }

   /**
    * Is remote access enabled ?
    * @return The value
    */
   public boolean isRemoteAccess()
   {
      return remoteAccess;
   }

   /**
    * Set the port for remote access; default <code>1202</code>
    * @param v The value
    * @return The configuration
    */
   public KernelConfiguration remotePort(int v)
   {
      this.remotePort = v;

      return this;
   }

   /**
    * Get the remote port
    * @return The value
    */
   public int getRemotePort()
   {
      return remotePort;
   }

   /**
    * Should hot deployment be enabled for the deploy directory; default <code>true</code>
    * @param v The value
    * @return The configuration
    */
   public KernelConfiguration hotDeployment(boolean v)
   {
      this.hotDeployment = v;

      return this;
   }

   /**
    * Is hot deployment enabled ?
    * @return The value
    */
   public boolean isHotDeployment()
   {
      return hotDeployment;
   }

   /**
    * Set the interval in seconds for the hot deployment check; default <code>5</code>
    * @param v The value
    * @return The configuration
    */
   public KernelConfiguration hotDeploymentInterval(int v)
   {
      this.hotDeploymentInterval = v;

      return this;
   }

   /**
    * Get the hot deployment interval in seconds
    * @return The value
    */
   public int getHotDeploymentInterval()
   {
      return hotDeploymentInterval;
   }

   /**
    * Add an event listener
    * @param el The event listener
    * @return The configuration
    */
   public KernelConfiguration eventListener(EventListener el)
   {
      if (eventListeners == null)
         eventListeners = new ArrayList<EventListener>(1);

      eventListeners.add(el);

      return this;
   }

   /**
    * Get the event listeners
    * @return The value
    */
   public List<EventListener> getEventListeners()
   {
      if (eventListeners == null)
         return Collections.emptyList();

      return Collections.unmodifiableList(eventListeners);
   }
}

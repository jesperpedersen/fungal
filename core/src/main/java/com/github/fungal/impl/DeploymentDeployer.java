/*
 * The Fungal kernel project
 * Copyright (C) 2011
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
import com.github.fungal.deployment.Unmarshaller;
import com.github.fungal.spi.deployers.CloneableDeployer;
import com.github.fungal.spi.deployers.DeployException;
import com.github.fungal.spi.deployers.Deployer;
import com.github.fungal.spi.deployers.Deployment;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
            for (Bean bt : deployment.getBean())
            {
               kernel.setBeanStatus(bt.getName(), ServiceLifecycle.NOT_STARTED);
            }

            kernel.beansRegistered();

            List<BeanDeployer> deployers = new ArrayList<BeanDeployer>(deployment.getBean().size());
            List<String> beans = Collections.synchronizedList(new ArrayList<String>(deployment.getBean().size()));
            Map<String, List<Method>> uninstall = 
               new ConcurrentHashMap<String, List<Method>>(deployment.getBean().size());
            Map<String, String> stops =
               Collections.synchronizedMap(new HashMap<String, String>(deployment.getBean().size()));
            Map<String, String> destroys =
               Collections.synchronizedMap(new HashMap<String, String>(deployment.getBean().size()));
            Set<String> ignoreStops = Collections.synchronizedSet(new HashSet<String>(deployment.getBean().size()));
            Set<String> ignoreDestroys = Collections.synchronizedSet(new HashSet<String>(deployment.getBean().size()));

            final CountDownLatch beansLatch = new CountDownLatch(deployment.getBean().size());

            for (Bean bt : deployment.getBean())
            {
               BeanDeployer deployer = new BeanDeployer(bt, beans, uninstall,
                                                        stops, destroys, ignoreStops, ignoreDestroys,
                                                        kernel, beansLatch, parent, log);
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
               return new BeanDeploymentImpl(url, beans, uninstall,
                                             stops, destroys, ignoreStops, ignoreDestroys, kernel);
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

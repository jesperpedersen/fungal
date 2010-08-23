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

import com.github.fungal.spi.deployers.CloneableDeployer;
import com.github.fungal.spi.deployers.Deployer;
import com.github.fungal.spi.deployers.DeployerOrder;
import com.github.fungal.spi.deployers.Deployment;
import com.github.fungal.spi.deployers.MultiStageDeployer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The main deployer for Fungal
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public final class MainDeployerImpl implements Cloneable, MainDeployerImplMBean
{
   private static List<Deployer> deployers = Collections.synchronizedList(new ArrayList<Deployer>());

   private KernelImpl kernel;
   private List<Deployer> copy;

   /**
    * Constructor
    * @param kernel The kernel
    */
   public MainDeployerImpl(KernelImpl kernel)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Kernel is null");

      this.kernel = kernel;
      this.copy = null;
   }

   /**
    * Add deployer
    * @param deployer The deployer
    */
   public void addDeployer(Deployer deployer)
   {
      if (deployer == null)
         throw new IllegalArgumentException("Deployer is null");

      deployers.add(deployer);
   }

   /**
    * Deploy uses the kernel class loader as the parent class loader
    * @param url The URL for the deployment
    * @exception Throwable If an error occurs
    */
   public synchronized void deploy(URL url) throws Throwable
   {
      deploy(url, true, kernel.getKernelClassLoader());
   }

   /**
    * Deploy
    * @param url The URL for the deployment
    * @param deployerPhases Run DeployerPhases hooks
    * @param classLoader The parent class loader for the deployment
    * @exception Throwable If an error occurs
    */
   @SuppressWarnings("unchecked")
   public synchronized void deploy(URL url, boolean deployerPhases, ClassLoader classLoader) throws Throwable
   {
      if (url == null)
         throw new IllegalArgumentException("URL is null");

      if (classLoader == null)
         throw new IllegalArgumentException("ClassLoader is null");

      if (copy == null || copy.size() != deployers.size())
      {
         List sorted = new ArrayList();
         List unsorted = new ArrayList();

         for (Deployer deployer : deployers)
         {
            if (deployer instanceof DeployerOrder)
            {
               if (deployer instanceof CloneableDeployer)
               {
                  try
                  {
                     sorted.add(((CloneableDeployer)deployer).clone());
                  }
                  catch (CloneNotSupportedException cnse)
                  {
                     // Add the deployer and assume synchronized access
                     sorted.add(deployer);
                  }
               }
               else
               {
                  // Assume synchronized access to deploy()
                  sorted.add(deployer);
               }
            }
            else
            {
               if (deployer instanceof CloneableDeployer)
               {
                  try
                  {
                     unsorted.add(((CloneableDeployer)deployer).clone());
                  }
                  catch (CloneNotSupportedException cnse)
                  {
                     // Add the deployer and assume synchronized access
                     unsorted.add(deployer);
                  }
               }
               else
               {
                  // Assume synchronized access to deploy()
                  unsorted.add(deployer);
               }
            }
         }

         Collections.sort(sorted, new DeployerOrderComparator());

         copy = new ArrayList<Deployer>(deployers.size());
         copy.addAll(sorted);
         copy.addAll(unsorted);
      }

      boolean done = false;
      int copySize = copy.size();

      if (deployerPhases)
         kernel.preDeploy(true);

      for (int i = 0; !done && i < copySize; i++)
      {
         Deployer deployer = copy.get(i);
            
         Deployment deployment = deployer.deploy(url, classLoader);
         if (deployment != null)
         {
            registerDeployment(deployment);

            if (!(deployer instanceof MultiStageDeployer))
               done = true;
         }
      }

      if (deployerPhases)
         kernel.postDeploy(true);
   }

   /**
    * Undeploy
    * @param url The URL for the deployment
    * @exception Throwable If an error occurs
    */
   public synchronized void undeploy(URL url) throws Throwable
   {
      if (url == null)
         throw new IllegalArgumentException("URL is null");

      Deployment deployment = kernel.getDeployment(url);
      if (deployment != null)
      {
         kernel.preUndeploy(true);

         unregisterDeployment(deployment);

         kernel.postUndeploy(true);
      }
   }

   /**
    * Register a deployment -- advanced usage
    * @param deployment The deployment
    */
   public synchronized void registerDeployment(Deployment deployment)
   {
      if (deployment == null)
         throw new IllegalArgumentException("Deployment is null");

      kernel.registerDeployment(deployment);
   }

   /**
    * Unregister a deployment -- advanced usage
    * @param deployment The deployment
    * @exception Throwable If an error occurs
    */
   public synchronized void unregisterDeployment(Deployment deployment) throws Throwable
   {
      if (deployment == null)
         throw new IllegalArgumentException("Deployment is null");

      kernel.shutdownDeployment(deployment);
   }

   /**
    * Clone
    * @return The copy of the object
    * @exception CloneNotSupportedException Thrown if a copy can't be created
    */
   public Object clone() throws CloneNotSupportedException
   {
      return new MainDeployerImpl(kernel);
   }
}

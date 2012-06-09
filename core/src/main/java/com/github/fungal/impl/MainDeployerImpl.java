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
import com.github.fungal.spi.deployers.Deployment;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main deployer for Fungal
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public final class MainDeployerImpl implements Cloneable, MainDeployerImplMBean
{
   /** The logger */
   private Logger log = Logger.getLogger(MainDeployerImpl.class.getName());

   /** Trace logging enabled */
   private boolean trace = log.isLoggable(Level.FINER);

   private KernelImpl kernel;
   private Deployers deployers;

   /**
    * Constructor
    * @param kernel The kernel
    * @param deployers The deployers
    */
   MainDeployerImpl(KernelImpl kernel, Deployers deployers)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Kernel is null");

      if (deployers == null)
         throw new IllegalArgumentException("Deployers is null");

      this.kernel = kernel;
      this.deployers = deployers;
   }

   /**
    * Add deployer
    * @param deployer The deployer
    */
   void addDeployer(Deployer deployer)
   {
      deployers.addDeployer(deployer);
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
   synchronized void deploy(URL url, boolean deployerPhases, ClassLoader classLoader) throws Throwable
   {
      if (url == null)
         throw new IllegalArgumentException("URL is null");

      if (classLoader == null)
         throw new IllegalArgumentException("ClassLoader is null");

      if (trace)
         log.log(Level.FINER, "Deploy: " + url.toExternalForm());

      List<Deployer> copy = new ArrayList<Deployer>(deployers.getDeployers().size());

      for (Deployer deployer : deployers.getDeployers())
      {
         if (deployer.accepts(url))
         {
            if (deployer instanceof CloneableDeployer)
            {
               try
               {
                  copy.add(((CloneableDeployer)deployer).clone());
               }
               catch (CloneNotSupportedException cnse)
               {
                  // Add the deployer and assume synchronized access
                  copy.add(deployer);
               }
            }
            else
            {
               // Assume synchronized access to deploy()
               copy.add(deployer);
            }
         }
      }

      Collections.sort(copy, new DeployerComparator());

      if (deployerPhases)
         kernel.preDeploy(true);

      ContextImpl context = new ContextImpl(kernel);

      Throwable throwable = null;
      try
      {
         for (int i = 0; i < copy.size(); i++)
         {
            Deployer deployer = copy.get(i);
            
            Deployment deployment = deployer.deploy(url, context, classLoader);
            if (deployment != null)
            {
               registerDeployment(deployment);
            }
         }
      }
      catch (Throwable t)
      {
         throwable = t;
      }

      context.clear();

      if (deployerPhases && throwable == null)
         kernel.postDeploy(true);

      if (throwable != null)
         throw throwable;
   }

   /**
    * Undeploy
    * @param url The URL for the deployment
    * @exception Throwable If an error occurs
    */
   public synchronized void undeploy(URL url) throws Throwable
   {
      undeploy(url, true);
   }

   /**
    * Undeploy
    * @param url The URL for the deployment
    * @param deployerPhases Enable deployer phases
    * @exception Throwable If an error occurs
    */
   synchronized void undeploy(URL url, boolean deployerPhases) throws Throwable
   {
      if (url == null)
         throw new IllegalArgumentException("URL is null");

      if (trace)
         log.log(Level.FINER, "Undeploy: " + url.toExternalForm());

      List<Deployment> deployments = kernel.getDeployments(url, false);
      if (deployments != null)
      {
         if (deployerPhases)
            kernel.preUndeploy(true);

         Collections.reverse(deployments);

         Throwable throwable = null;
         for (Deployment deployment : deployments)
         {
            try
            {
               unregisterDeployment(deployment);
            }
            catch (Throwable t)
            {
               if (throwable == null)
                  throwable = t;
            }
         }

         if (deployerPhases && throwable == null)
            kernel.postUndeploy(true);

         if (throwable != null)
            throw throwable;
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

      if (trace)
         log.log(Level.FINER, "RegisterDeployment: " + deployment);

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

      if (trace)
         log.log(Level.FINER, "UnregisterDeployment: " + deployment);

      kernel.shutdownDeployment(deployment);
   }

   /**
    * Clone
    * @return The copy of the object
    * @exception CloneNotSupportedException Thrown if a copy can't be created
    */
   public Object clone() throws CloneNotSupportedException
   {
      MainDeployerImpl md = (MainDeployerImpl)super.clone();
      md.kernel = kernel;
      md.deployers = deployers;
      
      return md;
   }
}

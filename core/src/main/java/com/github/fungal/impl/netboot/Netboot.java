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

package com.github.fungal.impl.netboot;

import com.github.fungal.api.util.FileUtil;
import com.github.fungal.api.util.Injection;
import com.github.fungal.bootstrap.Bootstrap;
import com.github.fungal.bootstrap.DependencyType;
import com.github.fungal.bootstrap.PropertyType;
import com.github.fungal.bootstrap.ProtocolType;
import com.github.fungal.bootstrap.ServerType;
import com.github.fungal.spi.netboot.Protocol;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * Netboot functionality
 */
public class Netboot
{
   /**
    * Constructor
    */
   private Netboot()
   {
   }

   /**
    * Resolve all dependencies
    * @param executorService The executor service
    * @param bootstrap The bootstrap descriptor
    * @param repositoryDirectory The repository directory
    * @param rootDirectory The root directory
    * @return True if netboot was active; otherwise false
    * @exception ResolveException Thrown if an artifact can't be resolved
    */
   public static boolean resolve(ExecutorService executorService,
                                 Bootstrap bootstrap, 
                                 File repositoryDirectory,
                                 File rootDirectory)
      throws ResolveException
   {
      if (bootstrap != null &&
          bootstrap.getServers() != null && bootstrap.getServers().getServer().size() > 0 &&
          bootstrap.getDependencies() != null && bootstrap.getDependencies().getDependency().size() > 0)
      {
         if (!repositoryDirectory.exists())
         {
            if (!repositoryDirectory.mkdirs())
               throw new ResolveException("Repository directory " + repositoryDirectory.getAbsolutePath() + 
                                          " couldn't be created");
         }

         if (!rootDirectory.exists())
         {
            throw new ResolveException("Root directory " + rootDirectory.getAbsolutePath() + 
                                       " doesn't exist");
         }

         try
         {
            DependencyTracker tracker = new DependencyTracker();
            List<DependencyType> dependencies = bootstrap.getDependencies().getDependency();
            List<ProtocolType> protocols = bootstrap.getProtocols().getProtocol();
            List<ServerType> servers = bootstrap.getServers().getServer();

            Map<String, Protocol> protocolMap = 
               new HashMap<String, Protocol>(protocols.size() != 0 ? protocols.size() : 1);

            if (protocols.size() > 0)
            {
               Injection injection = new Injection();
               for (ProtocolType pt : protocols)
               {
                  if (pt.getId() == null || pt.getId().trim().equals(""))
                     throw new IllegalArgumentException("Protocol id must be defined");

                  if (pt.getClassName() == null || pt.getClassName().trim().equals(""))
                     throw new IllegalArgumentException("Protocol class name must be defined");

                  Class<?> clz = Class.forName(pt.getClassName(), true, Thread.currentThread().getContextClassLoader());
                  Protocol p = (Protocol)clz.newInstance();

                  for (PropertyType property : pt.getProperty())
                  {
                     injection.inject(p, property.getName(), property.getValue());
                  }
                  
                  protocolMap.put(pt.getId(), p);
               }
            }
            else
            {
               protocolMap.put("http", new Http());
            }

            List<DependencyResolver> dependencyResolvers = new ArrayList<DependencyResolver>(dependencies.size());
            final CountDownLatch dependencyLatch = new CountDownLatch(dependencies.size());

            for (DependencyType dependency : dependencies)
            {
               DependencyResolver dependencyResolver = 
                  new DependencyResolver(servers, protocolMap, dependency, repositoryDirectory, 
                                         rootDirectory, tracker, dependencyLatch);

               dependencyResolvers.add(dependencyResolver);

               executorService.execute(dependencyResolver);
            }

            dependencyLatch.await();

            Iterator<DependencyResolver> it = dependencyResolvers.iterator();
            while (it.hasNext())
            {
               DependencyResolver resolver = it.next();
               if (resolver.getResolveException() != null)
               {
                  throw resolver.getResolveException();
               }
            }

            return true;
         }
         catch (InterruptedException ie)
         {
            Thread.interrupted();
            throw new ResolveException("Interrupted while resolving dependencies");
         }
         catch (Throwable t)
         {
            throw new ResolveException("Exception while resolving dependencies", t);
         }
      }

      return false;
   }

   /**
    * Dependency resolver
    */
   static class DependencyResolver implements Runnable
   {
      /** The servers */
      private List<ServerType> servers;

      /** The supported protocols */
      private Map<String, Protocol> protocolMap;

      /** The dependency */
      private DependencyType dependency;

      /** The repository directory */
      private File repositoryDirectory;

      /** The root directory */
      private File rootDirectory;

      /** The tracker */
      private DependencyTracker tracker;

      /** The latch */
      private CountDownLatch latch;

      /** ResolveException */
      private ResolveException resolveException;

      /**
       * Constructor
       * @param servers The servers
       * @param protocolMap The protocols
       * @param dependency The dependency
       * @param repositoryDirectory The repository directory
       * @param rootDirectory The root directory
       * @param tracker The dependency tracker
       * @param latch The latch
       */
      public DependencyResolver(final List<ServerType> servers,
                                final Map<String, Protocol> protocolMap,
                                final DependencyType dependency,
                                final File repositoryDirectory,
                                final File rootDirectory,
                                final DependencyTracker tracker,
                                final CountDownLatch latch)
      {
         this.servers = servers;
         this.protocolMap = protocolMap;
         this.dependency = dependency;
         this.repositoryDirectory = repositoryDirectory;
         this.rootDirectory = rootDirectory;
         this.tracker = tracker;
         this.latch = latch;
         this.resolveException = null;
      }

      /**
       * Run
       */
      public void run()
      {
         try
         {
            Repository repository = new Maven();
            List<DependencyType> artifacts = repository.resolve(servers, 
                                                                protocolMap, 
                                                                dependency, 
                                                                repositoryDirectory, 
                                                                tracker);

            if (artifacts != null)
            {
               FileUtil fileUtil = new FileUtil();

               for (DependencyType dependency : artifacts)
               {
                  File src = repository.getFile(dependency, repositoryDirectory);
                  File dest = new File(rootDirectory, 
                                       dependency.getTarget().replace('/', File.separatorChar) + File.separatorChar +
                                       dependency.getArtifact() + "." + dependency.getExt());

                  if (dest.getParentFile() != null && !dest.getParentFile().exists())
                  {
                     if (!dest.getParentFile().mkdirs())
                        throw new ResolveException("Directory " + dest.getParentFile().getAbsolutePath() + 
                                                   " couldn't be created");
                  }
                  
                  fileUtil.copy(src, dest);
               }
            }
         }
         catch (IOException ioe)
         {
            resolveException = new ResolveException("IOException while resolving", ioe);
         }
         catch (ResolveException re)
         {
            resolveException = re;
         }

         latch.countDown();
      }

      /**
       * Get resolve exception
       * @return null if no error; otherwise the exception
       */
      public ResolveException getResolveException()
      {
         return resolveException;
      }
   }
}

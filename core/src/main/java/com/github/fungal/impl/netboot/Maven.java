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

import com.github.fungal.bootstrap.DependencyType;
import com.github.fungal.bootstrap.ServerType;
import com.github.fungal.spi.netboot.Protocol;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represent a Maven repository
 */
public class Maven extends AbstractRepository
{
   /**
    * Constructor
    */
   public Maven()
   {
   }

   /**
    * {@inheritDoc}
    */
   public List<DependencyType> resolve(List<ServerType> servers,
                                       Map<String, Protocol> protocolMap,
                                       DependencyType dependency,
                                       File repository,
                                       DependencyTracker tracker)
      throws ResolveException
   {
      if ("pom".equals(dependency.getExt()))
      {
         return downloadPom(servers, 
                            protocolMap,
                            dependency,
                            repository,
                            tracker);
      }
      else
      {
         return downloadArtifact(servers, 
                                 protocolMap,
                                 dependency,
                                 repository,
                                 tracker);
      }
   }

   private List<DependencyType> downloadPom(List<ServerType> servers,
                                            Map<String, Protocol> protocolMap,
                                            DependencyType dependency,
                                            File repository,
                                            DependencyTracker tracker)
      throws ResolveException
   {
      List<DependencyType> result = downloadArtifact(servers, protocolMap, dependency, repository, tracker);

      if (result.size() == 0)
         return result;

      try
      {
         MavenUnmarshaller unmarshaller = new MavenUnmarshaller();
         File f = new File(repository, getPath(dependency));
         List<DependencyType> dependencies = unmarshaller.unmarshal(f.toURI().toURL());         

         if (dependencies != null && dependencies.size() > 0)
         {
            Iterator<DependencyType> dit = dependencies.iterator();
            while (dit.hasNext())
            {
               DependencyType dep = dit.next();
               List<DependencyType> l = downloadArtifact(servers, protocolMap, dep, repository, tracker);
               result.addAll(l);
            }
         }

         return result;
      }
      catch (Throwable t)
      {
         throw new ResolveException("The dependency couldn't be parsed", dependency);
      }
   }
}

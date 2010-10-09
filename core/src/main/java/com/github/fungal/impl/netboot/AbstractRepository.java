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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Represent a base class for a repository implementation
 */
public abstract class AbstractRepository implements Repository
{
   /**
    * Constructor
    */
   public AbstractRepository()
   {
   }

   /**
    * {@inheritDoc}
    */
   public File getFile(DependencyType dependency, File repository) throws IOException
   {
      File f = new File(repository, getPath(dependency));
      
      if (f.exists())
      {
         return f;
      }

      throw new IOException("Dependency " + dependency + " doesn't exist in the repository " +
                            repository.getAbsolutePath());

   }

   /**
    * Download an artifact
    * @param servers The servers
    * @param protocolMap The protocols
    * @param dependency The dependency
    * @param repository The repository
    * @param tracker The dependency tracker
    * @return The list of dependencies downloaded
    * @exception ResolveException Thrown in case of an error
    */
   protected List<DependencyType> downloadArtifact(List<ServerType> servers,
                                                   Map<String, Protocol> protocolMap,
                                                   DependencyType dependency,
                                                   File repository,
                                                   DependencyTracker tracker)
      throws ResolveException
   {
      if (tracker.isTracked(dependency))
         return Collections.emptyList();

      if (!tracker.track(dependency))
         return Collections.emptyList();

      List<DependencyType> result = getArtifact(repository, dependency);

      if (result != null)
         return result;

      result = new ArrayList<DependencyType>(1);

      File f = new File(repository, getPath(dependency));

      if (!f.getParentFile().exists() && !f.getParentFile().mkdirs())
         throw new ResolveException(f.getParent() + " couldn't be created");

      Iterator<ServerType> it = servers.iterator();
      while (result.isEmpty() && it.hasNext())
      {
         try
         {
            ServerType server = it.next();
            String path = server.getValue();

            if (!path.endsWith("/"))
               path = path + "/";

            path += Pattern.resolve(server.getPattern(), dependency.getOrganisation(), dependency.getModule(),
                                    dependency.getRevision(), dependency.getArtifact(), dependency.getClassifier(),
                                    dependency.getExt());
            
            String protocolKey = server.getProtocol();

            if (protocolKey == null || protocolKey.trim().equals(""))
               protocolKey = "http";

            Protocol protocol = protocolMap.get(protocolKey);

            if (protocol != null)
            {
               Protocol copy = protocol.clone();
               if (copy.download(path, f))
                  result.add(dependency);
            }
            else
            {
               throw new ResolveException("Protocol (" + protocolKey + ") not defined for server " + server.getValue());
            }
         }
         catch (CloneNotSupportedException cnse)
         {
            // Shouldn't happen
         }
      }

      if (result.isEmpty())
         throw new ResolveException("The dependency couldn't be resolved", dependency);

      return result;
   }

   /**
    * Get an artifact
    * @param repository The repository
    * @param dependency The dependency
    * @return The dependency if it exists in the repository; otherwise null
    */
   protected List<DependencyType> getArtifact(File repository, DependencyType dependency)
   {
      File f = new File(repository, getPath(dependency));
      
      if (f.exists())
      {
         List<DependencyType> l = new ArrayList<DependencyType>(1);
         l.add(dependency);
         return l;
      }

      return null;
   }

   /**
    * Get the path for a dependency
    * @param dependency The dependency
    * @return The path
    */
   protected String getPath(DependencyType dependency)
   {
      String path = dependency.getOrganisation().replace('.', File.separatorChar) + File.separatorChar + 
         dependency.getArtifact() + File.separatorChar +
         dependency.getRevision() + File.separatorChar +
         dependency.getArtifact() + "-" + dependency.getRevision() + "." + dependency.getExt();

      return path;
   }

   /**
    * {@inheritDoc}
    */
   public abstract List<DependencyType> resolve(List<ServerType> servers,
                                                Map<String, Protocol> protocolMap,
                                                DependencyType dependency,
                                                File repository,
                                                DependencyTracker tracker)
      throws ResolveException;
}

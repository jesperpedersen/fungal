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
import java.util.List;
import java.util.Map;

/**
 * Represents a repository
 */
public interface Repository
{
   /**
    * Resolve a dependency
    * @param servers The servers
    * @param protocolMap The protocols
    * @param dependency The dependency
    * @param repository The repository for the container
    * @param tracker The dependency tracker
    * @return The list of dependencies downloaded
    * @exception ResolveException Thrown if the dependency can't be resolved
    */
   public List<DependencyType> resolve(List<ServerType> servers, 
                                       Map<String, Protocol> protocolMap,
                                       DependencyType dependency, 
                                       File repository,
                                       DependencyTracker tracker)
      throws ResolveException;

   /**
    * Get the file handle for a dependency
    * @param dependency The dependency
    * @param repository The repository for the container
    * @return The file handle
    * @exception IOException Thrown if the dependency doesn't exists
    */
   public File getFile(DependencyType dependency, File repository) throws IOException;
}

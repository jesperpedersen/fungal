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

import java.util.HashSet;
import java.util.Set;

/**
 * A dependency tracker that makes sure that a dependency is only
 * downloaded once per netboot sequence
 */
public class DependencyTracker
{
   private Set<DependencyType> dependencies;

   /**
    * Constructor
    */
   public DependencyTracker()
   {
      dependencies = new HashSet<DependencyType>();
   }

   /**
    * Is the dependency already been tracked
    * @param dependency The dependency
    * @return True if the dependency is tracked; otherwise false
    */
   public boolean isTracked(DependencyType dependency)
   {
      return dependencies.contains(dependency);
   }

   /**
    * Track a dependency
    * @param dependency The dependency
    * @return True if the caller should track the dependency; otherwise false
    */
   public boolean track(DependencyType dependency)
   {
      synchronized (dependencies)
      {
         if (!isTracked(dependency))
         {
            dependencies.add(dependency);
            return true;
         }
         else
         {
            return false;
         }
      }
   }
}

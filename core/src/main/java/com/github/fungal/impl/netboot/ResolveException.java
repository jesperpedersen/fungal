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

/**
 * Thrown in case a dependency can't be resolved against the list of 
 * servers given
 */
public class ResolveException extends Exception
{
   private static final long serialVersionUID = 1L;

   /**
    * Constructor
    * @param message The message
    */
   public ResolveException(String message)
   {
      super(message);
   }

   /**
    * Constructor
    * @param message The message
    * @param t The throwable
    */
   public ResolveException(String message, Throwable t)
   {
      super(message, t);
   }

   /**
    * Constructor
    * @param message The message
    * @param dt The dependency
    */
   public ResolveException(String message, DependencyType dt)
   {
      super(message + " [" + dt + "]");
   }
}

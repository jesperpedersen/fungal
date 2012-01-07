/*
 * The Fungal kernel project
 * Copyright (C) 2012
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

package com.github.fungal.spi.deployers;

import com.github.fungal.api.Kernel;

/**
 * Context for a deployment.
 *
 * The context will be kept around for the entire length of the processing
 * of the deployment
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public interface Context
{
   /**
    * Get the kernel instance for the deployment
    * @return The instance
    */
   public Kernel getKernel();

   /**
    * Exists
    * @param key The key of the resource
    * @return True if the key/value pair exists; otherwise false
    */
   public boolean exists(Object key);

   /**
    * Put
    * @param key The key of the resource
    * @param value The value of the resource
    * @return The previous value for the key
    */
   public Object put(Object key, Object value);

   /**
    * Get
    * @param key The key of the resource
    * @return The value of the resource
    */
   public Object get(Object key);

   /**
    * Remove
    * @param key The key of the resource
    * @return The value of the resource
    */
   public Object remove(Object key);
}

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

package com.github.fungal.impl;

import com.github.fungal.api.Kernel;
import com.github.fungal.spi.deployers.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Context implementation for a deployment.
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class ContextImpl implements Context
{
   /* The kernel */
   private Kernel kernel;

   /* The data */
   private Map<Object, Object> data;

   /**
    * Constructor
    * @param kernel The kernel
    */
   ContextImpl(Kernel kernel)
   {
      this.kernel = kernel;
      this.data = Collections.synchronizedMap(new HashMap<Object, Object>());
   }

   /**
    * {@inheritDoc}
    */
   public Kernel getKernel()
   {
      return kernel;
   }

   /**
    * {@inheritDoc}
    */
   public boolean exists(Object key)
   {
      return data.containsKey(key);
   }

   /**
    * {@inheritDoc}
    */
   public Object put(Object key, Object value)
   {
      return data.put(key, value);
   }

   /**
    * {@inheritDoc}
    */
   public Object get(Object key)
   {
      return data.get(key);
   }

   /**
    * {@inheritDoc}
    */
   public Object remove(Object key)
   {
      return data.remove(key);
   }

   /**
    * Clear
    */
   public void clear()
   {
      data.clear();
   }
}

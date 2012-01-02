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

package com.github.fungal.deployment;

import com.github.fungal.api.deployment.Bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a deployment
 */
public class Deployment
{
   private List<Bean> bean;

   /**
    * Constructor
    */
   public Deployment()
   {
      bean = null;
   }

   /**
    * Get the bean values
    * @return The value
    */
   public List<Bean> getBean()
   {
      if (bean == null)
         bean = new ArrayList<Bean>(1);

      return bean;
   }

   /**
    * String representation
    * @return The value
    */
   public String toString()
   {
      if (bean == null)
         return "<null>";

      return bean.toString();
   }
}

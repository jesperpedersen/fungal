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

package com.github.fungal.api.deployment;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a parameter element
 */
public class Parameter
{
   private List<Object> content;
   private String clazz;

   /**
    * Constructor
    */
   public Parameter()
   {
      content = null;
      clazz = null;
   }

   /**
    * Get the content values
    * @return The value
    */
   public List<Object> getContent()
   {
      if (content == null)
         content = new ArrayList<Object>(1);
      
      return this.content;
   }

   /**
    * Get the class
    * @return The value
    */
   public String getClazz()
   {
      return clazz;
   }

   /**
    * Set the class
    * @param value The value
    */
   public void setClazz(String value)
   {
      clazz = value;
   }

   /**
    * String representation
    * @return The string
    */
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb.append("ParameterType@").append(Integer.toHexString(System.identityHashCode(this)));
      sb.append("[clazz=").append(clazz);
      sb.append(" content=").append(content);
      sb.append("]");

      return sb.toString();
   }
}

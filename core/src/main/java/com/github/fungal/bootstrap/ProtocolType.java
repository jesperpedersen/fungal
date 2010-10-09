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

package com.github.fungal.bootstrap;

import java.util.ArrayList;
import java.util.List;

/**
 * A protocol tag
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class ProtocolType
{
   private String id;
   private String className;
   private List<PropertyType> property;

   /**
    * Constructor
    */
   public ProtocolType()
   {
      id = null;
      className = null;
      property = null;
   }

   /**
    * Get the id
    * @return The value
    */
   public String getId()
   {
      return id;
   }

   /**
    * Set the id
    * @param v The value
    */
   public void setId(String v)
   {
      id = v;
   }

   /**
    * Get the class name
    * @return The value
    */
   public String getClassName()
   {
      return className;
   }

   /**
    * Set the class name
    * @param v The value
    */
   public void setClassName(String v)
   {
      className = v;
   }

   /**
    * Get the property
    * @return The value
    */
   public List<PropertyType> getProperty()
   {
      if (property == null)
         property = new ArrayList<PropertyType>(1);

      return property;
   }
}

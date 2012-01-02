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

/**
 * Represents an inject element
 */
public class Inject
{
   private String value;
   private String bean;
   private String property;

   /**
    * Constructor
    * @param bean The bean
    */
   public Inject(String bean)
   {
      this.bean = bean;
      this.value = null;
      this.property = null;
   }

   /**
    * Get the value
    * @return The value
    */
   public String getValue()
   {
      return value;
   }

   /**
    * Set the value
    * @param value The value
    */
   public void setValue(String value)
   {
      this.value = value;
   }

   /**
    * Get the bean
    * @return The value
    */
   public String getBean()
   {
      return bean;
   }

   /**
    * Get the property
    * @return The value
    */
   public String getProperty()
   {
      return property;
   }
   
   /**
    * Set the property
    * @param value The value
    */
   public void setProperty(String value)
   {
      this.property = value;
   }

   /**
    * String representation
    * @return The string
    */
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb.append("Inject@").append(Integer.toHexString(System.identityHashCode(this)));
      sb.append("[value=").append(value);
      sb.append(" bean=").append(bean);
      sb.append(" property=").append(property);
      sb.append("]");

      return sb.toString();
   }
}

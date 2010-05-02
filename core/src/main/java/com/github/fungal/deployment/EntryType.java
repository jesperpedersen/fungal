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

/**
 * Represents an entry
 */
public class EntryType
{
   private KeyType key;
   private ValueType value;
   
   /**
    * Constructor
    */
   public EntryType()
   {
      key = null;
      value = null;
   }

   /**
    * Get the key
    * @return The value
    */
   public KeyType getKey()
   {
      return key;
   }
   
   /**
    * Set the key
    * @param value The value
    */
   public void setKey(KeyType value)
   {
      key = value;
   }

   /**
    * Get the value
    * @return The value
    */
   public ValueType getValue()
   {
      return value;
   }
   
   /**
    * Set the value
    * @param value The value
    */
   public void setValue(ValueType value)
   {
      this.value = value;
   }
}

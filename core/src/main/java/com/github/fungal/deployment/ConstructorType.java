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

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a constructor
 */
public class ConstructorType
{
   private List<ParameterType> parameter;
   private FactoryType factory;
   private String factoryMethod;
   private String factoryClass;
   
   /**
    * Constructor
    */
   public ConstructorType()
   {
      parameter = null;
      factory = null;
      factoryMethod = null;
      factoryClass = null;
   }

   /**
    * Get the parameter values
    * @return The value
    */
   public List<ParameterType> getParameter()
   {
      if (parameter == null)
         parameter = new ArrayList<ParameterType>(1);

      return parameter;
   }

   /**
    * Get the factory
    * @return The value
    */
   public FactoryType getFactory()
   {
      return factory;
   }
   
   /**
    * Set the factory
    * @param value The value
    */
   public void setFactory(FactoryType value)
   {
      factory = value;
   }

   /**
    * Get the factory method
    * @return The value
    */
   public String getFactoryMethod()
   {
      return factoryMethod;
   }
   
   /**
    * Set the factory method
    * @param value The value
    */
   public void setFactoryMethod(String value)
   {
      factoryMethod = value;
   }

   /**
    * Get the factory class
    * @return The value
    */
   public String getFactoryClass()
   {
      return factoryClass;
   }

   /**
    * Set the factory class
    * @param value The value
    */
   public void setFactoryClass(String value)
   {
      factoryClass = value;
   }
}

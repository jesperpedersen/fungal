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

package com.github.fungal.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Injection utility which can inject values into objects - used for the kernel
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
final class Injection extends com.github.fungal.api.util.Injection
{
   /**
    * Constructor
    */
   Injection()
   {
      super();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Method findMethod(Class<?> clz, String methodName, String propertyType)
   {
      return super.findMethod(clz, methodName, propertyType);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Field findField(Class<?> clz, String fieldName, String fieldType)
   {
      return super.findField(clz, fieldName, fieldType);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object getValue(String name, Class<?> clz, Object v, ClassLoader cl) throws Exception
   {
      return super.getValue(name, clz, v, cl);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String getSubstitutionValue(String input)
   {
      return super.getSubstitutionValue(input);
   }
}

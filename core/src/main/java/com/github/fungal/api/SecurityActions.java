/*
 * The Fungal kernel project
 * Copyright (C) 2014
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

package com.github.fungal.api;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Privileged Blocks
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
class SecurityActions
{ 
   /**
    * Constructor
    */
   private SecurityActions()
   {
   }

   /**
    * Set accessibleo
    * @param ao The object
    */
   static void setAccessible(final AccessibleObject ao)
   {
      if (System.getSecurityManager() == null)
         ao.setAccessible(true);

      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
         public Object run()
         {
            ao.setAccessible(true);
            return null;
         }
      });
   }

   /**
    * Get the constructor
    * @param c The class
    * @param params The parameters
    * @return The constructor
    * @exception NoSuchMethodException If a matching method is not found.
    */
   static Constructor<?> getConstructor(final Class<?> c, final Class<?>... params)
      throws NoSuchMethodException
   {
      if (System.getSecurityManager() == null)
         return c.getConstructor(params);

      Constructor<?> result = AccessController.doPrivileged(new PrivilegedAction<Constructor<?>>()
      {
         public Constructor<?> run()
         {
            try
            {
               return c.getConstructor(params);
            }
            catch (NoSuchMethodException e)
            {
               return null;
            }
         }
      });

      if (result != null)
         return result;

      throw new NoSuchMethodException();
   }
}

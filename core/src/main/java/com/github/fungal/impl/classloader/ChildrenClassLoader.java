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

package com.github.fungal.impl.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Children class loader
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
class ChildrenClassLoader extends URLClassLoader
{
   /** Delegate class loader */
   private ParentLastClassLoader delegate;

   /**
    * Constructor
    * @param urls The URLs for JAR archives or directories
    * @param parent The parent class loader
    * @param delegate The parent class loader
    */
   ChildrenClassLoader(URL[] urls, ClassLoader parent, ParentLastClassLoader delegate)
   {
      super(urls, parent);

      this.delegate = delegate;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      try
      {
         return super.loadClass(name);
      }
      catch (Throwable t)
      {
         // Default to delegate
      }

      return delegate.loadClass(name, false);
   }

   /**
    * Find a class
    * @param name The fully qualified class name
    * @return The class
    * @throws ClassNotFoundException If the class could not be found 
    */
   @Override
   public Class<?> findClass(String name) throws ClassNotFoundException
   {
      try
      {
         return super.findClass(name);
      }
      catch (Throwable t)
      {
         // Default to delegate
      }

      return delegate.lookup(name);
   }
}

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

import com.github.fungal.api.classloading.KernelClassLoader;

import java.net.URL;
import java.util.Arrays;

/**
 * Parent first class loader
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class ParentFirstClassLoader extends KernelClassLoader
{
   /**
    * Constructor
    * @param urls The URLs for JAR archives or directories
    * @param parent The parent class loader
    */
   public ParentFirstClassLoader(URL[] urls, ClassLoader parent)
   {
      super(urls, parent);
   }

   /**
    * Load a class
    * @param name The fully qualified class name
    * @return The class
    * @throws ClassNotFoundException If the class could not be found 
    */
   @Override
   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      Class<?> result = super.loadClass(name);

      if (result != null)
         return result;

      try
      {
         return loadClass(name, false);
      }
      catch (Throwable t)
      {
         // Ignore
      }

      return getParent().loadClass(name);
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb.append("ParentFirstClassLoader@").append(Integer.toHexString(System.identityHashCode(this)));
      sb.append("[parent=").append(getParent());
      sb.append(" urls=").append(Arrays.toString(getURLs()));
      sb.append("]");

      return sb.toString();
   }
}

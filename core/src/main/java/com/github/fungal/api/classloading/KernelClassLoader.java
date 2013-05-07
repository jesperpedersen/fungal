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

package com.github.fungal.api.classloading;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Kernel class loader
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 * @see com.github.fungal.api.classloading.ClassLoaderFactory
 */
public abstract class KernelClassLoader extends URLClassLoader implements Closeable
{
   /** Simple types */
   private static ConcurrentMap<String, Class<?>> simpleTypes = new ConcurrentHashMap<String, Class<?>>(9);

   static
   {
      simpleTypes.put(void.class.getName(), void.class);
      simpleTypes.put(byte.class.getName(), byte.class);
      simpleTypes.put(short.class.getName(), short.class);
      simpleTypes.put(int.class.getName(), int.class);
      simpleTypes.put(long.class.getName(), long.class);
      simpleTypes.put(char.class.getName(), char.class);
      simpleTypes.put(boolean.class.getName(), boolean.class);
      simpleTypes.put(float.class.getName(), float.class);
      simpleTypes.put(double.class.getName(), double.class);
   }

   /**
    * Constructor
    * @param urls The URLs for JAR archives or directories
    * @param parent The parent class loader
    */
   protected KernelClassLoader(URL[] urls, ClassLoader parent)
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
      return simpleTypes.get(name);
   }

   /**
    * Close - no operation as shutdown needs to be called explicit
    * @exception IOException Thrown if an error occurs
    */
   public void close() throws IOException
   {
   }

   /**
    * Shutdown
    * @exception IOException Thrown if an error occurs
    */
   public void shutdown() throws IOException
   {
      super.close();
   }
}

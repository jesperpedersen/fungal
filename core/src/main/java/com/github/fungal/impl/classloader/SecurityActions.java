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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

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
    * Get the system classloader
    * @return The classloader
    */
   static ClassLoader getSystemClassLoader()
   {
      if (System.getSecurityManager() == null)
         return ClassLoader.getSystemClassLoader();

      return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
      {
         public ClassLoader run()
         {
            return ClassLoader.getSystemClassLoader();
         }
      });
   }

   /**
    * Create an ArchiveClassLoader
    * @param id The class loader id
    * @param url The URL
    * @param exportPackages The export packages for the class loader
    * @param repository The repository
    * @return The class loader
    */
   static ArchiveClassLoader createArchiveClassLoader(final Integer id, 
                                                      final URL url,
                                                      final Set<String> exportPackages,
                                                      final ExportClassLoaderRepository repository)
   {
      if (System.getSecurityManager() == null)
         return new ArchiveClassLoader(id, url, exportPackages, repository);

      return AccessController.doPrivileged(new PrivilegedAction<ArchiveClassLoader>() 
      {
         public ArchiveClassLoader run()
         {
            return new ArchiveClassLoader(id, url, exportPackages, repository);
         }
      });
   }

   /**
    * Create a NonExportClassLoader
    * @param repository The repository
    * @return The class loader
    */
   static NonExportClassLoader createNonExportClassLoader(final ExportClassLoaderRepository repository)
   {
      if (System.getSecurityManager() == null)
         return new NonExportClassLoader(repository);

      return AccessController.doPrivileged(new PrivilegedAction<NonExportClassLoader>() 
      {
         public NonExportClassLoader run()
         {
            return new NonExportClassLoader(repository);
         }
      });
   }

   /**
    * Create a URLClassLoader
    * @param urls The URLs
    * @param parent The parent class loader
    * @return The class loader
    */
   static URLClassLoader createURLClassLoader(final URL[] urls, final ClassLoader parent)
   {
      if (System.getSecurityManager() == null)
         return new URLClassLoader(urls, parent);

      return AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() 
      {
         public URLClassLoader run()
         {
            return new URLClassLoader(urls, parent);
         }
      });
   }

   /**
    * Create a ChildrenClassLoader
    * @param urls The URLs
    * @param parent The parent class loader
    * @param delegate The delegate class loader
    * @return The class loader
    */
   static ChildrenClassLoader createChildrenClassLoader(final URL[] urls, 
                                                        final ClassLoader parent,
                                                        final ParentLastClassLoader delegate)
   {
      if (System.getSecurityManager() == null)
         return new ChildrenClassLoader(urls, parent, delegate);

      return AccessController.doPrivileged(new PrivilegedAction<ChildrenClassLoader>() 
      {
         public ChildrenClassLoader run()
         {
            return new ChildrenClassLoader(urls, parent, delegate);
         }
      });
   }
}

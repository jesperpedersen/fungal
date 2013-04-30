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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 * Parent last class loader
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class ParentLastClassLoader extends KernelClassLoader
{
   /** Children class loader */
   private ChildrenClassLoader children;

   /**
    * Constructor
    * @param urls The URLs for JAR archives or directories
    * @param parent The parent class loader
    */
   public ParentLastClassLoader(URL[] urls, ClassLoader parent)
   {
      super(new URL[0], parent);

      this.children = SecurityActions.createChildrenClassLoader(urls, ClassLoader.getSystemClassLoader(), this);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      Class<?> result = super.loadClass(name);

      if (result != null)
         return result;

      try
      {
         return children.loadClass(name);
      }
      catch (ClassNotFoundException cnfe)
      {
         // Default to parent
      }
      catch (NoClassDefFoundError ncdfe)
      {
         // Default to parent
      }

      return loadClass(name, false);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
   {
      return super.loadClass(name, resolve);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Class<?> findClass(String name) throws ClassNotFoundException
   {
      try
      {
         return children.findClass(name);
      }
      catch (Throwable t)
      {
         // Default to parent
      }

      return super.findClass(name);
   }

   /**
    * Lookup a class
    * @param name The fullt qualified class name
    * @return The class
    * @exception ClassNotFoundException Thrown if the class can't be found
    */
   Class<?> lookup(String name) throws ClassNotFoundException
   {
      return super.findClass(name);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public URL getResource(String name)
   {
      URL resource = children.getResource(name);

      if (resource != null)
         return resource;

      return super.getResource(name);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public InputStream getResourceAsStream(String name)
   {
      InputStream is = children.getResourceAsStream(name);

      if (is != null)
         return is;

      return super.getResourceAsStream(name);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Enumeration<URL> getResources(String name)
      throws IOException
   {
      Vector<URL> v = new Vector<URL>();

      Enumeration<URL> e = children.getResources(name);

      if (e != null)
      {
         while (e.hasMoreElements())
         {
            v.add(e.nextElement());
         }
      }

      e = super.getResources(name);

      if (e != null)
      {
         while (e.hasMoreElements())
         {
            v.add(e.nextElement());
         }
      }

      return v.elements();
   }

   /**
    * {@inheritDoc}
    */
   @Override 
   public void clearAssertionStatus()
   {
      super.clearAssertionStatus();
      children.clearAssertionStatus();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setClassAssertionStatus(String className, boolean enabled)
   {
      children.setClassAssertionStatus(className, enabled);
      super.setClassAssertionStatus(className, enabled);
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void setDefaultAssertionStatus(boolean enabled)
   {
      children.setDefaultAssertionStatus(enabled);
      super.setDefaultAssertionStatus(enabled);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void setPackageAssertionStatus(String packageName, boolean enabled)
   {
      children.setPackageAssertionStatus(packageName, enabled);
      super.setPackageAssertionStatus(packageName, enabled);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public URL[] getURLs()
   {
      List<URL> result = null;

      URL[] urls = children.getURLs();

      if (urls != null)
      {
         result = new ArrayList<URL>(urls.length);
         for (URL u : urls)
         {
            result.add(u);
         }
      }

      urls = super.getURLs();

      if (urls != null)
      {
         if (result == null)
            result = new ArrayList<URL>(urls.length);

         for (URL u : urls)
         {
            result.add(u);
         }
      }

      if (result == null)
         return new URL[0];

      return result.toArray(new URL[result.size()]);
   }

   /**
    * {@inheritDoc}
    */
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb.append("ParentLastClassLoader@").append(Integer.toHexString(System.identityHashCode(this)));
      sb.append("[parent=").append(getParent());
      sb.append(" urls=").append(Arrays.toString(getURLs()));
      sb.append("]");

      return sb.toString();
   }
}

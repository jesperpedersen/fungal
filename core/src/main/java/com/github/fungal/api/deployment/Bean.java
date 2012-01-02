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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a bean
 */
public class Bean
{
   private Constructor constructor;
   private List<Property> property;
   private List<Depends> depends;
   private List<Install> install;
   private List<Uninstall> uninstall;
   private List<Incallback> incallback;
   private List<Uncallback> uncallback;
   private Create create;
   private Start start;
   private Stop stop;
   private Destroy destroy;
   private boolean ignoreCreate;
   private boolean ignoreStart;
   private boolean ignoreStop;
   private boolean ignoreDestroy;
   private String name;
   private String interfaze;
   private String clazz;

   /**
    * Constructor
    * @param name The name of the bean
    */
   public Bean(String name)
   {
      this.constructor = null;
      this.property = null;
      this.depends = null;
      this.install = null;
      this.uninstall = null;
      this.incallback = null;
      this.uncallback = null;
      this.create = null;
      this.start = null;
      this.stop = null;
      this.destroy = null;
      this.ignoreCreate = false;
      this.ignoreStart = false;
      this.ignoreStop = false;
      this.ignoreDestroy = false;
      this.name = name;
      this.interfaze = null;
      this.clazz = null;
   }

   /**
    * Get the constructor
    * @return The value
    */
   public Constructor getConstructor()
   {
      return constructor;
   }

   /**
    * Set the constructor
    * @param value The value
    */
   public void setConstructor(Constructor value)
   {
      constructor = value;
   }

   /**
    * Get the property values
    * @return The value
    */
   public List<Property> getProperty()
   {
      if (property == null)
         property = new ArrayList<Property>(1);

      return property;
   }

   /**
    * Get the depends values
    * @return The value
    */
   public List<Depends> getDepends()
   {
      if (depends == null)
         depends = new ArrayList<Depends>(1);

      return depends;
   }

   /**
    * Get the install values
    * @return The value
    */
   public List<Install> getInstall()
   {
      if (install == null)
         install = new ArrayList<Install>(1);

      return install;
   }

   /**
    * Get the uninstall values
    * @return The value
    */
   public List<Uninstall> getUninstall()
   {
      if (uninstall == null)
         uninstall = new ArrayList<Uninstall>(1);

      return uninstall;
   }

   /**
    * Get the incallback values
    * @return The value
    */
   public List<Incallback> getIncallback()
   {
      if (incallback == null)
         incallback = new ArrayList<Incallback>(1);

      return incallback;
   }

   /**
    * Get the uncallback values
    * @return The value
    */
   public List<Uncallback> getUncallback()
   {
      if (uncallback == null)
         uncallback = new ArrayList<Uncallback>(1);

      return uncallback;
   }

   /**
    * Get the create value
    * @return The value
    */
   public Create getCreate()
   {
      return create;
   }

   /**
    * Set the create value
    * @param value The value
    */
   public void setCreate(Create value)
   {
      create = value;
   }

   /**
    * Get the start value
    * @return The value
    */
   public Start getStart()
   {
      return start;
   }

   /**
    * Set the start value
    * @param value The value
    */
   public void setStart(Start value)
   {
      start = value;
   }

   /**
    * Get the stop value
    * @return The value
    */
   public Stop getStop()
   {
      return stop;
   }

   /**
    * Set the stop value
    * @param value The value
    */
   public void setStop(Stop value)
   {
      stop = value;
   }

   /**
    * Get the destroy value
    * @return The value
    */
   public Destroy getDestroy()
   {
      return destroy;
   }

   /**
    * Set the destroy value
    * @param value The value
    */
   public void setDestroy(Destroy value)
   {
      destroy = value;
   }

   /**
    * Get the ignore create value
    * @return The value
    */
   public boolean isIgnoreCreate()
   {
      return ignoreCreate;
   }

   /**
    * Set the ignore create value
    * @param value The value
    */
   public void setIgnoreCreate(boolean value)
   {
      ignoreCreate = value;
   }

   /**
    * Get the ignore start value
    * @return The value
    */
   public boolean isIgnoreStart()
   {
      return ignoreStart;
   }

   /**
    * Set the ignore start value
    * @param value The value
    */
   public void setIgnoreStart(boolean value)
   {
      ignoreStart = value;
   }

   /**
    * Get the ignore stop value
    * @return The value
    */
   public boolean isIgnoreStop()
   {
      return ignoreStop;
   }

   /**
    * Set the ignore stop value
    * @param value The value
    */
   public void setIgnoreStop(boolean value)
   {
      ignoreStop = value;
   }

   /**
    * Get the ignore destroy value
    * @return The value
    */
   public boolean isIgnoreDestroy()
   {
      return ignoreDestroy;
   }

   /**
    * Set the ignore destroy value
    * @param value The value
    */
   public void setIgnoreDestroy(boolean value)
   {
      ignoreDestroy = value;
   }

   /**
    * Get the name
    * @return The value
    */
   public String getName()
   {
      return name;
   }

   /**
    * Get the interface
    * @return The value
    */
   public String getInterface()
   {
      return interfaze;
   }

   /**
    * Set the interface
    * @param value The value
    */
   public void setInterface(String value)
   {
      interfaze = value;
   }

   /**
    * Get the class
    * @return The value
    */
   public String getClazz()
   {
      return clazz;
   }

   /**
    * Set the class
    * @param value The value
    */
   public void setClazz(String value)
   {
      clazz = value;
   }
}

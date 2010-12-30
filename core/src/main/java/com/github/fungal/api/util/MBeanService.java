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

package com.github.fungal.api.util;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * A MBean service helper class which can register any
 * POJO object against an MBeanServer with using the ObjectName
 * specified.
 *
 * This service supports the lifecycle of the Fungal kernel
 */
public class MBeanService
{
   /** MBeanServer instance */
   private MBeanServer mbeanServer;

   /** Object Name */
   private String objectName;

   /** Object Instance */
   private Object object;

   /** The actual ObjectName instance */
   private ObjectName on;

   /** Registered */
   private boolean registered;

   /**
    * Constructor
    */
   public MBeanService()
   {
      this.mbeanServer = null;
      this.on = null;
      this.objectName = null;
      this.object = null;
      this.registered = false;
   }

   /**
    * Set the MBean server
    * @param v The value
    */
   public void setMBeanServer(MBeanServer v)
   {
      mbeanServer = v;
   }

   /**
    * Set the object name
    * @param v The value
    */
   public void setObjectName(String v)
   {
      objectName = v;
   }

   /**
    * Set the object
    * @param v The value
    */
   public void setObject(Object v)
   {
      object = v;
   }

   /**
    * Start
    * @exception Throwable Thrown in case of an error
    */
   public void start() throws Throwable
   {
      if (mbeanServer == null)
         throw new IllegalArgumentException("MBeanServer is null");

      if (objectName == null)
         throw new IllegalArgumentException("ObjectName is null");

      if (objectName.trim().equals(""))
         throw new IllegalArgumentException("ObjectName is empty");

      if (object == null)
         throw new IllegalArgumentException("Object is null");

      on = new ObjectName(objectName);

      try
      {
         mbeanServer.registerMBean(object, on);
      }
      catch (NotCompliantMBeanException ncme)
      {
         mbeanServer.registerMBean(JMX.createMBean(object), on);
      }

      registered = true;
   }

   /**
    * Stop
    * @exception Throwable Thrown in case of an error
    */
   public void stop() throws Throwable
   {
      if (registered)
         mbeanServer.unregisterMBean(on); 
   }
}

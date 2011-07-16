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

/**
 * The kernel MBean for Fungal
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public interface KernelImplMBean
{
   /**
    * Get the name of the kernel
    * @return The value
    */
   public String getName();

   /**
    * Get the version of the kernel
    * @return The value
    */
   public String getVersion();

   /**
    * Dump the bean information
    * @return The information
    */
   public String dump();

   /**
    * Dump the bean information
    * @param name The bean name
    * @return The information
    */
   public String dump(String name);
}

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

package com.github.fungal.bootstrap;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the bootstrap
 */
public class Bootstrap
{
   private List<String> url;
   private ProtocolsType protocols;
   private ServersType servers;
   private DependenciesType dependencies;

   /**
    * Constructor
    */
   public Bootstrap()
   {
      url = null;
      protocols = null;
      servers = null;
      dependencies = null;
   }

   /**
    * Get the value of the url property
    * @return The value
    */
   public List<String> getUrl()
   {
      if (url == null) 
         url = new ArrayList<String>(1);
      
      return url;
   }

   /**
    * Get the protocols
    * @return The value
    */
   public ProtocolsType getProtocols()
   {
      return protocols;
   }

   /**
    * Set the protocols
    * @param v The value
    */
   public void setProtocols(ProtocolsType v)
   {
      protocols = v;
   }

   /**
    * Get the servers
    * @return The value
    */
   public ServersType getServers()
   {
      return servers;
   }

   /**
    * Set the servers
    * @param v The value
    */
   public void setServers(ServersType v)
   {
      servers = v;
   }

   /**
    * Get the dependencies
    * @return The value
    */
   public DependenciesType getDependencies()
   {
      return dependencies;
   }

   /**
    * Set the dependencies
    * @param v The value
    */
   public void setDependencies(DependenciesType v)
   {
      dependencies = v;
   }

   /**
    * String representation
    * @return The value
    */
   public String toString()
   {
      if (url == null)
         return "<null>";

      return url.toString();
   }
}

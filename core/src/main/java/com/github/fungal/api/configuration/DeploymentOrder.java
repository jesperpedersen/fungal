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

package com.github.fungal.api.configuration;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DeploymentOrder defines the order of the deployment should be processed
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class DeploymentOrder implements Comparator<URL>, Serializable
{
   /** Serial version uid */
   private static final long serialVersionUID = 1L;

   /** Order */
   private List<String> order;

   /**
    * .xml files will be first
    */
   public DeploymentOrder()
   {
      this.order = new ArrayList<String>(1);
      
      this.order.add(".xml");
   }

   /**
    * User defined order
    * @param extensions The order of the file extensions
    */
   public DeploymentOrder(List<String> extensions)
   {
      if (extensions == null)
         throw new IllegalArgumentException("Extensions is null");

      if (extensions.size() == 0)
         throw new IllegalArgumentException("Extensions is empty");

      this.order = new ArrayList<String>(extensions);
   }

   /**
    * Get the order of the extensions
    * @return The order
    */
   public List<String> getOrder()
   {
      return Collections.unmodifiableList(order);
   }

   /**
    * Get the order index of an url
    * @param url The URL
    * @return The index; <code>Integer.MAX_VALUE</code> if not defined
    */
   public int getOrderIndex(URL url)
   {
      if (url == null)
         throw new IllegalArgumentException("URL is null");

      String file = url.getFile();
      int index = Integer.MAX_VALUE;

      for (int i = 0; i < order.size(); i++)
      {
         String extension = order.get(i);

         if (file.endsWith(extension))
            index = i;
      }

      return index;
   }

   /**
    * Compare
    * @param o1 The first object
    * @param o2 The second object
    * @return The files sorted according to the specified ordering; the rest according to
    *         their natural ordering
    */
   public int compare(URL o1, URL o2)
   {
      String f1 = o1.getFile();
      String f2 = o2.getFile();

      int v1 = Integer.MAX_VALUE;
      int v2 = Integer.MAX_VALUE;

      for (int i = 0; i < order.size(); i++)
      {
         String extension = order.get(i);

         if (f1.endsWith(extension))
            v1 = i;

         if (f2.endsWith(extension))
            v2 = i;
      }

      if (v1 < v2)
      {
         return -1;
      }
      else if (v1 > v2)
      {
         return 1;
      }
      else
      {
         return f1.compareTo(f2);
      }
   }

   /**
    * Hash code
    * @return The hash
    */
   public int hashCode()
   {
      return order.hashCode();
   }

   /**
    * Equals
    * @param o The object
    * @return True if equal; otherwise false
    */
   public boolean equals(Object o)
   {
      if (o == this)
         return true;

      if (o == null)
         return false;

      if (!(o instanceof DeploymentOrder))
         return false;

      DeploymentOrder other = (DeploymentOrder)o;

      return order.equals(other.order);
   }
}

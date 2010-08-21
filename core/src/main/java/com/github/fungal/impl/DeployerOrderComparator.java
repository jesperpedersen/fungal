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

import com.github.fungal.spi.deployers.DeployerOrder;

import java.io.Serializable;
import java.util.Comparator;

/**
 * DeploymentOrderComparator sorts DeployerOrder instances
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class DeployerOrderComparator implements Comparator<DeployerOrder>, Serializable
{
   /** Serial version uid */
   private static final long serialVersionUID = 1L;

   /**
    * Constructor
    */
   DeployerOrderComparator()
   {
   }

   /**
    * Compare
    * @param o1 The first object
    * @param o2 The second object
    * @return <code>-1</code> if o1 should be invoked first, <code>1</code> if o2 should
    *         be invoked first, otherwise <code>0</code>
    */
   public int compare(DeployerOrder o1, DeployerOrder o2)
   {
      if (o1.getOrder() < o2.getOrder())
      {
         return -1;
      }
      else if (o1.getOrder() > o2.getOrder())
      {
         return 1;
      }

      return 0;
   }

   /**
    * Hash code
    * @return The hash
    */
   public int hashCode()
   {
      return 42;
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

      if (!(o instanceof DeployerOrderComparator))
         return false;

      return true;
   }
}

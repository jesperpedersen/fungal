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

import java.io.Serializable;
import java.net.URL;
import java.util.Comparator;

/**
 * Url comparator
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
class UrlComparator implements Comparator<URL>, Serializable
{
   /** Serial version uid */
   private static final long serialVersionUID = 1L;

   /**
    * Constructor
    */
   UrlComparator()
   {
   }

   /**
    * Compare
    * @param o1 The first object
    * @param o2 The second object
    * @return XML files first according to their natural ordering; then other files
    *         according to their natural ordering
    */
   public int compare(URL o1, URL o2)
   {
      if (o1.getFile().endsWith(".xml") && o2.getFile().endsWith(".xml"))
         return o1.getFile().compareTo(o2.getFile());

      if (o1.getFile().endsWith(".xml"))
         return -1;

      if (o2.getFile().endsWith(".xml"))
         return 1;

      return o1.getFile().compareTo(o2.getFile());
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
      if (o == null)
         return false;

      if (!(o instanceof UrlComparator))
         return false;

      return true;
   }
}

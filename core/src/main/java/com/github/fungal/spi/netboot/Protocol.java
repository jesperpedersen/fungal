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

package com.github.fungal.spi.netboot;

import java.io.File;

/**
 * The protocol interface for Fungal's netboot functionality.
 *
 * Implementations of this interface must have a default constructor, and
 * its Java bean properties will be injected.
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public interface Protocol extends Cloneable
{
   /**
    * Download
    * @param path The path
    * @param target The target
    * @return True if artifact downloaded; otherwise false
    */
   public boolean download(String path, File target);

   /**
    * Clone the protocol implementation
    * @return A copy of the implementation
    * @exception CloneNotSupportedException Thrown if the copy operation isn't supported
    */
   public Protocol clone() throws CloneNotSupportedException;
}

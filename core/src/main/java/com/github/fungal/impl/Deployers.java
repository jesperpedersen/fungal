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

import com.github.fungal.spi.deployers.Deployer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The active deployers
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
final class Deployers
{
   private List<Deployer> deployers;

   /**
    * Constructor
    */
   Deployers()
   {
      this.deployers = Collections.synchronizedList(new ArrayList<Deployer>());
   }

   /**
    * Add deployer
    * @param deployer The deployer
    */
   void addDeployer(Deployer deployer)
   {
      if (deployer == null)
         throw new IllegalArgumentException("Deployer is null");

      deployers.add(deployer);
   }

   /**
    * Get deployers
    * @return The list of current deployers
    */
   List<Deployer> getDeployers()
   {
      return deployers;
   }
}

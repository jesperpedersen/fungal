/*
 * The Fungal kernel project
 * Copyright (C) 2012
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

package com.github.fungal.api.remote;

/**
 * The Communicator bean allows to register commands at run-time.
 * <p>
 * Usage: <code>Communicator communicator = kernel.getBean("Communicator", Communicator.class);</code>
 * </p>
 *
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public interface Communicator
{
   /**
    * Register command
    * @param command The command
    */
   public void registerCommand(Command command);

   /**
    * Unregister command
    * @param command The command
    */
   public void unregisterCommand(Command command);
}

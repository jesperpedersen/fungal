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

package com.github.fungal.impl.remote.commands;

import com.github.fungal.api.remote.Command;
import com.github.fungal.impl.remote.CommunicationServer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the getcommand command
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class GetCommand implements Command
{
   /** The logger */
   private static Logger log = Logger.getLogger(GetCommand.class.getName());

   /** Trace logging enabled */
   private static boolean trace = log.isLoggable(Level.FINEST);

   /** Command name */
   private static final String NAME = "getcommand";

   /** The communication server */
   private CommunicationServer cs;

   /**
    * GetCommand
    * @param cs The communication server
    */
   public GetCommand(CommunicationServer cs)
   {
      this.cs = cs;
   }

   /**
    * Get the name of the command
    * @return The name
    */
   public String getName()
   {
      return NAME;
   }

   /**
    * Get the parameter types of the command; <code>null</code> if none
    * @return The types
    */
   public Class[] getParameterTypes()
   {
      return new Class[] {String.class};
   }

   /**
    * Invoke
    * @param args The arguments
    * @return The return value
    */
   public Serializable invoke(Serializable[] args)
   {
      if (args == null || args.length != 1 || !(args[0] instanceof String))
         return new IllegalArgumentException("Unsupported argument list: " + Arrays.toString(args));

      String commandName = (String)args[0];

      Command c = cs.getCommand(commandName);

      if (c != null)
      {
         return c.getParameterTypes();
      }
      else
      {
         return new Exception("Unsupported command: " + commandName);
      }
   }

   /**
    * Is it a public command
    * @return True if system-wide; false if internal
    */
   public boolean isPublic()
   {
      return false;
   }
}

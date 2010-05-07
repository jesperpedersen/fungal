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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the help command
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class Help implements Command
{
   /** The logger */
   private static Logger log = Logger.getLogger(Help.class.getName());

   /** Trace logging enabled */
   private static boolean trace = log.isLoggable(Level.FINEST);

   /** Command name */
   private static final String NAME = "help";

   /** The communication server */
   private CommunicationServer cs;

   /**
    * Help
    * @param cs The communication server
    */
   public Help(CommunicationServer cs)
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
      return null;
   }

   /**
    * Invoke
    * @param args The arguments
    * @return The return value
    */
   public Serializable invoke(Serializable[] args)
   {
      if (args != null)
         return new IllegalArgumentException("Unsupported argument list: " + Arrays.toString(args));

      StringBuilder sb = new StringBuilder();

      Set<String> commands = cs.getCommandNames();
      if (commands != null)
      {
         for (String commandName : commands)
         {
            Command command = cs.getCommand(commandName);

            if (command.isPublic())
            {
               sb = sb.append(commandName);

               Class[] parameterTypes = command.getParameterTypes();
               if (parameterTypes != null)
               {
                  sb = sb.append(" ");

                  for (int i = 0; i < parameterTypes.length; i++)
                  {
                     Class<?> parameterType = parameterTypes[i];
                     sb = sb.append("<");
                     sb = sb.append(parameterType.getName());
                     sb = sb.append(">");

                     if (i < parameterTypes.length - 1)
                        sb = sb.append(" ");
                  }
               }

               sb = sb.append("\n");
            }
         }
      }

      return sb.toString();
   }

   /**
    * Is it a public command
    * @return True if system-wide; false if internal
    */
   public boolean isPublic()
   {
      return true;
   }
}

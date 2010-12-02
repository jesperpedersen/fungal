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

import com.github.fungal.api.deployer.MainDeployer;
import com.github.fungal.api.remote.Command;
import com.github.fungal.impl.HotDeployer;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a undeploy command
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class Undeploy implements Command
{
   /** Command name */
   private static final String NAME = "undeploy";

   /** The logger */
   private Logger log = Logger.getLogger(Undeploy.class.getName());

   /** Trace logging enabled */
   private boolean trace = log.isLoggable(Level.FINEST);

   /** The main deployer */
   private MainDeployer mainDeployer;

   /** The hot deployer */
   private HotDeployer hotDeployer;

   /**
    * Undeploy
    * @param mainDeployer The main deployer
    * @param hotDeployer The hot deployer
    */
   public Undeploy(MainDeployer mainDeployer, HotDeployer hotDeployer)
   {
      this.mainDeployer = mainDeployer;
      this.hotDeployer = hotDeployer;
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
      return new Class[] {URL.class};
   }

   /**
    * Invoke
    * @param args The arguments
    * @return The return value
    */
   public Serializable invoke(Serializable[] args)
   {
      if (args == null || args.length != 1 || !(args[0] instanceof URL))
         return new IllegalArgumentException("Unsupported argument list: " + Arrays.toString(args));

      URL url = (URL)args[0];

      try
      {
         if (hotDeployer != null)
            hotDeployer.unregister(url);
         
         mainDeployer.undeploy(url);

         return null;
      }
      catch (Throwable t)
      {
         StringWriter sw = new StringWriter();
         sw.write(t.getMessage());
         sw.write('\n');

         t.printStackTrace(new PrintWriter(sw));

         return new Exception(sw.toString());
      }
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

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

package com.github.fungal.impl.remote;

import com.github.fungal.api.remote.Command;
import com.github.fungal.impl.KernelImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The communication server
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class CommunicationServer implements Runnable
{
   /** The logger */
   private Logger log = Logger.getLogger(CommunicationServer.class.getName());

   /** Trace logging enabled */
   private boolean trace = log.isLoggable(Level.FINEST);

   /** The kernel */
   private KernelImpl kernel;

   /** The bind address */
   private String bindAddress;

   /** The bind port */
   private int bindPort;

   /** Is the server running ? */
   private AtomicBoolean running;

   /** The server socket */
   private ServerSocket ss;

   /** The available commands */
   private ConcurrentMap<String, Command> commands;

   /**
    * Constructor
    * @param kernel The kernel
    * @param bindAddress The bind address
    * @param bindPort The bind port
    * @exception IOException Thrown if a server socket can not be created
    */
   public CommunicationServer(KernelImpl kernel, String bindAddress, int bindPort) throws IOException
   {
      if (bindAddress == null)
         bindAddress = "localhost";

      this.kernel = kernel;
      this.bindAddress = bindAddress;
      this.bindPort = bindPort;
      this.running = new AtomicBoolean(false);
      this.ss = null;
      this.commands = new ConcurrentHashMap<String, Command>();
   }

   /**
    * Register command
    * @param command The command
    */
   public void registerCommand(Command command)
   {
      if (command == null)
         throw new IllegalArgumentException("Command is null");

      if (!commands.containsKey(command.getName()))
      {
         commands.put(command.getName(), command);
      }
   }

   /**
    * Unregister command
    * @param command The command
    */
   public void unregisterCommand(Command command)
   {
      if (command == null)
         throw new IllegalArgumentException("Command is null");

      if (commands.containsKey(command.getName()))
      {
         commands.remove(command.getName());
      }
   }

   /**
    * Get command names
    * @return The command names
    */
   public Set<String> getCommandNames()
   {
      return commands.keySet();
   }

   /**
    * Get command
    * @param name The name of the command
    * @return The command
    */
   public Command getCommand(String name)
   {
      return commands.get(name);
   }

   /**
    * Start
    * @exception Throwable Thrown if an error occurs
    */
   public void start() throws Throwable
   {
      if (!running.get())
      {
         InetSocketAddress address = new InetSocketAddress(bindAddress, bindPort); 

         ss = new ServerSocket();
         ss.bind(address);

         running.set(true);
      }
   }

   /**
    * Stop
    * @exception Throwable Thrown if an error occurs
    */
   public void stop() throws Throwable
   {
      running.set(false);

      if (ss != null)
      {
         try
         {
            ss.close();
         }
         catch (IOException ioe)
         {
            if (log.isLoggable(Level.FINE))
               log.fine(ioe.getMessage());
         }
      }
   }

   /**
    * Run
    */
   public void run()
   {
      while (running.get())
      {
         try
         {
            Socket socket = ss.accept();

            Runnable r = new Communication(this, socket);
            kernel.getExecutorService().submit(r);
         }
         catch (IOException ioe)
         {
            if (log.isLoggable(Level.FINE))
               log.fine(ioe.getMessage());
         }
      }
   }
}

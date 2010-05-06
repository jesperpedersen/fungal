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

import com.github.fungal.impl.KernelImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
   private static Logger log = Logger.getLogger(CommunicationServer.class.getName());

   /** Trace logging enabled */
   private static boolean trace = log.isLoggable(Level.FINEST);

   /** The kernel */
   private KernelImpl kernel;

   private AtomicBoolean running;

   private ServerSocket ss;

   /**
    * Constructor
    * @param kernel The kernel
    * @param bindAddress The bind address
    * @param bindPort The bind port
    * @exception IOException Thrown if a server socket can not be created
    */
   public CommunicationServer(KernelImpl kernel, String bindAddress, int bindPort) throws IOException
   {
      this.kernel = kernel;

      if (bindAddress == null)
         bindAddress = "localhost";

      InetSocketAddress address = new InetSocketAddress(bindAddress, bindPort); 

      this.ss = new ServerSocket();
      this.ss.bind(address);

      this.running = new AtomicBoolean(true);
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

            Runnable r = new Communication(socket, kernel.getMainDeployer(), kernel.getHotDeployer());
            kernel.getExecutorService().submit(r);
         }
         catch (IOException ioe)
         {
            if (log.isLoggable(Level.FINE))
               log.fine(ioe.getMessage());
         }
      }
   }

   /**
    * Stop
    */
   public void stop()
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
}

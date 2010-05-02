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

import com.github.fungal.api.deployer.MainDeployer;
import com.github.fungal.impl.HotDeployer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The communication between client and server
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class Communication implements Runnable
{
   /** The logger */
   private static Logger log = Logger.getLogger(Communication.class.getName());

   /** Trace logging enabled */
   private static boolean trace = log.isLoggable(Level.FINEST);

   /** The socket */
   private Socket socket;

   /** The main deployer */
   private MainDeployer mainDeployer;

   /** The hot deployer */
   private HotDeployer hotDeployer;

   /**
    * Constructor
    * @param socket The socket
    * @param mainDeployer The main deployer
    * @param hotDeployer The hot deployer
    */
   public Communication(Socket socket, MainDeployer mainDeployer, HotDeployer hotDeployer)
   {
      this.socket = socket;
      this.mainDeployer = mainDeployer;
      this.hotDeployer = hotDeployer;
   }

   /**
    * Run
    */
   public void run()
   {
      try
      {
         ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

         int command = ois.readInt();

         if (command == 0)
         {
            URL url = new URL(ois.readUTF());

            if (hotDeployer != null)
               hotDeployer.register(url);

            mainDeployer.deploy(url);
         }
         else if (command == 1)
         {
            URL url = new URL(ois.readUTF());

            if (hotDeployer != null)
               hotDeployer.unregister(url);

            mainDeployer.undeploy(url);
         }
         else
         {
            throw new IOException("Unknown command: " + command);
         }

         ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

         oos.writeBoolean(true);
         oos.writeUTF("");

         oos.flush();
      }
      catch (Throwable t)
      {
         try
         {
            StringWriter sw = new StringWriter();
            sw.write(t.getMessage());
            sw.write('\n');

            t.printStackTrace(new PrintWriter(sw));

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            oos.writeBoolean(false);
            oos.writeUTF(sw.toString());
            
            oos.flush();
         }
         catch (IOException ioe)
         {
            log.log(Level.SEVERE, ioe.getMessage(), ioe);
         }
      }
      finally
      {
         try
         {
            if (socket != null)
               socket.close();
         }
         catch (IOException ignore)
         {
            // Ignore
         }
      }
   }
}

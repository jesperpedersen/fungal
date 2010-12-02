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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The communication between client and server
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class Communication implements Runnable
{
   /** The logger */
   private Logger log = Logger.getLogger(Communication.class.getName());

   /** Trace logging enabled */
   private boolean trace = log.isLoggable(Level.FINEST);

   /** The communication server */
   private CommunicationServer cs;

   /** The socket */
   private Socket socket;

   /**
    * Constructor
    * @param cs The communication server
    * @param socket The socket
    */
   public Communication(CommunicationServer cs, Socket socket)
   {
      this.cs = cs;
      this.socket = socket;
   }

   /**
    * Run
    */
   public void run()
   {
      try
      {
         ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

         String commandName = ois.readUTF();
         Command command = cs.getCommand(commandName);
         Serializable result = null;

         if (command != null)
         {
            Class[] parameterTypes = command.getParameterTypes();
            Serializable[] arguments = null;

            if (parameterTypes != null)
            {
               arguments = new Serializable[parameterTypes.length];
               for (int i = 0; i < parameterTypes.length; i++)
               {
                  arguments[i] = (Serializable)ois.readObject();
               }
            }

            result = command.invoke(arguments);
         }
         else
         {
            result = new IOException("Unknown command: " + commandName);
         }

         ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
         oos.writeObject(result);
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
            oos.writeObject(sw.toString());
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

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

package com.github.fungal.cli;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;

/**
 * The client communicator
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class Communicator
{
   /**
    * Constructor
    */
   public Communicator()
   {
   }

   /**
    * Deploy
    * @param socket The socket
    * @param url The file URL
    * @exception IOException Thrown if a communication error occurs
    */
   public void deploy(Socket socket, URL url) throws IOException
   {
      ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

      oos.writeInt(0);
      oos.writeUTF(url.toString());

      oos.flush();
      
      ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            
      boolean result = ois.readBoolean();
      String message = ois.readUTF();

      if (!result)
         System.out.println(message);
   }

   /**
    * Undeploy
    * @param socket The socket
    * @param url The file URL
    * @exception IOException Thrown if a communication error occurs
    */
   public void undeploy(Socket socket, URL url) throws IOException
   {
      ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

      oos.writeInt(1);
      oos.writeUTF(url.toString());

      oos.flush();
      
      ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            
      boolean result = ois.readBoolean();
      String message = ois.readUTF();

      if (!result)
         System.out.println(message);
   }
}

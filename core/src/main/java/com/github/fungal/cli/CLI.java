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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.URL;

/**
 * The command line interface for the Fungal kernel
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class CLI
{
   /**
    * Constructor
    */
   private CLI()
   {
   }

   /**
    * Main
    * @param args The command line arguments
    */
   public static void main(String[] args)
   {
      if (args.length < 1)
      {
         usage();
      }
      else
      {
         Socket socket = null;
         try
         {
            String host = null;
            int port = 1202;
            int counter = 0;
            String command = "";

            if ("-h".equals(args[counter]))
            {
               counter++;
               host = args[counter];
               counter++;
            }

            if ("-p".equals(args[counter]))
            {
               counter++;
               port = Integer.valueOf(args[counter]).intValue();
               counter++;
            }

            command = args[counter];
            counter++;

            if (host == null)
               host = "localhost";

            socket = new Socket(host, port);

            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            
            oos.writeUTF("getcommand");
            oos.writeObject(command);
            
            oos.flush();
            
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            
            Serializable result = (Serializable)ois.readObject();

            if (result == null || result instanceof Class[])
            {
               Class[] parameterTypes = (Class[])result;
               Serializable[] arguments = null;
               if (parameterTypes != null)
               {
                  arguments = new Serializable[parameterTypes.length];
                  for (int i = 0; i < parameterTypes.length; i++)
                  {
                     arguments[i] = getValue(parameterTypes[i], args[counter]);
                     counter++;
                  }
               }

               oos.writeUTF(command);

               if (arguments != null)
               {
                  for (Serializable argument : arguments)
                  {
                     oos.writeObject(argument);
                  }
               }
               
               oos.flush();

               result = (Serializable)ois.readObject();

               if (result != null)
               {
                  System.out.println(result);
               }
            }
            else
            {
               System.err.println(result);
            }
         }
         catch (Throwable t)
         {
            t.printStackTrace(System.err);
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

   /**
    * Get an instance of the type
    * @param type The type
    * @param value The string representation
    */
   private static Serializable getValue(Class<?> type, String value)
   {
      if (value == null)
         return null;

      if (String.class.equals(type))
      {
         return value;
      }
      else if (boolean.class.equals(type))
      {
         return Boolean.valueOf(value).booleanValue();
      }
      else if (Boolean.class.equals(type))
      {
         return Boolean.valueOf(value);
      }
      else if (byte.class.equals(type))
      {
         return Byte.valueOf(value).byteValue();
      }
      else if (Byte.class.equals(type))
      {
         return Byte.valueOf(value);
      }
      else if (char.class.equals(type))
      {
         return Character.valueOf(value.charAt(0)).charValue();
      }
      else if (Character.class.equals(type))
      {
         return Character.valueOf(value.charAt(0));
      }
      else if (double.class.equals(type))
      {
         return Double.valueOf(value).doubleValue();
      }
      else if (Double.class.equals(type))
      {
         return Double.valueOf(value);
      }
      else if (float.class.equals(type))
      {
         return Float.valueOf(value).floatValue();
      }
      else if (Float.class.equals(type))
      {
         return Float.valueOf(value);
      }
      else if (int.class.equals(type))
      {
         return Integer.valueOf(value).intValue();
      }
      else if (Integer.class.equals(type))
      {
         return Integer.valueOf(value);
      }
      else if (long.class.equals(type))
      {
         return Long.valueOf(value).longValue();
      }
      else if (Long.class.equals(type))
      {
         return Long.valueOf(value);
      }
      else if (short.class.equals(type))
      {
         return Short.valueOf(value).shortValue();
      }
      else if (Short.class.equals(type))
      {
         return Short.valueOf(value);
      }
      else if (URL.class.equals(type))
      {
         try
         {
            URL url = null;

            if (!(value.startsWith("file:") || value.startsWith("http:")))
            {
               url = new File(value).toURI().toURL();
            }
            else
            {
               url = new URL(value);
            }
            
            return url;
         }
         catch (Throwable t)
         {
            System.err.println(t);
         }

         return null;
      }
      else
      {
         System.err.println("Unknown type: " + type.getName() + " Value: " + value);
      }

      return null;
   }

   /**
    * Usage
    */
   private static void usage()
   {
      System.out.println("Usage: CLI <common> <command>");

      System.out.println(" Common:");
      System.out.println(" -------");
      System.out.println(" -h <host> (default: localhost)");
      System.out.println(" -p <port> (default: 1202)");

      System.out.println("");

      System.out.println(" Commands:");
      System.out.println(" ---------");
      System.out.println(" For a list of commands use \"help\"");
   }
}

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

package com.github.fungal.impl.netboot;

import com.github.fungal.spi.netboot.Protocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Support the http:// protocol
 */
public class Http implements Protocol
{
   /**
    * Constructor
    */
   public Http()
   {
   }

   /**
    * {@inheritDoc}
    */
   public boolean download(String path, File target)
   {
      boolean redirect = HttpURLConnection.getFollowRedirects();
      HttpURLConnection.setFollowRedirects(true);

      InputStream is = null;
      OutputStream os = null;
      try
      {
         URL u = new URL(path);
         URLConnection connection = u.openConnection();

         connection.connect();

         is = new BufferedInputStream(connection.getInputStream(), 8192);
         os = new BufferedOutputStream(new FileOutputStream(target), 8192);

         int b;
         while ((b = is.read()) != -1)
         {
            os.write(b);
         }

         os.flush();

         return true;
      }
      catch (Throwable t)
      {
         // Nothing to do
      }
      finally
      {
         if (is != null)
         {
            try
            {
               is.close();
            }
            catch (IOException ignore)
            {
               // Ignore
            }
         }
         if (os != null)
         {
            try
            {
               os.close();
            }
            catch (IOException ignore)
            {
               // Ignore
            }
         }

         HttpURLConnection.setFollowRedirects(redirect);
      }

      return false;
   }

   /**
    * Clone the protocol implementation
    * @return A copy of the implementation
    * @exception CloneNotSupportedException Thrown if the copy operation isn't supported
    */
   public Protocol clone() throws CloneNotSupportedException
   {
      Http h = (Http)super.clone();

      return h;
   }
}

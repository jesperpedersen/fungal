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

import com.github.fungal.bootstrap.DependencyType;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Unmarshaller for a Maven POM XML file
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class MavenUnmarshaller
{
   /**
    * Constructor
    */
   public MavenUnmarshaller()
   {
   }

   /**
    * Unmarshal
    * @param url The URL
    * @return The result
    * @exception IOException If an I/O error occurs
    */
   public List<DependencyType> unmarshal(URL url) throws IOException
   {
      if (url == null)
         throw new IllegalArgumentException("File is null");

      InputStream is = null;
      try
      {
         List<DependencyType> result = new ArrayList<DependencyType>(1);

         if ("file".equals(url.getProtocol()))
         {
            File file = new File(url.toURI());
            is = new FileInputStream(file);
         }
         else if ("jar".equals(url.getProtocol()))
         {
            JarURLConnection jarConnection = (JarURLConnection)url.openConnection();
            is = jarConnection.getInputStream();
         }
         else
         {
            throw new IOException("Unsupport protocol: " + url);
         }

         is = new BufferedInputStream(is, 4096);

         XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        
         XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(is);

         while (xmlStreamReader.hasNext())
         {
            int eventCode = xmlStreamReader.next();

            switch (eventCode)
            {
               case XMLStreamReader.START_ELEMENT :

                  if ("dependency".equals(xmlStreamReader.getLocalName()))
                     result.add(readDependency(xmlStreamReader));

                  break;
               default :
            }
         }

         return result;
      }
      catch (Throwable t)
      {
         throw new IOException(t.getMessage(), t);
      }
      finally
      {
         try
         {
            if (is != null)
               is.close();
         }
         catch (IOException ioe)
         {
            // Ignore
         }
      }
   }

   /**
    * Read: <dependency>
    * @param xmlStreamReader The XML stream
    * @return The dependency
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private DependencyType readDependency(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      DependencyType result = new DependencyType();

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();
               if ("groupId".equals(name))
               {
                  result.setOrganisation(readString(xmlStreamReader));
               }
               else if ("artifactId".equals(name))
               {
                  result.setArtifact(readString(xmlStreamReader));
               }
               else if ("version".equals(name))
               {
                  result.setRevision(readString(xmlStreamReader));
               }
               else if ("type".equals(name))
               {
                  result.setExt(readString(xmlStreamReader));
               }
               else
               {
                  ignoreTag(xmlStreamReader);
               }

               break;
            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"dependency".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("dependency tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read a string
    * @param xmlStreamReader The XML stream
    * @return The parameter
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private String readString(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      String result = null;

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.CHARACTERS :
               if (!xmlStreamReader.getText().trim().equals(""))
                  result = xmlStreamReader.getText().trim();

               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      return result;
   }

   /**
    * Ignore a tag
    * @param xmlStreamReader The XML stream
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private void ignoreTag(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }
   }
}

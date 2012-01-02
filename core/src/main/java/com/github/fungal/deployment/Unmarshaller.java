/*
 * The Fungal kernel project
 * Copyright (C) 2011
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

package com.github.fungal.deployment;

import com.github.fungal.api.deployment.Bean;
import com.github.fungal.api.deployment.Constructor;
import com.github.fungal.api.deployment.Create;
import com.github.fungal.api.deployment.Depends;
import com.github.fungal.api.deployment.Destroy;
import com.github.fungal.api.deployment.Entry;
import com.github.fungal.api.deployment.Factory;
import com.github.fungal.api.deployment.Incallback;
import com.github.fungal.api.deployment.Inject;
import com.github.fungal.api.deployment.Install;
import com.github.fungal.api.deployment.Key;
import com.github.fungal.api.deployment.List;
import com.github.fungal.api.deployment.Map;
import com.github.fungal.api.deployment.Null;
import com.github.fungal.api.deployment.Parameter;
import com.github.fungal.api.deployment.Property;
import com.github.fungal.api.deployment.Set;
import com.github.fungal.api.deployment.Start;
import com.github.fungal.api.deployment.Stop;
import com.github.fungal.api.deployment.This;
import com.github.fungal.api.deployment.Uncallback;
import com.github.fungal.api.deployment.Uninstall;
import com.github.fungal.api.deployment.Value;
 
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Unmarshaller for a bean deployment XML file
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class Unmarshaller
{
   /** The logger */
   private Logger log = Logger.getLogger(Unmarshaller.class.getName());

   /** Trace logging enabled */
   private boolean trace = log.isLoggable(Level.FINEST);

   /**
    * Constructor
    */
   public Unmarshaller()
   {
   }

   /**
    * Unmarshal
    * @param url The URL
    * @return The result
    * @exception IOException If an I/O error occurs
    */
   public Deployment unmarshal(URL url) throws IOException
   {
      if (url == null)
         throw new IllegalArgumentException("File is null");

      InputStream is = null;
      try
      {
         Deployment deployment = new Deployment();

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

                  if ("bean".equals(xmlStreamReader.getLocalName()))
                     deployment.getBean().add(readBean(xmlStreamReader));

                  break;
               default :
            }
         }

         return deployment;
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
    * Read: <bean>
    * @param xmlStreamReader The XML stream
    * @return The bean
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Bean readBean(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      String beanName = null;
      String beanClazz = null;
      String beanInterface = null;

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("name".equals(name))
         {
            beanName = xmlStreamReader.getAttributeValue(i);
         }
         else if ("class".equals(name))
         {
            beanClazz = xmlStreamReader.getAttributeValue(i);
         }
         else if ("interface".equals(name))
         {
            beanInterface = xmlStreamReader.getAttributeValue(i);
         }
      }

      if (beanName == null || beanName.trim().equals(""))
         throw new XMLStreamException("bean name not defined", xmlStreamReader.getLocation());

      Bean result = new Bean(beanName);
      result.setClazz(beanClazz);
      result.setInterface(beanInterface);

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();
               if ("constructor".equals(name))
               {
                  result.setConstructor(readConstructor(xmlStreamReader));
               }
               else if ("property".equals(name))
               {
                  result.getProperty().add(readProperty(xmlStreamReader));
               }
               else if ("depends".equals(name))
               {
                  result.getDepends().add(readDepends(xmlStreamReader));
               }
               else if ("install".equals(name))
               {
                  result.getInstall().add(readInstall(xmlStreamReader));
               }
               else if ("uninstall".equals(name))
               {
                  result.getUninstall().add(readUninstall(xmlStreamReader));
               }
               else if ("incallback".equals(name))
               {
                  result.getIncallback().add(readIncallback(xmlStreamReader));
               }
               else if ("uncallback".equals(name))
               {
                  result.getUncallback().add(readUncallback(xmlStreamReader));
               }
               else if ("create".equals(name))
               {
                  result.setCreate(readCreate(xmlStreamReader));
               }
               else if ("start".equals(name))
               {
                  result.setStart(readStart(xmlStreamReader));
               }
               else if ("stop".equals(name))
               {
                  result.setStop(readStop(xmlStreamReader));
               }
               else if ("destroy".equals(name))
               {
                  result.setDestroy(readDestroy(xmlStreamReader));
               }
               else if ("ignoreCreate".equals(name))
               {
                  result.setIgnoreCreate(readIgnoreCreate(xmlStreamReader));
               }
               else if ("ignoreStart".equals(name))
               {
                  result.setIgnoreStart(readIgnoreStart(xmlStreamReader));
               }
               else if ("ignoreStop".equals(name))
               {
                  result.setIgnoreStop(readIgnoreStop(xmlStreamReader));
               }
               else if ("ignoreDestroy".equals(name))
               {
                  result.setIgnoreDestroy(readIgnoreDestroy(xmlStreamReader));
               }

               break;
            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"bean".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("bean tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <constructor>
    * @param xmlStreamReader The XML stream
    * @return The constructor
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Constructor readConstructor(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Constructor result = new Constructor();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("factoryMethod".equals(name))
         {
            result.setFactoryMethod(xmlStreamReader.getAttributeValue(i));
         }
         else if ("factoryClass".equals(name))
         {
            result.setFactoryClass(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();

               if ("parameter".equals(name))
               {
                  result.getParameter().add(readParameter(xmlStreamReader));
               }
               else if ("factory".equals(name))
               {
                  result.setFactory(readFactory(xmlStreamReader));
               }

               break;
            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"constructor".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("constructor tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <parameter>
    * @param xmlStreamReader The XML stream
    * @return The parameter
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Parameter readParameter(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Parameter result = new Parameter();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("class".equals(name))
         {
            result.setClazz(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();

               if ("inject".equals(name))
               {
                  result.getContent().add(readInject(xmlStreamReader));
               }
               else if ("null".equals(name))
               {
                  result.getContent().add(readNull(xmlStreamReader));
               }

               break;

            case XMLStreamReader.CHARACTERS :
               if (!xmlStreamReader.getText().trim().equals(""))
                  result.getContent().add(xmlStreamReader.getText());

               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"parameter".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("parameter tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <property>
    * @param xmlStreamReader The XML stream
    * @return The property
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Property readProperty(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      String propertyName = null;
      String clazz = null;

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("name".equals(name))
         {
            propertyName = xmlStreamReader.getAttributeValue(i);
         }
         else if ("class".equals(name))
         {
            clazz = xmlStreamReader.getAttributeValue(i);
         }
      }

      if (propertyName == null || propertyName.trim().equals(""))
         throw new XMLStreamException("Name is mandatory", xmlStreamReader.getLocation());

      Property result = new Property(propertyName);
      result.setClazz(clazz);

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();

               if ("inject".equals(name))
               {
                  result.getContent().add(readInject(xmlStreamReader));
               }
               else if ("set".equals(name))
               {
                  result.getContent().add(readSet(xmlStreamReader));
               }
               else if ("map".equals(name))
               {
                  result.getContent().add(readMap(xmlStreamReader));
               }
               else if ("list".equals(name))
               {
                  result.getContent().add(readList(xmlStreamReader));
               }
               else if ("null".equals(name))
               {
                  result.getContent().add(readNull(xmlStreamReader));
               }
               else if ("this".equals(name))
               {
                  result.getContent().add(readThis(xmlStreamReader));
               }

               break;

            case XMLStreamReader.CHARACTERS :
               if (!xmlStreamReader.getText().trim().equals(""))
                  result.getContent().add(xmlStreamReader.getText());

               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"property".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("property tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <inject>
    * @param xmlStreamReader The XML stream
    * @return The inject
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Inject readInject(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      String bean = null;
      String property = null;

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("bean".equals(name))
         {
            bean = xmlStreamReader.getAttributeValue(i);
         }
         else if ("property".equals(name))
         {
            property = xmlStreamReader.getAttributeValue(i);
         }
      }

      if (bean == null || bean.trim().equals(""))
         throw new XMLStreamException("Bean is mandatory", xmlStreamReader.getLocation());

      Inject result = new Inject(bean);
      result.setProperty(property);

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.CHARACTERS :
               result.setValue(xmlStreamReader.getText());
               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"inject".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("inject tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <depends>
    * @param xmlStreamReader The XML stream
    * @return The depends
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Depends readDepends(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Depends result = new Depends();

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.CHARACTERS :
               result.setValue(xmlStreamReader.getText());
               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"depends".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("depends tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <install>
    * @param xmlStreamReader The XML stream
    * @return The install
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Install readInstall(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Install result = new Install();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"install".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("install tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <uninstall>
    * @param xmlStreamReader The XML stream
    * @return The install
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Uninstall readUninstall(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Uninstall result = new Uninstall();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"uninstall".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("uninstall tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <incallback>
    * @param xmlStreamReader The XML stream
    * @return The incallback
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Incallback readIncallback(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Incallback result = new Incallback();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"incallback".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("incallback tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <uncallback>
    * @param xmlStreamReader The XML stream
    * @return The uncallback
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Uncallback readUncallback(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Uncallback result = new Uncallback();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"uncallback".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("uncallback tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <map>
    * @param xmlStreamReader The XML stream
    * @return The map
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Map readMap(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      String keyClass = null;
      String valueClass = null;
      String clazz = null;

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("keyClass".equals(name))
         {
            keyClass = xmlStreamReader.getAttributeValue(i);
         }
         else if ("valueClass".equals(name))
         {
            valueClass = xmlStreamReader.getAttributeValue(i);
         }
         else if ("class".equals(name))
         {
            clazz = xmlStreamReader.getAttributeValue(i);
         }
      }

      if (keyClass == null || keyClass.trim().equals(""))
         throw new XMLStreamException("Key class is mandatory", xmlStreamReader.getLocation());

      if (valueClass == null || valueClass.trim().equals(""))
         throw new XMLStreamException("Value class is mandatory", xmlStreamReader.getLocation());

      Map result = new Map(keyClass, valueClass);
      result.setClazz(clazz);

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();

               if ("entry".equals(name))
                  result.getEntry().add(readEntry(xmlStreamReader));

               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"map".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("map tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <set>
    * @param xmlStreamReader The XML stream
    * @return The set
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Set readSet(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      String elementClass = null;
      String clazz = null;

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("elementClass".equals(name))
         {
            elementClass = xmlStreamReader.getAttributeValue(i);
         }
         else if ("class".equals(name))
         {
            clazz = xmlStreamReader.getAttributeValue(i);
         }
      }

      if (elementClass == null || elementClass.trim().equals(""))
         throw new XMLStreamException("Element class is mandatory", xmlStreamReader.getLocation());

      Set result = new Set(elementClass);
      result.setClazz(clazz);

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();

               if ("value".equals(name))
                  result.getValue().add(readValue(xmlStreamReader));

               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"set".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("set tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <list>
    * @param xmlStreamReader The XML stream
    * @return The list
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private List readList(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      String elementClass = null;
      String clazz = null;

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("elementClass".equals(name))
         {
            elementClass = xmlStreamReader.getAttributeValue(i);
         }
         else if ("class".equals(name))
         {
            clazz = xmlStreamReader.getAttributeValue(i);
         }
      }

      if (elementClass == null || elementClass.trim().equals(""))
         throw new XMLStreamException("Element class is mandatory", xmlStreamReader.getLocation());

      List result = new List(elementClass);
      result.setClazz(clazz);

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();

               if ("value".equals(name))
                  result.getValue().add(readValue(xmlStreamReader));

               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"list".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("list tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <entry>
    * @param xmlStreamReader The XML stream
    * @return The entry
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Entry readEntry(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Entry result = new Entry();

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.START_ELEMENT :
               String name = xmlStreamReader.getLocalName();

               if ("key".equals(name))
               {
                  result.setKey(readKey(xmlStreamReader));
               }
               else if ("value".equals(name))
               {
                  result.setValue(readValue(xmlStreamReader));
               }

               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"entry".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("entry tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <key>
    * @param xmlStreamReader The XML stream
    * @return The key
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Key readKey(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Key result = new Key();

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.CHARACTERS :
               result.setValue(xmlStreamReader.getText());
               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"key".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("key tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <value>
    * @param xmlStreamReader The XML stream
    * @return The value
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Value readValue(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Value result = new Value();

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         switch (eventCode)
         {
            case XMLStreamReader.CHARACTERS :
               result.setValue(xmlStreamReader.getText());
               break;

            default :
         }

         eventCode = xmlStreamReader.next();
      }

      if (!"value".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("value tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <null>
    * @param xmlStreamReader The XML stream
    * @return The null
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Null readNull(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Null result = new Null();

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"null".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("null tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <this>
    * @param xmlStreamReader The XML stream
    * @return The this
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private This readThis(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      This result = new This();

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"this".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("this tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <factory>
    * @param xmlStreamReader The XML stream
    * @return The factory
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Factory readFactory(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Factory result = new Factory();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("bean".equals(name))
         {
            result.setBean(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"factory".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("factory tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <create>
    * @param xmlStreamReader The XML stream
    * @return The create
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Create readCreate(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Create result = new Create();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"create".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("create tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <start>
    * @param xmlStreamReader The XML stream
    * @return The start
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Start readStart(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Start result = new Start();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"start".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("start tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <stop>
    * @param xmlStreamReader The XML stream
    * @return The stop
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Stop readStop(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Stop result = new Stop();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"stop".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("stop tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <destroy>
    * @param xmlStreamReader The XML stream
    * @return The destroy
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private Destroy readDestroy(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      Destroy result = new Destroy();

      for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++)
      {
         String name = xmlStreamReader.getAttributeLocalName(i);
         if ("method".equals(name))
         {
            result.setMethod(xmlStreamReader.getAttributeValue(i));
         }
      }

      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"destroy".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("destroy tag not completed", xmlStreamReader.getLocation());

      return result;
   }

   /**
    * Read: <ignoreCreate>
    * @param xmlStreamReader The XML stream
    * @return The ignoreCreate
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private boolean readIgnoreCreate(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"ignoreCreate".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("ignoreCreate tag not completed", xmlStreamReader.getLocation());

      return true;
   }

   /**
    * Read: <ignoreStart>
    * @param xmlStreamReader The XML stream
    * @return The ignoreStart
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private boolean readIgnoreStart(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"ignoreStart".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("ignoreStart tag not completed", xmlStreamReader.getLocation());

      return true;
   }

   /**
    * Read: <ignoreStop>
    * @param xmlStreamReader The XML stream
    * @return The ignoreStop
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private boolean readIgnoreStop(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"ignoreStop".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("ignoreStop tag not completed", xmlStreamReader.getLocation());

      return true;
   }

   /**
    * Read: <ignoreDestroy>
    * @param xmlStreamReader The XML stream
    * @return The ignoreDestroy
    * @exception XMLStreamException Thrown if an exception occurs
    */
   private boolean readIgnoreDestroy(XMLStreamReader xmlStreamReader) throws XMLStreamException
   {
      int eventCode = xmlStreamReader.next();

      while (eventCode != XMLStreamReader.END_ELEMENT)
      {
         eventCode = xmlStreamReader.next();
      }

      if (!"ignoreDestroy".equals(xmlStreamReader.getLocalName()))
         throw new XMLStreamException("ignoreDestroy tag not completed", xmlStreamReader.getLocation());

      return true;
   }
}

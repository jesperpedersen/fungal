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

package com.github.fungal.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;

/**
 * Enables JSR-160 access to the kernel MBeanServer
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
class JmxRemote
{
   /** The RMI server host name */
   private static final String SERVER_HOSTNAME = "java.rmi.server.hostname";

   /** The binding name */
   private static final String RMI_BIND_NAME = "jmxrmi";   

   /** The backlog */
   private static final int BACKLOG = 50;
   
   /** The logger */
   private Logger log = Logger.getLogger(JmxRemote.class.getName());
   
   /** The MBeanServer */
   private MBeanServer mbeanServer;

   /** The bind address */
   private String bindAddress;

   /** The registry port */
   private int registryPort;

   /** The server port */
   private int serverPort;

   /** The connector server */
   private RMIConnectorServer adapter;

   /** The RMI server */
   private RMIJRMPServerImpl rmiServer;

   /** The RMI registry */
   private Registry registry;
   
   /**
    * Constructor
    * @param mbeanServer The MBean server
    * @param bindAddress The bind address
    * @param registryPort The registry port
    * @param serverPort The server port
    */
   JmxRemote(MBeanServer mbeanServer, String bindAddress, int registryPort, int serverPort)
   {
      this.mbeanServer = mbeanServer;
      this.bindAddress = bindAddress;
      this.registryPort = registryPort;
      this.serverPort = serverPort;

      this.adapter = null;
      this.rmiServer = null;
      this.registry = null;
   }

   /**
    * Start
    * @exception IOException Thrown if an I/O error occurs
    * @exception RemoteException Thrown if a RMI error occurs
    */
   void start() throws IOException, RemoteException
   {
      SecurityActions.setSystemProperty(SERVER_HOSTNAME, bindAddress);

      InetAddress ia = InetAddress.getByName(bindAddress);
      RMIServerSocketFactory serverSocketFactory = new JMXServerSocketFactory(ia);
         
      registry = LocateRegistry.createRegistry(registryPort, null, serverSocketFactory);

      HashMap<String, Object> env = new HashMap<String, Object>();
      rmiServer = new RMIJRMPServerImpl(serverPort, null, serverSocketFactory, env);

      JMXServiceURL url = createJMXServiceURL(bindAddress);
      adapter = new RMIConnectorServer(url, env, rmiServer, mbeanServer);
      adapter.start();

      registry.rebind(RMI_BIND_NAME, rmiServer.toStub());
   }
   
   /**
    * Stop
    */
   void stop()
   {
      try
      {
         registry.unbind(RMI_BIND_NAME);
      } 
      catch (Exception e)
      {
         log.log(Level.SEVERE, "Could not unbind jmx connector from registry", e);
      }
      finally
      {
         try
         {
            adapter.stop();
         }
         catch (Exception e)
         {
            log.log(Level.SEVERE, "Could not stop connector server", e);
         }
         finally
         {
            try
            {
               UnicastRemoteObject.unexportObject(registry, true);
            }
            catch (Exception e)
            {
               log.log(Level.SEVERE, "Could not shutdown rmi registry");
            }
         }
      }
   }

   /**
    * Create a JMXServiceURL
    * @param host The host
    * @return The url
    * @exception MalformedURLException Thrown if the url couldn't be constructed
    */
   private JMXServiceURL createJMXServiceURL(String host) throws MalformedURLException
   {
      if (host.indexOf(':') != -1)
      {
         // Is this a IPV6 literal address? if yes, surround with square brackets as per rfc2732.
         // IPV6 literal addresses have one or more colons
         // IPV4 addresses/hostnames have no colons
         host = "[" + host + "]";
      }

      JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + host);
      return url;
   }
   
   /**
    * A JMX ServerSocketFactory
    */
   private static class JMXServerSocketFactory implements RMIServerSocketFactory, Serializable
   {
      private static final long serialVersionUID = 1564081885379700777L;
      private final InetAddress address;
      
      /**
       * Constructor
       * @param address The address
       */
      JMXServerSocketFactory(InetAddress address)
      {
         this.address = address;
      }
      
      /**
       * Create a server socket
       * @param port The port
       * @return The socket
       * @exception IOException Thrown if an error occurs
       */
      @Override
      public ServerSocket createServerSocket(int port) throws IOException
      {
         return new ServerSocket(port, BACKLOG, address);
      }
   }
}

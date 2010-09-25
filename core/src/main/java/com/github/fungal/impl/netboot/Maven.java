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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Represent a Maven repository
 */
public class Maven implements Repository
{
   /**
    * Constructor
    */
   public Maven()
   {
   }

   /**
    * {@inheritDoc}
    */
   public List<DependencyType> resolve(List<String> servers,
                                       DependencyType dependency,
                                       File repository,
                                       DependencyTracker tracker)
      throws ResolveException
   {
      if ("pom".equals(dependency.getType()))
      {
         return downloadPom(servers, 
                            dependency,
                            repository,
                            tracker);
      }
      else
      {
         return downloadArtifact(servers, 
                                 dependency,
                                 repository,
                                 tracker);
      }
   }

   /**
    * {@inheritDoc}
    */
   public File getFile(DependencyType dependency, File repository) throws IOException
   {
      File f = new File(repository, getPath(dependency));
      
      if (f.exists())
      {
         return f;
      }

      throw new IOException("Dependency " + dependency + " doesn't exist in the repository " +
                            repository.getAbsolutePath());

   }

   private List<DependencyType> downloadPom(List<String> servers,
                                            DependencyType dependency,
                                            File repository,
                                            DependencyTracker tracker)
      throws ResolveException
   {
      List<DependencyType> result = downloadArtifact(servers, dependency, repository, tracker);

      if (result.size() == 0)
         return result;

      try
      {
         MavenUnmarshaller unmarshaller = new MavenUnmarshaller();
         File f = new File(repository, getPath(dependency));
         List<DependencyType> dependencies = unmarshaller.unmarshal(f.toURI().toURL());         

         if (dependencies != null && dependencies.size() > 0)
         {
            Iterator<DependencyType> dit = dependencies.iterator();
            while (dit.hasNext())
            {
               DependencyType dep = dit.next();
               List<DependencyType> l = downloadArtifact(servers, dep, repository, tracker);
               result.addAll(l);
            }
         }

         return result;
      }
      catch (Throwable t)
      {
         throw new ResolveException("The dependency couldn't be parsed", dependency);
      }
   }

   private List<DependencyType> downloadArtifact(List<String> servers,
                                                 DependencyType dependency,
                                                 File repository,
                                                 DependencyTracker tracker)
      throws ResolveException
   {
      if (tracker.isTracked(dependency))
         return Collections.emptyList();

      if (!tracker.track(dependency))
         return Collections.emptyList();

      List<DependencyType> result = getArtifact(repository, dependency);

      if (result != null)
         return result;

      result = new ArrayList<DependencyType>(1);

      File f = new File(repository, getPath(dependency));

      if (!f.getParentFile().mkdirs())
         throw new ResolveException(f.getParent() + " couldn't be created");

      boolean redirect = HttpURLConnection.getFollowRedirects();
      HttpURLConnection.setFollowRedirects(true);

      Iterator<String> it = servers.iterator();
      while (result.isEmpty() && it.hasNext())
      {
         InputStream is = null;
         OutputStream os = null;
         try
         {
            String server = it.next();

            if (!server.endsWith("/"))
               server = server + "/";

            URL u = new URL(server + getPath(dependency).replace(File.separatorChar, '/'));
            URLConnection connection = u.openConnection();

            connection.connect();

            is = new BufferedInputStream(connection.getInputStream(), 8192);
            os = new BufferedOutputStream(new FileOutputStream(f), 8192);

            int b;
            while ((b = is.read()) != -1)
            {
               os.write(b);
            }

            os.flush();

            result.add(dependency);
         }
         catch (Throwable t)
         {
            // Nothing to do - try next server
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
         }
      }

      HttpURLConnection.setFollowRedirects(redirect);

      if (result.isEmpty())
         throw new ResolveException("The dependency couldn't be resolved", dependency);

      return result;
   }

   private List<DependencyType> getArtifact(File repository, DependencyType dependency)
   {
      File f = new File(repository, getPath(dependency));
      
      if (f.exists())
      {
         List<DependencyType> l = new ArrayList<DependencyType>(1);
         l.add(dependency);
         return l;
      }

      return null;
   }

   private String getPath(DependencyType dependency)
   {
      String path = dependency.getGroupId().replace('.', File.separatorChar) + File.separatorChar + 
         dependency.getArtifactId() + File.separatorChar +
         dependency.getVersion() + File.separatorChar +
         dependency.getArtifactId() + "-" + dependency.getVersion() + "." + dependency.getType();

      return path;
   }
}

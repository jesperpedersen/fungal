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

package com.github.fungal.bootstrap;

/**
 * A dependency
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class DependencyType
{
   private String target;

   private String groupId;
   private String artifactId;
   private String version;
   private String type;

   /**
    * Constructor
    */
   public DependencyType()
   {
      target = "lib";

      groupId = null;
      artifactId = null;
      version = null;
      type = "jar";
   }

   /**
    * Get the target
    * @return The value
    */
   public String getTarget()
   {
      return target;
   }

   /**
    * Set the target
    * @param v The value
    */
   public void setTarget(String v)
   {
      if (v != null && !v.trim().equals(""))
         target = v;
   }

   /**
    * Get the group id
    * @return The value
    */
   public String getGroupId()
   {
      return groupId;
   }

   /**
    * Set the group id
    * @param v The value
    */
   public void setGroupId(String v)
   {
      groupId = v;
   }

   /**
    * Get the artifact id
    * @return The value
    */
   public String getArtifactId()
   {
      return artifactId;
   }

   /**
    * Set the artifact id
    * @param v The value
    */
   public void setArtifactId(String v)
   {
      artifactId = v;
   }

   /**
    * Get the version
    * @return The value
    */
   public String getVersion()
   {
      return version;
   }

   /**
    * Set the version
    * @param v The value
    */
   public void setVersion(String v)
   {
      version = v;
   }

   /**
    * Get the type
    * @return The value
    */
   public String getType()
   {
      return type;
   }

   /**
    * Set the type
    * @param v The value
    */
   public void setType(String v)
   {
      if (v != null && !v.trim().equals(""))
         type = v;
   }

   /**
    * Equals
    * @param obj The other object
    * @return True if they are equal; otherwise false
    */
   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;

      if (obj == this)
         return true;

      if (!(obj instanceof DependencyType))
         return false;

      DependencyType dt = (DependencyType)obj;

      if (!groupId.equals(dt.getGroupId()))
         return false;

      if (!artifactId.equals(dt.getArtifactId()))
         return false;

      if (!version.equals(dt.getVersion()))
         return false;

      if (!type.equals(dt.getType()))
         return false;

      return true;
   }

   /**
    * Hash code
    * @return The value
    */
   @Override
   public int hashCode()
   {
      int hash = 7;

      hash += 7 * groupId.hashCode();
      hash += 7 * artifactId.hashCode();
      hash += 7 * version.hashCode();
      hash += 7 * type.hashCode();

      return hash;
   }

   /**
    * String representation
    * @return The string
    */
   @Override
   public String toString()
   {
      StringBuilder sb = new StringBuilder();

      sb = sb.append("Dependency[");

      sb = sb.append("GroupId=").append(groupId).append(",");
      sb = sb.append("ArtifactId=").append(artifactId).append(",");
      sb = sb.append("Version=").append(version).append(",");
      sb = sb.append("Type=").append(type);

      sb = sb.append("]");

      return sb.toString();
   }
}

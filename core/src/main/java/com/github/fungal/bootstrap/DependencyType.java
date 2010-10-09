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

   private String organisation;
   private String module;
   private String artifact;
   private String revision;
   private String classifier;
   private String ext;

   /**
    * Constructor
    */
   public DependencyType()
   {
      target = "lib";

      organisation = "";
      module = "";
      artifact = "";
      revision = "";
      classifier = "";
      ext = "jar";
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
    * Get the organisation
    * @return The value
    */
   public String getOrganisation()
   {
      return organisation;
   }

   /**
    * Set the organisation
    * @param v The value
    */
   public void setOrganisation(String v)
   {
      if (v != null && !v.trim().equals(""))
         organisation = v;
   }

   /**
    * Get the module
    * @return The value
    */
   public String getModule()
   {
      if (module == null || module.trim().equals(""))
         return getArtifact();

      return module;
   }

   /**
    * Set the module
    * @param v The value
    */
   public void setModule(String v)
   {
      if (v != null && !v.trim().equals(""))
         module = v;
   }

   /**
    * Get the artifact
    * @return The value
    */
   public String getArtifact()
   {
      return artifact;
   }

   /**
    * Set the artifact id
    * @param v The value
    */
   public void setArtifact(String v)
   {
      if (v != null && !v.trim().equals(""))
         artifact = v;
   }

   /**
    * Get the revision
    * @return The value
    */
   public String getRevision()
   {
      return revision;
   }

   /**
    * Set the revision
    * @param v The value
    */
   public void setRevision(String v)
   {
      if (v != null && !v.trim().equals(""))
         revision = v;
   }

   /**
    * Get the classifier
    * @return The value
    */
   public String getClassifier()
   {
      return classifier;
   }

   /**
    * Set the classifier
    * @param v The value
    */
   public void setClassifier(String v)
   {
      if (v != null && !v.trim().equals(""))
         classifier = v;
   }

   /**
    * Get the ext
    * @return The value
    */
   public String getExt()
   {
      return ext;
   }

   /**
    * Set the ext
    * @param v The value
    */
   public void setExt(String v)
   {
      if (v != null && !v.trim().equals(""))
         ext = v;
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

      if (!organisation.equals(dt.getOrganisation()))
         return false;

      if (!module.equals(dt.getModule()))
         return false;

      if (!artifact.equals(dt.getArtifact()))
         return false;

      if (!revision.equals(dt.getRevision()))
         return false;

      if (!classifier.equals(dt.getClassifier()))
         return false;

      if (!ext.equals(dt.getExt()))
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

      hash += organisation != null ? 7 * organisation.hashCode() : 3;
      hash += module != null ? 7 * module.hashCode() : 3;
      hash += artifact != null ? 7 * artifact.hashCode() : 3;
      hash += revision != null ? 7 * revision.hashCode() : 3;
      hash += classifier != null ? 7 * classifier.hashCode() : 3;
      hash += ext != null ? 7 * ext.hashCode() : 3;

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

      sb = sb.append("Organisation=").append(organisation).append(",");
      sb = sb.append("Module=").append(module).append(",");
      sb = sb.append("Artifact=").append(artifact).append(",");
      sb = sb.append("Revision=").append(revision).append(",");
      sb = sb.append("Classifier=").append(classifier).append(",");
      sb = sb.append("Ext=").append(ext);

      sb = sb.append("]");

      return sb.toString();
   }
}

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

/**
 * Pattern matching functionality
 */
public class Pattern
{
   /**
    * Constructor
    */
   private Pattern()
   {
   }

   /**
    * Resolve
    * @param pattern The pattern
    * @param organisation The organisation
    * @param module The module
    * @param revision The revision
    * @param artifact The artifact
    * @param classifier The classifier
    * @param ext The extension
    * @return The result
    */
   public static String resolve(String pattern, String organisation, String module,
                                String revision, String artifact, String classifier,
                                String ext)
   {
      String input = pattern;

      while (input.indexOf("(") != -1)
      {
         int pStart = input.indexOf("(");
         int pEnd = input.indexOf(")", pStart);
         boolean removeWhole = false;

         if (input.substring(pStart, pEnd).indexOf("[") != -1)
         {
            int bStart = input.indexOf("[", pStart);
            int bEnd = input.indexOf("]", bStart);

            String check = input.substring(bStart + 1, bEnd);

            if ("organisation".equals(check))
            {
               if (organisation == null || organisation.trim().equals(""))
                  removeWhole = true;
            }
            else if ("module".equals(check))
            {
               if (module == null || module.trim().equals(""))
                  removeWhole = true;
            }
            else if ("revision".equals(check))
            {
               if (revision == null || revision.trim().equals(""))
                  removeWhole = true;
            }
            else if ("artifact".equals(check))
            {
               if (artifact == null || artifact.trim().equals(""))
                  removeWhole = true;
            }
            else if ("classifier".equals(check))
            {
               if (classifier == null || classifier.trim().equals(""))
                  removeWhole = true;
            }
            else if ("ext".equals(check))
            {
               if (ext == null || ext.trim().equals(""))
                  removeWhole = true;
            }
         }

         if (removeWhole)
         {
            input = input.substring(0, pStart) + input.substring(pEnd + 1);
         }
         else
         {
            if (pEnd < input.length() - 1)
            {
               input = input.substring(0, pEnd) + input.substring(pEnd + 1);
            }
            else
            {
               input = input.substring(0, pEnd);
            }

            if (pStart > 0)
            {
               input = input.substring(0, pStart) + input.substring(pStart + 1);
            }
            else
            {
               input = input.substring(pStart + 1);
            }
         }
      }

      String result = input;

      if (organisation != null)
      {
         String org = organisation.replaceAll("\\.", "/");
         result = result.replaceAll("\\[organisation\\]", org);
      }
      else
      {
         result = result.replaceAll("\\[organisation\\]", "");
      }

      if (module != null)
      {
         result = result.replaceAll("\\[module\\]", module);
      }
      else
      {
         result = result.replaceAll("\\[module\\]", "");
      }

      if (revision != null)
      {
         result = result.replaceAll("\\[revision\\]", revision);
      }
      else
      {
         result = result.replaceAll("\\[revision\\]", "");
      }

      if (artifact != null)
      {
         result = result.replaceAll("\\[artifact\\]", artifact);
      }
      else
      {
         result = result.replaceAll("\\[artifact\\]", "");
      }

      if (classifier != null)
      {
         result = result.replaceAll("\\[classifier\\]", classifier);
      }
      else
      {
         result = result.replaceAll("\\[classifier\\]", "");
      }

      if (ext != null)
      {
         result = result.replaceAll("\\[ext\\]", ext);
      }
      else
      {
         result = result.replaceAll("\\[ext\\]", "");
      }

      return result;
   }
}

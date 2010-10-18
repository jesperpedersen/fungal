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

package com.github.fungal.api.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

/**
 * This class will create a MBean representation of any POJO object.
 *
 * The various methods allows the developer to filter attributes and operations
 * from the generated dynamic management view.
 */
public class JMX
{
   /** GET constant */
   private static final String GET = "get";

   /** SET constant */
   private static final String SET = "set";

   /**
    * Constructor
    */
   private JMX()
   {
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj) throws SecurityException
   {
      return createMBean(obj, "", null, null, null, null);
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @param description The description for the object
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj, String description) throws SecurityException
   {
      return createMBean(obj, description, null, null, null, null);
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @param description The description for the object
    * @param writeableAttributes The set of attributes that are writeable specified by regular expressions
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj, String description, Set<String> writeableAttributes) 
      throws SecurityException
   {
      return createMBean(obj, description, writeableAttributes, null, null, null);
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @param description The description for the object
    * @param writeableAttributes The set of attributes that are writeable specified by regular expressions
    * @param descriptions Descriptions for the attributes and operations on the object
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj, String description, 
                                          Set<String> writeableAttributes, Map<String, String> descriptions)
      throws SecurityException
   {
      return createMBean(obj, description, writeableAttributes, descriptions, null, null);
   }

   /**
    * Create a MBean representation for the object argument
    * @param obj The object
    * @param description The description for the object
    * @param writeableAttributes The set of attributes that are writeable specified by regular expressions
    * @param descriptions Descriptions for the attributes and operations on the object
    * @param excludeAttributes A set of attributes specified by regular expressions that 
    *                          should be excluded from the management facade
    * @param excludeOperations A set of operations specified by regular expressions that 
    *                          should be excluded from the management facade
    * @return The management facade for the object
    * @exception SecurityException Thrown if there isn't sufficient permissions
    */
   public static DynamicMBean createMBean(Object obj, 
                                          String description,
                                          Set<String> writeableAttributes,
                                          Map<String, String> descriptions,
                                          Set<String> excludeAttributes,
                                          Set<String> excludeOperations)
      throws SecurityException
   {
      if (obj == null)
         throw new IllegalArgumentException("Object is null");

      if (obj instanceof DynamicMBean)
         return (DynamicMBean)obj;

      return new ManagementDelegator(obj, description, writeableAttributes, 
                                     descriptions, excludeAttributes, excludeOperations);
   }

   /**
    * Management delegator class based on reflection
    */
   static class ManagementDelegator implements DynamicMBean
   {
      private WeakReference<Object> ref;
      private MBeanInfo info;

      /**
       * Constructor
       * @param instance The object instance
       * @param description The description for the object
       * @param writeableAttributes The set of attributes that are writeable
       * @param descriptions Descriptions for the attributes and operations on the object
       * @param excludeAttributes A set of attributes that should be excluded from the management facade
       * @param excludeOperations A set of operations that should be excluded from the management facade
       * @exception SecurityException Thrown if there isn't sufficient permissions
       */
      public ManagementDelegator(Object instance, 
                                 String description,
                                 Set<String> writeableAttributes,
                                 Map<String, String> descriptions,
                                 Set<String> excludeAttributes,
                                 Set<String> excludeOperations)
         throws SecurityException
      {
         this.ref = new WeakReference<Object>(instance);

         List<MBeanAttributeInfo> attrs = new ArrayList<MBeanAttributeInfo>();
         List<MBeanOperationInfo> ops = new ArrayList<MBeanOperationInfo>();
         Set<Pattern> writeableAttributePatterns = null;
         Set<Pattern> attributePatterns = null;
         Set<Pattern> operationPatterns = null;

         Map<String, Map<String, Method>> attributeMap = new HashMap<String, Map<String, Method>>();
         Map<String, Method> operationMap = new HashMap<String, Method>();

         if (writeableAttributes != null)
         {
            writeableAttributePatterns = new HashSet<Pattern>(writeableAttributes.size());
            for (String pattern : writeableAttributes)
            {
               Pattern p = Pattern.compile(pattern);
               writeableAttributePatterns.add(p);
            }
         }

         if (excludeAttributes != null)
         {
            attributePatterns = new HashSet<Pattern>(excludeAttributes.size());
            for (String pattern : excludeAttributes)
            {
               Pattern p = Pattern.compile(pattern);
               attributePatterns.add(p);
            }
         }

         if (excludeOperations != null)
         {
            operationPatterns = new HashSet<Pattern>(excludeOperations.size());
            for (String pattern : excludeOperations)
            {
               Pattern p = Pattern.compile(pattern);
               operationPatterns.add(p);
            }
         }

         Method[] methods = instance.getClass().getMethods();
         for (Method method : methods)
         {
            if (!method.getDeclaringClass().getName().startsWith("java."))
            {
               if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && 
                   method.getParameterTypes().length == 0)
               {
                  String s = method.getName().startsWith("get") ? method.getName().substring(3) :
                     method.getName().substring(2);

                  String name = s.substring(0, 1).toUpperCase(Locale.US);
                  if (s.length() > 1)
                     name += s.substring(1);

                  boolean include = true;

                  if (attributePatterns != null)
                  {
                     Iterator<Pattern> it = attributePatterns.iterator();
                     while (include && it.hasNext())
                     {
                        Pattern p = it.next();
                        if (p.matcher(name).matches())
                           include = false;
                     }
                  }

                  if (include)
                  {
                     Map<String, Method> m = attributeMap.get(name);

                     if (m == null)
                        m = new HashMap<String, Method>(2);

                     m.put(GET, method);
                     attributeMap.put(name, m);
                  }
               }
               else if (method.getName().startsWith("set") && method.getParameterTypes().length == 1)
               {
                  String s = method.getName().substring(3);

                  String name = s.substring(0, 1).toUpperCase(Locale.US);
                  if (s.length() > 1)
                     name += s.substring(1);

                  boolean include = true;

                  if (attributePatterns != null)
                  {
                     Iterator<Pattern> it = attributePatterns.iterator();
                     while (include && it.hasNext())
                     {
                        Pattern p = it.next();
                        if (p.matcher(name).matches())
                           include = false;
                     }
                  }
                  
                  if (include)
                  {
                     if (writeableAttributePatterns != null)
                     {
                        boolean writeable = false;

                        Iterator<Pattern> it = writeableAttributePatterns.iterator();
                        while (!writeable && it.hasNext())
                        {
                           Pattern p = it.next();
                           if (p.matcher(name).matches())
                              writeable = true;
                        }

                        if (writeable)
                        {
                           Map<String, Method> m = attributeMap.get(name);

                           if (m == null)
                              m = new HashMap<String, Method>(2);
                           
                           m.put(SET, method);
                           attributeMap.put(name, m);
                        }
                     }
                  }
               }
               else
               {
                  String name = method.getName();
                  boolean include = true;
                  
                  if (operationPatterns != null)
                  {
                     Iterator<Pattern> it = operationPatterns.iterator();
                     while (include && it.hasNext())
                     {
                        Pattern p = it.next();
                        if (p.matcher(name).matches())
                           include = false;
                     }
                  }
                  
                  if (include)
                  {
                     operationMap.put(name, method);
                  }
               }
            }
         }

         Iterator<Map.Entry<String, Map<String, Method>>> ait = attributeMap.entrySet().iterator();
         while (ait.hasNext())
         {
            Map.Entry<String, Map<String, Method>> entry = ait.next();
            String name = entry.getKey();
            Map<String, Method> m = entry.getValue();

            Method getMethod = m.get(GET);
            Method setMethod = m.get(SET);

            try
            {
               String desc = "";
               if (descriptions != null && descriptions.get(name) != null)
                  desc = descriptions.get(name);
            
               MBeanAttributeInfo mai = new MBeanAttributeInfo(name, desc, getMethod, setMethod);
               attrs.add(mai);
            }
            catch (Throwable t)
            {
               // Nothing to do
            }
         }

         Iterator<Map.Entry<String, Method>> oit = operationMap.entrySet().iterator();
         while (oit.hasNext())
         {
            Map.Entry<String, Method> entry = oit.next();

            String name = entry.getKey();
            Method operation = entry.getValue();

            try
            {
               String desc = "";
               if (descriptions != null && descriptions.get(name) != null)
                  desc = descriptions.get(name);

               MBeanParameterInfo[] signature = null;

               if (operation.getParameterTypes().length > 0)
               {
                  signature = new MBeanParameterInfo[operation.getParameterTypes().length];
                  for (int i = 0; i < operation.getParameterTypes().length; i++)
                  {
                     MBeanParameterInfo pi = new MBeanParameterInfo("p" + (i + 1),
                                                                    operation.getParameterTypes()[i].getName(),
                                                                    "");

                     signature[i] = pi;
                  }
               }

               MBeanOperationInfo moi = new MBeanOperationInfo(name,
                                                               desc, 
                                                               signature,
                                                               operation.getReturnType().getName(),
                                                               MBeanOperationInfo.UNKNOWN);

               ops.add(moi);
            }
            catch (Throwable t)
            {
               // Nothing to do
            }
         }

         if (attrs.size() > 0)
            Collections.sort(attrs, new MBeanAttributeComparator());

         if (ops.size() > 0)
            Collections.sort(ops, new MBeanOperationComparator());

         this.info = new MBeanInfo(instance.getClass().getName(),
                                   description != null ? description : "",
                                   attrs.size() > 0 ? attrs.toArray(new MBeanAttributeInfo[attrs.size()]) : null,
                                   null,
                                   ops.size() > 0 ? ops.toArray(new MBeanOperationInfo[ops.size()]) : null,
                                   null);
      }

      /**
       * {@inheritDoc}
       */
      public Object getAttribute(String attribute) throws AttributeNotFoundException,
                                                          MBeanException,
                                                          ReflectionException 
      {
         if (attribute == null)
            throw new AttributeNotFoundException("Invalid attribute name: null");

         Object instance = ref.get();
         if (instance == null)
            throw new MBeanException(null, "Instance garbaged collected");

         String name = attribute.substring(0, 1).toUpperCase(Locale.US);
         if (attribute.length() > 1)
            name += attribute.substring(1);

         for (MBeanAttributeInfo mai : info.getAttributes())
         {
            if (name.equals(mai.getName()))
            {
               try
               {
                  Method method = null;

                  if (!mai.isIs())
                  {
                     method = instance.getClass().getMethod("get" + name, (Class[])null);
                  }
                  else
                  {
                     method = instance.getClass().getMethod("is" + name, (Class[])null);
                  }

                  return method.invoke(instance, (Object[])null);
               }
               catch (Exception e)
               {
                  throw new MBeanException(e, "Exception during getAttribute(" + attribute + ")");
               }
            }
         }

         throw new AttributeNotFoundException("Invalid attribute name: " + attribute);
      }

      /**
       * {@inheritDoc}
       */
      public AttributeList getAttributes(String[] attributes)
      {
         if (attributes != null)
         {
            AttributeList result = new AttributeList();

            for (String attr : attributes)
            {
               try
               {
                  result.add(getAttribute(attr));
               }
               catch (Throwable t)
               {
                  // Nothing to do
               }
            }

            return result;
         }

         return null;
      }

      /**
       * {@inheritDoc}
       */
      public MBeanInfo getMBeanInfo()
      {
         return info;
      }

      /**
       * {@inheritDoc}
       */
      public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
                                                                                          ReflectionException
      {
         Object instance = ref.get();
         if (instance == null)
            throw new MBeanException(null, "Instance garbaged collected");

         for (MBeanOperationInfo moi : info.getOperations())
         {
            if (actionName.equals(moi.getName()))
            {
               boolean correct = false;
               
               if (signature == null && moi.getSignature() == null)
               {
                  correct = true;
               }
               else if (signature != null && moi.getSignature() != null)
               {
                  if (signature.length == moi.getSignature().length)
                  {
                     correct = true;

                     for (int i = 0; correct && i < signature.length; i++)
                     {
                        MBeanParameterInfo mpi = moi.getSignature()[i];

                        if (!signature[i].equals(mpi.getType()))
                           correct = false;
                     }
                  }
               }

               if (correct)
               {
                  try
                  {
                     Class[] paramTypes = null;

                     if (signature != null && signature.length > 0)
                     {
                        List<Class<?>> l = new ArrayList<Class<?>>(signature.length);

                        for (String paramType : signature)
                        {
                           Class<?> clz = Class.forName(paramType, true, instance.getClass().getClassLoader());
                           l.add(clz);
                        }

                        paramTypes = l.toArray(new Class[l.size()]);
                     }

                     Method method = instance.getClass().getMethod(actionName, paramTypes);

                     return method.invoke(instance, params);
                  }
                  catch (Exception e)
                  {
                     throw new MBeanException(e, "Exception during invoke(" + actionName + ", " +
                                              params + ", " + signature + ")");
                  }
               }
            }
         }

         return null;
      }

      /**
       * {@inheritDoc}
       */
      public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
                                                           InvalidAttributeValueException,
                                                           MBeanException,
                                                           ReflectionException
      {
         if (attribute == null)
            throw new AttributeNotFoundException("Invalid attribute name: null");

         Object instance = ref.get();
         if (instance == null)
            throw new MBeanException(null, "Instance garbaged collected");

         String name = attribute.getName().substring(0, 1).toUpperCase(Locale.US);
         if (attribute.getName().length() > 1)
            name += attribute.getName().substring(1);

         for (MBeanAttributeInfo mai : info.getAttributes())
         {
            if (name.equals(mai.getName()))
            {
               try
               {
                  Class<?> type = Class.forName(mai.getType(), true, instance.getClass().getClassLoader());
                  Method method = instance.getClass().getMethod("set" + name, new Class[] {type});

                  method.invoke(instance, new Object[] {attribute.getValue()});
               }
               catch (Exception e)
               {
                  throw new MBeanException(e, "Exception during setAttribute(" + attribute + ")");
               }
            }
         }
      }

      /**
       * {@inheritDoc}
       */
      public AttributeList setAttributes(AttributeList attributes)
      {
         if (attributes != null)
         {
            AttributeList result = new AttributeList();

            for (Attribute attr : attributes.asList())
            {
               try
               {
                  setAttribute(attr);
                  result.add(attr);
               }
               catch (Throwable t)
               {
                  // Nothing to do
               }
            }

            return result;
         }

         return null;
      }
   }

   /**
    * Comparator for MBeanAttributeInfo sorting
    */
   static class MBeanAttributeComparator implements Comparator<MBeanAttributeInfo>
   {
      /**
       * Constructor
       */
      public MBeanAttributeComparator()
      {
      }

      /**
       * Compare
       * @param o1 The first instance
       * @param o2 The second instance
       * @return -1 if o1 is less than o2; 1 if o1 is greater than o2; otherwise 0
       */
      public int compare(MBeanAttributeInfo o1, MBeanAttributeInfo o2)
      {
         return o1.getName().compareTo(o2.getName());
      }

      /**
       * Equals
       * @param obj The other object
       * @return True if equal; otherwise false
       */
      public boolean equals(Object obj)
      {
         if (obj == null)
            return false;

         if (obj == this)
            return true;

         return obj.getClass().equals(MBeanAttributeComparator.class);
      }

      /**
       * Hash code
       * @return The value
       */
      public int hashCode()
      {
         return 42;
      }
   }

   /**
    * Comparator for MBeanOperationInfo sorting
    */
   static class MBeanOperationComparator implements Comparator<MBeanOperationInfo>
   {
      /**
       * Constructor
       */
      public MBeanOperationComparator()
      {
      }

      /**
       * Compare
       * @param o1 The first instance
       * @param o2 The second instance
       * @return -1 if o1 is less than o2; 1 if o1 is greater than o2; otherwise 0
       */
      public int compare(MBeanOperationInfo o1, MBeanOperationInfo o2)
      {
         int result = o1.getName().compareTo(o2.getName());

         if (result == 0)
         {
            int p1 = o1.getSignature().length;
            int p2 = o2.getSignature().length;

            if (p1 < p2)
            {
               return -1;
            }
            else if (p1 > p2)
            {
               return 1;
            }
            else
            {
               for (int i = 0; i < o1.getSignature().length; i++)
               {
                  MBeanParameterInfo pi1 = o1.getSignature()[i];
                  MBeanParameterInfo pi2 = o2.getSignature()[i];

                  result = pi1.getType().compareTo(pi2.getType());

                  if (result != 0)
                     return result;
               }

               return 0;
            }
         }

         return result;
      }

      /**
       * Equals
       * @param obj The other object
       * @return True if equal; otherwise false
       */
      public boolean equals(Object obj)
      {
         if (obj == null)
            return false;

         if (obj == this)
            return true;

         return obj.getClass().equals(MBeanOperationComparator.class);
      }

      /**
       * Hash code
       * @return The value
       */
      public int hashCode()
      {
         return 42;
      }
   }
}

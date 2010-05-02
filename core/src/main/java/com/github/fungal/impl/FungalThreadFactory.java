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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The thread factory for Fungal
 * @author <a href="mailto:jesper.pedersen@comcast.net">Jesper Pedersen</a>
 */
public class FungalThreadFactory implements ThreadFactory
{
   /** The thread group */
   private ThreadGroup tg;

   /** Thread number */
   private AtomicInteger threadNumber;

   /**
    * Constructor
    * @param tg The thread group
    */
   public FungalThreadFactory(ThreadGroup tg)
   {
      this.tg = tg;
      this.threadNumber = new AtomicInteger(1);
   }

   /**
    * Create a new thread
    * @param r The runnable
    * @return The thread
    */
   public Thread newThread(Runnable r)
   {
      return new Thread(tg, r, "fungal-" + threadNumber.getAndIncrement());
   }
}

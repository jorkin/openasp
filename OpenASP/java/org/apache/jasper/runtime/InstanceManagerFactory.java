/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jasper.runtime;

import javax.servlet.ServletConfig;

import com.zfbots.openasp.InstanceManager;

/**
 * @version $Id: InstanceManagerFactory.java 1057677 2011-01-11 14:57:17Z markt $
 */
public class InstanceManagerFactory {

    private InstanceManagerFactory() {
    }

    public static InstanceManager getInstanceManager(ServletConfig config) {
        InstanceManager instanceManager = 
                (InstanceManager) config.getServletContext().getAttribute(InstanceManager.class.getName());
        if (instanceManager == null) {
            throw new IllegalStateException("No org.apache.tomcat.InstanceManager set in ServletContext");
        }
        return instanceManager;
    }

}

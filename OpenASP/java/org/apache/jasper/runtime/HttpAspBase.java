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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.asp.HttpAspPage;

import org.apache.jasper.compiler.Localizer;

/**
 * This is the super class of all ASP-generated servlets.
 *
 * @author Anil K. Vijendran
 */
public abstract class HttpAspBase extends HttpServlet implements HttpAspPage {
    
    private static final long serialVersionUID = 1L;

    protected HttpAspBase() {
    }

    @Override
    public final void init(ServletConfig config) 
        throws ServletException 
    {
        super.init(config);
        aspInit();
        _aspInit();
    }
    
    @Override
    public String getServletInfo() {
        return Localizer.getMessage("asp.engine.info");
    }

    @Override
    public final void destroy() {
        aspDestroy();
        _aspDestroy();
    }

    /**
     * Entry point into service.
     */
    @Override
    public final void service(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException 
    {
        _aspService(request, response);
    }
    
    @Override
    public void aspInit() {
    }

    public void _aspInit() {
    }

    @Override
    public void aspDestroy() {
    }

    protected void _aspDestroy() {
    }

    @Override
    public abstract void _aspService(HttpServletRequest request, 
                                     HttpServletResponse response) 
        throws ServletException, IOException;
}

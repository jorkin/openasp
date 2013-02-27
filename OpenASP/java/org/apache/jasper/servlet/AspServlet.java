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

package org.apache.jasper.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.Constants;
import org.apache.jasper.EmbeddedServletOptions;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.AspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.security.SecurityUtil;
import org.apache.jasper.util.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.zfbots.openasp.PeriodicEventListener;

/**
 * The ASP engine (a.k.a Jasper).
 *
 * The servlet container is responsible for providing a
 * URLClassLoader for the web application context Jasper
 * is being used in. Jasper will try get the Tomcat
 * ServletContext attribute for its ServletContext class
 * loader, if that fails, it uses the parent class loader.
 * In either case, it must be a URLClassLoader.
 *
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 * @author Remy Maucherat
 * @author Kin-man Chung
 * @author Glenn Nielsen
 */
public class AspServlet extends HttpServlet implements PeriodicEventListener {

    private static final long serialVersionUID = 1L;

    // Logger
    private final transient Log log = LogFactory.getLog(AspServlet.class);

    private transient ServletContext context;
    private ServletConfig config;
    private transient Options options;
    private transient AspRuntimeContext rctxt;
    //aspFile for a asp configured explicitly as a servlet, in environments where this configuration is
    //translated into an init-param for this servlet.
    private String aspFile;


    /*
     * Initializes this AspServlet.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        
        super.init(config);
        this.config = config;
        this.context = config.getServletContext();
        
        // Initialize the ASP Runtime Context
        // Check for a custom Options implementation
        String engineOptionsName = 
            config.getInitParameter("engineOptionsClass");
        if (engineOptionsName != null) {
            // Instantiate the indicated Options implementation
            try {
                ClassLoader loader = Thread.currentThread()
                        .getContextClassLoader();
                Class<?> engineOptionsClass =
                    loader.loadClass(engineOptionsName);
                Class<?>[] ctorSig =
                    { ServletConfig.class, ServletContext.class };
                Constructor<?> ctor =
                    engineOptionsClass.getConstructor(ctorSig);
                Object[] args = { config, context };
                options = (Options) ctor.newInstance(args);
            } catch (Throwable e) {
                e = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(e);
                // Need to localize this.
                log.warn("Failed to load engineOptionsClass", e);
                // Use the default Options implementation
                options = new EmbeddedServletOptions(config, context);
            }
        } else {
            // Use the default Options implementation
            options = new EmbeddedServletOptions(config, context);
        }
        rctxt = new AspRuntimeContext(context, options);
        if (config.getInitParameter("aspFile") != null) {
            aspFile = config.getInitParameter("aspFile");
            try {
                if (null == context.getResource(aspFile)) {
                    throw new ServletException("missing aspFile");
                }
            } catch (MalformedURLException e) {
                throw new ServletException("Can not locate asp file", e);
            }
            try {
                if (SecurityUtil.isPackageProtectionEnabled()){
                   AccessController.doPrivileged(new PrivilegedExceptionAction<Object>(){
                        @Override
                        public Object run() throws IOException, ServletException {
                            serviceAspFile(null, null, aspFile, true);
                            return null;
                        }
                    });
                } else {
                    serviceAspFile(null, null, aspFile, true);
                }
            } catch (IOException e) {
                throw new ServletException("Could not precompile asp: " + aspFile, e);
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof ServletException) throw (ServletException)t;
                throw new ServletException("Could not precompile asp: " + aspFile, e);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(Localizer.getMessage("asp.message.scratch.dir.is",
                    options.getScratchDir().toString()));
            log.debug(Localizer.getMessage("asp.message.dont.modify.servlets"));
        }
    }


    /**
     * Returns the number of JSPs for which AspServletWrappers exist, i.e.,
     * the number of JSPs that have been loaded into the webapp with which
     * this AspServlet is associated.
     *
     * <p>This info may be used for monitoring purposes.
     *
     * @return The number of JSPs that have been loaded into the webapp with
     * which this AspServlet is associated
     */
    public int getAspCount() {
        return this.rctxt.getAspCount();
    }


    /**
     * Resets the JSP reload counter.
     *
     * @param count Value to which to reset the JSP reload counter
     */
    public void setAspReloadCount(int count) {
        this.rctxt.setAspReloadCount(count);
    }


    /**
     * Gets the number of JSPs that have been reloaded.
     *
     * <p>This info may be used for monitoring purposes.
     *
     * @return The number of JSPs (in the webapp with which this AspServlet is
     * associated) that have been reloaded
     */
    public int getAspReloadCount() {
        return this.rctxt.getAspReloadCount();
    }


    /**
     * Gets the number of JSPs that are in the JSP limiter queue
     *
     * <p>This info may be used for monitoring purposes.
     *
     * @return The number of JSPs (in the webapp with which this AspServlet is
     * associated) that are in the JSP limiter queue
     */
    public int getAspQueueLength() {
        return this.rctxt.getAspQueueLength();
    }


    /**
     * Gets the number of JSPs that have been unloaded.
     *
     * <p>This info may be used for monitoring purposes.
     *
     * @return The number of JSPs (in the webapp with which this AspServlet is
     * associated) that have been unloaded
     */
    public int getAspUnloadCount() {
        return this.rctxt.getAspUnloadCount();
    }


    /**
     * <p>Look for a <em>precompilation request</em> as described in
     * Section 8.4.2 of the JSP 1.2 Specification.  <strong>WARNING</strong> -
     * we cannot use <code>request.getParameter()</code> for this, because
     * that will trigger parsing all of the request parameters, and not give
     * a servlet the opportunity to call
     * <code>request.setCharacterEncoding()</code> first.</p>
     *
     * @param request The servlet request we are processing
     *
     * @exception ServletException if an invalid parameter value for the
     *  <code>asp_precompile</code> parameter name is specified
     */
    boolean preCompile(HttpServletRequest request) throws ServletException {

        String queryString = request.getQueryString();
        if (queryString == null) {
            return (false);
        }
        int start = queryString.indexOf(Constants.PRECOMPILE);
        if (start < 0) {
            return (false);
        }
        queryString =
            queryString.substring(start + Constants.PRECOMPILE.length());
        if (queryString.length() == 0) {
            return (true);             // ?asp_precompile
        }
        if (queryString.startsWith("&")) {
            return (true);             // ?asp_precompile&foo=bar...
        }
        if (!queryString.startsWith("=")) {
            return (false);            // part of some other name or value
        }
        int limit = queryString.length();
        int ampersand = queryString.indexOf("&");
        if (ampersand > 0) {
            limit = ampersand;
        }
        String value = queryString.substring(1, limit);
        if (value.equals("true")) {
            return (true);             // ?asp_precompile=true
        } else if (value.equals("false")) {
            // Spec says if asp_precompile=false, the request should not
            // be delivered to the ASP page; the easiest way to implement
            // this is to set the flag to true, and precompile the page anyway.
            // This still conforms to the spec, since it says the
            // precompilation request can be ignored.
            return (true);             // ?asp_precompile=false
        } else {
            throw new ServletException("Cannot have request parameter " +
                                       Constants.PRECOMPILE + " set to " +
                                       value);
        }

    }
    

    @Override
    public void service (HttpServletRequest request, 
                             HttpServletResponse response)
                throws ServletException, IOException {
        //aspFile may be configured as an init-param for this servlet instance
        String aspUri = aspFile;

        if (aspUri == null) {
            // JSP specified via <asp-file> in <servlet> declaration and supplied through
            //custom servlet container code
            aspUri = (String) request.getAttribute(Constants.JSP_FILE);
        }
        if (aspUri == null) {
            /*
             * Check to see if the requested JSP has been the target of a
             * RequestDispatcher.include()
             */
            aspUri = (String) request.getAttribute(
                    RequestDispatcher.INCLUDE_SERVLET_PATH);
            if (aspUri != null) {
                /*
                 * Requested JSP has been target of
                 * RequestDispatcher.include(). Its path is assembled from the
                 * relevant javax.servlet.include.* request attributes
                 */
                String pathInfo = (String) request.getAttribute(
                        RequestDispatcher.INCLUDE_PATH_INFO);
                if (pathInfo != null) {
                    aspUri += pathInfo;
                }
            } else {
                /*
                 * Requested JSP has not been the target of a 
                 * RequestDispatcher.include(). Reconstruct its path from the
                 * request's getServletPath() and getPathInfo()
                 */
                aspUri = request.getServletPath();
                String pathInfo = request.getPathInfo();
                if (pathInfo != null) {
                    aspUri += pathInfo;
                }
            }
        }

        if (log.isDebugEnabled()) {    
            log.debug("AspEngine --> " + aspUri);
            log.debug("\t     ServletPath: " + request.getServletPath());
            log.debug("\t        PathInfo: " + request.getPathInfo());
            log.debug("\t        RealPath: " + context.getRealPath(aspUri));
            log.debug("\t      RequestURI: " + request.getRequestURI());
            log.debug("\t     QueryString: " + request.getQueryString());
        }

        try {
            boolean precompile = preCompile(request);
            serviceAspFile(request, response, aspUri, precompile);
        } catch (RuntimeException e) {
            throw e;
        } catch (ServletException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            ExceptionUtils.handleThrowable(e);
            throw new ServletException(e);
        }

    }

    @Override
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("AspServlet.destroy()");
        }

        rctxt.destroy();
    }


    @Override
    public void periodicEvent() {
        rctxt.checkUnload();
        rctxt.checkCompile();
    }

    // -------------------------------------------------------- Private Methods

    private void serviceAspFile(HttpServletRequest request,
                                HttpServletResponse response, String aspUri,
                                boolean precompile)
        throws ServletException, IOException {

        AspServletWrapper wrapper = rctxt.getWrapper(aspUri);
        if (wrapper == null) {
            synchronized(this) {
                wrapper = rctxt.getWrapper(aspUri);
                if (wrapper == null) {
                    // Check if the requested JSP page exists, to avoid
                    // creating unnecessary directories and files.
                    if (null == context.getResource(aspUri)) {
                        handleMissingResource(request, response, aspUri);
                        return;
                    }
                    wrapper = new AspServletWrapper(config, options, aspUri,
                                                    rctxt);
                    rctxt.addWrapper(aspUri,wrapper);
                }
            }
        }

        try {
            wrapper.service(request, response, precompile);
        } catch (FileNotFoundException fnfe) {
            handleMissingResource(request, response, aspUri);
        }

    }


    private void handleMissingResource(HttpServletRequest request,
            HttpServletResponse response, String aspUri)
            throws ServletException, IOException {

        String includeRequestUri =
            (String)request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);

        if (includeRequestUri != null) {
            // This file was included. Throw an exception as
            // a response.sendError() will be ignored
            String msg =
                Localizer.getMessage("asp.error.file.not.found",aspUri);
            // Strictly, filtering this is an application
            // responsibility but just in case...
            throw new ServletException(SecurityUtil.filter(msg));
        } else {
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        request.getRequestURI());
            } catch (IllegalStateException ise) {
                log.error(Localizer.getMessage("asp.error.file.not.found",
                        aspUri));
            }
        }
        return;
    }


}

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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.asp.tagext.TagInfo;

import org.apache.jasper.Constants;
import org.apache.jasper.JasperException;
import org.apache.jasper.AspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.compiler.ErrorDispatcher;
import org.apache.jasper.compiler.JarResource;
import org.apache.jasper.compiler.JavacErrorDetail;
import org.apache.jasper.compiler.AspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.runtime.InstanceManagerFactory;
import org.apache.jasper.runtime.AspSourceDependent;
import org.apache.jasper.util.ExceptionUtils;
import org.apache.jasper.util.FastRemovalDequeue;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.zfbots.openasp.InstanceManager;

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
 * @author Tim Fennell
 */

@SuppressWarnings("deprecation") // Have to support SingleThreadModel
public class AspServletWrapper {

    private static final Map<String,Long> ALWAYS_OUTDATED_DEPENDENCIES =
        new HashMap<String,Long>();

    static {
        // If this is missing, 
        ALWAYS_OUTDATED_DEPENDENCIES.put("/WEB-INF/web.xml", Long.valueOf(-1));
    }

    // Logger
    private final Log log = LogFactory.getLog(AspServletWrapper.class);

    private Servlet theServlet;
    private String aspUri;
    private Class<?> tagHandlerClass;
    private AspCompilationContext ctxt;
    private long available = 0L;
    private ServletConfig config;
    private Options options;
    private boolean firstTime = true;
    /** Whether the servlet needs reloading on next access */
    private volatile boolean reload = true;
    private boolean isTagFile;
    private int tripCount;
    private JasperException compileException;
    /** Timestamp of last time servlet resource was modified */
    private volatile long servletClassLastModifiedTime;
    private long lastModificationTest = 0L;
    private long lastUsageTime = System.currentTimeMillis();
    private FastRemovalDequeue<AspServletWrapper>.Entry unloadHandle;
    private final boolean unloadAllowed;
    private final boolean unloadByCount;
    private final boolean unloadByIdle;

    /*
     * AspServletWrapper for ASP pages.
     */
    public AspServletWrapper(ServletConfig config, Options options,
            String aspUri, AspRuntimeContext rctxt) {

        this.isTagFile = false;
        this.config = config;
        this.options = options;
        this.aspUri = aspUri;
        unloadByCount = options.getMaxLoadedAsps() > 0 ? true : false;
        unloadByIdle = options.getAspIdleTimeout() > 0 ? true : false;
        unloadAllowed = unloadByCount || unloadByIdle ? true : false;
        ctxt = new AspCompilationContext(aspUri, options,
                                         config.getServletContext(),
                                         this, rctxt);
    }

    /*
     * AspServletWrapper for tag files.
     */
    public AspServletWrapper(ServletContext servletContext,
                             Options options,
                             String tagFilePath,
                             TagInfo tagInfo,
                             AspRuntimeContext rctxt,
                             JarResource tagJarResource) {

        this.isTagFile = true;
        this.config = null;        // not used
        this.options = options;
        this.aspUri = tagFilePath;
        this.tripCount = 0;
        unloadByCount = options.getMaxLoadedAsps() > 0 ? true : false;
        unloadByIdle = options.getAspIdleTimeout() > 0 ? true : false;
        unloadAllowed = unloadByCount || unloadByIdle ? true : false;
        ctxt = new AspCompilationContext(aspUri, tagInfo, options,
                                         servletContext, this, rctxt,
                                         tagJarResource);
    }

    public AspCompilationContext getAspEngineContext() {
        return ctxt;
    }

    public void setReload(boolean reload) {
        this.reload = reload;
    }

    public Servlet getServlet() throws ServletException {
        // DCL on 'reload' requires that 'reload' be volatile
        // (this also forces a read memory barrier, ensuring the 
        // new servlet object is read consistently)
        if (reload) {
            synchronized (this) {
                // Synchronizing on jsw enables simultaneous loading
                // of different pages, but not the same page.
                if (reload) {
                    // This is to maintain the original protocol.
                    destroy();
                    
                    final Servlet servlet;

                    try {
                        InstanceManager instanceManager = InstanceManagerFactory.getInstanceManager(config);
                        servlet = (Servlet) instanceManager.newInstance(ctxt.getFQCN(), ctxt.getAspLoader());
                    } catch (Exception e) {
                        Throwable t = ExceptionUtils
                                .unwrapInvocationTargetException(e);
                        ExceptionUtils.handleThrowable(t);
                        throw new JasperException(t);
                    }
                    
                    servlet.init(config);

                    if (!firstTime) {
                        ctxt.getRuntimeContext().incrementAspReloadCount();
                    }

                    theServlet = servlet;
                    reload = false;
                    // Volatile 'reload' forces in order write of 'theServlet' and new servlet object
                }
            }    
        }
        return theServlet;
    }

    public ServletContext getServletContext() {
        return ctxt.getServletContext();
    }

    /**
     * Sets the compilation exception for this AspServletWrapper.
     *
     * @param je The compilation exception
     */
    public void setCompilationException(JasperException je) {
        this.compileException = je;
    }

    /**
     * Sets the last-modified time of the servlet class file associated with
     * this AspServletWrapper.
     *
     * @param lastModified Last-modified time of servlet class
     */
    public void setServletClassLastModifiedTime(long lastModified) {
        // DCL requires servletClassLastModifiedTime be volatile
        // to force read and write barriers on access/set
        // (and to get atomic write of long)
        if (this.servletClassLastModifiedTime < lastModified) {
            synchronized (this) {
                if (this.servletClassLastModifiedTime < lastModified) {
                    this.servletClassLastModifiedTime = lastModified;
                    reload = true;
                }
            }
        }
    }

    /**
     * Compile (if needed) and load a tag file
     */
    public Class<?> loadTagFile() throws JasperException {

        try {
            if (ctxt.isRemoved()) {
                throw new FileNotFoundException(aspUri);
            }
            if (options.getDevelopment() || firstTime ) {
                synchronized (this) {
                    firstTime = false;
                    ctxt.compile();
                }
            } else {
                if (compileException != null) {
                    throw compileException;
                }
            }

            if (reload) {
                tagHandlerClass = ctxt.load();
                reload = false;
            }
        } catch (FileNotFoundException ex) {
            throw new JasperException(ex);
        }

        return tagHandlerClass;
    }

    /**
     * Compile and load a prototype for the Tag file.  This is needed
     * when compiling tag files with circular dependencies.  A prototype
     * (skeleton) with no dependencies on other other tag files is
     * generated and compiled.
     */
    public Class<?> loadTagFilePrototype() throws JasperException {

        ctxt.setPrototypeMode(true);
        try {
            return loadTagFile();
        } finally {
            ctxt.setPrototypeMode(false);
        }
    }

    /**
     * Get a list of files that the current page has source dependency on.
     */
    public java.util.Map<String,Long> getDependants() {
        try {
            Object target;
            if (isTagFile) {
                if (reload) {
                    tagHandlerClass = ctxt.load();
                    reload = false;
                }
                target = tagHandlerClass.newInstance();
            } else {
                target = getServlet();
            }
            if (target != null && target instanceof AspSourceDependent) {
                return ((AspSourceDependent) target).getDependants();
            }
        } catch (AbstractMethodError ame) {
            // Almost certainly a pre Tomcat 7.0.17 compiled JSP using the old
            // version of the interface. Force a re-compile.
            return ALWAYS_OUTDATED_DEPENDENCIES;
        } catch (Throwable ex) {
            ExceptionUtils.handleThrowable(ex);
        }
        return null;
    }

    public boolean isTagFile() {
        return this.isTagFile;
    }

    public int incTripCount() {
        return tripCount++;
    }

    public int decTripCount() {
        return tripCount--;
    }

    public String getAspUri() {
        return aspUri;
    }

    public FastRemovalDequeue<AspServletWrapper>.Entry getUnloadHandle() {
        return unloadHandle;
    }

    public void service(HttpServletRequest request, 
                        HttpServletResponse response,
                        boolean precompile)
            throws ServletException, IOException, FileNotFoundException {
        
        Servlet servlet;

        try {

            if (ctxt.isRemoved()) {
                throw new FileNotFoundException(aspUri);
            }

            if ((available > 0L) && (available < Long.MAX_VALUE)) {
                if (available > System.currentTimeMillis()) {
                    response.setDateHeader("Retry-After", available);
                    response.sendError
                        (HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                         Localizer.getMessage("asp.error.unavailable"));
                    return;
                }

                // Wait period has expired. Reset.
                available = 0;
            }

            /*
             * (1) Compile
             */
            if (options.getDevelopment() || firstTime ) {
                synchronized (this) {
                    firstTime = false;

                    // The following sets reload to true, if necessary
                    ctxt.compile();
                }
            } else {
                if (compileException != null) {
                    // Throw cached compilation exception
                    throw compileException;
                }
            }

            /*
             * (2) (Re)load servlet class file
             */
            servlet = getServlet();

            // If a page is to be precompiled only, return.
            if (precompile) {
                return;
            }

        } catch (ServletException ex) {
            if (options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw ex;
        } catch (FileNotFoundException fnfe) {
            // File has been removed. Let caller handle this.
            throw fnfe;
        } catch (IOException ex) {
            if (options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw ex;
        } catch (IllegalStateException ex) {
            if (options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw ex;
        } catch (Exception ex) {
            if (options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw new JasperException(ex);
        }

        try {
            
            /*
             * (3) Handle limitation of number of loaded Asps
             */
            if (unloadAllowed) {
                synchronized(this) {
                    if (unloadByCount) {
                        if (unloadHandle == null) {
                            unloadHandle = ctxt.getRuntimeContext().push(this);
                        } else if (lastUsageTime < ctxt.getRuntimeContext().getLastAspQueueUpdate()) {
                            ctxt.getRuntimeContext().makeYoungest(unloadHandle);
                            lastUsageTime = System.currentTimeMillis();
                        }
                    } else {
                        if (lastUsageTime < ctxt.getRuntimeContext().getLastAspQueueUpdate()) {
                            lastUsageTime = System.currentTimeMillis();
                        }
                    }
                }
            }
            /*
             * (4) Service request
             */
            if (servlet instanceof SingleThreadModel) {
               // sync on the wrapper so that the freshness
               // of the page is determined right before servicing
               synchronized (this) {
                   servlet.service(request, response);
                }
            } else {
                servlet.service(request, response);
            }
        } catch (UnavailableException ex) {
            String includeRequestUri = (String)
                request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
            if (includeRequestUri != null) {
                // This file was included. Throw an exception as
                // a response.sendError() will be ignored by the
                // servlet engine.
                throw ex;
            }

            int unavailableSeconds = ex.getUnavailableSeconds();
            if (unavailableSeconds <= 0) {
                unavailableSeconds = 60;        // Arbitrary default
            }
            available = System.currentTimeMillis() +
                (unavailableSeconds * 1000L);
            response.sendError
                (HttpServletResponse.SC_SERVICE_UNAVAILABLE, 
                 ex.getMessage());
        } catch (ServletException ex) {
            if(options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw ex;
        } catch (IOException ex) {
            if(options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw ex;
        } catch (IllegalStateException ex) {
            if(options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw ex;
        } catch (Exception ex) {
            if(options.getDevelopment()) {
                throw handleAspException(ex);
            }
            throw new JasperException(ex);
        }
    }

    public void destroy() {
        if (theServlet != null) {
            theServlet.destroy();
            InstanceManager instanceManager = InstanceManagerFactory.getInstanceManager(config);
            try {
                instanceManager.destroyInstance(theServlet);
            } catch (Exception e) {
                Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
                ExceptionUtils.handleThrowable(t);
                // Log any exception, since it can't be passed along
                log.error(Localizer.getMessage("asp.error.file.not.found",
                        e.getMessage()), t);
            }
        }
    }

    /**
     * @return Returns the lastModificationTest.
     */
    public long getLastModificationTest() {
        return lastModificationTest;
    }
    /**
     * @param lastModificationTest The lastModificationTest to set.
     */
    public void setLastModificationTest(long lastModificationTest) {
        this.lastModificationTest = lastModificationTest;
    }

    /**
     * @return the lastUsageTime.
     */
    public long getLastUsageTime() {
        return lastUsageTime;
    }

    /**
     * <p>Attempts to construct a JasperException that contains helpful information
     * about what went wrong. Uses the ASP compiler system to translate the line
     * number in the generated servlet that originated the exception to a line
     * number in the ASP.  Then constructs an exception containing that
     * information, and a snippet of the ASP to help debugging.
     * Please see http://issues.apache.org/bugzilla/show_bug.cgi?id=37062 and
     * http://www.tfenne.com/jasper/ for more details.
     *</p>
     *
     * @param ex the exception that was the cause of the problem.
     * @return a JasperException with more detailed information
     */
    protected JasperException handleAspException(Exception ex) {
        try {
            Throwable realException = ex;
            if (ex instanceof ServletException) {
                realException = ((ServletException) ex).getRootCause();
            }

            // First identify the stack frame in the trace that represents the JSP
            StackTraceElement[] frames = realException.getStackTrace();
            StackTraceElement aspFrame = null;

            for (int i=0; i<frames.length; ++i) {
                if ( frames[i].getClassName().equals(this.getServlet().getClass().getName()) ) {
                    aspFrame = frames[i];
                    break;
                }
            }

            
            if (aspFrame == null ||
                    this.ctxt.getCompiler().getPageNodes() == null) {
                // If we couldn't find a frame in the stack trace corresponding
                // to the generated servlet class or we don't have a copy of the
                // parsed ASP to hand, we can't really add anything
                return new JasperException(ex);
            }

            int javaLineNumber = aspFrame.getLineNumber();
            JavacErrorDetail detail = ErrorDispatcher.createJavacError(
                    aspFrame.getMethodName(),
                    this.ctxt.getCompiler().getPageNodes(),
                    null,
                    javaLineNumber,
                    ctxt);

            // If the line number is less than one we couldn't find out
            // where in the JSP things went wrong
            int aspLineNumber = detail.getAspBeginLineNumber();
            if (aspLineNumber < 1) {
                throw new JasperException(ex);
            }

            if (options.getDisplaySourceFragment()) {
                return new JasperException(Localizer.getMessage
                        ("asp.exception", detail.getAspFileName(),
                                "" + aspLineNumber) + Constants.NEWLINE +
                                Constants.NEWLINE + detail.getAspExtract() +
                                Constants.NEWLINE + Constants.NEWLINE + 
                                "Stacktrace:", ex);
                
            }

            return new JasperException(Localizer.getMessage
                    ("asp.exception", detail.getAspFileName(),
                            "" + aspLineNumber), ex);
        } catch (Exception je) {
            // If anything goes wrong, just revert to the original behaviour
            if (ex instanceof JasperException) {
                return (JasperException) ex;
            }
            return new JasperException(ex);
        }
    }

}

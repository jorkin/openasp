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

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.asp.AspApplicationContext;
import javax.servlet.asp.AspEngineInfo;
import javax.servlet.asp.AspFactory;
import javax.servlet.asp.PageContext;

import org.apache.jasper.Constants;
import org.apache.jasper.util.ExceptionUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Implementation of AspFactory.
 *
 * @author Anil K. Vijendran
 */
public class AspFactoryImpl extends AspFactory {

    // Logger
    private final Log log = LogFactory.getLog(AspFactoryImpl.class);

    private static final String SPEC_VERSION = "2.1";
    private static final boolean USE_POOL = 
        Boolean.valueOf(System.getProperty("org.apache.jasper.runtime.AspFactoryImpl.USE_POOL", "true")).booleanValue();
    private static final int POOL_SIZE = 
        Integer.valueOf(System.getProperty("org.apache.jasper.runtime.AspFactoryImpl.POOL_SIZE", "8")).intValue();

    private ThreadLocal<PageContextPool> localPool = new ThreadLocal<PageContextPool>();

    @Override
    public PageContext getPageContext(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {

        if( Constants.IS_SECURITY_ENABLED ) {
            PrivilegedGetPageContext dp = new PrivilegedGetPageContext(
                    this, servlet, request, response, errorPageURL,
                    needsSession, bufferSize, autoflush);
            return AccessController.doPrivileged(dp);
        } else {
            return internalGetPageContext(servlet, request, response,
                    errorPageURL, needsSession,
                    bufferSize, autoflush);
        }
    }

    @Override
    public void releasePageContext(PageContext pc) {
        if( pc == null )
            return;
        if( Constants.IS_SECURITY_ENABLED ) {
            PrivilegedReleasePageContext dp = new PrivilegedReleasePageContext(
                    this,pc);
            AccessController.doPrivileged(dp);
        } else {
            internalReleasePageContext(pc);
        }
    }

    @Override
    public AspEngineInfo getEngineInfo() {
        return new AspEngineInfo() {
            @Override
            public String getSpecificationVersion() {
                return SPEC_VERSION;
            }
        };
    }

    private PageContext internalGetPageContext(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL, boolean needsSession,
            int bufferSize, boolean autoflush) {
        try {
            PageContext pc;
            if (USE_POOL) {
                PageContextPool pool = localPool.get();
                if (pool == null) {
                    pool = new PageContextPool();
                    localPool.set(pool);
                }
                pc = pool.get();
                if (pc == null) {
                    pc = new PageContextImpl();
                }
            } else {
                pc = new PageContextImpl();
            }
            pc.initialize(servlet, request, response, errorPageURL, 
                    needsSession, bufferSize, autoflush);
            return pc;
        } catch (Throwable ex) {
            ExceptionUtils.handleThrowable(ex);
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            log.fatal("Exception initializing page context", ex);
            return null;
        }
    }

    private void internalReleasePageContext(PageContext pc) {
        pc.release();
        if (USE_POOL && (pc instanceof PageContextImpl)) {
            localPool.get().put(pc);
        }
    }

    private static class PrivilegedGetPageContext
            implements PrivilegedAction<PageContext> {

        private AspFactoryImpl factory;
        private Servlet servlet;
        private ServletRequest request;
        private ServletResponse response;
        private String errorPageURL;
        private boolean needsSession;
        private int bufferSize;
        private boolean autoflush;

        PrivilegedGetPageContext(AspFactoryImpl factory, Servlet servlet,
                ServletRequest request, ServletResponse response, String errorPageURL,
                boolean needsSession, int bufferSize, boolean autoflush) {
            this.factory = factory;
            this.servlet = servlet;
            this.request = request;
            this.response = response;
            this.errorPageURL = errorPageURL;
            this.needsSession = needsSession;
            this.bufferSize = bufferSize;
            this.autoflush = autoflush;
        }

        @Override
        public PageContext run() {
            return factory.internalGetPageContext(servlet, request, response,
                    errorPageURL, needsSession, bufferSize, autoflush);
        }
    }

    private static class PrivilegedReleasePageContext
            implements PrivilegedAction<Void> {

        private AspFactoryImpl factory;
        private PageContext pageContext;

        PrivilegedReleasePageContext(AspFactoryImpl factory,
                PageContext pageContext) {
            this.factory = factory;
            this.pageContext = pageContext;
        }

        @Override
        public Void run() {
            factory.internalReleasePageContext(pageContext);
            return null;
        }
    }

    protected static final class PageContextPool  {

        private PageContext[] pool;

        private int current = -1;

        public PageContextPool() {
            this.pool = new PageContext[POOL_SIZE];
        }

        public void put(PageContext o) {
            if (current < (POOL_SIZE - 1)) {
                current++;
                pool[current] = o;
            }
        }

        public PageContext get() {
            PageContext item = null;
            if (current >= 0) {
                item = pool[current];
                current--;
            }
            return item;
        }

    }

    @Override
    public AspApplicationContext getAspApplicationContext(
            final ServletContext context) {
        if (Constants.IS_SECURITY_ENABLED) {
            return AccessController.doPrivileged(
                    new PrivilegedAction<AspApplicationContext>() {
                @Override
                public AspApplicationContext run() {
                    return AspApplicationContextImpl.getInstance(context);
                }
            });
        } else {
            return AspApplicationContextImpl.getInstance(context);
        }
    }
}

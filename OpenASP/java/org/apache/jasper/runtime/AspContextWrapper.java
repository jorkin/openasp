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
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.asp.AspContext;
import javax.servlet.asp.AspWriter;
import javax.servlet.asp.PageContext;
import javax.servlet.asp.el.ELException;
import javax.servlet.asp.el.ExpressionEvaluator;
import javax.servlet.asp.el.VariableResolver;
import javax.servlet.asp.tagext.BodyContent;
import javax.servlet.asp.tagext.VariableInfo;

import org.apache.jasper.compiler.Localizer;

/**
 * Implementation of a ASP Context Wrapper.
 * 
 * The ASP Context Wrapper is a AspContext created and maintained by a tag
 * handler implementation. It wraps the Invoking ASP Context, that is, the
 * AspContext instance passed to the tag handler by the invoking page via
 * setAspContext().
 * 
 * @author Kin-man Chung
 * @author Jan Luehe
 * @author Jacob Hookom
 */
public class AspContextWrapper extends PageContext implements VariableResolver {

    // Invoking JSP context
    private PageContext invokingAspCtxt;

    private transient HashMap<String, Object> pageAttributes;

    // ArrayList of NESTED scripting variables
    private ArrayList<String> nestedVars;

    // ArrayList of AT_BEGIN scripting variables
    private ArrayList<String> atBeginVars;

    // ArrayList of AT_END scripting variables
    private ArrayList<String> atEndVars;

    private Map<String,String> aliases;

    private HashMap<String, Object> originalNestedVars;

    public AspContextWrapper(AspContext aspContext,
            ArrayList<String> nestedVars, ArrayList<String> atBeginVars,
            ArrayList<String> atEndVars, Map<String,String> aliases) {
        this.invokingAspCtxt = (PageContext) aspContext;
        this.nestedVars = nestedVars;
        this.atBeginVars = atBeginVars;
        this.atEndVars = atEndVars;
        this.pageAttributes = new HashMap<String, Object>(16);
        this.aliases = aliases;

        if (nestedVars != null) {
            this.originalNestedVars = new HashMap<String, Object>(nestedVars.size());
        }
        syncBeginTagFile();
    }

    @Override
    public void initialize(Servlet servlet, ServletRequest request,
            ServletResponse response, String errorPageURL,
            boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException, IllegalStateException, IllegalArgumentException {
    }

    @Override
    public Object getAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        return pageAttributes.get(name);
    }

    @Override
    public Object getAttribute(String name, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            return pageAttributes.get(name);
        }

        return invokingAspCtxt.getAttribute(name, scope);
    }

    @Override
    public void setAttribute(String name, Object value) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        if (value != null) {
            pageAttributes.put(name, value);
        } else {
            removeAttribute(name, PAGE_SCOPE);
        }
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            if (value != null) {
                pageAttributes.put(name, value);
            } else {
                removeAttribute(name, PAGE_SCOPE);
            }
        } else {
            invokingAspCtxt.setAttribute(name, value, scope);
        }
    }

    @Override
    public Object findAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        Object o = pageAttributes.get(name);
        if (o == null) {
            o = invokingAspCtxt.getAttribute(name, REQUEST_SCOPE);
            if (o == null) {
                if (getSession() != null) {
                    o = invokingAspCtxt.getAttribute(name, SESSION_SCOPE);
                }
                if (o == null) {
                    o = invokingAspCtxt.getAttribute(name, APPLICATION_SCOPE);
                }
            }
        }

        return o;
    }

    @Override
    public void removeAttribute(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        pageAttributes.remove(name);
        invokingAspCtxt.removeAttribute(name, REQUEST_SCOPE);
        if (getSession() != null) {
            invokingAspCtxt.removeAttribute(name, SESSION_SCOPE);
        }
        invokingAspCtxt.removeAttribute(name, APPLICATION_SCOPE);
    }

    @Override
    public void removeAttribute(String name, int scope) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        if (scope == PAGE_SCOPE) {
            pageAttributes.remove(name);
        } else {
            invokingAspCtxt.removeAttribute(name, scope);
        }
    }

    @Override
    public int getAttributesScope(String name) {

        if (name == null) {
            throw new NullPointerException(Localizer
                    .getMessage("asp.error.attribute.null_name"));
        }

        if (pageAttributes.get(name) != null) {
            return PAGE_SCOPE;
        } else {
            return invokingAspCtxt.getAttributesScope(name);
        }
    }

    @Override
    public Enumeration<String> getAttributeNamesInScope(int scope) {
        if (scope == PAGE_SCOPE) {
            return Collections.enumeration(pageAttributes.keySet());
        }

        return invokingAspCtxt.getAttributeNamesInScope(scope);
    }

    @Override
    public void release() {
        invokingAspCtxt.release();
    }

    @Override
    public AspWriter getOut() {
        return invokingAspCtxt.getOut();
    }

    @Override
    public HttpSession getSession() {
        return invokingAspCtxt.getSession();
    }

    @Override
    public Object getPage() {
        return invokingAspCtxt.getPage();
    }

    @Override
    public ServletRequest getRequest() {
        return invokingAspCtxt.getRequest();
    }

    @Override
    public ServletResponse getResponse() {
        return invokingAspCtxt.getResponse();
    }

    @Override
    public Exception getException() {
        return invokingAspCtxt.getException();
    }

    @Override
    public ServletConfig getServletConfig() {
        return invokingAspCtxt.getServletConfig();
    }

    @Override
    public ServletContext getServletContext() {
        return invokingAspCtxt.getServletContext();
    }

    @Override
    public void forward(String relativeUrlPath) throws ServletException,
            IOException {
        invokingAspCtxt.forward(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath) throws ServletException,
            IOException {
        invokingAspCtxt.include(relativeUrlPath);
    }

    @Override
    public void include(String relativeUrlPath, boolean flush)
            throws ServletException, IOException {
        invokingAspCtxt.include(relativeUrlPath, false);
    }

    @Override
    @Deprecated
    public VariableResolver getVariableResolver() {
        return this;
    }

    @Override
    public BodyContent pushBody() {
        return invokingAspCtxt.pushBody();
    }

    @Override
    public AspWriter pushBody(Writer writer) {
        return invokingAspCtxt.pushBody(writer);
    }

    @Override
    public AspWriter popBody() {
        return invokingAspCtxt.popBody();
    }

    @Override
    @Deprecated
    public ExpressionEvaluator getExpressionEvaluator() {
        return invokingAspCtxt.getExpressionEvaluator();
    }

    @Override
    public void handlePageException(Exception ex) throws IOException,
            ServletException {
        // Should never be called since handleException() called with a
        // Throwable in the generated servlet.
        handlePageException((Throwable) ex);
    }

    @Override
    public void handlePageException(Throwable t) throws IOException,
            ServletException {
        invokingAspCtxt.handlePageException(t);
    }

    /**
     * VariableResolver interface
     */
    @Override
    @Deprecated
    public Object resolveVariable(String pName) throws ELException {
        ELContext ctx = this.getELContext();
        return ctx.getELResolver().getValue(ctx, null, pName);
    }

    /**
     * Synchronize variables at begin of tag file
     */
    public void syncBeginTagFile() {
        saveNestedVariables();
    }

    /**
     * Synchronize variables before fragment invocation
     */
    public void syncBeforeInvoke() {
        copyTagToPageScope(VariableInfo.NESTED);
        copyTagToPageScope(VariableInfo.AT_BEGIN);
    }

    /**
     * Synchronize variables at end of tag file
     */
    public void syncEndTagFile() {
        copyTagToPageScope(VariableInfo.AT_BEGIN);
        copyTagToPageScope(VariableInfo.AT_END);
        restoreNestedVariables();
    }

    /**
     * Copies the variables of the given scope from the virtual page scope of
     * this JSP context wrapper to the page scope of the invoking JSP context.
     * 
     * @param scope
     *            variable scope (one of NESTED, AT_BEGIN, or AT_END)
     */
    private void copyTagToPageScope(int scope) {
        Iterator<String> iter = null;

        switch (scope) {
        case VariableInfo.NESTED:
            if (nestedVars != null) {
                iter = nestedVars.iterator();
            }
            break;
        case VariableInfo.AT_BEGIN:
            if (atBeginVars != null) {
                iter = atBeginVars.iterator();
            }
            break;
        case VariableInfo.AT_END:
            if (atEndVars != null) {
                iter = atEndVars.iterator();
            }
            break;
        }

        while ((iter != null) && iter.hasNext()) {
            String varName = iter.next();
            Object obj = getAttribute(varName);
            varName = findAlias(varName);
            if (obj != null) {
                invokingAspCtxt.setAttribute(varName, obj);
            } else {
                invokingAspCtxt.removeAttribute(varName, PAGE_SCOPE);
            }
        }
    }

    /**
     * Saves the values of any NESTED variables that are present in the invoking
     * JSP context, so they can later be restored.
     */
    private void saveNestedVariables() {
        if (nestedVars != null) {
            Iterator<String> iter = nestedVars.iterator();
            while (iter.hasNext()) {
                String varName = iter.next();
                varName = findAlias(varName);
                Object obj = invokingAspCtxt.getAttribute(varName);
                if (obj != null) {
                    originalNestedVars.put(varName, obj);
                }
            }
        }
    }

    /**
     * Restores the values of any NESTED variables in the invoking JSP context.
     */
    private void restoreNestedVariables() {
        if (nestedVars != null) {
            Iterator<String> iter = nestedVars.iterator();
            while (iter.hasNext()) {
                String varName = iter.next();
                varName = findAlias(varName);
                Object obj = originalNestedVars.get(varName);
                if (obj != null) {
                    invokingAspCtxt.setAttribute(varName, obj);
                } else {
                    invokingAspCtxt.removeAttribute(varName, PAGE_SCOPE);
                }
            }
        }
    }

    /**
     * Checks to see if the given variable name is used as an alias, and if so,
     * returns the variable name for which it is used as an alias.
     * 
     * @param varName
     *            The variable name to check
     * @return The variable name for which varName is used as an alias, or
     *         varName if it is not being used as an alias
     */
    private String findAlias(String varName) {

        if (aliases == null)
            return varName;

        String alias = aliases.get(varName);
        if (alias == null) {
            return varName;
        }
        return alias;
    }

    //private ELContextImpl elContext;

    @Override
    public ELContext getELContext() {
        // instead decorate!!!
        
        return this.invokingAspCtxt.getELContext();
        
        /*
        if (this.elContext != null) {
            AspFactory aspFact = AspFactory.getDefaultFactory();
            ServletContext servletContext = this.getServletContext();
            AspApplicationContextImpl aspCtx = (AspApplicationContextImpl) aspFact
                    .getAspApplicationContext(servletContext);
            this.elContext = aspCtx.createELContext(this);
        }
        return this.elContext;
        */
    }
}

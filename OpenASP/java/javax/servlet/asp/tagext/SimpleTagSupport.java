/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package javax.servlet.asp.tagext;

import java.io.IOException;

import javax.servlet.asp.AspContext;
import javax.servlet.asp.AspException;

/**
 * A base class for defining tag handlers implementing SimpleTag.
 * <p>
 * The SimpleTagSupport class is a utility class intended to be used
 * as the base class for new simple tag handlers.  The SimpleTagSupport
 * class implements the SimpleTag interface and adds additional
 * convenience methods including getter methods for the properties in
 * SimpleTag.
 *
 * @since 2.0
 */
public class SimpleTagSupport implements SimpleTag {
    /** Reference to the enclosing tag. */
    private AspTag parentTag;
    
    /** The JSP context for the upcoming tag invocation. */
    private AspContext aspContext;
    
    /** The body of the tag. */
    private AspFragment aspBody;
    
    /**
     * Sole constructor. (For invocation by subclass constructors, 
     * typically implicit.)
     */
    public SimpleTagSupport() {
        // NOOP by default
    }
    
    /** 
     * Default processing of the tag does nothing.
     *
     * @throws AspException Subclasses can throw AspException to indicate
     *     an error occurred while processing this tag.
     * @throws javax.servlet.asp.SkipPageException If the page that
     *     (either directly or indirectly) invoked this tag is to
     *     cease evaluation.  A Simple Tag Handler generated from a 
     *     tag file must throw this exception if an invoked Classic 
     *     Tag Handler returned SKIP_PAGE or if an invoked Simple
     *     Tag Handler threw SkipPageException or if an invoked Asp Fragment
     *     threw a SkipPageException.
     * @throws IOException Subclasses can throw IOException if there was
     *     an error writing to the output stream
     * @see SimpleTag#doTag()
     */ 
    @Override
    public void doTag() throws AspException, IOException {
        // NOOP by default
    }
    
    /**
     * Sets the parent of this tag, for collaboration purposes.
     * <p>
     * The container invokes this method only if this tag invocation is
     * nested within another tag invocation.
     *
     * @param parent the tag that encloses this tag
     */
    @Override
    public void setParent( AspTag parent ) {
        this.parentTag = parent;
    }
    
    /**
     * Returns the parent of this tag, for collaboration purposes.
     *
     * @return the parent of this tag
     */ 
    @Override
    public AspTag getParent() {
        return this.parentTag;
    }
    
    /**
     * Stores the provided JSP context in the private aspContext field.
     * Subclasses can access the <code>AspContext</code> via 
     * <code>getAspContext()</code>.
     * 
     * @param pc the page context for this invocation
     * @see SimpleTag#setAspContext
     */
    @Override
    public void setAspContext( AspContext pc ) {
        this.aspContext = pc;
    }
    
    /**
     * Returns the page context passed in by the container via 
     * setAspContext.
     *
     * @return the page context for this invocation
     */
    protected AspContext getAspContext() {
        return this.aspContext;
    }
                
    /** 
     * Stores the provided AspFragment.
     *
     * @param aspBody The fragment encapsulating the body of this tag.
     *     If the action element is empty in the page, this method is 
     *     not called at all.
     * @see SimpleTag#setAspBody
     */ 
    @Override
    public void setAspBody( AspFragment aspBody ) {
        this.aspBody = aspBody;
    }
    
    /**
     * Returns the body passed in by the container via setAspBody.
     *
     * @return the fragment encapsulating the body of this tag, or
     *    null if the action element is empty in the page.
     */
    protected AspFragment getAspBody() {
        return this.aspBody;
    }

    /**
     * Find the instance of a given class type that is closest to a given
     * instance.
     * This method uses the getParent method from the Tag and/or SimpleTag
     * interfaces.  This method is used for coordination among 
     * cooperating tags.
     *
     * <p> For every instance of TagAdapter
     * encountered while traversing the ancestors, the tag handler returned by
     * <tt>TagAdapter.getAdaptee()</tt> - instead of the TagAdpater itself -
     * is compared to <tt>klass</tt>. If the tag handler matches, it - and
     * not its TagAdapter - is returned.
     *
     * <p>
     * The current version of the specification only provides one formal
     * way of indicating the observable type of a tag handler: its
     * tag handler implementation class, described in the tag-class
     * subelement of the tag element.  This is extended in an
     * informal manner by allowing the tag library author to
     * indicate in the description subelement an observable type.
     * The type should be a subtype of the tag handler implementation
     * class or void.
     * This additional constraint can be exploited by a
     * specialized container that knows about that specific tag library,
     * as in the case of the JSP standard tag library.
     *
     * <p>
     * When a tag library author provides information on the
     * observable type of a tag handler, client programmatic code
     * should adhere to that constraint.  Specifically, the Class
     * passed to findAncestorWithClass should be a subtype of the
     * observable type.
     * 
     *
     * @param from The instance from where to start looking.
     * @param klass The subclass of AspTag or interface to be matched
     * @return the nearest ancestor that implements the interface
     * or is an instance of the class specified
     */
    public static final AspTag findAncestorWithClass(
        AspTag from, Class<?> klass) 
    {
        boolean isInterface = false;

        if (from == null || klass == null
                || (!AspTag.class.isAssignableFrom(klass)
                    && !(isInterface = klass.isInterface()))) {
            return null;
        }

        for (;;) {
            AspTag parent = null;
            if( from instanceof SimpleTag ) {
                parent = ((SimpleTag)from).getParent();
            }
            else if( from instanceof Tag ) {
                parent = ((Tag)from).getParent();
            }
            if (parent == null) {
                return null;
            }

            if (parent instanceof TagAdapter) {
                parent = ((TagAdapter) parent).getAdaptee();
            }

            if ((isInterface && klass.isInstance(parent))
                    || klass.isAssignableFrom(parent.getClass())) {
                return parent;
            }

            from = parent;
        }
    }    
}

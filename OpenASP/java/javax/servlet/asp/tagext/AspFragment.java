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
import java.io.Writer;

import javax.servlet.asp.AspContext;
import javax.servlet.asp.AspException;

/**
 * Encapsulates a portion of JSP code in an object that 
 * can be invoked as many times as needed.  JSP Fragments are defined 
 * using JSP syntax as the body of a tag for an invocation to a SimpleTag 
 * handler, or as the body of a &lt;asp:attribute&gt; standard action
 * specifying the value of an attribute that is declared as a fragment,
 * or to be of type AspFragment in the TLD.
 * <p>
 * The definition of the JSP fragment must only contain template 
 * text and JSP action elements.  In other words, it must not contain
 * scriptlets or scriptlet expressions.  At translation time, the 
 * container generates an implementation of the AspFragment abstract class
 * capable of executing the defined fragment.
 * <p>
 * A tag handler can invoke the fragment zero or more times, or 
 * pass it along to other tags, before returning.  To communicate values
 * to/from a JSP fragment, tag handlers store/retrieve values in 
 * the AspContext associated with the fragment.
 * <p>
 * Note that tag library developers and page authors should not generate
 * AspFragment implementations manually.
 * <p>
 * <i>Implementation Note</i>: It is not necessary to generate a 
 * separate class for each fragment.  One possible implementation is 
 * to generate a single helper class for each page that implements 
 * AspFragment. Upon construction, a discriminator can be passed to 
 * select which fragment that instance will execute.
 *
 * @since 2.0
 */
public abstract class AspFragment {

    /**
     * Executes the fragment and directs all output to the given Writer,
     * or the AspWriter returned by the getOut() method of the AspContext
     * associated with the fragment if out is null.
     *
     * @param out The Writer to output the fragment to, or null if 
     *     output should be sent to AspContext.getOut().
     * @throws javax.servlet.asp.AspException Thrown if an error occurred
     *     while invoking this fragment.
     * @throws javax.servlet.asp.SkipPageException Thrown if the page
     *     that (either directly or indirectly) invoked the tag handler that
     *     invoked this fragment is to cease evaluation.  The container
     *     must throw this exception if a Classic Tag Handler returned
     *     Tag.SKIP_PAGE or if a Simple Tag Handler threw SkipPageException.
     * @throws java.io.IOException If there was an error writing to the 
     *     stream.
     */
    public abstract void invoke( Writer out )
        throws AspException, IOException;

    /**
     * Returns the AspContext that is bound to this AspFragment.
     *
     * @return The AspContext used by this fragment at invocation time.
     */
    public abstract AspContext getAspContext();

}

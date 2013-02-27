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

import javax.servlet.asp.AspContext;
import javax.servlet.asp.PageContext;
import javax.servlet.asp.tagext.AspFragment;
import javax.servlet.asp.tagext.AspTag;

/**
 * Helper class from which all Asp Fragment helper classes extend.
 * This class allows for the emulation of numerous fragments within
 * a single class, which in turn reduces the load on the class loader
 * since there are potentially many AspFragments in a single page.
 * <p>
 * The class also provides various utility methods for AspFragment
 * implementations.
 *
 * @author Mark Roth
 */
public abstract class AspFragmentHelper 
    extends AspFragment 
{
    
    protected int discriminator;
    protected AspContext aspContext;
    protected PageContext _aspx_page_context;
    protected AspTag parentTag;

    public AspFragmentHelper( int discriminator, AspContext aspContext, 
        AspTag parentTag ) 
    {
        this.discriminator = discriminator;
        this.aspContext = aspContext;
        this._aspx_page_context = null;
        if( aspContext instanceof PageContext ) {
            _aspx_page_context = (PageContext)aspContext;
        }
        this.parentTag = parentTag;
    }
    
    @Override
    public AspContext getAspContext() {
        return this.aspContext;
    }
    
    public AspTag getParentTag() {
        return this.parentTag;
    }
    
}

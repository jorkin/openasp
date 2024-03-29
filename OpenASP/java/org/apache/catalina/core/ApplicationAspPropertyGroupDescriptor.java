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
package org.apache.catalina.core;

import java.util.Collection;

import javax.servlet.descriptor.AspPropertyGroupDescriptor;

import org.apache.catalina.deploy.AspPropertyGroup;


public class ApplicationAspPropertyGroupDescriptor
        implements AspPropertyGroupDescriptor{

    /**
     * @deprecated  Will be made private in 8.0.x
     */
    @Deprecated
    AspPropertyGroup aspPropertyGroup;

    
    public ApplicationAspPropertyGroupDescriptor(
            AspPropertyGroup aspPropertyGroup) {
        this.aspPropertyGroup = aspPropertyGroup;
    }

    
    @Override
    public String getBuffer() {
        String result = null;
        
        if (aspPropertyGroup.getBuffer() != null) {
            result = aspPropertyGroup.getBuffer().toString();
        }
        
        return result;
    }

    
    @Override
    public String getDefaultContentType() {
        String result = null;
        
        if (aspPropertyGroup.getDefaultContentType() != null) {
            result = aspPropertyGroup.getDefaultContentType().toString();
        }
        
        return result;
    }

    
    @Override
    public String getDeferredSyntaxAllowedAsLiteral() {
        String result = null;
        
        if (aspPropertyGroup.getDeferredSyntax() != null) {
            result = aspPropertyGroup.getDeferredSyntax().toString();
        }
        
        return result;
    }

    
    @Override
    public String getElIgnored() {
        String result = null;
        
        if (aspPropertyGroup.getElIgnored() != null) {
            result = aspPropertyGroup.getElIgnored().toString();
        }
        
        return result;
    }

    
    @Override
    public String getErrorOnUndeclaredNamespace() {
        String result = null;
        
        if (aspPropertyGroup.getErrorOnUndeclaredNamespace() != null) {
            result =
                aspPropertyGroup.getErrorOnUndeclaredNamespace().toString();
        }
        
        return result;
    }

    
    @Override
    public Collection<String> getIncludeCodas() {
        return aspPropertyGroup.getIncludeCodas();
    }

    
    @Override
    public Collection<String> getIncludePreludes() {
        return aspPropertyGroup.getIncludePreludes();
    }

    
    @Override
    public String getIsXml() {
        String result = null;
        
        if (aspPropertyGroup.getIsXml() != null) {
            result = aspPropertyGroup.getIsXml().toString();
        }
        
        return result;
    }

    
    @Override
    public String getPageEncoding() {
        String result = null;
        
        if (aspPropertyGroup.getPageEncoding() != null) {
            result = aspPropertyGroup.getPageEncoding().toString();
        }
        
        return result;
    }

    
    @Override
    public String getScriptingInvalid() {
        String result = null;
        
        if (aspPropertyGroup.getScriptingInvalid() != null) {
            result = aspPropertyGroup.getScriptingInvalid().toString();
        }
        
        return result;
    }

    
    @Override
    public String getTrimDirectiveWhitespaces() {
        String result = null;
        
        if (aspPropertyGroup.getTrimWhitespace() != null) {
            result = aspPropertyGroup.getTrimWhitespace().toString();
        }
        
        return result;
    }

    
    @Override
    public Collection<String> getUrlPatterns() {
        return aspPropertyGroup.getUrlPatterns();
    }
}

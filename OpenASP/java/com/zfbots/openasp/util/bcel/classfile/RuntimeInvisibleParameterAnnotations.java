/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.zfbots.openasp.util.bcel.classfile;

import java.io.DataInputStream;
import java.io.IOException;

import com.zfbots.openasp.util.bcel.Constants;

/**
 * represents a parameter annotation that is represented in the class file
 * but is not provided to the JVM.
 * 
 * @version $Id: RuntimeInvisibleParameterAnnotations
 * @author  <A HREF="mailto:dbrosius@qis.net">D. Brosius</A>
 * @since 5.3
 */
public class RuntimeInvisibleParameterAnnotations extends ParameterAnnotations {

    private static final long serialVersionUID = -6819370369102352536L;


    /**
     * @param name_index Index pointing to the name <em>Code</em>
     * @param length Content length in bytes
     * @param file Input stream
     * @param constant_pool Array of constants
     */
    RuntimeInvisibleParameterAnnotations(int name_index, int length, DataInputStream file,
            ConstantPool constant_pool) throws IOException {
        super(Constants.ATTR_RUNTIMEIN_VISIBLE_PARAMETER_ANNOTATIONS, name_index, length, file,
                constant_pool);
    }


    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy( ConstantPool constant_pool ) {
        Annotations c = (Annotations) clone();
        return c;
    }
}

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
 * This class represents a constant pool reference to an interface method.
 *
 * @version $Id: ConstantInterfaceMethodref.java 1181133 2011-10-10 18:49:14Z markt $
 * @author  <A HREF="mailto:m.dahm@gmx.de">M. Dahm</A>
 */
public final class ConstantInterfaceMethodref extends ConstantCP {


    private static final long serialVersionUID = -8587605570227841891L;

    /**
     * Initialize instance from file data.
     *
     * @param file input stream
     * @throws IOException
     */
    ConstantInterfaceMethodref(DataInputStream file) throws IOException {
        super(Constants.CONSTANT_InterfaceMethodref, file);
    }
}

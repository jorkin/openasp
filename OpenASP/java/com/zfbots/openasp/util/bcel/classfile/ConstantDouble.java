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

import java.io.DataInput;
import java.io.IOException;

import com.zfbots.openasp.util.bcel.Constants;

/** 
 * This class is derived from the abstract 
 * <A HREF="org.apache.tomcat.util.bcel.classfile.Constant.html">Constant</A> class 
 * and represents a reference to a Double object.
 *
 * @version $Id: ConstantDouble.java 1377533 2012-08-26 22:22:59Z markt $
 * @author  <A HREF="mailto:m.dahm@gmx.de">M. Dahm</A>
 * @see     Constant
 */
public final class ConstantDouble extends Constant {

    private static final long serialVersionUID = 3450743772468544760L;
    private double bytes;


    /** 
     * @param bytes Data
     */
    public ConstantDouble(double bytes) {
        super(Constants.CONSTANT_Double);
        this.bytes = bytes;
    }


    /** 
     * Initialize instance from file data.
     *
     * @param file Input stream
     * @throws IOException
     */
    ConstantDouble(DataInput file) throws IOException {
        this(file.readDouble());
    }


    /**
     * @return data, i.e., 8 bytes.
     */
    public final double getBytes() {
        return bytes;
    }


    /**
     * @return String representation.
     */
    @Override
    public final String toString() {
        return super.toString() + "(bytes = " + bytes + ")";
    }
}

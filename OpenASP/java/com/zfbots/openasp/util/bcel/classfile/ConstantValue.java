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
 */
package com.zfbots.openasp.util.bcel.classfile;

import java.io.DataInput;
import java.io.IOException;

import com.zfbots.openasp.util.bcel.Constants;

/**
 * This class is derived from <em>Attribute</em> and represents a constant 
 * value, i.e., a default value for initializing a class field.
 * This class is instantiated by the <em>Attribute.readAttribute()</em> method.
 *
 * @version $Id: ConstantValue.java 1377533 2012-08-26 22:22:59Z markt $
 * @author  <A HREF="mailto:m.dahm@gmx.de">M. Dahm</A>
 * @see     Attribute
 */
public final class ConstantValue extends Attribute {

    private static final long serialVersionUID = -388222612752527969L;
    private int constantvalue_index;


    /**
     * Construct object from file stream.
     * @param name_index Name index in constant pool
     * @param length Content length in bytes
     * @param file Input stream
     * @param constant_pool Array of constants
     * @throws IOException
     */
    ConstantValue(int name_index, int length, DataInput file, ConstantPool constant_pool)
            throws IOException {
        this(name_index, length, file.readUnsignedShort(), constant_pool);
    }


    /**
     * @param name_index Name index in constant pool
     * @param length Content length in bytes
     * @param constantvalue_index Index in constant pool
     * @param constant_pool Array of constants
     */
    public ConstantValue(int name_index, int length, int constantvalue_index,
            ConstantPool constant_pool) {
        super(Constants.ATTR_CONSTANT_VALUE, name_index, length, constant_pool);
        this.constantvalue_index = constantvalue_index;
    }


    /**
     * @return String representation of constant value.
     */
    @Override
    public final String toString() {
        Constant c = constant_pool.getConstant(constantvalue_index);
        String buf;
        int i;
        // Print constant to string depending on its type
        switch (c.getTag()) {
            case Constants.CONSTANT_Long:
                buf = String.valueOf(((ConstantLong) c).getBytes());
                break;
            case Constants.CONSTANT_Float:
                buf = String.valueOf(((ConstantFloat) c).getBytes());
                break;
            case Constants.CONSTANT_Double:
                buf = String.valueOf(((ConstantDouble) c).getBytes());
                break;
            case Constants.CONSTANT_Integer:
                buf = String.valueOf(((ConstantInteger) c).getBytes());
                break;
            case Constants.CONSTANT_String:
                i = ((ConstantString) c).getStringIndex();
                c = constant_pool.getConstant(i, Constants.CONSTANT_Utf8);
                buf = "\"" + Utility.convertString(((ConstantUtf8) c).getBytes()) + "\"";
                break;
            default:
                throw new IllegalStateException("Type of ConstValue invalid: " + c);
        }
        return buf;
    }


    /**
     * @return deep copy of this attribute
     */
    @Override
    public Attribute copy( ConstantPool _constant_pool ) {
        ConstantValue c = (ConstantValue) clone();
        c.constant_pool = _constant_pool;
        return c;
    }
}

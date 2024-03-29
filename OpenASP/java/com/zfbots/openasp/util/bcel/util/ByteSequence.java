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
package com.zfbots.openasp.util.bcel.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * Utility class that implements a sequence of bytes which can be read
 * via the `readByte()' method. This is used to implement a wrapper for the 
 * Java byte code stream to gain some more readability.
 *
 * @version $Id: ByteSequence.java 1057670 2011-01-11 14:52:05Z markt $
 * @author  <A HREF="mailto:m.dahm@gmx.de">M. Dahm</A>
 */
public final class ByteSequence extends DataInputStream {

    private ByteArrayStream byte_stream;


    public ByteSequence(byte[] bytes) {
        super(new ByteArrayStream(bytes));
        byte_stream = (ByteArrayStream) in;
    }


    public final int getIndex() {
        return byte_stream.getPosition();
    }


    private static final class ByteArrayStream extends ByteArrayInputStream {

        ByteArrayStream(byte[] bytes) {
            super(bytes);
        }


        final int getPosition() {
            return pos;
        } // is protected in ByteArrayInputStream
    }
}

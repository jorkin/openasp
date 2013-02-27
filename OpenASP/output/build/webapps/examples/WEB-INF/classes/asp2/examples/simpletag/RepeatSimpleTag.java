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


package asp2.examples.simpletag;

import java.io.IOException;

import javax.servlet.asp.AspException;
import javax.servlet.asp.tagext.SimpleTagSupport;

/**
 * SimpleTag handler that accepts a num attribute and
 * invokes its body 'num' times.
 */
public class RepeatSimpleTag extends SimpleTagSupport {
    private int num;

    @Override
    public void doTag() throws AspException, IOException {
        for (int i=0; i<num; i++) {
            getAspContext().setAttribute("count", String.valueOf( i + 1 ) );
            getAspBody().invoke(null);
        }
    }

    public void setNum(int num) {
        this.num = num;
    }
}

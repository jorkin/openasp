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

package org.apache.jasper.compiler;

import org.apache.jasper.JasperException;
import org.xml.sax.Attributes;

class Dumper {

    static class DumpVisitor extends Node.Visitor {
        private int indent = 0;

        private String getAttributes(Attributes attrs) {
            if (attrs == null)
                return "";

            StringBuilder buf = new StringBuilder();
            for (int i=0; i < attrs.getLength(); i++) {
                buf.append(" " + attrs.getQName(i) + "=\""
                           + attrs.getValue(i) + "\"");
            }
            return buf.toString();
        }

        private void printString(String str) {
            printIndent();
            System.out.print(str);
        }

        private void printString(String prefix, String str, String suffix) {
            printIndent();
            if (str != null) {
                System.out.print(prefix + str + suffix);
            } else {
                System.out.print(prefix + suffix);
            }
        }

        private void printAttributes(String prefix, Attributes attrs,
                                     String suffix) {
            printString(prefix, getAttributes(attrs), suffix);
        }

        private void dumpBody(Node n) throws JasperException {
            Node.Nodes page = n.getBody();
            if (page != null) {
//                indent++;
                page.visit(this);
//                indent--;
            }
        }

        @Override
        public void visit(Node.PageDirective n) throws JasperException {
            printAttributes("<%@ page", n.getAttributes(), "%>");
        }

        @Override
        public void visit(Node.TaglibDirective n) throws JasperException {
            printAttributes("<%@ taglib", n.getAttributes(), "%>");
        }

        @Override
        public void visit(Node.IncludeDirective n) throws JasperException {
            printAttributes("<%@ include", n.getAttributes(), "%>");
            dumpBody(n);
        }

        @Override
        public void visit(Node.Comment n) throws JasperException {
            printString("<%--", n.getText(), "--%>");
        }

        @Override
        public void visit(Node.Declaration n) throws JasperException {
            printString("<%!", n.getText(), "%>");
        }

        @Override
        public void visit(Node.Expression n) throws JasperException {
            printString("<%=", n.getText(), "%>");
        }

        @Override
        public void visit(Node.Scriptlet n) throws JasperException {
            printString("<%", n.getText(), "%>");
        }

        @Override
        public void visit(Node.IncludeAction n) throws JasperException {
            printAttributes("<asp:include", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:include>");
        }

        @Override
        public void visit(Node.ForwardAction n) throws JasperException {
            printAttributes("<asp:forward", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:forward>");
        }

        @Override
        public void visit(Node.GetProperty n) throws JasperException {
            printAttributes("<asp:getProperty", n.getAttributes(), "/>");
        }

        @Override
        public void visit(Node.SetProperty n) throws JasperException {
            printAttributes("<asp:setProperty", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:setProperty>");
        }

        @Override
        public void visit(Node.UseBean n) throws JasperException {
            printAttributes("<asp:useBean", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:useBean>");
        }
        
        @Override
        public void visit(Node.PlugIn n) throws JasperException {
            printAttributes("<asp:plugin", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:plugin>");
        }
        
        @Override
        public void visit(Node.ParamsAction n) throws JasperException {
            printAttributes("<asp:params", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:params>");
        }
        
        @Override
        public void visit(Node.ParamAction n) throws JasperException {
            printAttributes("<asp:param", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:param>");
        }
        
        @Override
        public void visit(Node.NamedAttribute n) throws JasperException {
            printAttributes("<asp:attribute", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:attribute>");
        }

        @Override
        public void visit(Node.AspBody n) throws JasperException {
            printAttributes("<asp:body", n.getAttributes(), ">");
            dumpBody(n);
            printString("</asp:body>");
        }
        
        @Override
        public void visit(Node.ELExpression n) throws JasperException {
            printString( "${" + n.getText() + "}" );
        }

        @Override
        public void visit(Node.CustomTag n) throws JasperException {
            printAttributes("<" + n.getQName(), n.getAttributes(), ">");
            dumpBody(n);
            printString("</" + n.getQName() + ">");
        }

        @Override
        public void visit(Node.UninterpretedTag n) throws JasperException {
            String tag = n.getQName();
            printAttributes("<"+tag, n.getAttributes(), ">");
            dumpBody(n);
            printString("</" + tag + ">");
        }

        @Override
        public void visit(Node.TemplateText n) throws JasperException {
            printString(n.getText());
        }

        private void printIndent() {
            for (int i=0; i < indent; i++) {
                System.out.print("  ");
            }
        }
    }

    public static void dump(Node n) {
        try {
            n.accept(new DumpVisitor());        
        } catch (JasperException e) {
            e.printStackTrace();
        }
    }

    public static void dump(Node.Nodes page) {
        try {
            page.visit(new DumpVisitor());
        } catch (JasperException e) {
            e.printStackTrace();
        }
    }
}


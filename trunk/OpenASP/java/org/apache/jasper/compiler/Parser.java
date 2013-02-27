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

import java.io.CharArrayWriter;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.asp.tagext.TagAttributeInfo;
import javax.servlet.asp.tagext.TagFileInfo;
import javax.servlet.asp.tagext.TagInfo;
import javax.servlet.asp.tagext.TagLibraryInfo;

import org.apache.jasper.JasperException;
import org.apache.jasper.AspCompilationContext;
import org.apache.jasper.util.UniqueAttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class implements a parser for a JSP page (non-xml view). JSP page
 * grammar is included here for reference. The token '#' that appears in the
 * production indicates the current input token location in the production.
 * 
 * @author Kin-man Chung
 * @author Shawn Bayern
 * @author Mark Roth
 */

class Parser implements TagConstants {

    private ParserController parserController;

    private AspCompilationContext ctxt;

    private AspReader reader;

    private Mark start;

    private ErrorDispatcher err;

    private int scriptlessCount;

    private boolean isTagFile;

    private boolean directivesOnly;

    private JarResource jarResource;

    private PageInfo pageInfo;

    // Virtual body content types, to make parsing a little easier.
    // These are not accessible from outside the parser.
    private static final String JAVAX_BODY_CONTENT_PARAM =
        "JAVAX_BODY_CONTENT_PARAM";

    private static final String JAVAX_BODY_CONTENT_PLUGIN =
        "JAVAX_BODY_CONTENT_PLUGIN";

    private static final String JAVAX_BODY_CONTENT_TEMPLATE_TEXT =
        "JAVAX_BODY_CONTENT_TEMPLATE_TEXT";

    /* System property that controls if the strict white space rules are
     * applied.
     */ 
    private static final boolean STRICT_WHITESPACE = Boolean.valueOf(
            System.getProperty(
                    "org.apache.jasper.compiler.Parser.STRICT_WHITESPACE",
                    "true")).booleanValue();
    /**
     * The constructor
     */
    private Parser(ParserController pc, AspReader reader, boolean isTagFile,
            boolean directivesOnly, JarResource jarResource) {
        this.parserController = pc;
        this.ctxt = pc.getAspCompilationContext();
        this.pageInfo = pc.getCompiler().getPageInfo();
        this.err = pc.getCompiler().getErrorDispatcher();
        this.reader = reader;
        this.scriptlessCount = 0;
        this.isTagFile = isTagFile;
        this.directivesOnly = directivesOnly;
        this.jarResource = jarResource;
        start = reader.mark();
    }

    /**
     * The main entry for Parser
     * 
     * @param pc
     *            The ParseController, use for getting other objects in compiler
     *            and for parsing included pages
     * @param reader
     *            To read the page
     * @param parent
     *            The parent node to this page, null for top level page
     * @return list of nodes representing the parsed page
     */
    public static Node.Nodes parse(ParserController pc, AspReader reader,
            Node parent, boolean isTagFile, boolean directivesOnly,
            JarResource jarResource, String pageEnc, String aspConfigPageEnc,
            boolean isDefaultPageEncoding, boolean isBomPresent)
            throws JasperException {

        Parser parser = new Parser(pc, reader, isTagFile, directivesOnly,
                jarResource);

        Node.Root root = new Node.Root(reader.mark(), parent, false);
        root.setPageEncoding(pageEnc);
        root.setAspConfigPageEncoding(aspConfigPageEnc);
        root.setIsDefaultPageEncoding(isDefaultPageEncoding);
        root.setIsBomPresent(isBomPresent);

        // For the Top level page, add include-prelude and include-coda
        PageInfo pageInfo = pc.getCompiler().getPageInfo();
        if (parent == null && !isTagFile) {
            parser.addInclude(root, pageInfo.getIncludePrelude());
        }
        if (directivesOnly) {
            parser.parseFileDirectives(root);
        } else {
            while (reader.hasMoreInput()) {
                parser.parseElements(root);
            }
        }
        if (parent == null && !isTagFile) {
            parser.addInclude(root, pageInfo.getIncludeCoda());
        }

        Node.Nodes page = new Node.Nodes(root);
        return page;
    }

    /**
     * Attributes ::= (S Attribute)* S?
     */
    Attributes parseAttributes() throws JasperException {
        return parseAttributes(false);
    }
    Attributes parseAttributes(boolean pageDirective) throws JasperException {
        UniqueAttributesImpl attrs = new UniqueAttributesImpl(pageDirective);

        reader.skipSpaces();
        int ws = 1;

        try {
            while (parseAttribute(attrs)) {
                if (ws == 0 && STRICT_WHITESPACE) {
                    err.aspError(reader.mark(),
                            "asp.error.attribute.nowhitespace");
                }
                ws = reader.skipSpaces();
            }
        } catch (IllegalArgumentException iae) {
            // Duplicate attribute
            err.aspError(reader.mark(), "asp.error.attribute.duplicate");
        }

        return attrs;
    }

    /**
     * Parse Attributes for a reader, provided for external use
     */
    public static Attributes parseAttributes(ParserController pc,
            AspReader reader) throws JasperException {
        Parser tmpParser = new Parser(pc, reader, false, false, null);
        return tmpParser.parseAttributes(true);
    }

    /**
     * Attribute ::= Name S? Eq S? ( '"<%=' RTAttributeValueDouble | '"'
     * AttributeValueDouble | "'<%=" RTAttributeValueSingle | "'"
     * AttributeValueSingle } Note: JSP and XML spec does not allow while spaces
     * around Eq. It is added to be backward compatible with Tomcat, and with
     * other xml parsers.
     */
    private boolean parseAttribute(AttributesImpl attrs)
            throws JasperException {

        // Get the qualified name
        String qName = parseName();
        if (qName == null)
            return false;

        // Determine prefix and local name components
        String localName = qName;
        String uri = "";
        int index = qName.indexOf(':');
        if (index != -1) {
            String prefix = qName.substring(0, index);
            uri = pageInfo.getURI(prefix);
            if (uri == null) {
                err.aspError(reader.mark(),
                        "asp.error.attribute.invalidPrefix", prefix);
            }
            localName = qName.substring(index + 1);
        }

        reader.skipSpaces();
        if (!reader.matches("="))
            err.aspError(reader.mark(), "asp.error.attribute.noequal");

        reader.skipSpaces();
        char quote = (char) reader.nextChar();
        if (quote != '\'' && quote != '"')
            err.aspError(reader.mark(), "asp.error.attribute.noquote");

        String watchString = "";
        if (reader.matches("<%="))
            watchString = "%>";
        watchString = watchString + quote;

        String attrValue = parseAttributeValue(watchString);
        attrs.addAttribute(uri, localName, qName, "CDATA", attrValue);
        return true;
    }

    /**
     * Name ::= (Letter | '_' | ':') (Letter | Digit | '.' | '_' | '-' | ':')*
     */
    private String parseName() throws JasperException {
        char ch = (char) reader.peekChar();
        if (Character.isLetter(ch) || ch == '_' || ch == ':') {
            StringBuilder buf = new StringBuilder();
            buf.append(ch);
            reader.nextChar();
            ch = (char) reader.peekChar();
            while (Character.isLetter(ch) || Character.isDigit(ch) || ch == '.'
                    || ch == '_' || ch == '-' || ch == ':') {
                buf.append(ch);
                reader.nextChar();
                ch = (char) reader.peekChar();
            }
            return buf.toString();
        }
        return null;
    }

    /**
     * AttributeValueDouble ::= (QuotedChar - '"')* ('"' | <TRANSLATION_ERROR>)
     * RTAttributeValueDouble ::= ((QuotedChar - '"')* - ((QuotedChar-'"')'%>"')
     * ('%>"' | TRANSLATION_ERROR)
     */
    private String parseAttributeValue(String watch) throws JasperException {
        Mark start = reader.mark();
        Mark stop = reader.skipUntilIgnoreEsc(watch);
        if (stop == null) {
            err.aspError(start, "asp.error.attribute.unterminated", watch);
        }

        String ret = null;
        try {
            char quote = watch.charAt(watch.length() - 1);
            
            // If watch is longer than 1 character this is a scripting
            // expression and EL is always ignored
            boolean isElIgnored =
                pageInfo.isELIgnored() || watch.length() > 1;
            
            ret = AttributeParser.getUnquoted(reader.getText(start, stop),
                    quote, isElIgnored,
                    pageInfo.isDeferredSyntaxAllowedAsLiteral());
        } catch (IllegalArgumentException iae) {
            err.aspError(start, iae.getMessage());
        }
        if (watch.length() == 1) // quote
            return ret;

        // Put back delimiter '<%=' and '%>', since they are needed if the
        // attribute does not allow RTexpression.
        return "<%=" + ret + "%>";
    }

    private String parseScriptText(String tx) {
        CharArrayWriter cw = new CharArrayWriter();
        int size = tx.length();
        int i = 0;
        while (i < size) {
            char ch = tx.charAt(i);
            if (i + 2 < size && ch == '%' && tx.charAt(i + 1) == '\\'
                    && tx.charAt(i + 2) == '>') {
                cw.write('%');
                cw.write('>');
                i += 3;
            } else {
                cw.write(ch);
                ++i;
            }
        }
        cw.close();
        return cw.toString();
    }

    /*
     * Invokes parserController to parse the included page
     */
    private void processIncludeDirective(String file, Node parent)
            throws JasperException {
        if (file == null) {
            return;
        }

        try {
            parserController.parse(file, parent, jarResource);
        } catch (FileNotFoundException ex) {
            err.aspError(start, "asp.error.file.not.found", file);
        } catch (Exception ex) {
            err.aspError(start, ex.getMessage());
        }
    }

    /*
     * Parses a page directive with the following syntax: PageDirective ::= ( S
     * Attribute)*
     */
    private void parsePageDirective(Node parent) throws JasperException {
        Attributes attrs = parseAttributes(true);
        Node.PageDirective n = new Node.PageDirective(attrs, start, parent);

        /*
         * A page directive may contain multiple 'import' attributes, each of
         * which consists of a comma-separated list of package names. Store each
         * list with the node, where it is parsed.
         */
        for (int i = 0; i < attrs.getLength(); i++) {
            if ("import".equals(attrs.getQName(i))) {
                n.addImport(attrs.getValue(i));
            }
        }
    }

    /*
     * Parses an include directive with the following syntax: IncludeDirective
     * ::= ( S Attribute)*
     */
    private void parseIncludeDirective(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();

        // Included file expanded here
        Node includeNode = new Node.IncludeDirective(attrs, start, parent);
        processIncludeDirective(attrs.getValue("file"), includeNode);
    }

    /**
     * Add a list of files. This is used for implementing include-prelude and
     * include-coda of asp-config element in web.xml
     */
    private void addInclude(Node parent, List<String> files) throws JasperException {
        if (files != null) {
            Iterator<String> iter = files.iterator();
            while (iter.hasNext()) {
                String file = iter.next();
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "file", "file", "CDATA", file);

                // Create a dummy Include directive node
                Node includeNode = new Node.IncludeDirective(attrs, reader
                        .mark(), parent);
                processIncludeDirective(file, includeNode);
            }
        }
    }

    /*
     * Parses a taglib directive with the following syntax: Directive ::= ( S
     * Attribute)*
     */
    private void parseTaglibDirective(Node parent) throws JasperException {

        Attributes attrs = parseAttributes();
        String uri = attrs.getValue("uri");
        String prefix = attrs.getValue("prefix");
        if (prefix != null) {
            Mark prevMark = pageInfo.getNonCustomTagPrefix(prefix);
            if (prevMark != null) {
                err.aspError(reader.mark(), "asp.error.prefix.use_before_dcl",
                        prefix, prevMark.getFile(), ""
                                + prevMark.getLineNumber());
            }
            if (uri != null) {
                String uriPrev = pageInfo.getURI(prefix);
                if (uriPrev != null && !uriPrev.equals(uri)) {
                    err.aspError(reader.mark(), "asp.error.prefix.refined",
                            prefix, uri, uriPrev);
                }
                if (pageInfo.getTaglib(uri) == null) {
                    TagLibraryInfoImpl impl = null;
                    if (ctxt.getOptions().isCaching()) {
                        impl = (TagLibraryInfoImpl) ctxt.getOptions()
                                .getCache().get(uri);
                    }
                    if (impl == null) {
                        TldLocation location = ctxt.getTldLocation(uri);
                        impl = new TagLibraryInfoImpl(ctxt, parserController,
                                pageInfo, prefix, uri, location, err,
                                reader.mark());
                        if (ctxt.getOptions().isCaching()) {
                            ctxt.getOptions().getCache().put(uri, impl);
                        }
                    } else {
                        // Current compilation context needs location of cached
                        // tag files
                        for (TagFileInfo info : impl.getTagFiles()) {
                            ctxt.setTagFileJarResource(info.getPath(),
                                    ctxt.getTagFileJarResource());
                        }
                    }
                    pageInfo.addTaglib(uri, impl);
                }
                pageInfo.addPrefixMapping(prefix, uri);
            } else {
                String tagdir = attrs.getValue("tagdir");
                if (tagdir != null) {
                    String urnTagdir = URN_JSPTAGDIR + tagdir;
                    if (pageInfo.getTaglib(urnTagdir) == null) {
                        pageInfo.addTaglib(urnTagdir,
                                new ImplicitTagLibraryInfo(ctxt,
                                        parserController, pageInfo, prefix,
                                        tagdir, err));
                    }
                    pageInfo.addPrefixMapping(prefix, urnTagdir);
                }
            }
        }

        new Node.TaglibDirective(attrs, start, parent);
    }

    /*
     * Parses a directive with the following syntax: Directive ::= S? ( 'page'
     * PageDirective | 'include' IncludeDirective | 'taglib' TagLibDirective) S?
     * '%>'
     * 
     * TagDirective ::= S? ('tag' PageDirective | 'include' IncludeDirective |
     * 'taglib' TagLibDirective) | 'attribute AttributeDirective | 'variable
     * VariableDirective S? '%>'
     */
    private void parseDirective(Node parent) throws JasperException {
        reader.skipSpaces();

        String directive = null;
        if (reader.matches("page")) {
            directive = "&lt;%@ page";
            if (isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.istagfile",
                        directive);
            }
            parsePageDirective(parent);
        } else if (reader.matches("include")) {
            directive = "&lt;%@ include";
            parseIncludeDirective(parent);
        } else if (reader.matches("taglib")) {
            if (directivesOnly) {
                // No need to get the tagLibInfo objects. This alos suppresses
                // parsing of any tag files used in this tag file.
                return;
            }
            directive = "&lt;%@ taglib";
            parseTaglibDirective(parent);
        } else if (reader.matches("tag")) {
            directive = "&lt;%@ tag";
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.isnottagfile",
                        directive);
            }
            parseTagDirective(parent);
        } else if (reader.matches("attribute")) {
            directive = "&lt;%@ attribute";
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.isnottagfile",
                        directive);
            }
            parseAttributeDirective(parent);
        } else if (reader.matches("variable")) {
            directive = "&lt;%@ variable";
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.isnottagfile",
                        directive);
            }
            parseVariableDirective(parent);
        } else {
            err.aspError(reader.mark(), "asp.error.invalid.directive");
        }

        reader.skipSpaces();
        if (!reader.matches("%>")) {
            err.aspError(start, "asp.error.unterminated", directive);
        }
    }

    /*
     * Parses a directive with the following syntax:
     * 
     * XMLJSPDirectiveBody ::= S? ( ( 'page' PageDirectiveAttrList S? ( '/>' | (
     * '>' S? ETag ) ) | ( 'include' IncludeDirectiveAttrList S? ( '/>' | ( '>'
     * S? ETag ) ) | <TRANSLATION_ERROR>
     * 
     * XMLTagDefDirectiveBody ::= ( ( 'tag' TagDirectiveAttrList S? ( '/>' | (
     * '>' S? ETag ) ) | ( 'include' IncludeDirectiveAttrList S? ( '/>' | ( '>'
     * S? ETag ) ) | ( 'attribute' AttributeDirectiveAttrList S? ( '/>' | ( '>'
     * S? ETag ) ) | ( 'variable' VariableDirectiveAttrList S? ( '/>' | ( '>' S?
     * ETag ) ) ) | <TRANSLATION_ERROR>
     */
    private void parseXMLDirective(Node parent) throws JasperException {
        reader.skipSpaces();

        String eTag = null;
        if (reader.matches("page")) {
            eTag = "asp:directive.page";
            if (isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.istagfile",
                        "&lt;" + eTag);
            }
            parsePageDirective(parent);
        } else if (reader.matches("include")) {
            eTag = "asp:directive.include";
            parseIncludeDirective(parent);
        } else if (reader.matches("tag")) {
            eTag = "asp:directive.tag";
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.isnottagfile",
                        "&lt;" + eTag);
            }
            parseTagDirective(parent);
        } else if (reader.matches("attribute")) {
            eTag = "asp:directive.attribute";
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.isnottagfile",
                        "&lt;" + eTag);
            }
            parseAttributeDirective(parent);
        } else if (reader.matches("variable")) {
            eTag = "asp:directive.variable";
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.directive.isnottagfile",
                        "&lt;" + eTag);
            }
            parseVariableDirective(parent);
        } else {
            err.aspError(reader.mark(), "asp.error.invalid.directive");
        }

        reader.skipSpaces();
        if (reader.matches(">")) {
            reader.skipSpaces();
            if (!reader.matchesETag(eTag)) {
                err.aspError(start, "asp.error.unterminated", "&lt;" + eTag);
            }
        } else if (!reader.matches("/>")) {
            err.aspError(start, "asp.error.unterminated", "&lt;" + eTag);
        }
    }

    /*
     * Parses a tag directive with the following syntax: PageDirective ::= ( S
     * Attribute)*
     */
    private void parseTagDirective(Node parent) throws JasperException {
        Attributes attrs = parseAttributes(true);
        Node.TagDirective n = new Node.TagDirective(attrs, start, parent);

        /*
         * A page directive may contain multiple 'import' attributes, each of
         * which consists of a comma-separated list of package names. Store each
         * list with the node, where it is parsed.
         */
        for (int i = 0; i < attrs.getLength(); i++) {
            if ("import".equals(attrs.getQName(i))) {
                n.addImport(attrs.getValue(i));
            }
        }
    }

    /*
     * Parses a attribute directive with the following syntax:
     * AttributeDirective ::= ( S Attribute)*
     */
    private void parseAttributeDirective(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        new Node.AttributeDirective(attrs, start, parent);
    }

    /*
     * Parses a variable directive with the following syntax:
     * PageDirective ::= ( S Attribute)*
     */
    private void parseVariableDirective(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        new Node.VariableDirective(attrs, start, parent);
    }

    /*
     * JSPCommentBody ::= (Char* - (Char* '--%>')) '--%>'
     */
    private void parseComment(Node parent) throws JasperException {
        start = reader.mark();
        Mark stop = reader.skipUntil("--%>");
        if (stop == null) {
            err.aspError(start, "asp.error.unterminated", "&lt;%--");
        }

        new Node.Comment(reader.getText(start, stop), start, parent);
    }

    /*
     * DeclarationBody ::= (Char* - (char* '%>')) '%>'
     */
    private void parseDeclaration(Node parent) throws JasperException {
        start = reader.mark();
        Mark stop = reader.skipUntil("%>");
        if (stop == null) {
            err.aspError(start, "asp.error.unterminated", "&lt;%!");
        }

        new Node.Declaration(parseScriptText(reader.getText(start, stop)),
                start, parent);
    }

    /*
     * XMLDeclarationBody ::= ( S? '/>' ) | ( S? '>' (Char* - (char* '<'))
     * CDSect?)* ETag | <TRANSLATION_ERROR> CDSect ::= CDStart CData CDEnd
     * CDStart ::= '<![CDATA[' CData ::= (Char* - (Char* ']]>' Char*)) CDEnd
     * ::= ']]>'
     */
    private void parseXMLDeclaration(Node parent) throws JasperException {
        reader.skipSpaces();
        if (!reader.matches("/>")) {
            if (!reader.matches(">")) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:declaration&gt;");
            }
            Mark stop;
            String text;
            while (true) {
                start = reader.mark();
                stop = reader.skipUntil("<");
                if (stop == null) {
                    err.aspError(start, "asp.error.unterminated",
                            "&lt;asp:declaration&gt;");
                }
                text = parseScriptText(reader.getText(start, stop));
                new Node.Declaration(text, start, parent);
                if (reader.matches("![CDATA[")) {
                    start = reader.mark();
                    stop = reader.skipUntil("]]>");
                    if (stop == null) {
                        err.aspError(start, "asp.error.unterminated", "CDATA");
                    }
                    text = parseScriptText(reader.getText(start, stop));
                    new Node.Declaration(text, start, parent);
                } else {
                    break;
                }
            }

            if (!reader.matchesETagWithoutLessThan("asp:declaration")) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:declaration&gt;");
            }
        }
    }

    /*
     * ExpressionBody ::= (Char* - (char* '%>')) '%>'
     */
    private void parseExpression(Node parent) throws JasperException {
        start = reader.mark();
        Mark stop = reader.skipUntil("%>");
        if (stop == null) {
            err.aspError(start, "asp.error.unterminated", "&lt;%=");
        }

        new Node.Expression(parseScriptText(reader.getText(start, stop)),
                start, parent);
    }

    /*
     * XMLExpressionBody ::= ( S? '/>' ) | ( S? '>' (Char* - (char* '<'))
     * CDSect?)* ETag ) | <TRANSLATION_ERROR>
     */
    private void parseXMLExpression(Node parent) throws JasperException {
        reader.skipSpaces();
        if (!reader.matches("/>")) {
            if (!reader.matches(">")) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:expression&gt;");
            }
            Mark stop;
            String text;
            while (true) {
                start = reader.mark();
                stop = reader.skipUntil("<");
                if (stop == null) {
                    err.aspError(start, "asp.error.unterminated",
                            "&lt;asp:expression&gt;");
                }
                text = parseScriptText(reader.getText(start, stop));
                new Node.Expression(text, start, parent);
                if (reader.matches("![CDATA[")) {
                    start = reader.mark();
                    stop = reader.skipUntil("]]>");
                    if (stop == null) {
                        err.aspError(start, "asp.error.unterminated", "CDATA");
                    }
                    text = parseScriptText(reader.getText(start, stop));
                    new Node.Expression(text, start, parent);
                } else {
                    break;
                }
            }
            if (!reader.matchesETagWithoutLessThan("asp:expression")) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:expression&gt;");
            }
        }
    }

    /*
     * ELExpressionBody (following "${" to first unquoted "}") // XXX add formal
     * production and confirm implementation against it, // once it's decided
     */
    private void parseELExpression(Node parent, char type)
            throws JasperException {
        start = reader.mark();
        Mark last = null;
        boolean singleQuoted = false, doubleQuoted = false;
        int currentChar;
        do {
            // XXX could move this logic to AspReader
            last = reader.mark(); // XXX somewhat wasteful
            currentChar = reader.nextChar();
            if (currentChar == '\\' && (singleQuoted || doubleQuoted)) {
                // skip character following '\' within quotes
                reader.nextChar();
                currentChar = reader.nextChar();
            }
            if (currentChar == -1)
                err.aspError(start, "asp.error.unterminated", type + "{");
            if (currentChar == '"' && !singleQuoted)
                doubleQuoted = !doubleQuoted;
            if (currentChar == '\'' && !doubleQuoted)
                singleQuoted = !singleQuoted;
        } while (currentChar != '}' || (singleQuoted || doubleQuoted));

        new Node.ELExpression(type, reader.getText(start, last), start, parent);
    }

    /*
     * ScriptletBody ::= (Char* - (char* '%>')) '%>'
     */
    private void parseScriptlet(Node parent) throws JasperException {
        start = reader.mark();
        Mark stop = reader.skipUntil("%>");
        if (stop == null) {
            err.aspError(start, "asp.error.unterminated", "&lt;%");
        }

        new Node.Scriptlet(parseScriptText(reader.getText(start, stop)), start,
                parent);
    }

    /*
     * XMLScriptletBody ::= ( S? '/>' ) | ( S? '>' (Char* - (char* '<'))
     * CDSect?)* ETag ) | <TRANSLATION_ERROR>
     */
    private void parseXMLScriptlet(Node parent) throws JasperException {
        reader.skipSpaces();
        if (!reader.matches("/>")) {
            if (!reader.matches(">")) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:scriptlet&gt;");
            }
            Mark stop;
            String text;
            while (true) {
                start = reader.mark();
                stop = reader.skipUntil("<");
                if (stop == null) {
                    err.aspError(start, "asp.error.unterminated",
                            "&lt;asp:scriptlet&gt;");
                }
                text = parseScriptText(reader.getText(start, stop));
                new Node.Scriptlet(text, start, parent);
                if (reader.matches("![CDATA[")) {
                    start = reader.mark();
                    stop = reader.skipUntil("]]>");
                    if (stop == null) {
                        err.aspError(start, "asp.error.unterminated", "CDATA");
                    }
                    text = parseScriptText(reader.getText(start, stop));
                    new Node.Scriptlet(text, start, parent);
                } else {
                    break;
                }
            }

            if (!reader.matchesETagWithoutLessThan("asp:scriptlet")) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:scriptlet&gt;");
            }
        }
    }

    /**
     * Param ::= '<asp:param' S Attributes S? EmptyBody S?
     */
    private void parseParam(Node parent) throws JasperException {
        if (!reader.matches("<asp:param")) {
            err.aspError(reader.mark(), "asp.error.paramexpected");
        }
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node paramActionNode = new Node.ParamAction(attrs, start, parent);

        parseEmptyBody(paramActionNode, "asp:param");

        reader.skipSpaces();
    }

    /*
     * For Include: StdActionContent ::= Attributes ParamBody
     * 
     * ParamBody ::= EmptyBody | ( '>' S? ( '<asp:attribute' NamedAttributes )? '<asp:body'
     * (AspBodyParam | <TRANSLATION_ERROR> ) S? ETag ) | ( '>' S? Param* ETag )
     * 
     * EmptyBody ::= '/>' | ( '>' ETag ) | ( '>' S? '<asp:attribute'
     * NamedAttributes ETag )
     * 
     * AspBodyParam ::= S? '>' Param* '</asp:body>'
     */
    private void parseInclude(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node includeNode = new Node.IncludeAction(attrs, start, parent);

        parseOptionalBody(includeNode, "asp:include", JAVAX_BODY_CONTENT_PARAM);
    }

    /*
     * For Forward: StdActionContent ::= Attributes ParamBody
     */
    private void parseForward(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node forwardNode = new Node.ForwardAction(attrs, start, parent);

        parseOptionalBody(forwardNode, "asp:forward", JAVAX_BODY_CONTENT_PARAM);
    }

    private void parseInvoke(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node invokeNode = new Node.InvokeAction(attrs, start, parent);

        parseEmptyBody(invokeNode, "asp:invoke");
    }

    private void parseDoBody(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node doBodyNode = new Node.DoBodyAction(attrs, start, parent);

        parseEmptyBody(doBodyNode, "asp:doBody");
    }

    private void parseElement(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node elementNode = new Node.AspElement(attrs, start, parent);

        parseOptionalBody(elementNode, "asp:element", TagInfo.BODY_CONTENT_JSP);
    }

    /*
     * For GetProperty: StdActionContent ::= Attributes EmptyBody
     */
    private void parseGetProperty(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node getPropertyNode = new Node.GetProperty(attrs, start, parent);

        parseOptionalBody(getPropertyNode, "asp:getProperty",
                TagInfo.BODY_CONTENT_EMPTY);
    }

    /*
     * For SetProperty: StdActionContent ::= Attributes EmptyBody
     */
    private void parseSetProperty(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node setPropertyNode = new Node.SetProperty(attrs, start, parent);

        parseOptionalBody(setPropertyNode, "asp:setProperty",
                TagInfo.BODY_CONTENT_EMPTY);
    }

    /*
     * EmptyBody ::= '/>' | ( '>' ETag ) | ( '>' S? '<asp:attribute'
     * NamedAttributes ETag )
     */
    private void parseEmptyBody(Node parent, String tag) throws JasperException {
        if (reader.matches("/>")) {
            // Done
        } else if (reader.matches(">")) {
            if (reader.matchesETag(tag)) {
                // Done
            } else if (reader.matchesOptionalSpacesFollowedBy("<asp:attribute")) {
                // Parse the one or more named attribute nodes
                parseNamedAttributes(parent);
                if (!reader.matchesETag(tag)) {
                    // Body not allowed
                    err.aspError(reader.mark(),
                            "asp.error.aspbody.emptybody.only", "&lt;" + tag);
                }
            } else {
                err.aspError(reader.mark(), "asp.error.aspbody.emptybody.only",
                        "&lt;" + tag);
            }
        } else {
            err.aspError(reader.mark(), "asp.error.unterminated", "&lt;" + tag);
        }
    }

    /*
     * For UseBean: StdActionContent ::= Attributes OptionalBody
     */
    private void parseUseBean(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node useBeanNode = new Node.UseBean(attrs, start, parent);

        parseOptionalBody(useBeanNode, "asp:useBean", TagInfo.BODY_CONTENT_JSP);
    }

    /*
     * Parses OptionalBody, but also reused to parse bodies for plugin and param
     * since the syntax is identical (the only thing that differs substantially
     * is how to process the body, and thus we accept the body type as a
     * parameter).
     * 
     * OptionalBody ::= EmptyBody | ActionBody
     * 
     * ScriptlessOptionalBody ::= EmptyBody | ScriptlessActionBody
     * 
     * TagDependentOptionalBody ::= EmptyBody | TagDependentActionBody
     * 
     * EmptyBody ::= '/>' | ( '>' ETag ) | ( '>' S? '<asp:attribute'
     * NamedAttributes ETag )
     * 
     * ActionBody ::= AspAttributeAndBody | ( '>' Body ETag )
     * 
     * ScriptlessActionBody ::= AspAttributeAndBody | ( '>' ScriptlessBody ETag )
     * 
     * TagDependentActionBody ::= AspAttributeAndBody | ( '>' TagDependentBody
     * ETag )
     * 
     */
    private void parseOptionalBody(Node parent, String tag, String bodyType)
            throws JasperException {
        if (reader.matches("/>")) {
            // EmptyBody
            return;
        }

        if (!reader.matches(">")) {
            err.aspError(reader.mark(), "asp.error.unterminated", "&lt;" + tag);
        }

        if (reader.matchesETag(tag)) {
            // EmptyBody
            return;
        }

        if (!parseAspAttributeAndBody(parent, tag, bodyType)) {
            // Must be ( '>' # Body ETag )
            parseBody(parent, tag, bodyType);
        }
    }

    /**
     * Attempts to parse 'AspAttributeAndBody' production. Returns true if it
     * matched, or false if not. Assumes EmptyBody is okay as well.
     * 
     * AspAttributeAndBody ::= ( '>' # S? ( '<asp:attribute' NamedAttributes )? '<asp:body' (
     * AspBodyBody | <TRANSLATION_ERROR> ) S? ETag )
     */
    private boolean parseAspAttributeAndBody(Node parent, String tag,
            String bodyType) throws JasperException {
        boolean result = false;

        if (reader.matchesOptionalSpacesFollowedBy("<asp:attribute")) {
            // May be an EmptyBody, depending on whether
            // There's a "<asp:body" before the ETag

            // First, parse <asp:attribute> elements:
            parseNamedAttributes(parent);

            result = true;
        }

        if (reader.matchesOptionalSpacesFollowedBy("<asp:body")) {
            // ActionBody
            parseAspBody(parent, bodyType);
            reader.skipSpaces();
            if (!reader.matchesETag(tag)) {
                err.aspError(reader.mark(), "asp.error.unterminated", "&lt;"
                        + tag);
            }

            result = true;
        } else if (result && !reader.matchesETag(tag)) {
            // If we have <asp:attribute> but something other than
            // <asp:body> or the end tag, translation error.
            err.aspError(reader.mark(), "asp.error.aspbody.required", "&lt;"
                    + tag);
        }

        return result;
    }

    /*
     * Params ::= `>' S? ( ( `<asp:body>' ( ( S? Param+ S? `</asp:body>' ) |
     * <TRANSLATION_ERROR> ) ) | Param+ ) '</asp:params>'
     */
    private void parseAspParams(Node parent) throws JasperException {
        Node aspParamsNode = new Node.ParamsAction(start, parent);
        parseOptionalBody(aspParamsNode, "asp:params", JAVAX_BODY_CONTENT_PARAM);
    }

    /*
     * Fallback ::= '/>' | ( `>' S? `<asp:body>' ( ( S? ( Char* - ( Char* `</asp:body>' ) ) `</asp:body>'
     * S? ) | <TRANSLATION_ERROR> ) `</asp:fallback>' ) | ( '>' ( Char* - (
     * Char* '</asp:fallback>' ) ) '</asp:fallback>' )
     */
    private void parseFallBack(Node parent) throws JasperException {
        Node fallBackNode = new Node.FallBackAction(start, parent);
        parseOptionalBody(fallBackNode, "asp:fallback",
                JAVAX_BODY_CONTENT_TEMPLATE_TEXT);
    }

    /*
     * For Plugin: StdActionContent ::= Attributes PluginBody
     * 
     * PluginBody ::= EmptyBody | ( '>' S? ( '<asp:attribute' NamedAttributes )? '<asp:body' (
     * AspBodyPluginTags | <TRANSLATION_ERROR> ) S? ETag ) | ( '>' S? PluginTags
     * ETag )
     * 
     * EmptyBody ::= '/>' | ( '>' ETag ) | ( '>' S? '<asp:attribute'
     * NamedAttributes ETag )
     * 
     */
    private void parsePlugin(Node parent) throws JasperException {
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        Node pluginNode = new Node.PlugIn(attrs, start, parent);

        parseOptionalBody(pluginNode, "asp:plugin", JAVAX_BODY_CONTENT_PLUGIN);
    }

    /*
     * PluginTags ::= ( '<asp:params' Params S? )? ( '<asp:fallback' Fallback?
     * S? )?
     */
    private void parsePluginTags(Node parent) throws JasperException {
        reader.skipSpaces();

        if (reader.matches("<asp:params")) {
            parseAspParams(parent);
            reader.skipSpaces();
        }

        if (reader.matches("<asp:fallback")) {
            parseFallBack(parent);
            reader.skipSpaces();
        }
    }

    /*
     * StandardAction ::= 'include' StdActionContent | 'forward'
     * StdActionContent | 'invoke' StdActionContent | 'doBody' StdActionContent |
     * 'getProperty' StdActionContent | 'setProperty' StdActionContent |
     * 'useBean' StdActionContent | 'plugin' StdActionContent | 'element'
     * StdActionContent
     */
    private void parseStandardAction(Node parent) throws JasperException {
        Mark start = reader.mark();

        if (reader.matches(INCLUDE_ACTION)) {
            parseInclude(parent);
        } else if (reader.matches(FORWARD_ACTION)) {
            parseForward(parent);
        } else if (reader.matches(INVOKE_ACTION)) {
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.action.isnottagfile",
                        "&lt;asp:invoke");
            }
            parseInvoke(parent);
        } else if (reader.matches(DOBODY_ACTION)) {
            if (!isTagFile) {
                err.aspError(reader.mark(), "asp.error.action.isnottagfile",
                        "&lt;asp:doBody");
            }
            parseDoBody(parent);
        } else if (reader.matches(GET_PROPERTY_ACTION)) {
            parseGetProperty(parent);
        } else if (reader.matches(SET_PROPERTY_ACTION)) {
            parseSetProperty(parent);
        } else if (reader.matches(USE_BEAN_ACTION)) {
            parseUseBean(parent);
        } else if (reader.matches(PLUGIN_ACTION)) {
            parsePlugin(parent);
        } else if (reader.matches(ELEMENT_ACTION)) {
            parseElement(parent);
        } else if (reader.matches(ATTRIBUTE_ACTION)) {
            err.aspError(start, "asp.error.namedAttribute.invalidUse");
        } else if (reader.matches(BODY_ACTION)) {
            err.aspError(start, "asp.error.aspbody.invalidUse");
        } else if (reader.matches(FALLBACK_ACTION)) {
            err.aspError(start, "asp.error.fallback.invalidUse");
        } else if (reader.matches(PARAMS_ACTION)) {
            err.aspError(start, "asp.error.params.invalidUse");
        } else if (reader.matches(PARAM_ACTION)) {
            err.aspError(start, "asp.error.param.invalidUse");
        } else if (reader.matches(OUTPUT_ACTION)) {
            err.aspError(start, "asp.error.aspoutput.invalidUse");
        } else {
            err.aspError(start, "asp.error.badStandardAction");
        }
    }

    /*
     * # '<' CustomAction CustomActionBody
     * 
     * CustomAction ::= TagPrefix ':' CustomActionName
     * 
     * TagPrefix ::= Name
     * 
     * CustomActionName ::= Name
     * 
     * CustomActionBody ::= ( Attributes CustomActionEnd ) | <TRANSLATION_ERROR>
     * 
     * Attributes ::= ( S Attribute )* S?
     * 
     * CustomActionEnd ::= CustomActionTagDependent | CustomActionJSPContent |
     * CustomActionScriptlessContent
     * 
     * CustomActionTagDependent ::= TagDependentOptionalBody
     * 
     * CustomActionJSPContent ::= OptionalBody
     * 
     * CustomActionScriptlessContent ::= ScriptlessOptionalBody
     */
    private boolean parseCustomTag(Node parent) throws JasperException {

        if (reader.peekChar() != '<') {
            return false;
        }

        // Parse 'CustomAction' production (tag prefix and custom action name)
        reader.nextChar(); // skip '<'
        String tagName = reader.parseToken(false);
        int i = tagName.indexOf(':');
        if (i == -1) {
            reader.reset(start);
            return false;
        }

        String prefix = tagName.substring(0, i);
        String shortTagName = tagName.substring(i + 1);

        // Check if this is a user-defined tag.
        String uri = pageInfo.getURI(prefix);
        if (uri == null) {
            if (pageInfo.isErrorOnUndeclaredNamespace()) {
                err.aspError(start, "asp.error.undeclared_namespace", prefix);
            } else {
                reader.reset(start);
                // Remember the prefix for later error checking
                pageInfo.putNonCustomTagPrefix(prefix, reader.mark());
                return false;
            }
        }

        TagLibraryInfo tagLibInfo = pageInfo.getTaglib(uri);
        TagInfo tagInfo = tagLibInfo.getTag(shortTagName);
        TagFileInfo tagFileInfo = tagLibInfo.getTagFile(shortTagName);
        if (tagInfo == null && tagFileInfo == null) {
            err.aspError(start, "asp.error.bad_tag", shortTagName, prefix);
        }
        Class<?> tagHandlerClass = null;
        if (tagInfo != null) {
            // Must be a classic tag, load it here.
            // tag files will be loaded later, in TagFileProcessor
            String handlerClassName = tagInfo.getTagClassName();
            try {
                tagHandlerClass = ctxt.getClassLoader().loadClass(
                        handlerClassName);
            } catch (Exception e) {
                err.aspError(start, "asp.error.loadclass.taghandler",
                        handlerClassName, tagName);
            }
        }

        // Parse 'CustomActionBody' production:
        // At this point we are committed - if anything fails, we produce
        // a translation error.

        // Parse 'Attributes' production:
        Attributes attrs = parseAttributes();
        reader.skipSpaces();

        // Parse 'CustomActionEnd' production:
        if (reader.matches("/>")) {
            if (tagInfo != null) {
                new Node.CustomTag(tagName, prefix, shortTagName, uri, attrs,
                        start, parent, tagInfo, tagHandlerClass);
            } else {
                new Node.CustomTag(tagName, prefix, shortTagName, uri, attrs,
                        start, parent, tagFileInfo);
            }
            return true;
        }

        // Now we parse one of 'CustomActionTagDependent',
        // 'CustomActionJSPContent', or 'CustomActionScriptlessContent'.
        // depending on body-content in TLD.

        // Looking for a body, it still can be empty; but if there is a
        // a tag body, its syntax would be dependent on the type of
        // body content declared in the TLD.
        String bc;
        if (tagInfo != null) {
            bc = tagInfo.getBodyContent();
        } else {
            bc = tagFileInfo.getTagInfo().getBodyContent();
        }

        Node tagNode = null;
        if (tagInfo != null) {
            tagNode = new Node.CustomTag(tagName, prefix, shortTagName, uri,
                    attrs, start, parent, tagInfo, tagHandlerClass);
        } else {
            tagNode = new Node.CustomTag(tagName, prefix, shortTagName, uri,
                    attrs, start, parent, tagFileInfo);
        }

        parseOptionalBody(tagNode, tagName, bc);

        return true;
    }

    /*
     * Parse for a template text string until '<' or "${" or "#{" is encountered,
     * recognizing escape sequences "<\%", "\$", and "\#".
     */
    private void parseTemplateText(Node parent) throws JasperException {

        if (!reader.hasMoreInput())
            return;

        CharArrayWriter ttext = new CharArrayWriter();
        // Output the first character
        int ch = reader.nextChar();
        if (ch == '\\') {
            reader.pushChar();
        } else {
            ttext.write(ch);
        }

        while (reader.hasMoreInput()) {
            int prev = ch;
            ch = reader.nextChar();
            if (ch == '<') {
                reader.pushChar();
                break;
            } else if ((ch == '$' || ch == '#') && !pageInfo.isELIgnored()) {
                if (!reader.hasMoreInput()) {
                    ttext.write(ch);
                    break;
                }
                if (reader.nextChar() == '{') {
                    reader.pushChar();
                    reader.pushChar();
                    break;
                }
                ttext.write(ch);
                reader.pushChar();
                continue;
            } else if (ch == '\\') {
                if (!reader.hasMoreInput()) {
                    ttext.write('\\');
                    break;
                }
                char next = (char) reader.peekChar();
                // Looking for \% or \$ or \#
                if ((prev == '<' && next == '%') ||
                        ((next == '$' || next == '#') &&
                                !pageInfo.isELIgnored())) {
                    ch = reader.nextChar();
                }
            }
            ttext.write(ch);
        }
        new Node.TemplateText(ttext.toString(), start, parent);
    }

    /*
     * XMLTemplateText ::= ( S? '/>' ) | ( S? '>' ( ( Char* - ( Char* ( '<' |
     * '${' ) ) ) ( '${' ELExpressionBody )? CDSect? )* ETag ) |
     * <TRANSLATION_ERROR>
     */
    private void parseXMLTemplateText(Node parent) throws JasperException {
        reader.skipSpaces();
        if (!reader.matches("/>")) {
            if (!reader.matches(">")) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:text&gt;");
            }
            CharArrayWriter ttext = new CharArrayWriter();
            while (reader.hasMoreInput()) {
                int ch = reader.nextChar();
                if (ch == '<') {
                    // Check for <![CDATA[
                    if (!reader.matches("![CDATA[")) {
                        break;
                    }
                    start = reader.mark();
                    Mark stop = reader.skipUntil("]]>");
                    if (stop == null) {
                        err.aspError(start, "asp.error.unterminated", "CDATA");
                    }
                    String text = reader.getText(start, stop);
                    ttext.write(text, 0, text.length());
                } else if (ch == '\\') {
                    if (!reader.hasMoreInput()) {
                        ttext.write('\\');
                        break;
                    }
                    ch = reader.nextChar();
                    if (ch != '$' && ch != '#') {
                        ttext.write('\\');
                    }
                    ttext.write(ch);
                } else if (ch == '$' || ch == '#') {
                    if (!reader.hasMoreInput()) {
                        ttext.write(ch);
                        break;
                    }
                    if (reader.nextChar() != '{') {
                        ttext.write(ch);
                        reader.pushChar();
                        continue;
                    }
                    // Create a template text node
                    new Node.TemplateText(ttext.toString(), start, parent);

                    // Mark and parse the EL expression and create its node:
                    start = reader.mark();
                    parseELExpression(parent, (char) ch);

                    start = reader.mark();
                    ttext = new CharArrayWriter();
                } else {
                    ttext.write(ch);
                }
            }

            new Node.TemplateText(ttext.toString(), start, parent);

            if (!reader.hasMoreInput()) {
                err.aspError(start, "asp.error.unterminated",
                        "&lt;asp:text&gt;");
            } else if (!reader.matchesETagWithoutLessThan("asp:text")) {
                err.aspError(start, "asp.error.asptext.badcontent");
            }
        }
    }

    /*
     * AllBody ::= ( '<%--' JSPCommentBody ) | ( '<%@' DirectiveBody ) | ( '<asp:directive.'
     * XMLDirectiveBody ) | ( '<%!' DeclarationBody ) | ( '<asp:declaration'
     * XMLDeclarationBody ) | ( '<%=' ExpressionBody ) | ( '<asp:expression'
     * XMLExpressionBody ) | ( '${' ELExpressionBody ) | ( '<%' ScriptletBody ) | ( '<asp:scriptlet'
     * XMLScriptletBody ) | ( '<asp:text' XMLTemplateText ) | ( '<asp:'
     * StandardAction ) | ( '<' CustomAction CustomActionBody ) | TemplateText
     */
    private void parseElements(Node parent) throws JasperException {
        if (scriptlessCount > 0) {
            // vc: ScriptlessBody
            // We must follow the ScriptlessBody production if one of
            // our parents is ScriptlessBody.
            parseElementsScriptless(parent);
            return;
        }

        start = reader.mark();
        if (reader.matches("<%--")) {
            parseComment(parent);
        } else if (reader.matches("<%@")) {
            parseDirective(parent);
        } else if (reader.matches("<asp:directive.")) {
            parseXMLDirective(parent);
        } else if (reader.matches("<%!")) {
            parseDeclaration(parent);
        } else if (reader.matches("<asp:declaration")) {
            parseXMLDeclaration(parent);
        } else if (reader.matches("<%=")) {
            parseExpression(parent);
        } else if (reader.matches("<asp:expression")) {
            parseXMLExpression(parent);
        } else if (reader.matches("<%")) {
            parseScriptlet(parent);
        } else if (reader.matches("<asp:scriptlet")) {
            parseXMLScriptlet(parent);
        } else if (reader.matches("<asp:text")) {
            parseXMLTemplateText(parent);
        } else if (!pageInfo.isELIgnored() && reader.matches("${")) {
            parseELExpression(parent, '$');
        } else if (!pageInfo.isELIgnored()
                && !pageInfo.isDeferredSyntaxAllowedAsLiteral()
                && reader.matches("#{")) {
            parseELExpression(parent, '#');
        } else if (reader.matches("<asp:")) {
            parseStandardAction(parent);
        } else if (!parseCustomTag(parent)) {
            checkUnbalancedEndTag();
            parseTemplateText(parent);
        }
    }

    /*
     * ScriptlessBody ::= ( '<%--' JSPCommentBody ) | ( '<%@' DirectiveBody ) | ( '<asp:directive.'
     * XMLDirectiveBody ) | ( '<%!' <TRANSLATION_ERROR> ) | ( '<asp:declaration'
     * <TRANSLATION_ERROR> ) | ( '<%=' <TRANSLATION_ERROR> ) | ( '<asp:expression'
     * <TRANSLATION_ERROR> ) | ( '<%' <TRANSLATION_ERROR> ) | ( '<asp:scriptlet'
     * <TRANSLATION_ERROR> ) | ( '<asp:text' XMLTemplateText ) | ( '${'
     * ELExpressionBody ) | ( '<asp:' StandardAction ) | ( '<' CustomAction
     * CustomActionBody ) | TemplateText
     */
    private void parseElementsScriptless(Node parent) throws JasperException {
        // Keep track of how many scriptless nodes we've encountered
        // so we know whether our child nodes are forced scriptless
        scriptlessCount++;

        start = reader.mark();
        if (reader.matches("<%--")) {
            parseComment(parent);
        } else if (reader.matches("<%@")) {
            parseDirective(parent);
        } else if (reader.matches("<asp:directive.")) {
            parseXMLDirective(parent);
        } else if (reader.matches("<%!")) {
            err.aspError(reader.mark(), "asp.error.no.scriptlets");
        } else if (reader.matches("<asp:declaration")) {
            err.aspError(reader.mark(), "asp.error.no.scriptlets");
        } else if (reader.matches("<%=")) {
            err.aspError(reader.mark(), "asp.error.no.scriptlets");
        } else if (reader.matches("<asp:expression")) {
            err.aspError(reader.mark(), "asp.error.no.scriptlets");
        } else if (reader.matches("<%")) {
            err.aspError(reader.mark(), "asp.error.no.scriptlets");
        } else if (reader.matches("<asp:scriptlet")) {
            err.aspError(reader.mark(), "asp.error.no.scriptlets");
        } else if (reader.matches("<asp:text")) {
            parseXMLTemplateText(parent);
        } else if (!pageInfo.isELIgnored() && reader.matches("${")) {
            parseELExpression(parent, '$');
        } else if (!pageInfo.isELIgnored()
                && !pageInfo.isDeferredSyntaxAllowedAsLiteral()
                && reader.matches("#{")) {
            parseELExpression(parent, '#');
        } else if (reader.matches("<asp:")) {
            parseStandardAction(parent);
        } else if (!parseCustomTag(parent)) {
            checkUnbalancedEndTag();
            parseTemplateText(parent);
        }

        scriptlessCount--;
    }

    /*
     * TemplateTextBody ::= ( '<%--' JSPCommentBody ) | ( '<%@' DirectiveBody ) | ( '<asp:directive.'
     * XMLDirectiveBody ) | ( '<%!' <TRANSLATION_ERROR> ) | ( '<asp:declaration'
     * <TRANSLATION_ERROR> ) | ( '<%=' <TRANSLATION_ERROR> ) | ( '<asp:expression'
     * <TRANSLATION_ERROR> ) | ( '<%' <TRANSLATION_ERROR> ) | ( '<asp:scriptlet'
     * <TRANSLATION_ERROR> ) | ( '<asp:text' <TRANSLATION_ERROR> ) | ( '${'
     * <TRANSLATION_ERROR> ) | ( '<asp:' <TRANSLATION_ERROR> ) | TemplateText
     */
    private void parseElementsTemplateText(Node parent) throws JasperException {
        start = reader.mark();
        if (reader.matches("<%--")) {
            parseComment(parent);
        } else if (reader.matches("<%@")) {
            parseDirective(parent);
        } else if (reader.matches("<asp:directive.")) {
            parseXMLDirective(parent);
        } else if (reader.matches("<%!")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Declarations");
        } else if (reader.matches("<asp:declaration")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Declarations");
        } else if (reader.matches("<%=")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Expressions");
        } else if (reader.matches("<asp:expression")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Expressions");
        } else if (reader.matches("<%")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Scriptlets");
        } else if (reader.matches("<asp:scriptlet")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Scriptlets");
        } else if (reader.matches("<asp:text")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "&lt;asp:text");
        } else if (!pageInfo.isELIgnored() && reader.matches("${")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Expression language");
        } else if (!pageInfo.isELIgnored()
                && !pageInfo.isDeferredSyntaxAllowedAsLiteral()
                && reader.matches("#{")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Expression language");
        } else if (reader.matches("<asp:")) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Standard actions");
        } else if (parseCustomTag(parent)) {
            err.aspError(reader.mark(), "asp.error.not.in.template",
                    "Custom actions");
        } else {
            checkUnbalancedEndTag();
            parseTemplateText(parent);
        }
    }

    /*
     * Flag as error if an unbalanced end tag appears by itself.
     */
    private void checkUnbalancedEndTag() throws JasperException {

        if (!reader.matches("</")) {
            return;
        }

        // Check for unbalanced standard actions
        if (reader.matches("asp:")) {
            err.aspError(start, "asp.error.unbalanced.endtag", "asp:");
        }

        // Check for unbalanced custom actions
        String tagName = reader.parseToken(false);
        int i = tagName.indexOf(':');
        if (i == -1 || pageInfo.getURI(tagName.substring(0, i)) == null) {
            reader.reset(start);
            return;
        }

        err.aspError(start, "asp.error.unbalanced.endtag", tagName);
    }

    /**
     * TagDependentBody :=
     */
    private void parseTagDependentBody(Node parent, String tag)
            throws JasperException {
        Mark bodyStart = reader.mark();
        Mark bodyEnd = reader.skipUntilETag(tag);
        if (bodyEnd == null) {
            err.aspError(start, "asp.error.unterminated", "&lt;" + tag);
        }
        new Node.TemplateText(reader.getText(bodyStart, bodyEnd), bodyStart,
                parent);
    }

    /*
     * Parses asp:body action.
     */
    private void parseAspBody(Node parent, String bodyType)
            throws JasperException {
        Mark start = reader.mark();
        Node bodyNode = new Node.AspBody(start, parent);

        reader.skipSpaces();
        if (!reader.matches("/>")) {
            if (!reader.matches(">")) {
                err.aspError(start, "asp.error.unterminated", "&lt;asp:body");
            }
            parseBody(bodyNode, "asp:body", bodyType);
        }
    }

    /*
     * Parse the body as JSP content. @param tag The name of the tag whose end
     * tag would terminate the body @param bodyType One of the TagInfo body
     * types
     */
    private void parseBody(Node parent, String tag, String bodyType)
            throws JasperException {
        if (bodyType.equalsIgnoreCase(TagInfo.BODY_CONTENT_TAG_DEPENDENT)) {
            parseTagDependentBody(parent, tag);
        } else if (bodyType.equalsIgnoreCase(TagInfo.BODY_CONTENT_EMPTY)) {
            if (!reader.matchesETag(tag)) {
                err.aspError(start, "jasper.error.emptybodycontent.nonempty",
                        tag);
            }
        } else if (bodyType == JAVAX_BODY_CONTENT_PLUGIN) {
            // (note the == since we won't recognize JAVAX_*
            // from outside this module).
            parsePluginTags(parent);
            if (!reader.matchesETag(tag)) {
                err.aspError(reader.mark(), "asp.error.unterminated", "&lt;"
                        + tag);
            }
        } else if (bodyType.equalsIgnoreCase(TagInfo.BODY_CONTENT_JSP)
                || bodyType.equalsIgnoreCase(TagInfo.BODY_CONTENT_SCRIPTLESS)
                || (bodyType == JAVAX_BODY_CONTENT_PARAM)
                || (bodyType == JAVAX_BODY_CONTENT_TEMPLATE_TEXT)) {
            while (reader.hasMoreInput()) {
                if (reader.matchesETag(tag)) {
                    return;
                }

                // Check for nested asp:body or asp:attribute
                if (tag.equals("asp:body") || tag.equals("asp:attribute")) {
                    if (reader.matches("<asp:attribute")) {
                        err.aspError(reader.mark(),
                                "asp.error.nested.aspattribute");
                    } else if (reader.matches("<asp:body")) {
                        err.aspError(reader.mark(), "asp.error.nested.aspbody");
                    }
                }

                if (bodyType.equalsIgnoreCase(TagInfo.BODY_CONTENT_JSP)) {
                    parseElements(parent);
                } else if (bodyType
                        .equalsIgnoreCase(TagInfo.BODY_CONTENT_SCRIPTLESS)) {
                    parseElementsScriptless(parent);
                } else if (bodyType == JAVAX_BODY_CONTENT_PARAM) {
                    // (note the == since we won't recognize JAVAX_*
                    // from outside this module).
                    reader.skipSpaces();
                    parseParam(parent);
                } else if (bodyType == JAVAX_BODY_CONTENT_TEMPLATE_TEXT) {
                    parseElementsTemplateText(parent);
                }
            }
            err.aspError(start, "asp.error.unterminated", "&lt;" + tag);
        } else {
            err.aspError(start, "jasper.error.bad.bodycontent.type");
        }
    }

    /*
     * Parses named attributes.
     */
    private void parseNamedAttributes(Node parent) throws JasperException {
        do {
            Mark start = reader.mark();
            Attributes attrs = parseAttributes();
            Node.NamedAttribute namedAttributeNode = new Node.NamedAttribute(
                    attrs, start, parent);

            reader.skipSpaces();
            if (!reader.matches("/>")) {
                if (!reader.matches(">")) {
                    err.aspError(start, "asp.error.unterminated",
                            "&lt;asp:attribute");
                }
                if (namedAttributeNode.isTrim()) {
                    reader.skipSpaces();
                }
                parseBody(namedAttributeNode, "asp:attribute",
                        getAttributeBodyType(parent, attrs.getValue("name")));
                if (namedAttributeNode.isTrim()) {
                    Node.Nodes subElems = namedAttributeNode.getBody();
                    if (subElems != null) {
                        Node lastNode = subElems.getNode(subElems.size() - 1);
                        if (lastNode instanceof Node.TemplateText) {
                            ((Node.TemplateText) lastNode).rtrim();
                        }
                    }
                }
            }
            reader.skipSpaces();
        } while (reader.matches("<asp:attribute"));
    }

    /**
     * Determine the body type of <asp:attribute> from the enclosing node
     */
    private String getAttributeBodyType(Node n, String name) {

        if (n instanceof Node.CustomTag) {
            TagInfo tagInfo = ((Node.CustomTag) n).getTagInfo();
            TagAttributeInfo[] tldAttrs = tagInfo.getAttributes();
            for (int i = 0; i < tldAttrs.length; i++) {
                if (name.equals(tldAttrs[i].getName())) {
                    if (tldAttrs[i].isFragment()) {
                        return TagInfo.BODY_CONTENT_SCRIPTLESS;
                    }
                    if (tldAttrs[i].canBeRequestTime()) {
                        return TagInfo.BODY_CONTENT_JSP;
                    }
                }
            }
            if (tagInfo.hasDynamicAttributes()) {
                return TagInfo.BODY_CONTENT_JSP;
            }
        } else if (n instanceof Node.IncludeAction) {
            if ("page".equals(name)) {
                return TagInfo.BODY_CONTENT_JSP;
            }
        } else if (n instanceof Node.ForwardAction) {
            if ("page".equals(name)) {
                return TagInfo.BODY_CONTENT_JSP;
            }
        } else if (n instanceof Node.SetProperty) {
            if ("value".equals(name)) {
                return TagInfo.BODY_CONTENT_JSP;
            }
        } else if (n instanceof Node.UseBean) {
            if ("beanName".equals(name)) {
                return TagInfo.BODY_CONTENT_JSP;
            }
        } else if (n instanceof Node.PlugIn) {
            if ("width".equals(name) || "height".equals(name)) {
                return TagInfo.BODY_CONTENT_JSP;
            }
        } else if (n instanceof Node.ParamAction) {
            if ("value".equals(name)) {
                return TagInfo.BODY_CONTENT_JSP;
            }
        } else if (n instanceof Node.AspElement) {
            return TagInfo.BODY_CONTENT_JSP;
        }

        return JAVAX_BODY_CONTENT_TEMPLATE_TEXT;
    }

    private void parseFileDirectives(Node parent) throws JasperException {
        reader.setSingleFile(true);
        reader.skipUntil("<");
        while (reader.hasMoreInput()) {
            start = reader.mark();
            if (reader.matches("%--")) {
                // Comment
                reader.skipUntil("--%>");
            } else if (reader.matches("%@")) {
                parseDirective(parent);
            } else if (reader.matches("asp:directive.")) {
                parseXMLDirective(parent);
            } else if (reader.matches("%!")) {
                // Declaration
                reader.skipUntil("%>");
            } else if (reader.matches("%=")) {
                // Expression
                reader.skipUntil("%>");
            } else if (reader.matches("%")) {
                // Scriptlet
                reader.skipUntil("%>");
            }
            reader.skipUntil("<");
        }
    }
}

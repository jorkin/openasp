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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jasper.JasperException;
import org.apache.jasper.AspCompilationContext;
import org.apache.jasper.Options;
import org.apache.jasper.servlet.AspServletWrapper;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Main ASP compiler class. This class uses Ant for compiling.
 * 
 * @author Anil K. Vijendran
 * @author Mandar Raje
 * @author Pierre Delisle
 * @author Kin-man Chung
 * @author Remy Maucherat
 * @author Mark Roth
 */
public abstract class Compiler {
    
    private final Log log = LogFactory.getLog(Compiler.class); // must not be static

    // ----------------------------------------------------- Instance Variables

    protected AspCompilationContext ctxt;

    protected ErrorDispatcher errDispatcher;

    protected PageInfo pageInfo;

    protected AspServletWrapper jsw;

    protected TagFileProcessor tfp;

    protected Options options;

    protected Node.Nodes pageNodes;

    // ------------------------------------------------------------ Constructor

    public void init(AspCompilationContext ctxt, AspServletWrapper jsw) {
        this.jsw = jsw;
        this.ctxt = ctxt;
        this.options = ctxt.getOptions();
    }

    // --------------------------------------------------------- Public Methods

    /**
     * <p>
     * Retrieves the parsed nodes of the JSP page, if they are available. May
     * return null. Used in development mode for generating detailed error
     * messages. http://issues.apache.org/bugzilla/show_bug.cgi?id=37062.
     * </p>
     */
    public Node.Nodes getPageNodes() {
        return this.pageNodes;
    }

    /**
     * Compile the asp file into equivalent servlet in .java file
     * 
     * @return a smap for the current JSP page, if one is generated, null
     *         otherwise
     */
    protected String[] generateJava() throws Exception {

        String[] smapStr = null;

        long t1, t2, t3, t4;

        t1 = t2 = t3 = t4 = 0;

        if (log.isDebugEnabled()) {
            t1 = System.currentTimeMillis();
        }

        // Setup page info area
        pageInfo = new PageInfo(new BeanRepository(ctxt.getClassLoader(),
                errDispatcher), ctxt.getAspFile(), ctxt.isTagFile());

        AspConfig aspConfig = options.getAspConfig();
        AspConfig.AspProperty aspProperty = aspConfig.findAspProperty(ctxt
                .getAspFile());

        /*
         * If the current uri is matched by a pattern specified in a
         * asp-property-group in web.xml, initialize pageInfo with those
         * properties.
         */
        if (aspProperty.isELIgnored() != null) {
            pageInfo.setELIgnored(AspUtil.booleanValue(aspProperty
                    .isELIgnored()));
        }
        if (aspProperty.isScriptingInvalid() != null) {
            pageInfo.setScriptingInvalid(AspUtil.booleanValue(aspProperty
                    .isScriptingInvalid()));
        }
        if (aspProperty.getIncludePrelude() != null) {
            pageInfo.setIncludePrelude(aspProperty.getIncludePrelude());
        }
        if (aspProperty.getIncludeCoda() != null) {
            pageInfo.setIncludeCoda(aspProperty.getIncludeCoda());
        }
        if (aspProperty.isDeferedSyntaxAllowedAsLiteral() != null) {
            pageInfo.setDeferredSyntaxAllowedAsLiteral(AspUtil.booleanValue(aspProperty
                    .isDeferedSyntaxAllowedAsLiteral()));
        }
        if (aspProperty.isTrimDirectiveWhitespaces() != null) {
            pageInfo.setTrimDirectiveWhitespaces(AspUtil.booleanValue(aspProperty
                    .isTrimDirectiveWhitespaces()));
        }
        // Default ContentType processing is deferred until after the page has
        // been parsed
        if (aspProperty.getBuffer() != null) {
            pageInfo.setBufferValue(aspProperty.getBuffer(), null,
                    errDispatcher);
        }
        if (aspProperty.isErrorOnUndeclaredNamespace() != null) {
            pageInfo.setErrorOnUndeclaredNamespace(
                    AspUtil.booleanValue(
                            aspProperty.isErrorOnUndeclaredNamespace()));
        }
        if (ctxt.isTagFile()) {
            try {
                double libraryVersion = Double.parseDouble(ctxt.getTagInfo()
                        .getTagLibrary().getRequiredVersion());
                if (libraryVersion < 2.0) {
                    pageInfo.setIsELIgnored("true", null, errDispatcher, true);
                }
                if (libraryVersion < 2.1) {
                    pageInfo.setDeferredSyntaxAllowedAsLiteral("true", null,
                            errDispatcher, true);
                }
            } catch (NumberFormatException ex) {
                errDispatcher.aspError(ex);
            }
        }

        ctxt.checkOutputDir();
        String javaFileName = ctxt.getServletJavaFileName();

        ServletWriter writer = null;
        try {
            /*
             * The setting of isELIgnored changes the behaviour of the parser
             * in subtle ways. To add to the 'fun', isELIgnored can be set in
             * any file that forms part of the translation unit so setting it
             * in a file included towards the end of the translation unit can
             * change how the parser should have behaved when parsing content
             * up to the point where isELIgnored was set. Arghh!
             * Previous attempts to hack around this have only provided partial
             * solutions. We now use two passes to parse the translation unit.
             * The first just parses the directives and the second parses the
             * whole translation unit once we know how isELIgnored has been set.
             * TODO There are some possible optimisations of this process.  
             */ 
            // Parse the file
            ParserController parserCtl = new ParserController(ctxt, this);
            
            // Pass 1 - the directives
            Node.Nodes directives =
                parserCtl.parseDirectives(ctxt.getAspFile());
            Validator.validateDirectives(this, directives);
            
            // Pass 2 - the whole translation unit
            pageNodes = parserCtl.parse(ctxt.getAspFile());

            // Leave this until now since it can only be set once - bug 49726
            if (pageInfo.getContentType() == null &&
                    aspProperty.getDefaultContentType() != null) {
                pageInfo.setContentType(aspProperty.getDefaultContentType());
            }

            if (ctxt.isPrototypeMode()) {
                // generate prototype .java file for the tag file
                writer = setupContextWriter(javaFileName);
                Generator.generate(writer, this, pageNodes);
                writer.close();
                writer = null;
                return null;
            }

            // Validate and process attributes - don't re-validate the
            // directives we validated in pass 1
            Validator.validateExDirectives(this, pageNodes);

            if (log.isDebugEnabled()) {
                t2 = System.currentTimeMillis();
            }

            // Collect page info
            Collector.collect(this, pageNodes);

            // Compile (if necessary) and load the tag files referenced in
            // this compilation unit.
            tfp = new TagFileProcessor();
            tfp.loadTagFiles(this, pageNodes);

            if (log.isDebugEnabled()) {
                t3 = System.currentTimeMillis();
            }

            // Determine which custom tag needs to declare which scripting vars
            ScriptingVariabler.set(pageNodes, errDispatcher);

            // Optimizations by Tag Plugins
            TagPluginManager tagPluginManager = options.getTagPluginManager();
            tagPluginManager.apply(pageNodes, errDispatcher, pageInfo);

            // Optimization: concatenate contiguous template texts.
            TextOptimizer.concatenate(this, pageNodes);

            // Generate static function mapper codes.
            ELFunctionMapper.map(pageNodes);

            // generate servlet .java file
            writer = setupContextWriter(javaFileName);
            Generator.generate(writer, this, pageNodes);
            writer.close();
            writer = null;

            // The writer is only used during the compile, dereference
            // it in the AspCompilationContext when done to allow it
            // to be GC'd and save memory.
            ctxt.setWriter(null);

            if (log.isDebugEnabled()) {
                t4 = System.currentTimeMillis();
                log.debug("Generated " + javaFileName + " total=" + (t4 - t1)
                        + " generate=" + (t4 - t3) + " validate=" + (t2 - t1));
            }

        } catch (Exception e) {
            if (writer != null) {
                try {
                    writer.close();
                    writer = null;
                } catch (Exception e1) {
                    // do nothing
                }
            }
            // Remove the generated .java file
            File file = new File(javaFileName);
            if (file.exists()) {
                if (!file.delete()) {
                    log.warn(Localizer.getMessage(
                            "asp.warning.compiler.javafile.delete.fail",
                            file.getAbsolutePath()));
                }
            }
            throw e;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e2) {
                    // do nothing
                }
            }
        }

        // JSR45 Support
        if (!options.isSmapSuppressed()) {
            smapStr = SmapUtil.generateSmap(ctxt, pageNodes);
        }

        // If any proto type .java and .class files was generated,
        // the prototype .java may have been replaced by the current
        // compilation (if the tag file is self referencing), but the
        // .class file need to be removed, to make sure that javac would
        // generate .class again from the new .java file just generated.
        tfp.removeProtoTypeFiles(ctxt.getClassFileName());

        return smapStr;
    }

    private ServletWriter setupContextWriter(String javaFileName)
            throws FileNotFoundException, JasperException {
        ServletWriter writer;
        // Setup the ServletWriter
        String javaEncoding = ctxt.getOptions().getJavaEncoding();
        OutputStreamWriter osw = null;

        try {
            osw = new OutputStreamWriter(
                    new FileOutputStream(javaFileName), javaEncoding);
        } catch (UnsupportedEncodingException ex) {
            errDispatcher.aspError("asp.error.needAlternateJavaEncoding",
                    javaEncoding);
        }

        writer = new ServletWriter(new PrintWriter(osw));
        ctxt.setWriter(writer);
        return writer;
    }

    /**
     * Compile the servlet from .java file to .class file
     */
    protected abstract void generateClass(String[] smap)
            throws FileNotFoundException, JasperException, Exception;

    /**
     * Compile the asp file from the current engine context
     */
    public void compile() throws FileNotFoundException, JasperException,
            Exception {
        compile(true);
    }

    /**
     * Compile the asp file from the current engine context. As an side- effect,
     * tag files that are referenced by this page are also compiled.
     * 
     * @param compileClass
     *            If true, generate both .java and .class file If false,
     *            generate only .java file
     */
    public void compile(boolean compileClass) throws FileNotFoundException,
            JasperException, Exception {
        compile(compileClass, false);
    }

    /**
     * Compile the asp file from the current engine context. As an side- effect,
     * tag files that are referenced by this page are also compiled.
     * 
     * @param compileClass
     *            If true, generate both .java and .class file If false,
     *            generate only .java file
     * @param aspcMode
     *            true if invoked from AspC, false otherwise
     */
    public void compile(boolean compileClass, boolean aspcMode)
            throws FileNotFoundException, JasperException, Exception {
        if (errDispatcher == null) {
            this.errDispatcher = new ErrorDispatcher(aspcMode);
        }

        try {
            String[] smap = generateJava();
            File javaFile = new File(ctxt.getServletJavaFileName());
            Long aspLastModified = ctxt.getLastModified(ctxt.getAspFile());
            javaFile.setLastModified(aspLastModified.longValue());
            if (compileClass) {
                generateClass(smap);
                // Fix for bugzilla 41606
                // Set AspServletWrapper.servletClassLastModifiedTime after successful compile
                String targetFileName = ctxt.getClassFileName();
                if (targetFileName != null) {
                    File targetFile = new File(targetFileName);
                    if (targetFile.exists()) {
                        targetFile.setLastModified(aspLastModified.longValue());
                        if (jsw != null) {
                            jsw.setServletClassLastModifiedTime(
                                    aspLastModified.longValue());
                        }
                    }
                }
            }
        } finally {
            if (tfp != null && ctxt.isPrototypeMode()) {
                tfp.removeProtoTypeFiles(null);
            }
            // Make sure these object which are only used during the
            // generation and compilation of the JSP page get
            // dereferenced so that they can be GC'd and reduce the
            // memory footprint.
            tfp = null;
            errDispatcher = null;
            pageInfo = null;

            // Only get rid of the pageNodes if in production.
            // In development mode, they are used for detailed
            // error messages.
            // http://issues.apache.org/bugzilla/show_bug.cgi?id=37062
            if (!this.options.getDevelopment()) {
                pageNodes = null;
            }

            if (ctxt.getWriter() != null) {
                ctxt.getWriter().close();
                ctxt.setWriter(null);
            }
        }
    }

    /**
     * This is a protected method intended to be overridden by subclasses of
     * Compiler. This is used by the compile method to do all the compilation.
     */
    public boolean isOutDated() {
        return isOutDated(true);
    }

    /**
     * Determine if a compilation is necessary by checking the time stamp of the
     * JSP page with that of the corresponding .class or .java file. If the page
     * has dependencies, the check is also extended to its dependents, and so
     * on. This method can by overridden by a subclasses of Compiler.
     * 
     * @param checkClass
     *            If true, check against .class file, if false, check against
     *            .java file.
     */
    public boolean isOutDated(boolean checkClass) {

        if (jsw != null
                && (ctxt.getOptions().getModificationTestInterval() > 0)) {

            if (jsw.getLastModificationTest()
                    + (ctxt.getOptions().getModificationTestInterval() * 1000) > System
                    .currentTimeMillis()) {
                return false;
            }
            jsw.setLastModificationTest(System.currentTimeMillis());
        }

        Long aspRealLastModified = ctxt.getLastModified(ctxt.getAspFile());
        if (aspRealLastModified.longValue() < 0) {
            // Something went wrong - assume modification
            return true;
        }

        long targetLastModified = 0;
        File targetFile;

        if (checkClass) {
            targetFile = new File(ctxt.getClassFileName());
        } else {
            targetFile = new File(ctxt.getServletJavaFileName());
        }

        if (!targetFile.exists()) {
            return true;
        }

        targetLastModified = targetFile.lastModified();
        if (checkClass && jsw != null) {
            jsw.setServletClassLastModifiedTime(targetLastModified);
        }
        if (targetLastModified != aspRealLastModified.longValue()) {
            if (log.isDebugEnabled()) {
                log.debug("Compiler: outdated: " + targetFile + " "
                        + targetLastModified);
            }
            return true;
        }

        // determine if source dependent files (e.g. includes using include
        // directives) have been changed.
        if (jsw == null) {
            return false;
        }

        Map<String,Long> depends = jsw.getDependants();
        if (depends == null) {
            return false;
        }

        Iterator<Entry<String,Long>> it = depends.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String,Long> include = it.next();
            try {
                String key = include.getKey();
                URL includeUrl;
                if (key.startsWith("jar:") || key.startsWith("file:")) {
                    includeUrl = new URL(key);
                } else {
                    includeUrl = ctxt.getResource(include.getKey());
                }
                if (includeUrl == null) {
                    return true;
                }

                URLConnection iuc = includeUrl.openConnection();
                long includeLastModified = 0;
                if (iuc instanceof JarURLConnection) {
                    includeLastModified =
                        ((JarURLConnection) iuc).getJarEntry().getTime();
                } else {
                    includeLastModified = iuc.getLastModified();
                }
                iuc.getInputStream().close();

                if (includeLastModified != include.getValue().longValue()) {
                    return true;
                }
            } catch (Exception e) {
                if (log.isDebugEnabled())
                    log.debug("Problem accessing resource. Treat as outdated.",
                            e);
                return true;
            }
        }

        return false;

    }

    /**
     * Gets the error dispatcher.
     */
    public ErrorDispatcher getErrorDispatcher() {
        return errDispatcher;
    }

    /**
     * Gets the info about the page under compilation
     */
    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public AspCompilationContext getCompilationContext() {
        return ctxt;
    }

    /**
     * Remove generated files
     */
    public void removeGeneratedFiles() {
        removeGeneratedClassFiles();

        try {
            String javaFileName = ctxt.getServletJavaFileName();
            if (javaFileName != null) {
                File javaFile = new File(javaFileName);
                if (log.isDebugEnabled())
                    log.debug("Deleting " + javaFile);
                if (javaFile.exists()) {
                    if (!javaFile.delete()) {
                        log.warn(Localizer.getMessage(
                                "asp.warning.compiler.javafile.delete.fail",
                                javaFile.getAbsolutePath()));
                    }
                }
            }
        } catch (Exception e) {
            // Remove as much as possible, log possible exceptions
            log.warn(Localizer.getMessage("asp.warning.compiler.classfile.delete.fail.unknown"),
                     e);
        }
    }

    public void removeGeneratedClassFiles() {
        try {
            String classFileName = ctxt.getClassFileName();
            if (classFileName != null) {
                File classFile = new File(classFileName);
                if (log.isDebugEnabled())
                    log.debug("Deleting " + classFile);
                if (classFile.exists()) {
                    if (!classFile.delete()) {
                        log.warn(Localizer.getMessage(
                                "asp.warning.compiler.classfile.delete.fail",
                                classFile.getAbsolutePath()));
                    }
                }
            }
        } catch (Exception e) {
            // Remove as much as possible, log possible exceptions
            log.warn(Localizer.getMessage("asp.warning.compiler.classfile.delete.fail.unknown"),
                     e);
        }
    }
}

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

package org.apache.jasper;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.asp.tagext.TagLibraryInfo;

import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.AspConfig;
import org.apache.jasper.compiler.AspRuntimeContext;
import org.apache.jasper.compiler.Localizer;
import org.apache.jasper.compiler.TagPluginManager;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.jasper.servlet.AspCServletContext;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

/**
 * Shell for the aspc compiler.  Handles all options associated with the
 * command line and creates compilation contexts which it then compiles
 * according to the specified options.
 *
 * This version can process files from a _single_ webapp at once, i.e.
 * a single docbase can be specified.
 *
 * It can be used as an Ant task using:
 * <pre>
 *   &lt;taskdef classname="org.apache.jasper.AspC" name="jasper" &gt;
 *      &lt;classpath&gt;
 *          &lt;pathelement location="${java.home}/../lib/tools.jar"/&gt;
 *          &lt;fileset dir="${ENV.CATALINA_HOME}/lib"&gt;
 *              &lt;include name="*.jar"/&gt;
 *          &lt;/fileset&gt;
 *          &lt;path refid="myjars"/&gt;
 *       &lt;/classpath&gt;
 *  &lt;/taskdef&gt;
 *
 *  &lt;jasper verbose="0"
 *           package="my.package"
 *           uriroot="${webapps.dir}/${webapp.name}"
 *           webXmlFragment="${build.dir}/generated_web.xml"
 *           outputDir="${webapp.dir}/${webapp.name}/WEB-INF/src/my/package" /&gt;
 * </pre>
 *
 * @author Danno Ferrin
 * @author Pierre Delisle
 * @author Costin Manolache
 * @author Yoav Shapira
 */
public class AspC extends Task implements Options {

    public static final String DEFAULT_IE_CLASS_ID =
            "clsid:8AD9C840-044E-11D1-B3E9-00805F499D93";

    // Logger
    private static final Log log = LogFactory.getLog(AspC.class);

    protected static final String SWITCH_VERBOSE = "-v";
    protected static final String SWITCH_HELP = "-help";
    protected static final String SWITCH_OUTPUT_DIR = "-d";
    protected static final String SWITCH_PACKAGE_NAME = "-p";
    protected static final String SWITCH_CACHE = "-cache";
    protected static final String SWITCH_CLASS_NAME = "-c";
    protected static final String SWITCH_FULL_STOP = "--";
    protected static final String SWITCH_COMPILE = "-compile";
    protected static final String SWITCH_SOURCE = "-source";
    protected static final String SWITCH_TARGET = "-target";
    protected static final String SWITCH_URI_BASE = "-uribase";
    protected static final String SWITCH_URI_ROOT = "-uriroot";
    protected static final String SWITCH_FILE_WEBAPP = "-webapp";
    protected static final String SWITCH_WEBAPP_INC = "-webinc";
    protected static final String SWITCH_WEBAPP_XML = "-webxml";
    protected static final String SWITCH_WEBAPP_XML_ENCODING = "-webxmlencoding";
    protected static final String SWITCH_ADD_WEBAPP_XML_MAPPINGS = "-addwebxmlmappings";
    protected static final String SWITCH_MAPPED = "-mapped";
    protected static final String SWITCH_XPOWERED_BY = "-xpoweredBy";
    protected static final String SWITCH_TRIM_SPACES = "-trimSpaces";
    protected static final String SWITCH_CLASSPATH = "-classpath";
    protected static final String SWITCH_DIE = "-die";
    protected static final String SWITCH_POOLING = "-poolingEnabled";
    protected static final String SWITCH_ENCODING = "-javaEncoding";
    protected static final String SWITCH_SMAP = "-smap";
    protected static final String SWITCH_DUMP_SMAP = "-dumpsmap";
    protected static final String SHOW_SUCCESS ="-s";
    protected static final String LIST_ERRORS = "-l";
    protected static final int INC_WEBXML = 10;
    protected static final int ALL_WEBXML = 20;
    protected static final int DEFAULT_DIE_LEVEL = 1;
    protected static final int NO_DIE_LEVEL = 0;
    protected static final Set<String> insertBefore = new HashSet<String>();
    
    static {
        insertBefore.add("</web-app>");
        insertBefore.add("<servlet-mapping>");
        insertBefore.add("<session-config>");
        insertBefore.add("<mime-mapping>");
        insertBefore.add("<welcome-file-list>");
        insertBefore.add("<error-page>");
        insertBefore.add("<taglib>");
        insertBefore.add("<resource-env-ref>");
        insertBefore.add("<resource-ref>");
        insertBefore.add("<security-constraint>");
        insertBefore.add("<login-config>");
        insertBefore.add("<security-role>");
        insertBefore.add("<env-entry>");
        insertBefore.add("<ejb-ref>");
        insertBefore.add("<ejb-local-ref>");
    }

    protected String classPath = null;
    protected URLClassLoader loader = null;
    protected boolean trimSpaces = false;
    protected boolean genStringAsCharArray = false;
    protected boolean xpoweredBy;
    protected boolean mappedFile = false;
    protected boolean poolingEnabled = true;
    protected File scratchDir;
    protected String ieClassId = DEFAULT_IE_CLASS_ID;
    protected String targetPackage;
    protected String targetClassName;
    protected String uriBase;
    protected String uriRoot;
    protected int dieLevel;
    protected boolean helpNeeded = false;
    protected boolean compile = false;
    protected boolean smapSuppressed = true;
    protected boolean smapDumped = false;
    protected boolean caching = true;
    protected final Map<String, TagLibraryInfo> cache =
        new HashMap<String, TagLibraryInfo>();

    protected String compiler = null;

    protected String compilerTargetVM = "1.6";
    protected String compilerSourceVM = "1.6";

    protected boolean classDebugInfo = true;

    /**
     * Throw an exception if there's a compilation error, or swallow it.
     * Default is true to preserve old behavior.
     */
    protected boolean failOnError = true;

    /**
     * The file extensions to be handled as JSP files.
     * Default list is .asp and .aspx.
     */
    protected List<String> extensions;

    /**
     * The pages.
     */
    protected final List<String> pages = new Vector<String>();

    /**
     * Needs better documentation, this data member does.
     * True by default.
     */
    protected boolean errorOnUseBeanInvalidClassAttribute = true;

    /**
     * The java file encoding.  Default
     * is UTF-8.  Added per bugzilla 19622.
     */
    protected String javaEncoding = "UTF-8";

    // Generation of web.xml fragments
    protected String webxmlFile;
    protected int webxmlLevel;
    protected String webxmlEncoding;
    protected boolean addWebXmlMappings = false;

    protected Writer mapout;
    protected CharArrayWriter servletout;
    protected CharArrayWriter mappingout;

    /**
     * The servlet context.
     */
    protected AspCServletContext context;

    /**
     * The runtime context.
     * Maintain a dummy AspRuntimeContext for compiling tag files.
     */
    protected AspRuntimeContext rctxt;

    /**
     * Cache for the TLD locations
     */
    protected TldLocationsCache tldLocationsCache = null;

    protected AspConfig aspConfig = null;
    protected TagPluginManager tagPluginManager = null;

    protected boolean verbose = false;
    protected boolean listErrors = false;
    protected boolean showSuccess = false;
    protected int argPos;
    protected boolean fullstop = false;
    protected String args[];

    public static void main(String arg[]) {
        if (arg.length == 0) {
            System.out.println(Localizer.getMessage("aspc.usage"));
        } else {
            AspC aspc = new AspC();
            try {
                aspc.setArgs(arg);
                if (aspc.helpNeeded) {
                    System.out.println(Localizer.getMessage("aspc.usage"));
                } else {
                    aspc.execute();
                }
            } catch (JasperException je) {
                System.err.println(je);
                if (aspc.dieLevel != NO_DIE_LEVEL) {
                    System.exit(aspc.dieLevel);
                }
            } catch (BuildException je) {
                System.err.println(je);
                if (aspc.dieLevel != NO_DIE_LEVEL) {
                    System.exit(aspc.dieLevel);
                }
            }
        }
    }

    /**
     * Apply command-line arguments.
     * 
     * @param arg
     *            The arguments
     */
    public void setArgs(String[] arg) throws JasperException {
        args = arg;
        String tok;

        dieLevel = NO_DIE_LEVEL;

        while ((tok = nextArg()) != null) {
            if (tok.equals(SWITCH_VERBOSE)) {
                verbose = true;
                showSuccess = true;
                listErrors = true;
            } else if (tok.equals(SWITCH_OUTPUT_DIR)) {
                tok = nextArg();
                setOutputDir( tok );
            } else if (tok.equals(SWITCH_PACKAGE_NAME)) {
                targetPackage = nextArg();
            } else if (tok.equals(SWITCH_COMPILE)) {
                compile=true;
            } else if (tok.equals(SWITCH_CLASS_NAME)) {
                targetClassName = nextArg();
            } else if (tok.equals(SWITCH_URI_BASE)) {
                uriBase=nextArg();
            } else if (tok.equals(SWITCH_URI_ROOT)) {
                setUriroot( nextArg());
            } else if (tok.equals(SWITCH_FILE_WEBAPP)) {
                setUriroot( nextArg());
            } else if ( tok.equals( SHOW_SUCCESS ) ) {
                showSuccess = true;
            } else if ( tok.equals( LIST_ERRORS ) ) {
                listErrors = true;
            } else if (tok.equals(SWITCH_WEBAPP_INC)) {
                webxmlFile = nextArg();
                if (webxmlFile != null) {
                    webxmlLevel = INC_WEBXML;
                }
            } else if (tok.equals(SWITCH_WEBAPP_XML)) {
                webxmlFile = nextArg();
                if (webxmlFile != null) {
                    webxmlLevel = ALL_WEBXML;
                }
            } else if (tok.equals(SWITCH_WEBAPP_XML_ENCODING)) {
                setWebXmlEncoding(nextArg());
            } else if (tok.equals(SWITCH_ADD_WEBAPP_XML_MAPPINGS)) {
                setAddWebXmlMappings(true);
            } else if (tok.equals(SWITCH_MAPPED)) {
                mappedFile = true;
            } else if (tok.equals(SWITCH_XPOWERED_BY)) {
                xpoweredBy = true;
            } else if (tok.equals(SWITCH_TRIM_SPACES)) {
                setTrimSpaces(true);
            } else if (tok.equals(SWITCH_CACHE)) {
                tok = nextArg();
                if ("false".equals(tok)) {
                    caching = false;
                } else {
                    caching = true;
                }            
            } else if (tok.equals(SWITCH_CLASSPATH)) {
                setClassPath(nextArg());
            } else if (tok.startsWith(SWITCH_DIE)) {
                try {
                    dieLevel = Integer.parseInt(
                        tok.substring(SWITCH_DIE.length()));
                } catch (NumberFormatException nfe) {
                    dieLevel = DEFAULT_DIE_LEVEL;
                }
            } else if (tok.equals(SWITCH_HELP)) {
                helpNeeded = true;
            } else if (tok.equals(SWITCH_POOLING)) {
                tok = nextArg();
                if ("false".equals(tok)) {
                    poolingEnabled = false;
                } else {
                    poolingEnabled = true;
                }
            } else if (tok.equals(SWITCH_ENCODING)) {
                setJavaEncoding(nextArg());
            } else if (tok.equals(SWITCH_SOURCE)) {
                setCompilerSourceVM(nextArg());
            } else if (tok.equals(SWITCH_TARGET)) {
                setCompilerTargetVM(nextArg());
            } else if (tok.equals(SWITCH_SMAP)) {
                smapSuppressed = false;
            } else if (tok.equals(SWITCH_DUMP_SMAP)) {
                smapDumped = true;
            } else {
                if (tok.startsWith("-")) {
                    throw new JasperException("Unrecognized option: " + tok +
                        ".  Use -help for help.");
                }
                if (!fullstop) {
                    argPos--;
                }
                // Start treating the rest as JSP Pages
                break;
            }
        }

        // Add all extra arguments to the list of files
        while( true ) {
            String file = nextFile();
            if( file==null ) {
                break;
            }
            pages.add( file );
        }
    }

    /**
     * In AspC this always returns <code>true</code>.
     * {@inheritDoc}
     */
    @Override
    public boolean getKeepGenerated() {
        // isn't this why we are running aspc?
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getTrimSpaces() {
        return trimSpaces;
    }

    /**
     * Sets the option to trim white spaces between directives or actions.
     */
    public void setTrimSpaces(boolean ts) {
        this.trimSpaces = ts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPoolingEnabled() {
        return poolingEnabled;
    }

    /**
     * Sets the option to enable the tag handler pooling.
     */
    public void setPoolingEnabled(boolean poolingEnabled) {
        this.poolingEnabled = poolingEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isXpoweredBy() {
        return xpoweredBy;
    }

    /**
     * Sets the option to enable generation of X-Powered-By response header.
     */
    public void setXpoweredBy(boolean xpoweredBy) {
        this.xpoweredBy = xpoweredBy;
    }

    /**
     * In AspC this always returns <code>true</code>.
     * {@inheritDoc}
     */
    @Override
    public boolean getDisplaySourceFragment() {
        return true;
    }

    @Override
    public int getMaxLoadedAsps() {
        return -1;
    }

    @Override
    public int getAspIdleTimeout() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getErrorOnUseBeanInvalidClassAttribute() {
        return errorOnUseBeanInvalidClassAttribute;
    }

    /**
     * Sets the option to issue a compilation error if the class attribute
     * specified in useBean action is invalid.
     */
    public void setErrorOnUseBeanInvalidClassAttribute(boolean b) {
        errorOnUseBeanInvalidClassAttribute = b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getMappedFile() {
        return mappedFile;
    }

    /**
     * Sets the option to include debug information in compiled class.
     */
    public void setClassDebugInfo( boolean b ) {
        classDebugInfo=b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getClassDebugInfo() {
        // compile with debug info
        return classDebugInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCaching() {
        return caching;
    }

    /**
     * Sets the option to enable caching.
     * 
     * @see Options#isCaching()
     */
    public void setCaching(boolean caching) {
        this.caching = caching;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, TagLibraryInfo> getCache() {
        return cache;
    }

    /**
     * In AspC this always returns <code>0</code>.
     * {@inheritDoc}
     */
    @Override
    public int getCheckInterval() {
        return 0;
    }

    /**
     * In AspC this always returns <code>0</code>.
     * {@inheritDoc}
     */
    @Override
    public int getModificationTestInterval() {
        return 0;
    }


    /**
     * In AspC this always returns <code>false</code>.
     * {@inheritDoc}
     */
    @Override
    public boolean getRecompileOnFail() {
        return false;
    }
    
    
    /**
     * In AspC this always returns <code>false</code>.
     * {@inheritDoc}
     */
    @Override
    public boolean getDevelopment() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSmapSuppressed() {
        return smapSuppressed;
    }

    /**
     * Sets smapSuppressed flag.
     */
    public void setSmapSuppressed(boolean smapSuppressed) {
        this.smapSuppressed = smapSuppressed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSmapDumped() {
        return smapDumped;
    }

    /**
     * Sets smapDumped flag.
     * 
     * @see Options#isSmapDumped()
     */
    public void setSmapDumped(boolean smapDumped) {
        this.smapDumped = smapDumped;
    }

    
    /**
     * Determines whether text strings are to be generated as char arrays,
     * which improves performance in some cases.
     *
     * @param genStringAsCharArray true if text strings are to be generated as
     * char arrays, false otherwise
     */
    public void setGenStringAsCharArray(boolean genStringAsCharArray) {
        this.genStringAsCharArray = genStringAsCharArray;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean genStringAsCharArray() {
        return genStringAsCharArray;
    }

    /**
     * Sets the class-id value to be sent to Internet Explorer when using
     * &lt;asp:plugin&gt; tags.
     * 
     * @param ieClassId
     *            Class-id value
     */
    public void setIeClassId(String ieClassId) {
        this.ieClassId = ieClassId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIeClassId() {
        return ieClassId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getScratchDir() {
        return scratchDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCompiler() {
        return compiler;
    }

    /**
     * Sets the option to determine what compiler to use.
     * 
     * @see Options#getCompiler()
     */
    public void setCompiler(String c) {
        compiler=c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCompilerClassName() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCompilerTargetVM() {
        return compilerTargetVM;
    }

    /**
     * Sets the compiler target VM.
     * 
     * @see Options#getCompilerTargetVM()
     */
    public void setCompilerTargetVM(String vm) {
        compilerTargetVM = vm;
    }

    /**
     * {@inheritDoc}
     */
     @Override
    public String getCompilerSourceVM() {
         return compilerSourceVM;
     }

     /**
      * Sets the compiler source VM.
      * 
      * @see Options#getCompilerSourceVM()
      */
    public void setCompilerSourceVM(String vm) {
        compilerSourceVM = vm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TldLocationsCache getTldLocationsCache() {
        return tldLocationsCache;
    }

    /**
     * Returns the encoding to use for
     * java files.  The default is UTF-8.
     *
     * @return String The encoding
     */
    @Override
    public String getJavaEncoding() {
        return javaEncoding;
    }

    /**
     * Sets the encoding to use for
     * java files.
     *
     * @param encodingName The name, e.g. "UTF-8"
     */
    public void setJavaEncoding(String encodingName) {
        javaEncoding = encodingName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getFork() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassPath() {
        if( classPath != null )
            return classPath;
        return System.getProperty("java.class.path");
    }

    /**
     * Sets the classpath used while compiling the servlets generated from JSP
     * files
     */
    public void setClassPath(String s) {
        classPath=s;
    }

    /**
     * Returns the list of file extensions
     * that are treated as JSP files.
     *
     * @return The list of extensions
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * Adds the given file extension to the
     * list of extensions handled as JSP files.
     *
     * @param extension The extension to add, e.g. "myasp"
     */
    protected void addExtension(final String extension) {
        if(extension != null) {
            if(extensions == null) {
                extensions = new Vector<String>();
            }

            extensions.add(extension);
        }
    }

    /**
     * Base dir for the webapp. Used to generate class names and resolve
     * includes.
     */
    public void setUriroot( String s ) {
        if (s == null) {
            uriRoot = null;
            return;
        }
        try {
            uriRoot = resolveFile(s).getCanonicalPath();
        } catch( Exception ex ) {
            uriRoot = s;
        }
    }

    /**
     * Parses comma-separated list of JSP files to be processed.  If the argument
     * is null, nothing is done.
     *
     * <p>Each file is interpreted relative to uriroot, unless it is absolute,
     * in which case it must start with uriroot.</p>
     *
     * @param aspFiles Comma-separated list of JSP files to be processed
     */
    public void setAspFiles(final String aspFiles) {
        if(aspFiles == null) {
            return;
        }

        StringTokenizer tok = new StringTokenizer(aspFiles, ",");
        while (tok.hasMoreTokens()) {
            pages.add(tok.nextToken());
        }
    }

    /**
     * Sets the compile flag.
     *
     * @param b Flag value
     */
    public void setCompile( final boolean b ) {
        compile = b;
    }

    /**
     * Sets the verbosity level.  The actual number doesn't
     * matter: if it's greater than zero, the verbose flag will
     * be true.
     *
     * @param level Positive means verbose
     */
    public void setVerbose( final int level ) {
        if (level > 0) {
            verbose = true;
            showSuccess = true;
            listErrors = true;
        }
    }

    public void setValidateXml( boolean b ) {
        org.apache.jasper.xmlparser.ParserUtils.validating=b;
    }

    public void setListErrors( boolean b ) {
        listErrors = b;
    }

    public void setOutputDir( String s ) {
        if( s!= null ) {
            scratchDir = resolveFile(s).getAbsoluteFile();
        } else {
            scratchDir=null;
        }
    }

    /**
     * Sets the package name to be used for the generated servlet classes.
     */
    public void setPackage( String p ) {
        targetPackage=p;
    }

    /**
     * Class name of the generated file ( without package ).
     * Can only be used if a single file is converted.
     * XXX Do we need this feature ?
     */
    public void setClassName( String p ) {
        targetClassName=p;
    }

    /**
     * File where we generate a web.xml fragment with the class definitions.
     */
    public void setWebXmlFragment( String s ) {
        webxmlFile=resolveFile(s).getAbsolutePath();
        webxmlLevel=INC_WEBXML;
    }

    /**
     * File where we generate a complete web.xml with the class definitions.
     */
    public void setWebXml( String s ) {
        webxmlFile=resolveFile(s).getAbsolutePath();
        webxmlLevel=ALL_WEBXML;
    }

    /**
     * Sets the encoding to be used to read and write web.xml files.
     * 
     * <p>
     * If not specified, defaults to the platform default encoding.
     * </p>
     * 
     * @param encoding
     *            Encoding, e.g. "UTF-8".
     */
    public void setWebXmlEncoding(String encoding) {
        webxmlEncoding = encoding;
    }

    /**
     * Sets the option to merge generated web.xml fragment into the
     * WEB-INF/web.xml file of the web application that we were processing.
     * 
     * @param b
     *            <code>true</code> to merge the fragment into the existing
     *            web.xml file of the processed web application
     *            ({uriroot}/WEB-INF/web.xml), <code>false</code> to keep the
     *            generated web.xml fragment
     */
    public void setAddWebXmlMappings(boolean b) {
        addWebXmlMappings = b;
    }

    /**
     * Sets the option that throws an exception in case of a compilation error.
     */
    public void setFailOnError(final boolean b) {
        failOnError = b;
    }

    /**
     * Returns true if an exception will be thrown in case of a compilation
     * error.
     */
    public boolean getFailOnError() {
        return failOnError;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AspConfig getAspConfig() {
        return aspConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TagPluginManager getTagPluginManager() {
        return tagPluginManager;
    }

    /**
     * Adds servlet declaration and mapping for the JSP page servlet to the
     * generated web.xml fragment.
     * 
     * @param file
     *            Context-relative path to the JSP file, e.g.
     *            <code>/index.asp</code>
     * @param clctxt
     *            Compilation context of the servlet
     */
    public void generateWebMapping( String file, AspCompilationContext clctxt )
        throws IOException
    {
        if (log.isDebugEnabled()) {
            log.debug("Generating web mapping for file " + file
                      + " using compilation context " + clctxt);
        }

        String className = clctxt.getServletClassName();
        String packageName = clctxt.getServletPackageName();

        String thisServletName;
        if  ("".equals(packageName)) {
            thisServletName = className;
        } else {
            thisServletName = packageName + '.' + className;
        }

        if (servletout != null) {
            servletout.write("\n    <servlet>\n        <servlet-name>");
            servletout.write(thisServletName);
            servletout.write("</servlet-name>\n        <servlet-class>");
            servletout.write(thisServletName);
            servletout.write("</servlet-class>\n    </servlet>\n");
        }
        if (mappingout != null) {
            mappingout.write("\n    <servlet-mapping>\n        <servlet-name>");
            mappingout.write(thisServletName);
            mappingout.write("</servlet-name>\n        <url-pattern>");
            mappingout.write(file.replace('\\', '/'));
            mappingout.write("</url-pattern>\n    </servlet-mapping>\n");

        }
    }

    /**
     * Include the generated web.xml inside the webapp's web.xml.
     */
    protected void mergeIntoWebXml() throws IOException {

        File webappBase = new File(uriRoot);
        File webXml = new File(webappBase, "WEB-INF/web.xml");
        File webXml2 = new File(webappBase, "WEB-INF/web2.xml");
        String insertStartMarker =
            Localizer.getMessage("aspc.webinc.insertStart");
        String insertEndMarker =
            Localizer.getMessage("aspc.webinc.insertEnd");

        BufferedReader reader = new BufferedReader(openWebxmlReader(webXml));
        BufferedReader fragmentReader = new BufferedReader(
                openWebxmlReader(new File(webxmlFile)));
        PrintWriter writer = new PrintWriter(openWebxmlWriter(webXml2));

        // Insert the <servlet> and <servlet-mapping> declarations
        boolean inserted = false;
        int current = reader.read();
        while (current > -1) {
            if (current == '<') {
                String element = getElement(reader);
                if (!inserted && insertBefore.contains(element)) {
                    // Insert generated content here
                    writer.println(insertStartMarker);
                    while (true) {
                        String line = fragmentReader.readLine();
                        if (line == null) {
                            writer.println();
                            break;
                        }
                        writer.println(line);
                    }
                    writer.println(insertEndMarker);
                    writer.println();
                    writer.write(element);
                    inserted = true;
                } else if (element.equals(insertStartMarker)) {
                    // Skip the previous auto-generated content
                    while (true) {
                        current = reader.read();
                        if (current < 0) {
                            throw new EOFException();
                        }
                        if (current == '<') {
                            element = getElement(reader);
                            if (element.equals(insertEndMarker)) {
                                break;
                            }
                        }
                    }
                    current = reader.read();
                    while (current == '\n' || current == '\r') {
                        current = reader.read();
                    }
                    continue;
                } else {
                    writer.write(element);
                }
            } else {
                writer.write(current);
            }
            current = reader.read();
        }
        writer.close();

        reader.close();
        fragmentReader.close();

        FileInputStream fis = new FileInputStream(webXml2);
        FileOutputStream fos = new FileOutputStream(webXml);

        byte buf[] = new byte[512];
        while (true) {
            int n = fis.read(buf);
            if (n < 0) {
                break;
            }
            fos.write(buf, 0, n);
        }

        fis.close();
        fos.close();

        if(!webXml2.delete() && log.isDebugEnabled())
            log.debug(Localizer.getMessage("aspc.delete.fail",
                    webXml2.toString()));
        
        if (!(new File(webxmlFile)).delete() && log.isDebugEnabled())
            log.debug(Localizer.getMessage("aspc.delete.fail", webxmlFile));

    }
    
    /*
     * Assumes valid xml
     */
    private String getElement(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        result.append('<');
        
        boolean done = false;
        
        while (!done) {
            int current = reader.read();
            while (current != '>') {
                if (current < 0) {
                    throw new EOFException();
                }
                result.append((char) current);
                current = reader.read();
            }
            result.append((char) current);
            
            int len = result.length();
            if (len > 4 && result.substring(0, 4).equals("<!--")) {
                // This is a comment - make sure we are at the end
                if (len >= 7 && result.substring(len - 3, len).equals("-->")) {
                    done = true;
                }
            } else {
                done = true;
            }
        }
        
        
        return result.toString();
    }

    protected void processFile(String file)
        throws JasperException
    {
        if (log.isDebugEnabled()) {
            log.debug("Processing file: " + file);
        }

        ClassLoader originalClassLoader = null;

        try {
            // set up a scratch/output dir if none is provided
            if (scratchDir == null) {
                String temp = System.getProperty("java.io.tmpdir");
                if (temp == null) {
                    temp = "";
                }
                scratchDir = new File(new File(temp).getAbsolutePath());
            }

            String aspUri=file.replace('\\','/');
            AspCompilationContext clctxt = new AspCompilationContext
                ( aspUri, this, context, null, rctxt );

            /* Override the defaults */
            if ((targetClassName != null) && (targetClassName.length() > 0)) {
                clctxt.setServletClassName(targetClassName);
                targetClassName = null;
            }
            if (targetPackage != null) {
                clctxt.setServletPackageName(targetPackage);
            }

            originalClassLoader = Thread.currentThread().getContextClassLoader();
            if( loader==null ) {
                initClassLoader( clctxt );
            }
            Thread.currentThread().setContextClassLoader(loader);

            clctxt.setClassLoader(loader);
            clctxt.setClassPath(classPath);

            Compiler clc = clctxt.createCompiler();

            // If compile is set, generate both .java and .class, if
            // .asp file is newer than .class file;
            // Otherwise only generate .java, if .asp file is newer than
            // the .java file
            if( clc.isOutDated(compile) ) {
                if (log.isDebugEnabled()) {
                    log.debug(aspUri + " is out dated, compiling...");
                }

                clc.compile(compile, true);
            }

            // Generate mapping
            generateWebMapping( file, clctxt );
            if ( showSuccess ) {
                log.info( "Built File: " + file );
            }

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                    && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            if (rootCause != je) {
                log.error(Localizer.getMessage("aspc.error.generalException",
                                               file),
                          rootCause);
            }

            // Bugzilla 35114.
            if(getFailOnError()) {
                throw je;
            } else {
                log.error(je.getMessage());
            }

        } catch (Exception e) {
            if ((e instanceof FileNotFoundException) && log.isWarnEnabled()) {
                log.warn(Localizer.getMessage("aspc.error.fileDoesNotExist",
                                              e.getMessage()));
            }
            throw new JasperException(e);
        } finally {
            if(originalClassLoader != null) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Locate all asp files in the webapp. Used if no explicit
     * asps are specified.
     */
    public void scanFiles( File base ) throws JasperException {
        Stack<String> dirs = new Stack<String>();
        dirs.push(base.toString());

        // Make sure default extensions are always included
        if ((getExtensions() == null) || (getExtensions().size() < 2)) {
            addExtension("asp");
            addExtension("aspx");
        }

        while (!dirs.isEmpty()) {
            String s = dirs.pop();
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                String[] files = f.list();
                String ext;
                for (int i = 0; (files != null) && i < files.length; i++) {
                    File f2 = new File(s, files[i]);
                    if (f2.isDirectory()) {
                        dirs.push(f2.getPath());
                    } else {
                        String path = f2.getPath();
                        String uri = path.substring(uriRoot.length());
                        ext = files[i].substring(files[i].lastIndexOf('.') +1);
                        if (getExtensions().contains(ext) ||
                            aspConfig.isAspPage(uri)) {
                            pages.add(path);
                        }
                    }
                }
            }
        }
    }

    /**
     * Executes the compilation.
     *
     * @throws JasperException If an error occurs
     */
    @Override
    public void execute() {
        if(log.isDebugEnabled()) {
            log.debug("execute() starting for " + pages.size() + " pages.");
        }

        try {
            if (uriRoot == null) {
                if( pages.size() == 0 ) {
                    throw new JasperException(
                        Localizer.getMessage("asp.error.aspc.missingTarget"));
                }
                String firstAsp = pages.get( 0 );
                File firstAspF = new File( firstAsp );
                if (!firstAspF.exists()) {
                    throw new JasperException(
                        Localizer.getMessage("aspc.error.fileDoesNotExist",
                                             firstAsp));
                }
                locateUriRoot( firstAspF );
            }

            if (uriRoot == null) {
                throw new JasperException(
                    Localizer.getMessage("asp.error.aspc.no_uriroot"));
            }

            File uriRootF = new File(uriRoot);
            if (!uriRootF.isDirectory()) {
                throw new JasperException(
                    Localizer.getMessage("asp.error.aspc.uriroot_not_dir"));
            }

            if(context == null) {
                initServletContext();
            }

            // No explicit pages, we'll process all .asp in the webapp
            if (pages.size() == 0) {
                scanFiles(uriRootF);
            }

            initWebXml();

            Iterator<String> iter = pages.iterator();
            while (iter.hasNext()) {
                String nextasp = iter.next().toString();
                File fasp = new File(nextasp);
                if (!fasp.isAbsolute()) {
                    fasp = new File(uriRootF, nextasp);
                }
                if (!fasp.exists()) {
                    if (log.isWarnEnabled()) {
                        log.warn
                            (Localizer.getMessage
                             ("aspc.error.fileDoesNotExist", fasp.toString()));
                    }
                    continue;
                }
                String s = fasp.getAbsolutePath();
                if (s.startsWith(uriRoot)) {
                    nextasp = s.substring(uriRoot.length());
                }
                if (nextasp.startsWith("." + File.separatorChar)) {
                    nextasp = nextasp.substring(2);
                }
                processFile(nextasp);
            }

            completeWebXml();

            if (addWebXmlMappings) {
                mergeIntoWebXml();
            }

        } catch (IOException ioe) {
            throw new BuildException(ioe);

        } catch (JasperException je) {
            Throwable rootCause = je;
            while (rootCause instanceof JasperException
                    && ((JasperException) rootCause).getRootCause() != null) {
                rootCause = ((JasperException) rootCause).getRootCause();
            }
            if (rootCause != je) {
                rootCause.printStackTrace();
            }
            throw new BuildException(je);
        } finally {
            if (loader != null) {
                LogFactory.release(loader);
            }
        }
    }

    // ==================== protected utility methods ====================

    protected String nextArg() {
        if ((argPos >= args.length)
            || (fullstop = SWITCH_FULL_STOP.equals(args[argPos]))) {
            return null;
        } else {
            return args[argPos++];
        }
    }

    protected String nextFile() {
        if (fullstop) argPos++;
        if (argPos >= args.length) {
            return null;
        } else {
            return args[argPos++];
        }
    }

    protected void initWebXml() {
        try {
            if (webxmlLevel >= INC_WEBXML) {
                mapout = openWebxmlWriter(new File(webxmlFile));
                servletout = new CharArrayWriter();
                mappingout = new CharArrayWriter();
            } else {
                mapout = null;
                servletout = null;
                mappingout = null;
            }
            if (webxmlLevel >= ALL_WEBXML) {
                mapout.write(Localizer.getMessage("aspc.webxml.header"));
                mapout.flush();
            } else if ((webxmlLevel>= INC_WEBXML) && !addWebXmlMappings) {
                mapout.write(Localizer.getMessage("aspc.webinc.header"));
                mapout.flush();
            }
        } catch (IOException ioe) {
            mapout = null;
            servletout = null;
            mappingout = null;
        }
    }

    protected void completeWebXml() {
        if (mapout != null) {
            try {
                servletout.writeTo(mapout);
                mappingout.writeTo(mapout);
                if (webxmlLevel >= ALL_WEBXML) {
                    mapout.write(Localizer.getMessage("aspc.webxml.footer"));
                } else if ((webxmlLevel >= INC_WEBXML) && !addWebXmlMappings) {
                    mapout.write(Localizer.getMessage("aspc.webinc.footer"));
                }
                mapout.close();
            } catch (IOException ioe) {
                // noting to do if it fails since we are done with it
            }
        }
    }

    protected void initServletContext() {
        try {
            context =new AspCServletContext
                (new PrintWriter(System.out),
                 new URL("file:" + uriRoot.replace('\\','/') + '/'));
            tldLocationsCache = TldLocationsCache.getInstance(context);
        } catch (MalformedURLException me) {
            System.out.println("**" + me);
        }
        rctxt = new AspRuntimeContext(context, this);
        aspConfig = new AspConfig(context);
        tagPluginManager = new TagPluginManager(context);
    }

    /**
     * Initializes the classloader as/if needed for the given
     * compilation context.
     *
     * @param clctxt The compilation context
     * @throws IOException If an error occurs
     */
    protected void initClassLoader(AspCompilationContext clctxt)
        throws IOException {

        classPath = getClassPath();

        ClassLoader aspcLoader = getClass().getClassLoader();
        if (aspcLoader instanceof AntClassLoader) {
            classPath += File.pathSeparator
                + ((AntClassLoader) aspcLoader).getClasspath();
        }

        // Turn the classPath into URLs
        ArrayList<URL> urls = new ArrayList<URL>();
        StringTokenizer tokenizer = new StringTokenizer(classPath,
                                                        File.pathSeparator);
        while (tokenizer.hasMoreTokens()) {
            String path = tokenizer.nextToken();
            try {
                File libFile = new File(path);
                urls.add(libFile.toURI().toURL());
            } catch (IOException ioe) {
                // Failing a toCanonicalPath on a file that
                // exists() should be a JVM regression test,
                // therefore we have permission to freak uot
                throw new RuntimeException(ioe.toString());
            }
        }

        File webappBase = new File(uriRoot);
        if (webappBase.exists()) {
            File classes = new File(webappBase, "/WEB-INF/classes");
            try {
                if (classes.exists()) {
                    classPath = classPath + File.pathSeparator
                        + classes.getCanonicalPath();
                    urls.add(classes.getCanonicalFile().toURI().toURL());
                }
            } catch (IOException ioe) {
                // failing a toCanonicalPath on a file that
                // exists() should be a JVM regression test,
                // therefore we have permission to freak out
                throw new RuntimeException(ioe.toString());
            }
            File lib = new File(webappBase, "/WEB-INF/lib");
            if (lib.exists() && lib.isDirectory()) {
                String[] libs = lib.list();
                for (int i = 0; i < libs.length; i++) {
                    if( libs[i].length() <5 ) continue;
                    String ext=libs[i].substring( libs[i].length() - 4 );
                    if (! ".jar".equalsIgnoreCase(ext)) {
                        if (".tld".equalsIgnoreCase(ext)) {
                            log.warn("TLD files should not be placed in "
                                     + "/WEB-INF/lib");
                        }
                        continue;
                    }
                    try {
                        File libFile = new File(lib, libs[i]);
                        classPath = classPath + File.pathSeparator
                            + libFile.getAbsolutePath();
                        urls.add(libFile.getAbsoluteFile().toURI().toURL());
                    } catch (IOException ioe) {
                        // failing a toCanonicalPath on a file that
                        // exists() should be a JVM regression test,
                        // therefore we have permission to freak out
                        throw new RuntimeException(ioe.toString());
                    }
                }
            }
        }

        // What is this ??
        urls.add(new File(
                clctxt.getRealPath("/")).getCanonicalFile().toURI().toURL());

        URL urlsA[]=new URL[urls.size()];
        urls.toArray(urlsA);
        loader = new URLClassLoader(urlsA, this.getClass().getClassLoader());

    }

    /**
     * Find the WEB-INF dir by looking up in the directory tree.
     * This is used if no explicit docbase is set, but only files.
     * XXX Maybe we should require the docbase.
     */
    protected void locateUriRoot( File f ) {
        String tUriBase = uriBase;
        if (tUriBase == null) {
            tUriBase = "/";
        }
        try {
            if (f.exists()) {
                f = new File(f.getAbsolutePath());
                while (true) {
                    File g = new File(f, "WEB-INF");
                    if (g.exists() && g.isDirectory()) {
                        uriRoot = f.getCanonicalPath();
                        uriBase = tUriBase;
                        if (log.isInfoEnabled()) {
                            log.info(Localizer.getMessage(
                                        "aspc.implicit.uriRoot",
                                        uriRoot));
                        }
                        break;
                    }
                    if (f.exists() && f.isDirectory()) {
                        tUriBase = "/" + f.getName() + "/" + tUriBase;
                    }

                    String fParent = f.getParent();
                    if (fParent == null) {
                        break;
                    } else {
                        f = new File(fParent);
                    }

                    // If there is no acceptable candidate, uriRoot will
                    // remain null to indicate to the CompilerContext to
                    // use the current working/user dir.
                }

                if (uriRoot != null) {
                    File froot = new File(uriRoot);
                    uriRoot = froot.getCanonicalPath();
                }
            }
        } catch (IOException ioe) {
            // since this is an optional default and a null value
            // for uriRoot has a non-error meaning, we can just
            // pass straight through
        }
    }

    /**
     * Resolves the relative or absolute pathname correctly
     * in both Ant and command-line situations.  If Ant launched
     * us, we should use the basedir of the current project
     * to resolve relative paths.
     *
     * See Bugzilla 35571.
     *
     * @param s The file
     * @return The file resolved
     */
     protected File resolveFile(final String s) {
         if(getProject() == null) {
             // Note FileUtils.getFileUtils replaces FileUtils.newFileUtils in Ant 1.6.3
             return FileUtils.getFileUtils().resolveFile(null, s);
         } else {
             return FileUtils.getFileUtils().resolveFile(getProject().getBaseDir(), s);
         }
     }

    private Reader openWebxmlReader(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            return webxmlEncoding != null ? new InputStreamReader(fis,
                    webxmlEncoding) : new InputStreamReader(fis);
        } catch (IOException ex) {
            fis.close();
            throw ex;
        }
    }

    private Writer openWebxmlWriter(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            return webxmlEncoding != null ? new OutputStreamWriter(fos,
                    webxmlEncoding) : new OutputStreamWriter(fos);
        } catch (IOException ex) {
            fos.close();
            throw ex;
        }
    }
}

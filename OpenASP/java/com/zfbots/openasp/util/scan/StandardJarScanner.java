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

package com.zfbots.openasp.util.scan;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import com.zfbots.openasp.JarScanner;
import com.zfbots.openasp.JarScannerCallback;
import com.zfbots.openasp.util.file.Matcher;
import com.zfbots.openasp.util.res.StringManager;

/**
 * The default {@link JarScanner} implementation scans the WEB-INF/lib directory
 * followed by the provided classloader and then works up the classloader
 * hierarchy. This implementation is sufficient to meet the requirements of the
 * Servlet 3.0 specification as well as to provide a number of Tomcat specific
 * extensions. The extensions are:
 * <ul>
 *   <li>Scanning the classloader hierarchy (enabled by default)</li>
 *   <li>Testing all files to see if they are JARs (disabled by default)</li>
 *   <li>Testing all directories to see if they are exploded JARs
 *       (disabled by default)</li>
 * </ul>
 * All of the extensions may be controlled via configuration.
 */
public class StandardJarScanner implements JarScanner {

    private static final Log log = LogFactory.getLog(StandardJarScanner.class);

    private static final Set<String> defaultJarsToSkip = new HashSet<String>();

    /**
     * The string resources for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);

    static {
        String jarList = System.getProperty(Constants.SKIP_JARS_PROPERTY);
        if (jarList != null) {
            StringTokenizer tokenizer = new StringTokenizer(jarList, ",");
            while (tokenizer.hasMoreElements()) {
                defaultJarsToSkip.add(tokenizer.nextToken());
            }
        }
    }

    /**
     * Controls the classpath scanning extension.
     */
    private boolean scanClassPath = true;
    public boolean isScanClassPath() {
        return scanClassPath;
    }
    public void setScanClassPath(boolean scanClassPath) {
        this.scanClassPath = scanClassPath;
    }

    /**
     * Controls the testing all files to see of they are JAR files extension.
     */
    private boolean scanAllFiles = false;
    public boolean isScanAllFiles() {
        return scanAllFiles;
    }
    public void setScanAllFiles(boolean scanAllFiles) {
        this.scanAllFiles = scanAllFiles;
    }

    /**
     * Controls the testing all directories to see of they are exploded JAR
     * files extension.
     */
    private boolean scanAllDirectories = false;
    public boolean isScanAllDirectories() {
        return scanAllDirectories;
    }
    public void setScanAllDirectories(boolean scanAllDirectories) {
        this.scanAllDirectories = scanAllDirectories;
    }

    /**
     * Scan the provided ServletContext and classloader for JAR files. Each JAR
     * file found will be passed to the callback handler to be processed.
     *
     * @param context       The ServletContext - used to locate and access
     *                      WEB-INF/lib
     * @param classloader   The classloader - used to access JARs not in
     *                      WEB-INF/lib
     * @param callback      The handler to process any JARs found
     * @param jarsToSkip    List of JARs to ignore. If this list is null, a
     *                      default list will be read from the system property
     *                      defined by {@link Constants#SKIP_JARS_PROPERTY}
     */
    @Override
    public void scan(ServletContext context, ClassLoader classloader,
            JarScannerCallback callback, Set<String> jarsToSkip) {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("jarScan.webinflibStart"));
        }

        Set<String> ignoredJars;
        if (jarsToSkip == null) {
            ignoredJars = defaultJarsToSkip;
        } else {
            ignoredJars = jarsToSkip;
        }
        Set<String[]> ignoredJarsTokens = new HashSet<String[]>();
        for (String pattern: ignoredJars) {
            ignoredJarsTokens.add(Matcher.tokenizePathAsArray(pattern));
        }

        // Scan WEB-INF/lib
        Set<String> dirList = context.getResourcePaths(Constants.WEB_INF_LIB);
        if (dirList != null) {
            Iterator<String> it = dirList.iterator();
            while (it.hasNext()) {
                String path = it.next();
                if (path.endsWith(Constants.JAR_EXT) &&
                    !Matcher.matchPath(ignoredJarsTokens,
                        path.substring(path.lastIndexOf('/')+1))) {
                    // Need to scan this JAR
                    if (log.isDebugEnabled()) {
                        log.debug(sm.getString("jarScan.webinflibJarScan", path));
                    }
                    URL url = null;
                    try {
                        // File URLs are always faster to work with so use them
                        // if available.
                        String realPath = context.getRealPath(path);
                        if (realPath == null) {
                            url = context.getResource(path);
                        } else {
                            url = (new File(realPath)).toURI().toURL();
                        }
                        process(callback, url);
                    } catch (IOException e) {
                        log.warn(sm.getString("jarScan.webinflibFail", url), e);
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace(sm.getString("jarScan.webinflibJarNoScan", path));
                    }
                }
            }
        }

        // Scan the classpath
        if (scanClassPath) {
            if (log.isTraceEnabled()) {
                log.trace(sm.getString("jarScan.classloaderStart"));
            }

            ClassLoader loader =
                Thread.currentThread().getContextClassLoader();

            while (loader != null) {
                if (loader instanceof URLClassLoader) {
                    URL[] urls = ((URLClassLoader) loader).getURLs();
                    for (int i=0; i<urls.length; i++) {
                        // Extract the jarName if there is one to be found
                        String jarName = getJarName(urls[i]);

                        // Skip JARs known not to be interesting and JARs
                        // in WEB-INF/lib we have already scanned
                        if (jarName != null &&
                            !(Matcher.matchPath(ignoredJarsTokens, jarName) ||
                                urls[i].toString().contains(
                                        Constants.WEB_INF_LIB + jarName))) {
                            if (log.isDebugEnabled()) {
                                log.debug(sm.getString("jarScan.classloaderJarScan", urls[i]));
                            }
                            try {
                                process(callback, urls[i]);
                            } catch (IOException ioe) {
                                log.warn(sm.getString(
                                        "jarScan.classloaderFail",urls[i]), ioe);
                            }
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace(sm.getString("jarScan.classloaderJarNoScan", urls[i]));
                            }
                        }
                    }
                }
                loader = loader.getParent();
            }

        }
    }

    /*
     * Scan a URL for JARs with the optional extensions to look at all files
     * and all directories.
     */
    private void process(JarScannerCallback callback, URL url)
            throws IOException {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("jarScan.jarUrlStart", url));
        }

        URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
            callback.scan((JarURLConnection) conn);
        } else {
            String urlStr = url.toString();
            if (urlStr.startsWith("file:") || urlStr.startsWith("jndi:")) {
                if (urlStr.endsWith(Constants.JAR_EXT)) {
                    URL jarURL = new URL("jar:" + urlStr + "!/");
                    callback.scan((JarURLConnection) jarURL.openConnection());
                } else {
                    File f;
                    try {
                        f = new File(url.toURI());
                        if (f.isFile() && scanAllFiles) {
                            // Treat this file as a JAR
                            URL jarURL = new URL("jar:" + urlStr + "!/");
                            callback.scan((JarURLConnection) jarURL.openConnection());
                        } else if (f.isDirectory() && scanAllDirectories) {
                            File metainf = new File(f.getAbsoluteFile() +
                                    File.separator + "META-INF");
                            if (metainf.isDirectory()) {
                                callback.scan(f);
                            }
                        }
                    } catch (URISyntaxException e) {
                        // Wrap the exception and re-throw
                        IOException ioe = new IOException();
                        ioe.initCause(e);
                        throw ioe;
                    }
                }
            }
        }

    }

    /*
     * Extract the JAR name, if present, from a URL
     */
    private String getJarName(URL url) {

        String name = null;

        String path = url.getPath();
        int end = path.indexOf(Constants.JAR_EXT);
        if (end != -1) {
            int start = path.lastIndexOf('/', end);
            name = path.substring(start + 1, end + 4);
        } else if (isScanAllDirectories()){
            int start = path.lastIndexOf('/');
            name = path.substring(start + 1);
        }

        return name;
    }

}

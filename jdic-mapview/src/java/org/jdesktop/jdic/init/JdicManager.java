/*
 * Copyright (C) 2004 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.jdesktop.jdic.init;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.jdesktop.jdic.browser.internal.WebBrowserUtil;

import com.sun.jnlp.JNLPClassLoader;
import slash.common.util.Externalization;

/**
 * Initialization manager for JDIC to set the environment variables or
 * initialize the set up for native libraries and executable files.
 * <p>
 * There are 3 modes of operation: WebStart, file system, and .jar file.
 * <p>
 * When using WebStart, please specify a .jar file(jdic-native.jar) with the
 * native libraries for your platform to be loaded by WebStart in your JNPL.
 * This class will find the unjared native libraries and executables, and use
 * them directly.
 * <p>
 * If not in WebStart, the system will expect the native libraries to be located
 * in directory at the root of the classpath or .jar containing this class.
 * 
 * @author Michael Samblanet
 * @author Paul Huang
 * @author George Zhang
 * @author Michael Shan
 * @since July 29, 2004
 */
public class JdicManager {
    private boolean isShareNativeInitialized = false;
    private boolean isBrowserNativeInitialized = false;    

    /** If the current platform is Windows */
    boolean isWindows = 
        (System.getProperty("os.name").indexOf("Windows") >= 0) ? 
        true : false;
    
    /** The environment variable for library path setting */
    String libPathEnv = isWindows ? "PATH" : "LD_LIBRARY_PATH";

    /** The path for the JDIC native files (jdic.dll/libjdic.so, etc) */
    String nativeLibPath = null;

    /** Singleton instance of this class */
    private static JdicManager sSingleton = null;

    /**
     * Private constructor to prevent public construction.
     */
    private JdicManager() {
    }

    /**
     * Returns a singleton instance of <code>JdicManager</code>.
     */
    public static synchronized JdicManager getManager() {
        if (sSingleton == null) {
            sSingleton = new JdicManager();
        }
        return sSingleton;
    }

    /**
     * Initializes the shared native file settings for all the JDIC components/
     * packages. Set necessary environment variables for the shared native
     * library and executable files, including *.dll files on Windows, and *.so
     * files on Unix.
     * 
     * @exception JdicInitException Generic initialization exception
     */
    public void initShareNative() throws JdicInitException {
        WebBrowserUtil.trace("init share native..... ");
        // If the shared native file setting was already initialized,
        // just return.
        if (isShareNativeInitialized) {
             return;
        }

        try {
            // Find the root path of this class.
            String jwsVersion = System.getProperty("javawebstart.version");
            if (false && jwsVersion != null) {
                //loaded by JWS
                WebBrowserUtil.trace("Loaded by JavaWebStart,version is "
                                + jwsVersion);
                //native libs will be loaded by webstart automatically
                nativeLibPath = caculateNativeLibPathBySunJWS();
                WebBrowserUtil.trace("caculateNativeLibPathBySunJWS " + nativeLibPath);
                return;
            } else {
                /* RouteConverter: modified to work out of the temp directory
                // running url of current class
                String runningURL =(
                    new URL(JdicManager.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation(),
                        ".")
                ).openConnection().getPermission().getName();

                //running path of current class
                String runningPath = (new File(runningURL)).getCanonicalPath();
                */
                String runningPath = Externalization.getTempDirectory().getAbsolutePath();
                nativeLibPath = caculateNativeLibPath(runningPath);
                WebBrowserUtil.trace("caculateNativeLibPath " + nativeLibPath);

                // Add the binary path (including jdic.dll or libjdic.so) to
                // "java.library.path", since we need to use the native methods
                // in class InitUtility.
                String newLibPath = nativeLibPath + File.pathSeparator
                    + System.getProperty("java.library.path");

                System.setProperty("java.library.path", newLibPath);
                Field fieldSysPath = ClassLoader.class
                    .getDeclaredField("sys_paths");

                fieldSysPath.setAccessible(true);
                if (fieldSysPath != null) {
                    fieldSysPath.set(System.class.getClassLoader(), null);
                }
            }
        } catch (Throwable e) {
            throw new JdicInitException(e);
        }
        isShareNativeInitialized = true;
    }

    /**
     * To keep the using of crossplatform version of JDIC
     * 
     * @throws MalformedURLException
     * @throws JdicInitException
     *  
     */
    private String caculateNativeLibPath(String runningPath)
        throws MalformedURLException, JdicInitException
    {
        String platformPath = runningPath + File.separator + getPlatform();
        File jdicStubJarFile = new File(platformPath + File.separator
            + "jdic_stub.jar");
        if (!jdicStubJarFile.exists()) {
            //not cross platform version
            return runningPath;
        } else {
            //cross platform version
            String architecturePath = platformPath + File.separator
                + getArchitecture();
            ClassLoader cl = getClass().getClassLoader();
            if (!(cl instanceof URLClassLoader)) {
                //not URLClassLoader,omit it,in case the stub jar has been
                // set to claspath
                String exceptionInfo =
                    "We detect that you are not using java.net.URLClassLoader for cross platform version, "
                    + "you have to set jdic_stub.jar manually!";
                WebBrowserUtil.error(exceptionInfo);
                return architecturePath;//return the native lib path
            }
                    
            //set stub jars to classpath
            URLClassLoader urlCl = (URLClassLoader) cl;
            try {
                Method addURLMethod = URLClassLoader.class.getDeclaredMethod(
                    "addURL",
                    new Class[] { URL.class });
                addURLMethod.setAccessible(true);
                addURLMethod.invoke(
                    urlCl,
                    new Object[] { jdicStubJarFile.toURL() } );
                return architecturePath;//return the native lib path
            } catch (Throwable t) {
                t.printStackTrace();
                throw new JdicInitException(
                    "Error, could not add URL to system classloader");
            }
        }
    }

    /**
     * Initialize native libs' running path if loaded by webstart.This method
     * only works for sun webstart implementaion,for other webstart
     * implementations, you have to rewrite this method.
     * 
     * @throws IOException
     * @throws JdicInitException
     */
    private String caculateNativeLibPathBySunJWS() throws IOException,
                    JdicInitException
    {
        String jdicLibFolder = null;
        ClassLoader cl = this.getClass().getClassLoader();
        if (cl instanceof JNLPClassLoader) {
            // Initialize native libs' running path if loaded by webstart.This method
            // only works for sun webstart implementaion,for other webstart
            // implementations, you have to rewrite this method.
            jdicLibFolder = (new File(
                JNLPClassLoaderAccessor.findLibrary(
                    (JNLPClassLoader) cl, 
                    "jdic")
            )).getParentFile().getCanonicalPath();
            WebBrowserUtil.trace("running path " + nativeLibPath);
            isShareNativeInitialized = true;
        } else {
            // only run well for sun jre
            throw new JdicInitException(
                "Unexpected ClassLoader for webstart, only com.sun.jnlp.JNLPClassLoader is supported.");
        }
        return jdicLibFolder;
    }

    /**
     * Return the canonical name of the platform. This value is derived from the
     * System property os.name.
     * 
     * @return The platform string.
     */
    private static String getPlatform() {
        // See list of os names at: http://lopica.sourceforge.net/os.html
        // or at: http://www.tolstoy.com/samizdat/sysprops.html
        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")) {
                return "windows";
        }
        return canonical(osname);
    }

    /**
     * Return the name of the architecture. This value is determined by the
     * System property os.arch.
     * 
     * @return The architecture string.
     */
    private static String getArchitecture() {
        String arch = System.getProperty("os.arch");
        if (arch.endsWith("86")) {
                return "x86";
        }
        return canonical(arch);
    }

    /**
     * @param value
     *            The value to be canonicalized.
     * @return The value with all '/', '\' and ' ' replaced with '_', and all
     *         uppercase characters replaced with lower case equivalents.
     */
    private static String canonical(String value) {
        WebBrowserUtil.trace("value:" + value);
        WebBrowserUtil.trace("canonical:"
                        + value.toLowerCase().replaceAll("[\\\\/ ]", "_"));
        return value.toLowerCase().replaceAll("[\\\\/ ]", "_");
    }

    /**
     * Initializes the native file settings for the JDIC Browser component 
     * (package <code>org.jdecktop.jdic.browser</code>). Set necessary 
     * environment variables for the Browser specific native library and 
     * executable files, including *.exe files on Windows, and mozembed-<os>-gtk* 
     * files on Unix.
     * 
     * @exception JdicInitException Generic initialization exception
     */
    public void initBrowserNative() throws JdicInitException {
        // The Browser component is used.
        // If the Browser specific native file setting was already initialized, 
        // just return.
        if (isBrowserNativeInitialized) {
            return;
        }

        try {
            // Pre-append the JDIC binary path to PATH(on Windows) or 
            // LD_LIBRARY_PATH(on Unix).         
            String binaryPath = getBinaryPath();            
            InitUtility.preAppendEnv(libPathEnv, binaryPath); 

            String browserPath = WebBrowserUtil.getBrowserPath();
            if (browserPath == null) {
                throw new JdicInitException(
                    "Can't locate the native browser path!");
            }

            // RouteConverter: modified to avoid exceptions due to FF3 in c:\programme\mozilla firefox\firefox.exe as default browser
            if (WebBrowserUtil.isDefaultBrowserMozilla() && WebBrowserUtil.getBrowserPath().indexOf("firefox") == -1) {
                // Mozilla is the default/embedded browser.
                // Use the user defined value or the mozilla binary
                // path as the value of MOZILLA_FIVE_HOME env variable.
                String envMFH = InitUtility.getEnv("MOZILLA_FIVE_HOME");
                if (envMFH == null) {
                    File browserFile = new File(browserPath);
                    if (browserFile.isDirectory()) {
                        envMFH = browserFile.getCanonicalPath();
                    } else {
                        envMFH = browserFile.getCanonicalFile().getParent();
                    }                    
                }
                
                if (!isWindows) {
                    // On Unix, add the binary path to PATH.
                    InitUtility.preAppendEnv("PATH", binaryPath);
                    // When running on webstart, the browser binary will lose
                    // "x" permission after extracted from .jar file.
                    String embedBinary = WebBrowserUtil.getEmbedBinaryName();
                    Runtime.getRuntime().exec("chmod a+x "+ 
                            binaryPath + File.separator + embedBinary);                    
                } else {               
                    // Mozilla on Windows, reset MOZILLA_FIVE_HOME to the GRE 
                    // directory path:  
                    //   [Common Files]\mozilla.org\GRE\1.x_BUILDID, 
                    // if Mozilla installs from a .exe package.
                    //                
                    String xpcomPath = envMFH + File.separator + "xpcom.dll";                        
                    if (!(new File(xpcomPath).isFile())) {
                        // Mozilla installs from a .exe package. Check the 
                        // installed GRE directory.
                        String mozGreHome 
                            = WebBrowserUtil.getMozillaGreHome();
                        if (mozGreHome == null) {
                            throw new JdicInitException(
                                "Can't locate the GRE directory of the " +
                                "installed Mozilla binary: " + envMFH);
                        }                       
                        envMFH = mozGreHome;
                    }
                }              

                InitUtility.setEnv("MOZILLA_FIVE_HOME", envMFH);
                InitUtility.preAppendEnv(libPathEnv, envMFH);
            } // end - Mozilla is the default/embedded browser.
        } catch (Throwable e) {
            throw new JdicInitException(e);
        }
        
        isBrowserNativeInitialized = true;
    }

    public String getBinaryPath() {
        //WebBrowserUtil.trace("native lib path " + nativeLibPath);
        return nativeLibPath;
    }
}

// RouteConverter: copied from JDIC 0.9.5 since com.sun.jnlp.JNLPClassLoader#findLibrary is protected now
class JNLPClassLoaderAccessor {
    static java.lang.reflect.Method mdJNLPClassLoader_findLibrary = null;

    static {
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            public Object run() {
                try {
                    mdJNLPClassLoader_findLibrary = Class
                            .forName("com.sun.jnlp.JNLPClassLoader")
                            .getDeclaredMethod(
                                    "findLibrary",
                                    new Class[]{String.class});
                    mdJNLPClassLoader_findLibrary.setAccessible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // to please javac
                return null;
            }
        });
    }

    public static String findLibrary(JNLPClassLoader o, String name) {
        try {
            return (String) mdJNLPClassLoader_findLibrary.invoke(o, new Object[]{name});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

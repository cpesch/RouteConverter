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
package org.jdesktop.jdic.browser.internal;

import java.io.File;

/**
 * Utility class for <code>WebBrowser</code> class.
 */
public class WebBrowserUtil {
    // Native browser embedding binary: IeEmbed.exe or MozEmbed.exe on Windows,
    // mozembed-<os>-gtk<version> on Linux/Unix. Which runs as a standalone
    // native instance.
    private static final String EMBED_BINARY_WINDOWS_IE
        = "IeEmbed.exe";
    private static final String EMBED_BINARY_WINDOWS_MOZILLA
        = "MozEmbed.exe";
    private static final String EMBED_BINARY_LINUX_GTK1
        = "mozembed-linux-gtk1.2";
    private static final String EMBED_BINARY_LINUX_GTK2
        = "mozembed-linux-gtk2";
    private static final String EMBED_BINARY_FREEBSD_GTK1
        = "mozembed-freebsd-gtk1.2";
    private static final String EMBED_BINARY_FREEBSD_GTK2
        = "mozembed-freebsd-gtk2";
    private static final String EMBED_BINARY_SOLARIS_GTK1
        = "mozembed-solaris-gtk1.2";
    private static final String EMBED_BINARY_SOLARIS_GTK2
        = "mozembed-solaris-gtk2";

    private static String embedBinary;

    private static String browserPath = null;

    /* native functions */
    private static native String nativeGetBrowserPath();
    private static native String nativeGetMozillaGreHome();

    // Flag to enable or disable debug message output.
    private static boolean isDebugOn = false;


    /**
     * Returns the name of the native browser embedding binary. If no default
     * browser is set, null is returned.
     */
    public static String getEmbedBinaryName() {
        if (embedBinary != null && embedBinary.length() > 0)
            return embedBinary;

        String nativePath = WebBrowserUtil.getBrowserPath();
        if (null == nativePath) {
            trace("No default browser is found. " +
                    "Or environment variable MOZILLA_FIVE_HOME is not set to " +
                    "a Mozilla binary path if you are on Linux/Unix platform.");
            return null;
        }

        String osname = System.getProperty("os.name");
        if (osname.indexOf("Windows") >= 0) {
            String windowspath = nativePath;
            int index = windowspath.indexOf("mozilla.exe");
            if (index >= 0)
                embedBinary = EMBED_BINARY_WINDOWS_MOZILLA;
            else
                embedBinary = EMBED_BINARY_WINDOWS_IE;
        }
        else {
            String libwidgetpath = nativePath + File.separator +
                                   "components" + File.separator +
                                   "libwidget_gtk2.so";
            File file = new File(libwidgetpath);
            if (!file.exists()) {
                if (osname.indexOf("Linux") >= 0) {
                    embedBinary = EMBED_BINARY_LINUX_GTK1;
                }
                else if (osname.indexOf("SunOS") >= 0) {
                    embedBinary = EMBED_BINARY_SOLARIS_GTK1;
                }
                else if (osname.indexOf("FreeBSD") >= 0) {
                    embedBinary = EMBED_BINARY_FREEBSD_GTK1;
                }
            }
            else {
                if (osname.indexOf("Linux") >= 0) {
                    embedBinary = EMBED_BINARY_LINUX_GTK2;
                }
                else if (osname.indexOf("SunOS") >= 0) {
                    embedBinary = EMBED_BINARY_SOLARIS_GTK2;
                }
                else if (osname.indexOf("FreeBSD") >= 0) {
                    embedBinary = EMBED_BINARY_FREEBSD_GTK2;
                }
            }
        }

        return embedBinary;
    }

    /**
     *  Gets the native browser path.
     *  @return the path of the default browser in the current system
     */
    public static String getBrowserPath() {
        if (browserPath == null) {
            browserPath = nativeGetBrowserPath();
        }
        return browserPath;
    }

    /**
     * Checks if the default browser for the current platform is Mozilla.
     * @return true on Solaris and Linux and true on Windows platform if Mozilla
     * is set as the default browser.
     */
    public static boolean isDefaultBrowserMozilla() {
        // TODO cpe: disabled
        // System.err.println("isDefaultBrowserMozilla");
        String osName = System.getProperty("os.name").toLowerCase();

        if ((osName.indexOf("solaris") >= 0) ||
            (osName.indexOf("linux") >= 0) ) {
            return true;
        // TODO cpe: due to spaces in the file name always use IE
        } else if (osName.indexOf("windows") >= 0) {
            return false;
        } else {
            String nativeBrowserPath = getBrowserPath();
            // Only when Mozilla is set as the default browser, return true.
            // Or else, fall back to Internet Explorer.
            // FireFox 1.0 is statically linked into Gecko and therefore can not
            // be embedded. If FireFox is embeddable for some future version,
            // we would have to explicitly check for both Mozilla and FireFox.
            if (nativeBrowserPath.indexOf("mozilla") >= 0) {
            	return true;
            } else {
                return false;
            }
        }
    }

    /**
     *  Gets the native Mozilla GRE home directory installed with a .exe package.
     *  @return the GRE home directory of the currently installed Mozilla.
     */
    public static String getMozillaGreHome() {
        return nativeGetMozillaGreHome();
    }

    public static void enableDebugMessages(boolean b) {
        isDebugOn = b;
    }

    /**
     * Helper method to output given debug message.
     *
     * @param msg the given debug message.
     */
    public static void trace(String msg) {
        if (isDebugOn)
            System.out.println("*** Jtrace: " + msg);
    }
    public static void error(String msg) {
        System.err.println("*** Error: " + msg);
    }

    /*
     * Sets native environment variables for running native browser binary.
     */
    public static native void nativeSetEnv();
}

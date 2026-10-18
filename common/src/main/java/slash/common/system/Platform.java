/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.common.system;

import java.util.Locale;

/**
 * Provides platform detection functionality.
 *
 * @author Christian Pesch
 */

public class Platform {
    /**
     * Set by the launchers of the packages that bring their own Java: the Mac .app,
     * the Windows .exe, the Linux .deb and the Flatpak.
     */
    public static final String BUNDLED_JRE_PROPERTY = "routeconverter.bundledJre";

    public static boolean isLinux() {
        return getOperationSystem().contains("linux");
    }

    public static boolean isMac() {
        return getOperationSystem().contains("mac");
    }

    public static boolean isWindows() {
        return getOperationSystem().contains("windows");
    }

    public static String getPlatform() {
        return System.getProperty("os.name") + " " +
                System.getProperty("os.version") + " " +
                System.getProperty("os.arch");
    }

    static boolean isJavaLaterThan(String javaVersion, int version) {
        String versionString = version < 9 ? "1." + version : Integer.toString(version);
        return new Version(javaVersion).isLaterOrSameVersionThan(new Version(versionString));
    }

    public static String getJava() {
        return System.getProperty("java.vendor") + " Java " + System.getProperty("java.version") + " (" + getBits() + "-bit)";
    }

    public static String getBits() {
        return System.getProperty("sun.arch.data.model");
    }

    public static boolean isCurrentAtLeastMinimumVersion(String currentVersion, String minimumVersion) {
        return currentVersion.compareTo(minimumVersion) >= 0;
    }

    public static boolean isJava17OrLater() {
        return isJavaLaterThan(System.getProperty("java.version"), 17);
    }

    private static String canonical(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[\\\\/ ]", "_");
    }

    public static String getOperationSystem() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows"))
            return "windows";
        return canonical(osName);
    }

    public static String getArchitecture() {
        String osArch = System.getProperty("os.arch");
        if (osArch.endsWith("86"))
            return "x86";
        return canonical(osArch);
    }

    /**
     * Whether RouteConverter runs on the Java its package brings along, not on one the
     * user installed. jpackage.app-path (set by the .deb's launcher) and FLATPAK_ID (set
     * in every Flatpak sandbox) also cover packages whose launcher predates the property.
     */
    public static boolean isBundledJava() {
        return isBundledJava(System.getProperty(BUNDLED_JRE_PROPERTY),
                System.getProperty("jpackage.app-path"), System.getenv("FLATPAK_ID"));
    }

    static boolean isBundledJava(String bundledJreProperty, String jpackageAppPath, String flatpakId) {
        return Boolean.parseBoolean(bundledJreProperty) || jpackageAppPath != null || flatpakId != null;
    }

    public static long getMaximumMemory() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1000 /* to get 2^n numbers */);
    }
}

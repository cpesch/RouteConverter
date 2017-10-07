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

/**
 * Provides platform detection functionality.
 *
 * @author Christian Pesch
 */

public class Platform {
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

    public static boolean isJavaFX7() {
        return System.getProperty("java.vendor").contains("Oracle") &&
                System.getProperty("java.version").compareTo("1.7.0_40") >= 0;
    }

    public static boolean isJavaFX8() {
        return System.getProperty("java.vendor").contains("Oracle") &&
                System.getProperty("java.version").compareTo("1.8.0") >= 0;
    }

    public static boolean isJava9() {
        return System.getProperty("java.version").compareTo("1.9.0") >= 0;
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

    private static String canonical(String value) {
        return value.toLowerCase().replaceAll("[\\\\/ ]", "_");
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

    public static long getMaximumMemory() {
        return Runtime.getRuntime().maxMemory() / (1024 * 1000 /* to get 2^n numbers */);
    }
}

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

package slash.navigation.util;

/**
 * Provides platform detection functionality.
 *
 * @author Christian Pesch
 */

public class Platform {
    public static boolean isLinux() {
        return getOsName().indexOf("linux") != -1;
    }

    public static boolean isMac() {
        return getOsName().indexOf("mac") != -1;
    }

    public static boolean isWindows() {
        return getOsName().indexOf("windows") != -1;
    }

    private static boolean isWebStarted() {
        return System.getProperty("javawebstart.version") != null;
    }

    public static String getPlatform() {
        return System.getProperty("os.name") + " " +
                System.getProperty("os.version") + " " +
                System.getProperty("os.arch");
    }

    public static String getJvm() {
        return "Java " + System.getProperty("java.version");
    }

    private static String canonical(String value) {
        return value.toLowerCase().replaceAll("[\\\\/ ]", "_");
    }

    public static String getOsName() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows"))
            return "windows";
        return canonical(osName);
    }

    public static String getOsArchitecture() {
        String osArch = System.getProperty("os.arch");
        if (osArch.endsWith("86"))
            return "x86";
        return canonical(osArch);
    }
}

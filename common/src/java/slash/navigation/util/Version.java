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
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/

package slash.navigation.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Provides version parsing functionality.
 *
 * @author Christian Pesch
 */

public abstract class Version {
    public static final String ROUTECONVERTER_VERSION_KEY = "routeconverter.version";
    public static final String ROUTECONVERTER_IS_LATEST_KEY = "routeconverter.islatest";
    private static final Pattern TITLE_PATTERN = Pattern.compile("(.+) (\\d+\\.\\d+.?\\d*) (from|vom|van)(.+)");

    public static String getSystemProperty(String propertyName) {
        String propertyValue = propertyName;
        propertyValue += "=";
        try {
            propertyValue += System.getProperty(propertyName);
        }
        catch (Throwable t) {
            propertyValue += t.getMessage();
        }
        propertyValue += ",";
        return propertyValue;
    }

    public static String getRouteConverterVersion(String version) {
        return ROUTECONVERTER_VERSION_KEY + "=" + version + ",";
    }

    public static Map<String, String> parseParameters(String parameters) {
        StringTokenizer tokenizer = new StringTokenizer(parameters, ",");
        Map<String, String> map = new HashMap<String, String>();
        while (tokenizer.hasMoreTokens()) {
            String nv = tokenizer.nextToken();
            StringTokenizer nvTokenizer = new StringTokenizer(nv, "=");
            if (!nvTokenizer.hasMoreTokens())
                continue;
            String key = nvTokenizer.nextToken();
            if (!nvTokenizer.hasMoreTokens())
                continue;
            String value = nvTokenizer.nextToken();
            map.put(key, value);
        }
        return map;
    }

    public static String parseVersionFromParameters(String parameters) {
        Map<String, String> map = parseParameters(parameters);
        return map.get(ROUTECONVERTER_VERSION_KEY);
    }

    public static boolean isLatestVersionFromParameters(String parameters) {
        Map<String, String> map = parseParameters(parameters);
        return Boolean.parseBoolean(map.get(ROUTECONVERTER_IS_LATEST_KEY));
    }

    public static String getMajor(String version) {
        int index = version.indexOf('.');
        if (index != -1)
            return version.substring(0, index);
        return version;
    }

    public static String getMinor(String version) {
        int index = version.indexOf('.');
        if (index != -1)
            return version.substring(index + 1);
        return version;
    }

    public static boolean isLatestVersion(String latestVersion, String foundVersion) {
        String latestVersionMajor = getMajor(latestVersion);
        String foundMajor = getMajor(foundVersion);
        int major = latestVersionMajor.compareTo(foundMajor);
        if (major != 0)
            return major <= 0;
        String latestVersionMinor = getMinor(latestVersion);
        String foundMinor = getMinor(foundVersion);
        while(foundMinor.length() < latestVersionMinor.length())
            foundMinor = "0" + foundMinor;
        return latestVersionMinor.compareTo(foundMinor) <= 0;
    }

    public static String parseVersionFromTitle(String title) {
        Matcher titleMatcher = TITLE_PATTERN.matcher(title);
        if (!titleMatcher.matches())
            throw new IllegalArgumentException("'" + title + "' does not match");
        return titleMatcher.group(2).trim();
    }

    public static String parseVersionFromJar(JarFile file) {
        ZipEntry entry = file.getEntry("slash/navigation/converter/gui/RouteConverter.properties");
        if (entry == null)
            throw new IllegalArgumentException("slash/navigation/converter/gui/RouteConverter.properties does not exist");
        Properties properties = new Properties();
        try {
            properties.load(file.getInputStream(entry));
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot load slash/navigation/converter/gui/RouteConverter.properties", e);
        }
        String title = (String) properties.get("title");
        return parseVersionFromTitle(title);
    }
}

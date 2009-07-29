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
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Provides version parsing functionality.
 *
 * @author Christian Pesch
 */

public class Version {
    private static final String ROUTECONVERTER_VERSION_KEY = "routeconverter.version";
    private static final String ROUTECONVERTER_IS_LATEST_KEY = "routeconverter.islatest";

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
        while (foundMinor.length() < latestVersionMinor.length())
            foundMinor = "0" + foundMinor;
        return latestVersionMinor.compareTo(foundMinor) <= 0;
    }

    private static final SimpleDateFormat BUILD_DATE = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private String version;
    private String buildDate;

    private Version() {
    }

    public String getVersion() {
        return version != null ? version : "?";
    }

    public String getBuildDate() {
        if (buildDate != null) {
            try {
                Date date = BUILD_DATE.parse(buildDate);
                return DateFormat.getDateInstance(DateFormat.LONG).format(date);
            }
            catch (ParseException e) {
                // intentionally ignored
            }
        }
        return "?";
    }

    public static Version parseVersionFromManifest() {
        try {
            URL url = Version.class.getProtectionDomain().getCodeSource().getLocation();
            InputStream in = url.openStream();
            if (in != null) {
                JarInputStream jar = new JarInputStream(in);
                try {
                    Manifest manifest = jar.getManifest();
                    if (manifest != null) {
                        Version data = new Version();
                        data.version = manifest.getMainAttributes().getValue("Version");
                        data.buildDate = manifest.getMainAttributes().getValue("Build-Date");
                        return data;
                    }
                }
                finally {
                    jar.close();
                }
            }
        } catch (IOException e) {
            // intentionally ignored
        }
        return new Version();
    }
}

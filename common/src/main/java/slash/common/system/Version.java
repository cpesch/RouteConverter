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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Scanner;

import static java.text.DateFormat.LONG;
import static slash.common.type.CompactCalendar.UTC;
import static slash.common.type.CompactCalendar.createDateFormat;

/**
 * Provides Java and RouteConverter versions.
 *
 * @author Christian Pesch
 */

public class Version {
    private static final String BUILD_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final String version;
    private final String date;
    private final String name;

    public Version(String version, String date, String name) {
        this.version = version;
        this.date = date;
        this.name = name;
    }

    public Version(String version) {
        this(version, null, null);
    }

    private String removeSnapshot(String string) {
        return string.replaceAll("-SNAPSHOT", "").replaceAll("\\?", "9");
    }

    public int getMajor() {
        String major = version.startsWith("1.") ? version.substring(2) : version;

        Scanner s = createScanner(major);
        if(s.hasNextInt())
            return s.nextInt();
        // default is Java 8
        return 8;
    }

    public boolean isLaterVersionThan(Version other) {
        if(other.getVersion().equals("14-jpackage"))
            return false;
        return compareVersion(removeSnapshot(version), removeSnapshot(other.getVersion())) > 0;
    }

    public boolean isLaterOrSameVersionThan(Version other) {
        return compareVersion(removeSnapshot(version), removeSnapshot(other.getVersion())) >= 0;
    }

    private static Scanner createScanner(String string) {
        Scanner result = new Scanner(string);
        result.useDelimiter("\\D");
        return result;
    }

    /**
     * Compares two version strings.
     *
     * Use this instead of String.compareTo() for a non-lexicographical
     * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
     *
     * @param string1 a string of ordinal numbers separated by decimal points or underscores
     * @param string2 a string of ordinal numbers separated by decimal points or underscores
     * @return The result is a negative integer if str1 is _numerically_ less than str2.
     *         The result is a positive integer if str1 is _numerically_ greater than str2.
     *         The result is zero if the strings are _numerically_ equal.
     */
    static int compareVersion(String string1, String string2) {
        Scanner s1 = createScanner(string1);
        Scanner s2 = createScanner(string2);

        while(s1.hasNextInt() && s2.hasNextInt()) {
            int v1 = s1.nextInt();
            int v2 = s2.nextInt();
            if(v1 < v2) {
                return -1;
            } else if(v1 > v2) {
                return 1;
            }
        }

        if(s1.hasNextInt())
            return 1; //string1 has an additional lower-level version number
        return 0;
    }

    public String getVersion() {
        if (version != null) {
            if (version.contains("-SNAPSHOT") || version.endsWith("-jpackage"))
                return version;
            int index = version.indexOf('-');
            if (index != -1)
                return version.substring(0, index);
            else
                return version;
        }
        return "?";
    }

    public String getDate() {
        if (date != null) {
            try {
                DateFormat format = DateFormat.getDateInstance(LONG);
                format.setTimeZone(UTC);
                Date java = createDateFormat(BUILD_DATE_FORMAT).parse(date);
                return format.format(java);
            } catch (ParseException e) {
                // intentionally ignored
            }
        }
        return "?";
    }

    public String getOperationSystem() {
        if (name != null)
            return name;
        return "?";
    }

    public static Version parseVersionFromManifest() {
        Package aPackage = Version.class.getPackage();
        return new Version(aPackage.getSpecificationVersion(),
                aPackage.getImplementationVersion(),
                aPackage.getImplementationVendor());
    }
}

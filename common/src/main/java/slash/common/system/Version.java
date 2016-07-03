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
    private String version, date, name;

    public Version(String version, String date, String name) {
        this.version = version;
        this.date = date;
        this.name = name;
    }

    public Version(String version) {
        this(version, null, null);
    }

    public String getMajor() {
        int dot = version.indexOf('.');
        if (dot != -1)
            return version.substring(0, dot);
        return version;
    }

    public String getMinor() {
        int dot = version.indexOf('.');
        if (dot != -1)
            return version.substring(dot + 1);
        return version;
    }

    private String sameLength(String reference, String hasToHaveSameLength) {
        while (hasToHaveSameLength.length() < reference.length())
            hasToHaveSameLength = "0" + hasToHaveSameLength;
        return hasToHaveSameLength;
    }

    private String removeSnapshot(String string) {
        int index = string.indexOf("-");
        if (index != -1)
            string = string.substring(0, index);
        int dot = string.indexOf(".");
        if (dot != -1)
            string = string.substring(0, dot);
        return string;
    }

    public boolean isLaterVersionThan(Version other) {
        String major = getMajor();
        String otherMajor = sameLength(major, other.getMajor());
        int result = otherMajor.compareTo(major);
        if (result != 0)
            return result <= 0;

        String minor = removeSnapshot(getMinor());
        String otherMinor = sameLength(minor, removeSnapshot(other.getMinor()));
        result = otherMinor.compareTo(minor);
        return result <= 0;
    }

    public String getVersion() {
        if (version != null) {
            if (version.contains("-SNAPSHOT"))
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
        if (System.getProperty("javawebstart.version") != null)
            return "Webstart";
        return "?";
    }

    public static Version parseVersionFromManifest() {
        Package aPackage = Version.class.getPackage();
        return new Version(aPackage.getSpecificationVersion(),
                aPackage.getImplementationVersion(),
                aPackage.getImplementationVendor());
    }
}

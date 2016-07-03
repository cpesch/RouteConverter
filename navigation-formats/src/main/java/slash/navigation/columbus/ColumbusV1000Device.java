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

package slash.navigation.columbus;

import java.util.TimeZone;
import java.util.prefs.Preferences;

/**
 * Manages the timezone setup of Columbus V-1000 devices.
 *
 * @author Christian Pesch
 */
public class ColumbusV1000Device {
    private static final Preferences preferences = Preferences.userNodeForPackage(ColumbusV1000Device.class);
    private static final String TIMEZONE_PREFERENCE = "timeZone";
    private static final String USE_LOCAL_TIMEZONE_PREFERENCE = "useLocalTimeZone";

    public static boolean getUseLocalTimeZone() {
        return preferences.getBoolean(USE_LOCAL_TIMEZONE_PREFERENCE, true);
    }

    public static void setUseLocalTimeZone(boolean localTimeZone) {
        preferences.putBoolean(USE_LOCAL_TIMEZONE_PREFERENCE, localTimeZone);
    }

    public static String getTimeZone() {
        return preferences.get(TIMEZONE_PREFERENCE, TimeZone.getDefault().getID());
    }

    public static void setTimeZone(String timeZone) {
        preferences.put(TIMEZONE_PREFERENCE, timeZone);
    }
}

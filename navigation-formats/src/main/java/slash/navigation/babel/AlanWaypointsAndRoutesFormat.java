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

package slash.navigation.babel;

import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.gpx.GpxRoute;

import java.util.prefs.Preferences;

/**
 * Reads and writes Alan Map 500 Waypoints and Routes (.wpr) files.
 *
 * @author Christian Pesch
 */

public class AlanWaypointsAndRoutesFormat extends BabelFormat implements MultipleRoutesFormat<GpxRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(AlanWaypointsAndRoutesFormat.class);

    public String getExtension() {
        return ".wpr";
    }

    public String getName() {
        return "Alan Map 500 Waypoints and Routes (*" + getExtension() + ")";
    }

    protected String getFormatName() {
        return "alanwpr";
    }

    protected String[] getGlobalOptions() {
        return new String[]{"-r", "-w"};
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumAlanWaypointsAndRoutesPositionCount", 150);
    }

    public boolean isSupportsMultipleRoutes() {
        // the format supports more than one route per file but if set to true the current multiple routes per
        // file and splitting logic ignores the maximum position count and only one unreadable file is created
        return false;
    }

    protected boolean isStreamingCapable() {
        return true;
    }
}

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

import slash.navigation.gpx.Gpx10Format;

/**
 * Reads and writes Magellan MapSend (.wpt) files.
 *
 * @author Christian Pesch
 */

public class MagellanMapSendFormat extends BabelFormat {
    public String getExtension() {
        return ".wpt";
    }

    public String getName() {
        return "Magellan MapSend (*" + getExtension() + ")";
    }

    protected String getFormatName() {
        return "mapsend";
    }

    protected String[] getGlobalOptions() {
        return new String[]{"-r", "-w"};
    }

    public boolean isSupportsMultipleRoutes() {
        return false; // just guesses
    }

    protected boolean isStreamingCapable() {
        return true;
    }

    protected Gpx10Format createGpxFormat() {
        return new Gpx10Format(false, true) {
            protected String asWayPointDescription(String name, String description) {
                // ignore <name> from waypoints which is crippled to 7 or 8 characters by GPSBabel
                return asDescription(null, description);
            }
        };
    }
}

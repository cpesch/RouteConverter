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

package slash.navigation.ozi;

import slash.navigation.base.RouteCharacteristics;

import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads and writes OziExplorer Waypoint (.wpt) files.
 *
 * @author Christian Pesch
 */

public class OziExplorerWaypointFormat extends OziExplorerFormat {
    public String getExtension() {
        return ".wpt";
    }

    public String getName() {
        return "OziExplorer Waypoint (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected RouteCharacteristics getRouteCharacteristics() {
        return Waypoints;
    }

    protected String decodeText(String field) {
        return new String(field.getBytes(ISO_8859_1), Charset.forName("windows-1252"));
    }

    protected String encodeText(String text) {
        return new String(text.getBytes(ISO_8859_1), ISO_8859_1);
    }

    protected boolean isFileType(String firstLine) {
        // GPSBabel's ozi reader takes every file that is neither a track nor a route for a waypoint file
        return !firstLine.contains("Track Point") && !firstLine.contains("Route File");
    }
}

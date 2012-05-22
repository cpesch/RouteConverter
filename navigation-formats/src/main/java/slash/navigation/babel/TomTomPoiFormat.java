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
import slash.navigation.itn.TomTomRouteFormat;

import java.util.prefs.Preferences;

/**
 * Reads Tom Tom POI (.ov2) files.
 *
 * @author Christian Pesch
 */

public class TomTomPoiFormat extends BabelFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(TomTomRouteFormat.class);

    public String getName() {
        return "Tom Tom POI (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".ov2";
    }

    public int getMaximumFileNameLength() {
        return preferences.getInt("maximumFileNameLength", 18);
    }

    protected String getFormatName() {
        return "tomtom";
    }

    public boolean isSupportsMultipleRoutes() {
        return false; 
    }

    protected boolean isStreamingCapable() {
        return false;
    }

    protected Gpx10Format createGpxFormat() {
        return new Gpx10Format(false, false);
    }
}

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

import slash.navigation.gpx.GpxRoute;

/**
 * Reads and writes iGO8 Track (.trk) files.
 *
 * @author Christian Pesch
 */

public class Igo8TrackFormat extends BabelFormat {
    public String getExtension() {
        return ".trk";
    }

    public String getName() {
        return "iGO8 Track (*" + getExtension() + ")";
    }

    protected String getFormatName() {
        return "igo8";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected boolean isStreamingCapable() {
        return false;
    }

    protected boolean isValidRoute(GpxRoute route) {
        // is really greedy in parsing the data of NetS files
        double length = route.getDistance();
        double distancePerPosition = length / route.getPositionCount();
        return distancePerPosition < 1000000.0;
    }
}
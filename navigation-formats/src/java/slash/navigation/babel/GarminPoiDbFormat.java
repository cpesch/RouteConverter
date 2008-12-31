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

package slash.navigation.babel;

import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes Garmin POI Database (.xcsv) files.
 *
 * @author Christian Pesch
 */

public class GarminPoiDbFormat extends BabelFormat {
    public String getExtension() {
        return ".xcsv";
    }

    public String getName() {
        return "Garmin POI Database (*" + getExtension() + ")";
    }

    protected String getBabelFormatName() {
        return "garmin_poi";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    private boolean isNonsenseRoute(List<GpxPosition> positions) {
        int count = 0;
        for (GpxPosition position : positions) {
            if ((position.getLongitude() == 0.0 && position.getLatitude() == 0.0) ||
                    (position.getLatitude() == 0.0 && position.getElevation() == 0.0))
                count++;
        }
        return count == positions.size();
    }

    public List<GpxRoute> read(File source) throws IOException {
        List<GpxRoute> routes = super.read(source);
        if (routes == null)
            return null;

        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (GpxRoute route : routes) {
            // is really greedy in parsing the data of various text files
            if (!isNonsenseRoute(route.getPositions()))
                result.add(route);
        }
        return result.size() > 0 ? result : null;
    }
}

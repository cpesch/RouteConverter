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

import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.util.List;

import static java.util.Collections.singletonList;
import static slash.common.io.Transfer.isEmpty;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

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

    protected String getFormatName() {
        return "garmin_poi";
    }

    protected List<RouteCharacteristics> getBabelCharacteristics() {
        return singletonList(Waypoints);
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected boolean isStreamingCapable() {
        return true;
    }

    protected boolean isValidRoute(GpxRoute route) {
        // is really greedy in parsing the data of various text files
        List<GpxPosition> positions = route.getPositions();
        int count = 0;
        for (GpxPosition position : positions) {
            if ((isEmpty(position.getLongitude()) && isEmpty(position.getLatitude())) ||
                    (isEmpty(position.getLongitude()) && isEmpty(position.getElevation())) ||
                    (isEmpty(position.getLatitude()) && isEmpty(position.getElevation())))
                count++;
        }
        return count != positions.size();
    }
}

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
import slash.navigation.gpx.GpxPosition;
import slash.navigation.util.CompactCalendar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes Garmin PCX5 (.wpt) files.
 *
 * @author Christian Pesch
 */

public class GarminPcx5Format extends BabelFormat {
    public String getExtension() {
        return ".wpt";
    }

    public String getName() {
        return "Garmin PCX5 (*" + getExtension() + ")";
    }

    protected String getBabelFormatName() {
        return "pcx";
    }

    protected String getBabelOptions() {
        return "-r -w";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    protected boolean isStreamingCapable() {
        return true;
    }

    private boolean isNonsenseRoute(List<GpxPosition> positions) {
        int count = 0;
        for (GpxPosition position : positions) {
            if ((position.getLongitude() == 0.0 && position.getElevation() != null && position.getElevation() > 100000.0) ||
                (position.getLongitude() == 0.0 && position.getLatitude() == 0.0))
                count++;
        }
        return count == positions.size();
    }

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        List<GpxRoute> routes = super.read(source, startDate);
        if (routes == null)
            return null;

        List<GpxRoute> result = new ArrayList<GpxRoute>();
        for (GpxRoute route : routes) {
            // clashes with some TomTom POI .ov2 files
            if (route.getPositionCount() > 0 && !isNonsenseRoute(route.getPositions()))
                result.add(route);
        }
        return result.size() > 0 ? result : null;
    }
}

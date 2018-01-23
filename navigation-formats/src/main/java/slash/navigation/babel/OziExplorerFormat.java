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
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static java.lang.Math.min;
import static slash.common.io.Files.setExtension;
import static slash.common.io.Transfer.isEmpty;

/**
 * The base of all OziExplorer formats.
 *
 * @author Christian Pesch
 */

public abstract class OziExplorerFormat extends BabelFormat implements MultipleRoutesFormat<GpxRoute> {

    protected String getFormatName() {
        return "ozi,pack";
    }

    protected boolean isStreamingCapable() {
        return true;
    }

    protected GpxRoute sanitizeRouteAfterReading(GpxRoute route) {
        // all routes except for the first start with an 0.0/0.0/RPTxxx waypoint
        if(route != null &&
                route.getPositionCount() > 0 &&
                route.getPosition(0).getLongitude() == 0.0 &&
                route.getPosition(0).getLatitude() == 0.0 &&
                route.getPosition(0).getDescription() != null &&
                route.getPosition(0).getDescription().startsWith("RPT"))
            route.getPositions().remove(0);
        return route;
    }

    protected boolean isValidRoute(GpxRoute route) {
        List<GpxPosition> positions = route.getPositions();
        int count = 0;
        // has lots of zero element routes and routes with only one 0.0/0.0 waypoint
        for (GpxPosition position : positions) {
            if (isEmpty(position.getLongitude()) &&
                    isEmpty(position.getLatitude()) &&
                    isEmpty(position.getElevation()))
                count++;
        }
        return count != positions.size();
    }

    protected abstract RouteCharacteristics getRouteCharacteristics();

    private void prepareForWriting(GpxRoute route) {
        // otherwise the ozi gpsbabel module would write .rte for Routes, .plt for Tracks and .wpt for Waypoints
        route.setCharacteristics(getRouteCharacteristics());

        // reduce description to 40 chars and replace comma by space (character 209 aka \u00D1 didn't work)
        for(GpxPosition position : route.getPositions()) {
            String description = position.getDescription();
            description = description.substring(0, min(description.length(), 40));
            description = description.replaceAll(",", " ");
            position.setDescription(description);
        }
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        prepareForWriting(route);
        super.write(route, target, startIndex, endIndex);
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        for (GpxRoute route : routes)
            prepareForWriting(route);
        super.write(routes, target);
    }

    protected void delete(File file) {
        super.delete(file);
        // sometimes the ozi gpsbabel module writes .rte for Routes, .plt for Tracks and .wpt for Waypoints
        super.delete(new File(setExtension(file.getAbsolutePath(), ".plt")));
        super.delete(new File(setExtension(file.getAbsolutePath(), ".rte")));
        super.delete(new File(setExtension(file.getAbsolutePath(), ".wpt")));
    }
}
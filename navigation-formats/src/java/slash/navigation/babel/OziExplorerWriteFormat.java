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

import slash.navigation.MultipleRoutesFormat;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.GpxRoute;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The base of all OziExplorer write formats.
 *
 * @author Christian Pesch
 */

public abstract class OziExplorerWriteFormat extends BabelFormat implements MultipleRoutesFormat<GpxRoute> {

    protected String getBabelFormatName() {
        return "ozi,pack";
    }

    public boolean isSupportsReading() {
        return false;
    }

    public boolean isSupportsWriting() {
        return true;
    }

    protected abstract RouteCharacteristics getRouteCharacteristics();

    public void write(GpxRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        // otherwise the ozi gpsbabel module would write .rte for Routes, .plt for Tracks and .wpt for Waypoints
        route.setCharacteristics(getRouteCharacteristics());
        super.write(route, target, startIndex, endIndex, numberPositionNames);
    }

    public void write(List<GpxRoute> routes, File target) throws IOException {
        for (GpxRoute route : routes)
            // otherwise the ozi gpsbabel module would write .rte for Routes, .plt for Tracks and .wpt for Waypoints
            route.setCharacteristics(getRouteCharacteristics());
        super.write(routes, target);
    }
}
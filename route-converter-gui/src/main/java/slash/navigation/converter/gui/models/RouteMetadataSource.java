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
package slash.navigation.converter.gui.models;

import slash.navigation.common.DistanceAndTime;
import slash.navigation.routes.Route;

/**
 * Provides {@link DistanceAndTime} metadata for a {@link Route}, keyed by its URL.
 *
 * Sources are queried in priority order via {@link CompositeRouteMetadataSource}:
 * the session-scoped {@link RouteDistanceAndTimeCache} first, since values routed by
 * the client overwrite server-provided ones; then the server metadata from the
 * catalog XML attributes (specs/00055), which shows before a route is opened.
 *
 * @author Christian Pesch
 */

public interface RouteMetadataSource {
    /**
     * Returns the known distance and time for the route with the given URL.
     *
     * @param url the URL of the route
     * @return the distance and time or null if this source does not know the route
     */
    DistanceAndTime getDistanceAndTime(String url);

    /**
     * The single definition of a missing distance shared by the routes table renderer
     * (which shows {@code –}) and the length comparator (which sorts such rows last):
     * no metadata at all, no distance or a non-positive distance all count as missing.
     */
    static boolean hasNoDistance(DistanceAndTime distanceAndTime) {
        return distanceAndTime == null || distanceAndTime.distance() == null || distanceAndTime.distance() <= 0.0;
    }

    /**
     * The single definition of a missing duration shared by the routes table renderer
     * (which shows {@code –}) and the duration comparator (which sorts such rows last):
     * no metadata at all, no time or a non-positive time all count as missing.
     */
    static boolean hasNoTime(DistanceAndTime distanceAndTime) {
        return distanceAndTime == null || distanceAndTime.timeInMillis() == null || distanceAndTime.timeInMillis() <= 0;
    }
}

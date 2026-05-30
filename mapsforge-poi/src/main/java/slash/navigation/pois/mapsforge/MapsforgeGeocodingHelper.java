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
package slash.navigation.pois.mapsforge;

import org.mapsforge.core.model.LatLong;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;

import java.util.Locale;

/**
 * Shared helpers for Mapsforge based geocoding implementations.
 *
 * @author Christian Pesch
 */
final class MapsforgeGeocodingHelper {
    private MapsforgeGeocodingHelper() {
    }

    static String normalize(String value) {
        if (value == null)
            return "";
        return value.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    }


    static LatLong toLatLong(NavigationPosition position) {
        if (position == null || !position.hasCoordinates())
            return null;
        return new LatLong(position.getLatitude(), position.getLongitude());
    }

    static double distanceMeters(NavigationPosition reference, LatLong point) {
        LatLong referencePoint = toLatLong(reference);
        return referencePoint != null ? referencePoint.vincentyDistance(point) : 0.0;
    }

    static BoundingBox createBoundsAround(NavigationPosition position, int radiusMeters) {
        LatLong center = toLatLong(position);
        LatLong north = center.destinationPoint(radiusMeters, 0);
        LatLong east = center.destinationPoint(radiusMeters, 90);
        LatLong south = center.destinationPoint(radiusMeters, 180);
        LatLong west = center.destinationPoint(radiusMeters, 270);
        return new BoundingBox(east.longitude, north.latitude, west.longitude, south.latitude);
    }
}


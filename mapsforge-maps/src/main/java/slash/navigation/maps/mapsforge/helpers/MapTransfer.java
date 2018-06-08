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
package slash.navigation.maps.mapsforge.helpers;

import org.mapsforge.core.model.LatLong;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Provides mapsforge map data transfer functionality.
 *
 * @author Christian Pesch
 */

public class MapTransfer {
    public static BoundingBox toBoundingBox(org.mapsforge.core.model.BoundingBox boundingBox) {
        return new BoundingBox(
                boundingBox.maxLongitude,
                boundingBox.maxLatitude,
                boundingBox.minLongitude,
                boundingBox.minLatitude
        );
    }

    public static org.mapsforge.core.model.BoundingBox asBoundingBox(BoundingBox boundingBox) {
        return new org.mapsforge.core.model.BoundingBox(
                boundingBox.getSouthWest().getLatitude(),
                boundingBox.getSouthWest().getLongitude(),
                boundingBox.getNorthEast().getLatitude(),
                boundingBox.getNorthEast().getLongitude()
        );
    }

    public static NavigationPosition asNavigationPosition(LatLong latLong) {
        return new SimpleNavigationPosition(latLong.longitude, latLong.latitude);
    }

    public static LatLong asLatLong(NavigationPosition position) {
        return position != null ? new LatLong(position.getLatitude(), position.getLongitude()) : null;
    }

    public static List<LatLong> asLatLong(List<NavigationPosition> positions) {
        List<LatLong> result = new ArrayList<>();
        for (NavigationPosition position : positions) {
            LatLong latLong = asLatLong(position);
            if (latLong != null)
                result.add(latLong);
        }
        return result;
    }

    public static List<LatLong> asLatLong(BoundingBox boundingBox) {
        return asLatLong(asList(
                boundingBox.getNorthEast(),
                boundingBox.getSouthEast(),
                boundingBox.getSouthWest(),
                boundingBox.getNorthWest(),
                boundingBox.getNorthEast()
        ));
    }
}

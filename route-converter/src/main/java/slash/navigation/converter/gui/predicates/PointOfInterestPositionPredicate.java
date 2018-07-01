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
package slash.navigation.converter.gui.predicates;

import slash.common.filtering.FilterPredicate;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import java.util.List;

import static java.util.Arrays.asList;
import static slash.navigation.base.WaypointType.Photo;
import static slash.navigation.base.WaypointType.PointOfInterestC;
import static slash.navigation.base.WaypointType.PointOfInterestD;
import static slash.navigation.base.WaypointType.Voice;

/**
 * Includes {@link Wgs84Position}s which have a {@link WaypointType} from {@link #POINTS_OF_INTEREST_WAYPOINT_TYPES}.
 *
 * @author Christian Pesch
 */
public class PointOfInterestPositionPredicate implements FilterPredicate<NavigationPosition> {
    private final List<WaypointType> POINTS_OF_INTEREST_WAYPOINT_TYPES = asList(Photo, PointOfInterestC, PointOfInterestD, Voice);

    public String getName() {
        return "PointOfInterest";
    }

    public boolean shouldInclude(NavigationPosition position) {
        if (!(position instanceof Wgs84Position))
            return false;
        Wgs84Position poiPosition = Wgs84Position.class.cast(position);
        return POINTS_OF_INTEREST_WAYPOINT_TYPES.contains(poiPosition.getWaypointType());
    }
}

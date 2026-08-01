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

import org.junit.Test;
import slash.navigation.base.WaypointType;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static java.util.Set.of;
import static org.junit.Assert.assertEquals;
import static slash.navigation.base.WaypointType.Parking;
import static slash.navigation.base.WaypointType.PointOfInterestC;
import static slash.navigation.base.WaypointType.PointOfInterestD;
import static slash.navigation.base.WaypointType.Waypoint;
import static slash.navigation.converter.gui.models.WaypointTypeCountToJLabelAdapter.countWaypointTypes;

/**
 * Unit tests for {@link WaypointTypeCountToJLabelAdapter#countWaypointTypes}.
 *
 * @author Christian Pesch
 */
public class WaypointTypeCountToJLabelAdapterTest {

    private static Wgs84Position wgsWithType(WaypointType type) {
        Wgs84Position position = new Wgs84Position(1.0, 2.0, null, null, null, null);
        position.setWaypointType(type);
        return position;
    }

    private static List<NavigationPosition> mixedPositions() {
        List<NavigationPosition> positions = new ArrayList<>();
        for (int i = 0; i < 2; i++)
            positions.add(wgsWithType(Parking));
        for (int i = 0; i < 3; i++)
            positions.add(wgsWithType(PointOfInterestC));
        for (int i = 0; i < 2; i++)
            positions.add(wgsWithType(PointOfInterestD));
        for (int i = 0; i < 4; i++)
            positions.add(wgsWithType(Waypoint));
        positions.add(wgsWithType(null));
        return positions;
    }

    @Test
    public void countsParkingPositions() {
        assertEquals(2, countWaypointTypes(mixedPositions(), of(Parking)));
    }

    @Test
    public void countsPointOfInterestCAndDPositions() {
        assertEquals(5, countWaypointTypes(mixedPositions(), of(PointOfInterestC, PointOfInterestD)));
    }

    @Test
    public void countsZeroForEmptyList() {
        assertEquals(0, countWaypointTypes(new ArrayList<>(), of(Parking)));
        assertEquals(0, countWaypointTypes(new ArrayList<>(), of(PointOfInterestC, PointOfInterestD)));
    }
}

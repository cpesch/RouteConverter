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
package slash.navigation.base;

import org.junit.Test;
import slash.navigation.common.Bearing;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Tests the shared geo/units math of {@link BaseNavigationPosition} through the concrete
 * {@link Wgs84Position}. One degree of latitude is ~111 km, so distance/speed cases use
 * generous deltas to stay robust to the exact bearing constants.
 *
 * @author Christian Pesch
 */
public class BaseNavigationPositionTest {

    private static Wgs84Position at(Double longitude, Double latitude) {
        return new Wgs84Position(longitude, latitude, null, null, null, null);
    }

    @Test
    public void hasCoordinatesAndHasTimeReflectTheFields() {
        assertTrue(at(0.0, 0.0).hasCoordinates());
        assertFalse(at(null, 0.0).hasCoordinates());
        assertFalse(at(0.0, null).hasCoordinates());

        assertFalse(at(0.0, 0.0).hasTime());
        assertTrue(new Wgs84Position(0.0, 0.0, null, null, fromMillis(1000), null).hasTime());
    }

    @Test
    public void distanceBetweenAdjacentLatitudesIsAboutOneDegree() {
        Double distance = at(0.0, 0.0).calculateDistance(at(0.0, 1.0));

        assertNotNull(distance);
        assertEquals(111195.0, distance, 1000.0);   // ~111 km per degree of latitude
    }

    @Test
    public void distanceIsNullWithoutBothCoordinates() {
        assertNull(at(0.0, 0.0).calculateDistance(at(null, null)));
        assertNull(at(null, null).calculateDistance(at(0.0, 0.0)));
    }

    @Test
    public void angleDueEastIsNinetyDegrees() {
        Double angle = at(0.0, 0.0).calculateAngle(at(1.0, 0.0));

        assertNotNull(angle);
        assertEquals(90.0, angle, 2.0);
    }

    @Test
    public void elevationDifferenceIsOtherMinusThis() {
        Wgs84Position low = new Wgs84Position(0.0, 0.0, 100.0, null, null, null);
        Wgs84Position high = new Wgs84Position(0.0, 0.0, 250.0, null, null, null);

        assertEquals(150.0, low.calculateElevation(high), 0.0);
        assertNull(low.calculateElevation(at(0.0, 0.0)));   // other has no elevation
    }

    @Test
    public void timeDifferenceIsOtherMinusThisInMillis() {
        Wgs84Position first = new Wgs84Position(0.0, 0.0, null, null, fromMillis(1000), null);
        Wgs84Position second = new Wgs84Position(0.0, 0.0, null, null, fromMillis(4000), null);

        assertEquals(Long.valueOf(3000), first.calculateTime(second));
        assertNull(first.calculateTime(at(0.0, 0.0)));      // other has no time
    }

    @Test
    public void speedIsDistanceOverTimeInKmh() {
        // ~111 km covered in one hour -> ~111 km/h
        Wgs84Position start = new Wgs84Position(0.0, 0.0, null, null, fromMillis(0), null);
        Wgs84Position end = new Wgs84Position(0.0, 1.0, null, null, fromMillis(3_600_000), null);

        Double speed = start.calculateSpeed(end);

        assertNotNull(speed);
        assertEquals(111.0, speed, 2.0);
    }

    @Test
    public void speedIsNullWithoutBothTimes() {
        assertNull(at(0.0, 0.0).calculateSpeed(at(0.0, 1.0)));
    }

    @Test
    public void orthogonalDistanceOfAPointOnTheLineIsNearlyZero() {
        Wgs84Position onLine = at(0.0, 1.0);         // between A and B on the same meridian
        Double crossTrack = onLine.calculateOrthogonalDistance(at(0.0, 0.0), at(0.0, 2.0));

        assertNotNull(crossTrack);
        assertEquals(0.0, crossTrack, 1000.0);
    }

    @Test
    public void bearingCarriesTheGreatCircleDistance() {
        Bearing bearing = at(0.0, 0.0).calculateBearing(at(0.0, 1.0));

        assertNotNull(bearing);
        assertEquals(111195.0, bearing.getDistance(), 1000.0);
    }

    @Test
    public void bearingAngleAndOrthogonalDistanceAreNullWithoutBothCoordinates() {
        assertNull(at(0.0, 0.0).calculateBearing(at(null, null)));
        assertNull(at(0.0, 0.0).calculateAngle(at(null, null)));
        assertNull(at(0.0, 1.0).calculateOrthogonalDistance(at(null, null), at(0.0, 2.0)));
    }
}

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
package slash.navigation.mapview.mapsforge.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.mapview.mapsforge.models.RouteQualityClassifier.classify;

/**
 * Tests for {@link RouteQualityClassifier}.
 *
 * @author Christian Pesch
 */
public class RouteQualityClassifierTest {

    @Test
    public void notRoutableIsInvalidRegardlessOfDistances() {
        assertEquals(RouteQuality.Invalid, classify(false, 229.0, 108120.0));
        assertEquals(RouteQuality.Invalid, classify(false, null, null));
    }

    @Test
    public void hugeDetourIsDetour() {
        // granada.gpx case: ratio ~= 472, excess ~= 108 km
        assertEquals(RouteQuality.Detour, classify(true, 229.0, 108120.0));
    }

    @Test
    public void matchingStraightLineAndRoutedDistanceIsValid() {
        // trekking case
        assertEquals(RouteQuality.Valid, classify(true, 229.0, 229.0));
    }

    @Test
    public void ratioAboveThresholdButBelowAbsoluteFloorIsValid() {
        assertEquals(RouteQuality.Valid, classify(true, 150.0, 900.0));
    }

    @Test
    public void zeroOrNullStraightLineDistanceIsValid() {
        assertEquals(RouteQuality.Valid, classify(true, 0.0, 108120.0));
        assertEquals(RouteQuality.Valid, classify(true, null, 108120.0));
    }

    @Test
    public void nullRoutedDistanceIsValid() {
        assertEquals(RouteQuality.Valid, classify(true, 229.0, null));
    }
}

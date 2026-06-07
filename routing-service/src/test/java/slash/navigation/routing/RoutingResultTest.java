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

package slash.navigation.routing;

import org.junit.Test;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static slash.navigation.routing.RoutingResult.Validity.Invalid;
import static slash.navigation.routing.RoutingResult.Validity.PointNotFound;
import static slash.navigation.routing.RoutingResult.Validity.Valid;

public class RoutingResultTest {

    private final SimpleNavigationPosition posA = new SimpleNavigationPosition(10.0, 50.0);
    private final SimpleNavigationPosition posB = new SimpleNavigationPosition(11.0, 51.0);
    private final List<slash.navigation.common.NavigationPosition> positions = Arrays.asList(posA, posB);
    private final DistanceAndTime dat = new DistanceAndTime(12345.6, 3600L);

    @Test
    public void testPositionsAccessor() {
        RoutingResult result = new RoutingResult(positions, dat, Valid);
        assertSame(positions, result.positions());
        assertEquals(2, result.positions().size());
    }

    @Test
    public void testDistanceAndTimeAccessor() {
        RoutingResult result = new RoutingResult(positions, dat, Valid);
        assertSame(dat, result.distanceAndTime());
        assertEquals(12345.6, result.distanceAndTime().distance(), 0.001);
        assertEquals(3600L, (long) result.distanceAndTime().timeInMillis());
    }

    @Test
    public void testValidityValues() {
        assertEquals(Valid, new RoutingResult(positions, dat, Valid).validity());
        assertEquals(Invalid, new RoutingResult(positions, dat, Invalid).validity());
        assertEquals(PointNotFound, new RoutingResult(positions, dat, PointNotFound).validity());
    }

    @Test
    public void testValidityEnumOrdinals() {
        assertEquals(0, Valid.ordinal());
        assertEquals(1, Invalid.ordinal());
        assertEquals(2, PointNotFound.ordinal());
    }
}


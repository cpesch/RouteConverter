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

package slash.navigation.converter.gui.helpers;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.Wgs84Position;
import slash.navigation.converter.gui.models.PositionsModelImpl;
import slash.navigation.converter.gui.models.TimeZoneModel;
import slash.navigation.converter.gui.panels.PositionsModelCallbackImpl;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.util.TimeZone;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.calendar;

public class PositionAugmenterTest {
    private final PositionAugmenter augmenter = new PositionAugmenter(null, null, null, null, null);
    private final GpxPosition a = new GpxPosition(null, null, null, null, null, null);
    private final GpxPosition b = new GpxPosition(null, null, null, null, null, null);
    private final GpxPosition c = new GpxPosition(null, null, null, null, null, null);
    private final GpxPosition d = new GpxPosition(null, null, null, null, null, null);
    private final GpxPosition e = new GpxPosition(null, null, null, null, null, null);
    private final BaseRoute route = new GpxRoute(new Gpx11Format(), null, null, null, asList(a, b, c, d, e));
    private final PositionsModelImpl model = new PositionsModelImpl(null);

    @Before
    public void setUp() {
        model.setRoute(route);
    }

    @Test
    public void testFindPredecessorWithTimeSelectAll() {
        a.setTime(calendar(2017, 8, 15, 12, 0, 0));
        e.setTime(calendar(2017, 8, 15, 13, 0, 0));

        assertEquals(0, augmenter.findPredecessorWithTime(model, 0));
        assertEquals(0, augmenter.findPredecessorWithTime(model, 1));
        assertEquals(0, augmenter.findPredecessorWithTime(model, 2));
        assertEquals(0, augmenter.findPredecessorWithTime(model, 3));
        assertEquals(4, augmenter.findPredecessorWithTime(model, 4));
    }

    @Test
    public void testFindSuccessorWithTimeSelectAll() {
        a.setTime(calendar(2017, 8, 15, 12, 0, 0));
        e.setTime(calendar(2017, 8, 15, 13, 0, 0));

        assertEquals(0, augmenter.findSuccessorWithTime(model, 0));
        assertEquals(4, augmenter.findSuccessorWithTime(model, 1));
        assertEquals(4, augmenter.findSuccessorWithTime(model, 2));
        assertEquals(4, augmenter.findSuccessorWithTime(model, 3));
        assertEquals(4, augmenter.findSuccessorWithTime(model, 4));
    }

    @Test
    public void testFindPredecessorWithTimeNotFirstAndLast() {
        b.setTime(calendar(2017, 8, 15, 12, 0, 0));
        d.setTime(calendar(2017, 8, 15, 13, 0, 0));

        assertEquals(-1, augmenter.findPredecessorWithTime(model, 0));
        assertEquals(1, augmenter.findPredecessorWithTime(model, 1));
        assertEquals(1, augmenter.findPredecessorWithTime(model, 2));
        assertEquals(3, augmenter.findPredecessorWithTime(model, 3));
        assertEquals(3, augmenter.findPredecessorWithTime(model, 4));
    }

    @Test
    public void testFindSuccessorWithTimeNotFirstAndLast() {
        b.setTime(calendar(2017, 8, 15, 12, 0, 0));
        d.setTime(calendar(2017, 8, 15, 13, 0, 0));

        assertEquals(1, augmenter.findSuccessorWithTime(model, 0));
        assertEquals(1, augmenter.findSuccessorWithTime(model, 1));
        assertEquals(3, augmenter.findSuccessorWithTime(model, 2));
        assertEquals(3, augmenter.findSuccessorWithTime(model, 3));
        assertEquals(-1, augmenter.findSuccessorWithTime(model, 4));
    }

    @Test
    public void testDescribePositionWithDescription() {
        a.setDescription("Summit");

        assertEquals("1: Summit", augmenter.describePosition(0, a));
    }

    @Test
    public void testDescribePositionWithoutDescription() {
        assertEquals("3", augmenter.describePosition(2, b));
    }

    @Test
    public void testIsElevationLookupSkippedForFailedLookup() {
        GpxPosition withCoordinates = new GpxPosition(1.0, 2.0, null, null, null, null);

        assertTrue(augmenter.isElevationLookupSkipped(withCoordinates, null));
    }

    @Test
    public void testIsElevationLookupSkippedForSuccessfulLookup() {
        GpxPosition withCoordinates = new GpxPosition(1.0, 2.0, null, null, null, null);

        assertFalse(augmenter.isElevationLookupSkipped(withCoordinates, "123 m"));
    }

    @Test
    public void testIsElevationLookupSkippedForMissingCoordinates() {
        assertFalse(augmenter.isElevationLookupSkipped(a, null));
    }

    @Test
    public void testNoCoordinatePredicateSkipsPositionWithCoordinates() {
        Wgs84Position position = new Wgs84Position(1.0, 2.0, null, null, null, "description");

        assertFalse(PositionAugmenter.NO_COORDINATE_PREDICATE.shouldOverwrite(position));
    }

    @Test
    public void testNoCoordinatePredicateAcceptsPositionWithoutCoordinates() {
        Wgs84Position position = new Wgs84Position(null, null, null, null, null, "description");

        assertTrue(PositionAugmenter.NO_COORDINATE_PREDICATE.shouldOverwrite(position));
    }
}

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
import slash.navigation.converter.gui.models.PositionsModelImpl;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.calendar;

public class PositionAugmenterTest {
    private PositionAugmenter augmenter = new PositionAugmenter(null, null, null, null, null);
    private GpxPosition a = new GpxPosition(null, null, null, null, null, null);
    private GpxPosition b = new GpxPosition(null, null, null, null, null, null);
    private GpxPosition c = new GpxPosition(null, null, null, null, null, null);
    private GpxPosition d = new GpxPosition(null, null, null, null, null, null);
    private GpxPosition e = new GpxPosition(null, null, null, null, null, null);
    private BaseRoute route = new GpxRoute(new Gpx11Format(), null, null, null, asList(a, b, c, d, e));
    private PositionsModelImpl model = new PositionsModelImpl();

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
}

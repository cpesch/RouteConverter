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
import slash.common.io.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.bcr.MTP0607Format;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.calendar;

public class PositionsModelTest {
    PositionsModelImpl model = new PositionsModelImpl();
    BaseRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
    BcrPosition a = new BcrPosition(1, 1, 0, "a");
    BcrPosition b = new BcrPosition(3, 3, 0, "b");
    BcrPosition c = new BcrPosition(5, 5, 0, "c");
    BcrPosition d = new BcrPosition(7, 7, 0, "d");
    BcrPosition e = new BcrPosition(9, 9, 0, "e");

    @SuppressWarnings("unchecked")
    private void initialize() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        model.setRoute(route);
    }

    @Test
    public void testGetPositions() {
        initialize();
        assertEquals(5, model.getRowCount());
        assertEquals("b", model.getPosition(1).getComment());
        assertEquals(0, model.getPositions(1, 1).size());
        assertEquals("b", model.getPositions(1, 3).get(0).getComment());
        assertEquals("b", model.getPositions(0, 2).get(1).getComment());
        assertEquals(1, model.getPositions(new int[]{1}).size());
        assertEquals("b", model.getPositions(new int[]{1}).get(0).getComment());
        assertEquals("b", model.getPositions(new int[]{0, 1}).get(1).getComment());
    }

    @Test
    public void testRemoveWithFromAndTo() {
        initialize();
        model.remove(1, 4);
        assertEquals(2, model.getRowCount());
        assertEquals("a", model.getPosition(0).getComment());
        assertEquals("e", model.getPosition(1).getComment());
    }

    @Test
    public void testRemoveWithArray() {
        initialize();
        model.remove(new int[]{1, 2, 3});
        assertEquals(2, model.getRowCount());
        assertEquals("a", model.getPosition(0).getComment());
        assertEquals("e", model.getPosition(1).getComment());
    }

    @Test
    public void testParseTimeUTC() throws ParseException {
        CompactCalendar expectedCal = calendar(2010, 9, 18, 3, 13, 32, 0, "UTC");
        CompactCalendar actualCal = model.parseTime("18.09.2010 03:13:32", "UTC");
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        String actual = DateFormat.getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }

    @Test
    public void testParseTimeLocalTime() throws ParseException {
        CompactCalendar expectedCal = calendar(2010, 9, 18, 2, 13, 32, 0, "UTC");
        CompactCalendar actualCal = model.parseTime("18.09.2010 03:13:32", "GMT+1");
        String expected = DateFormat.getDateTimeInstance().format(expectedCal.getTime());
        String actual = DateFormat.getDateTimeInstance().format(actualCal.getTime());
        assertEquals(expected, actual);
        assertEquals(expectedCal, actualCal);
    }
}

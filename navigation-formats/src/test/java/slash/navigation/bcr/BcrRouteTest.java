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

package slash.navigation.bcr;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.gpx.GpxPosition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertDoubleArrayEquals;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.common.TestCase.assertIntArrayEquals;
import static slash.common.TestCase.assertNotNull;
import static slash.common.TestCase.assertNull;
import static slash.common.TestCase.calendar;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteComments.commentPositions;
import static slash.navigation.base.RouteComments.getNumberedPosition;
import static slash.navigation.base.RouteComments.getRouteName;
import static slash.navigation.common.NumberPattern.Description_Only;
import static slash.navigation.common.NumberPattern.Number_Directly_Followed_By_Description;
import static slash.navigation.common.NumberPattern.Number_Only;
import static slash.navigation.common.NumberPattern.Number_Space_Then_Description;

public class BcrRouteTest {
    BcrRoute route = new BcrRoute(new MTP0607Format(), "r", null, new ArrayList<BcrPosition>());
    BcrPosition a = new BcrPosition(1, 1, 0, "a");
    BcrPosition b = new BcrPosition(2, 1, 0, "b");
    BcrPosition c = new BcrPosition(3, 2, 0, "c");
    BcrPosition d = new BcrPosition(1, 3, 0, "d");
    BcrPosition e = new BcrPosition(1, 1, 0, "e");
    GpxPosition zero = new GpxPosition(null, null, null, null, null, null);

    private void initialize() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        a.setTime(calendar(2015, 10, 5, 1, 2, 0, 0));
        b.setTime(calendar(2015, 10, 5, 1, 2, 15, 0));
        c.setTime(calendar(2015, 10, 5, 1, 2, 15, 0));
        d.setTime(calendar(2015, 10, 5, 1, 2, 30, 0));
    }

    private void assertPositions(BcrPosition... expected) {
        List<BcrPosition> actual = route.getPositions();
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++)
            assertEquals("at index:" + i + " expected:" + expected[i].getDescription() +
                    " but was:" + actual.get(i).getDescription(), expected[i], actual.get(i));
    }

    @Test
    public void testTop() {
        initialize();
        assertPositions(a, b, c);
        route.top(2, 0);
        assertPositions(c, a, b);
        route.top(1, 0);
        assertPositions(a, c, b);
        route.top(0, 0);
        assertPositions(a, c, b);
    }

    @Test
    public void testTopCount() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(a);
        positions.add(b);
        positions.add(c);
        assertPositions(a, b, c, a, b, c);
        route.top(3, 0);
        assertPositions(a, a, b, c, b, c);
        route.top(4, 1);
        assertPositions(a, b, a, b, c, c);
        route.top(5, 2);
        assertPositions(a, b, c, a, b, c);
    }

    @Test
    public void testTopCountIsZero() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(a);
        positions.add(b);
        positions.add(c);
        assertPositions(a, b, c, a, b, c);
        route.top(3, 0);
        assertPositions(a, a, b, c, b, c);
        route.top(4, 0);
        assertPositions(b, a, a, b, c, c);
        route.top(5, 0);
        assertPositions(c, b, a, a, b, c);
    }

    @Test
    public void testBottom() {
        initialize();
        assertPositions(a, b, c);
        route.bottom(0, 0);
        assertPositions(b, c, a);
        route.bottom(1, 0);
        assertPositions(b, a, c);
        route.bottom(2, 0);
        assertPositions(b, a, c);
    }

    @Test
    public void testBottomCount() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(a);
        positions.add(b);
        positions.add(c);
        assertPositions(a, b, c, a, b, c);
        route.bottom(2, 0);
        assertPositions(a, b, a, b, c, c);
        route.bottom(1, 1);
        assertPositions(a, a, b, c, b, c);
        route.bottom(0, 2);
        assertPositions(a, b, c, a, b, c);
    }

    @Test
    public void testBottomCountIsZero() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(a);
        positions.add(b);
        positions.add(c);
        assertPositions(a, b, c, a, b, c);
        route.bottom(2, 0);
        assertPositions(a, b, a, b, c, c);
        route.bottom(1, 0);
        assertPositions(a, a, b, c, c, b);
        route.bottom(0, 0);
        assertPositions(a, b, c, c, b, a);
    }

    @Test
    public void testMoveUp() {
        initialize();
        assertPositions(a, b, c);
        route.move(2, 1);
        assertPositions(a, c, b);
        route.move(2, 1);
        assertPositions(a, b, c);
        route.move(1, 0);
        assertPositions(b, a, c);
        route.move(2, 0);
        assertPositions(c, a, b);
    }

    @Test
    public void testMoveDown() {
        initialize();
        assertPositions(a, b, c);
        route.move(1, 2);
        assertPositions(a, c, b);
        route.move(1, 2);
        assertPositions(a, b, c);
        route.move(0, 1);
        assertPositions(b, a, c);
        route.move(0, 2);
        assertPositions(c, a, b);
    }

    @Test
    public void testReverse() {
        initialize();
        assertPositions(a, b, c);
        route.revert();
        assertPositions(c, b, a);
        route.revert();
        assertPositions(a, b, c);
    }

    @Test
    public void testRemove() {
        initialize();
        assertPositions(a, b, c);
        route.remove(1);
        assertPositions(a, c);
        route.remove(1);
        assertPositions(a);
        route.remove(0);
        assertPositions();
    }

    @Test
    public void testCalculateDistance() {
        assertDoubleEquals(1.1131, a.calculateDistance(b));
        assertDoubleEquals(b.calculateDistance(a), a.calculateDistance(b));
        assertDoubleEquals(1.569, b.calculateDistance(c));
        assertDoubleEquals(2.4858, c.calculateDistance(d));
        assertDoubleEquals(2.2114, d.calculateDistance(e));
        assertDoubleEquals(0.0, e.calculateDistance(a));
    }

    @Test
    public void testCalculateNullDistance() {
        assertNull(a.calculateDistance(zero));
    }

    @Test
    public void testGetDistance() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        assertDoubleEquals(0.0, route.getDistance(0, 0));
        assertDoubleEquals(1.1131, route.getDistance(0, 1));
        assertDoubleEquals(1.1131 + 1.569, route.getDistance(0, 2));
        assertDoubleEquals(1.1131 + 1.569 + 2.4858, route.getDistance(0, 3));
        assertDoubleEquals(1.569 + 2.4858, route.getDistance(1, 3));
        assertDoubleEquals(route.getDistance(1, 2) + route.getDistance(2, 3), route.getDistance(1, 3));
        assertDoubleEquals(2.4858, route.getDistance(2, 3));
    }

    @Test
    public void testGetDistanceSamePositionTwiceInTheMiddle() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        assertDoubleEquals(0.0, route.getDistance(0, 0));
        assertDoubleEquals(1.1131, route.getDistance(0, 1));
        assertDoubleEquals(1.1131 + 1.569, route.getDistance(0, 2));
        assertDoubleEquals(1.1131 + 1.569, route.getDistance(0, 3));
        assertDoubleEquals(1.1131 + 1.569 + 2.4858, route.getDistance(0, 4));
        assertDoubleEquals(1.569 + 2.4858, route.getDistance(1, 4));
        assertDoubleEquals(route.getDistance(1, 2) + route.getDistance(2, 3) + route.getDistance(3, 4), route.getDistance(1, 4));
        assertDoubleEquals(2.4858, route.getDistance(2, 4));
    }

    @Test
    public void testGetDistancesFromStartBetweenStartAndEndIndex() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(0, 0));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131}, route.getDistancesFromStart(0, 1));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131, 1.1131+1.569}, route.getDistancesFromStart(0, 2));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131, 1.1131+1.569, 1.1131+1.569+2.4858, 1.1131+1.569+2.4858+2.2114}, route.getDistancesFromStart(0, 4));
        assertDoubleArrayEquals(new double[]{1.1131, 1.1131+1.569, 1.1131+1.569+2.4858}, route.getDistancesFromStart(1, 3));
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(0, 0));
        assertDoubleArrayEquals(new double[]{1.1131}, route.getDistancesFromStart(1, 1));
        assertDoubleArrayEquals(new double[]{1.1131+1.569}, route.getDistancesFromStart(2, 2));
        assertDoubleArrayEquals(new double[]{1.1131+1.569+2.4858}, route.getDistancesFromStart(3, 3));
    }

    @Test
    public void testGetDistancesFromStartSamePositionTwiceInTheMiddle() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(0, 0));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131}, route.getDistancesFromStart(0, 1));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131, 1.1131+1.569}, route.getDistancesFromStart(0, 2));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131, 1.1131+1.569, 1.1131+1.569, 1.1131+1.569+2.4858}, route.getDistancesFromStart(0, 4));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131, 1.1131+1.569, 1.1131+1.569, 1.1131+1.569+2.4858, 1.1131+1.569+2.4858+2.2114}, route.getDistancesFromStart(0, 5));
        assertDoubleArrayEquals(new double[]{1.1131, 1.1131+1.569, 1.1131+1.569}, route.getDistancesFromStart(1, 3));
        assertDoubleArrayEquals(new double[]{1.1131, 1.1131+1.569, 1.1131+1.569, 1.1131+1.569+2.4858}, route.getDistancesFromStart(1, 4));
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(0, 0));
        assertDoubleArrayEquals(new double[]{1.1131}, route.getDistancesFromStart(1, 1));
        assertDoubleArrayEquals(new double[]{1.1131+1.569}, route.getDistancesFromStart(2, 2));
        assertDoubleArrayEquals(new double[]{1.1131+1.569}, route.getDistancesFromStart(3, 3));
        assertDoubleArrayEquals(new double[]{1.1131+1.569+2.4858}, route.getDistancesFromStart(4, 4));
    }

    @Test
    public void testGetDistancesFromStartWithSelection() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(new int[]{0}));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131}, route.getDistancesFromStart(new int[]{0, 1}));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131, 1.1131+1.569}, route.getDistancesFromStart(new int[]{0, 1, 2}));
        assertDoubleArrayEquals(new double[]{0.0, 1.1131, 1.1131+1.569, 1.1131+1.569+2.4858, 1.1131+1.569+2.4858+2.2114}, route.getDistancesFromStart(new int[]{0, 1, 2, 3, 4}));
        assertDoubleArrayEquals(new double[]{1.1131, 1.1131+1.569, 1.1131+1.569+2.4858+2.2114}, route.getDistancesFromStart(new int[]{1,2,4}));
        assertDoubleArrayEquals(new double[]{1.1131, 1.1131+1.569, 1.1131+1.569+2.4858+2.2114}, route.getDistancesFromStart(new int[]{4,1,2}));
        assertDoubleArrayEquals(new double[]{1.1131+1.569+2.4858+2.2114}, route.getDistancesFromStart(new int[]{4}));
    }

    @Test
    public void testRouteLength() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        assertDoubleEquals(1.1131, route.getDistance());
        assertDoubleEquals(a.calculateDistance(b), route.getDistance());
        positions.add(c);
        assertDoubleEquals(1.1131+1.569, route.getDistance());
        positions.add(d);
        assertDoubleEquals(1.1131+1.569+2.4858, route.getDistance());
        positions.add(e);
        assertDoubleEquals(1.1131+1.569+2.4858+2.2114, route.getDistance());
    }

    @Test
    public void testRemoveDuplicates() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(c);
        positions.add(a);
        positions.add(a);
        positions.add(a);
        positions.add(a);
        positions.add(c);
        positions.add(b);
        positions.add(b);
        positions.add(b);
        positions.add(a);
        positions.add(c);
        positions.add(c);
        positions.add(a);
        assertPositions(c, a, a, a, a, c, b, b, b, a, c, c, a);
        route.removeDuplicates();
        assertPositions(c, a, c, b, a, c, a);
        route.removeDuplicates();
        assertPositions(c, a, c, b, a, c, a);
    }

    @Test
    public void testEnsureIncreasingTime() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        route.ensureIncreasingTime();
        assertNull(a.getTime());

        positions.add(b);
        route.ensureIncreasingTime();
        assertNotNull(a.getTime());
        assertEquals(a.getTime().getTimeInMillis() + 1113, b.getTime().getTimeInMillis());

        positions.clear();
        positions.add(c);
        positions.add(d);
        positions.add(e);
        route.ensureIncreasingTime();
        assertNotNull(c.getTime());
        long startTime = c.getTime().getTimeInMillis();
        assertEquals(startTime, c.getTime().getTimeInMillis());
        assertEquals(startTime + 2485, d.getTime().getTimeInMillis());
        assertEquals(startTime + 2485 + 2211, e.getTime().getTimeInMillis());
    }

    @Test
    public void testEnsureIncreasingTimeWithAverageSpeed() {
        BcrPosition x = new BcrPosition(1180598,7090272, 0, "x");
        x.setTime(CompactCalendar.fromCalendar(Calendar.getInstance()));
        BcrPosition y = new BcrPosition(1153565,7113439, 0, "y");
        BcrPosition z = new BcrPosition(1138352,7089963, 0, "z");
        final int COMPLETE_TIME = 3600 * 1000;
        z.setTime(fromMillis(x.getTime().getTimeInMillis() + COMPLETE_TIME));

        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(x);
        positions.add(y);
        positions.add(z);
        assertEquals(COMPLETE_TIME, route.getTime());

        route.ensureIncreasingTime();
        long startTime = x.getTime().getTimeInMillis();
        assertEquals(startTime, x.getTime().getTimeInMillis());
        assertEquals(startTime + 2016544, y.getTime().getTimeInMillis());
        assertEquals(startTime + route.getTime(), z.getTime().getTimeInMillis());
        assertEquals(startTime + COMPLETE_TIME, z.getTime().getTimeInMillis());
    }

    @Test
    public void testPositionsWithinDistanceToPredecessor() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        assertPositions(a, b, c, d, e);
        int[] in1mDistance = route.getPositionsWithinDistanceToPredecessor(1.0);
        assertIntArrayEquals(new int[0], in1mDistance);
        int[] in2mDistance = route.getPositionsWithinDistanceToPredecessor(2.0);
        assertIntArrayEquals(new int[]{1}, in2mDistance);
        int[] in5mDistance = route.getPositionsWithinDistanceToPredecessor(5.0);
        assertIntArrayEquals(new int[]{1, 2, 3}, in5mDistance);
    }

    @Test
    public void testCommentPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(new BcrPosition(i, i, i, null));
        }

        for (int i = 0; i < 10; i++) {
            assertNull(positions.get(i).getDescription());
        }

        commentPositions(positions);

        for (int i = 0; i < 10; i++) {
            assertEquals("Position " + (i + 1), positions.get(i).getDescription());
        }
    }

    @Test
    public void testCommentAndRenumberPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(new BcrPosition(i, i, i, null));
        }

        commentPositions(positions);

        positions.get(9).setDescription("Position 7: Hamburg");
        positions.get(7).setDescription("Hamburg (Position 7)");
        positions.get(3).setDescription("Hamburg");
        positions.remove(8);
        positions.remove(6);
        positions.remove(4);
        positions.remove(2);
        positions.remove(1);

        commentPositions(positions);

        assertEquals("Position 1", positions.get(0).getDescription());
        assertEquals("Hamburg", positions.get(1).getDescription());
        assertEquals("Position 3", positions.get(2).getDescription());
        assertEquals("Hamburg (Position 4)", positions.get(3).getDescription());
        assertEquals("Position 5: Hamburg", positions.get(4).getDescription());
    }

    @Test
    public void testNameRoute() {
        route.setName("r");
        assertEquals("r (1)", getRouteName(route, 1));

        route.setName("r (1)");
        assertEquals("r (2)", getRouteName(route, 2));

        route.setName("r (1) s");
        assertEquals("r (2) s", getRouteName(route, 2));

        route.setName("r (1)(1)");
        assertEquals("r (1)(2)", getRouteName(route, 2));
    }

    @Test
    public void getNumberPositions() {
        assertEquals("Hamburg", getNumberedPosition(new BcrPosition(1, 2, 3, "1234 Hamburg"), 5, 3, Description_Only));
        assertEquals("006", getNumberedPosition(new BcrPosition(1, 2, 3, " Position  9 "), 5, 3, Number_Only));
        assertEquals("006Position 6", getNumberedPosition(new BcrPosition(1, 2, 3, " Position  9 "), 5, 3, Number_Directly_Followed_By_Description));
        assertEquals("006 Position 6", getNumberedPosition(new BcrPosition(1, 2, 3, " Position  9 "), 5, 3, Number_Space_Then_Description));

        assertEquals("006 Position 6", getNumberedPosition(new BcrPosition(1, 2, 3, "9 Position 9"), 5, 3, Number_Space_Then_Description));
        assertEquals("006 Position 6", getNumberedPosition(new BcrPosition(1, 2, 3, "8Position 9"), 5, 3, Number_Space_Then_Description));
        assertEquals("006 Position 6", getNumberedPosition(new BcrPosition(1, 2, 3, " Position7 "), 5, 3, Number_Space_Then_Description));
        assertEquals("006 aPosition 6a", getNumberedPosition(new BcrPosition(1, 2, 3, "aPositiona5a"), 5, 3, Number_Space_Then_Description));
        assertEquals("006 a", getNumberedPosition(new BcrPosition(1, 2, 3, "04a"), 5, 3, Number_Space_Then_Description));
        assertEquals("006", getNumberedPosition(new BcrPosition(1, 2, 3, " 3 "), 5, 3, Number_Space_Then_Description));
        assertEquals("006", getNumberedPosition(new BcrPosition(1, 2, 3, "0002"), 5, 3, Number_Space_Then_Description));
    }

    @Test
    public void testNumberPositionsWithGetNumberPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(new BcrPosition(i, i, i, "description"));
        }

        for (int i = 0; i < positions.size(); i++) {
            BcrPosition position = positions.get(i);
            position.setDescription(getNumberedPosition(position, i, 0, Number_Directly_Followed_By_Description));
        }

        for (int i = 0; i < positions.size(); i++) {
            assertEquals((i + 1) + "description", positions.get(i).getDescription());
        }

        positions.remove(8);
        positions.remove(0);

        // check renumbering, add space
        for (int i = 0; i < positions.size(); i++) {
            BcrPosition position = positions.get(i);
            position.setDescription(getNumberedPosition(position, i, 0, Number_Space_Then_Description));
        }

        for (int i = 0; i < positions.size(); i++) {
            assertEquals((i + 1) + " description", positions.get(i).getDescription());
        }

        positions.remove(5);
        positions.remove(0);

        // check renumbering, check remove space again but have 2 digits and leading zeros
        for (int i = 0; i < positions.size(); i++) {
            BcrPosition position = positions.get(i);
            position.setDescription(getNumberedPosition(position, i, 2, Number_Directly_Followed_By_Description));
        }

        for (int i = 0; i < positions.size(); i++) {
            assertEquals(formatIntAsString(i + 1, 2) + "description", positions.get(i).getDescription());
        }
    }

    @Test
    public void testSuccessor() {
        initialize();
        assertEquals(b, route.getSuccessor(a));
        assertEquals(c, route.getSuccessor(b));
        assertNull(route.getSuccessor(c));
        assertNull(route.getSuccessor(d));
    }

    @Test
    public void testGetPosition() {
        initialize();
        assertEquals(b, route.getPosition(1));
    }

    @Test
    public void testGetClosestPositionByTimeExact() {
        initialize();
        assertEquals(1, route.getClosestPosition(b.getTime(), 0));
    }

    @Test
    public void testGetClosestPositionByTimeFirstWins() {
        initialize();
        List<BcrPosition> positions = route.getPositions();
        positions.add(d);
        assertEquals(1, route.getClosestPosition(calendar(2015, 10, 5, 1, 2, 15), 30*1000));
    }

    @Test
    public void testGetClosestPositionByTimeTimeZones() {
        initialize();
        assertEquals(1, route.getClosestPosition(calendar(2015, 10, 5, 2, 2, 15, 0, "GMT+1:00"), 1000));
    }
}

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
import slash.common.io.Transfer;
import slash.navigation.util.RouteComments;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.*;

public class BcrRouteTest {
    BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
    BcrPosition a = new BcrPosition(1, 1, 0, "a");
    BcrPosition b = new BcrPosition(3, 3, 0, "b");
    BcrPosition c = new BcrPosition(5, 5, 0, "c");
    BcrPosition d = new BcrPosition(7, 7, 0, "d");
    BcrPosition e = new BcrPosition(9, 9, 0, "e");

    private void initialize() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
    }

    private void assertPositions(BcrPosition... expected) {
        List<BcrPosition> actual = route.getPositions();
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++)
            assertEquals("at index:" + i + " expected:" + expected[i].getComment() +
                    " but was:" + actual.get(i).getComment(), expected[i], actual.get(i));
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
    public void testUp() {
        initialize();
        assertPositions(a, b, c);
        route.up(2, 1);
        assertPositions(a, c, b);
        route.up(2, 1);
        assertPositions(a, b, c);
        route.up(1, 0);
        assertPositions(b, a, c);
        route.up(2, 0);
        assertPositions(c, b, a);
    }

    @Test
    public void testDown() {
        initialize();
        assertPositions(a, b, c);
        route.down(1, 2);
        assertPositions(a, c, b);
        route.down(1, 2);
        assertPositions(a, b, c);
        route.down(0, 1);
        assertPositions(b, a, c);
        route.down(0, 2);
        assertPositions(a, c, b);
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
        assertDoubleEquals(3.138, a.calculateDistance(b));
        assertDoubleEquals(b.calculateDistance(a), a.calculateDistance(b));
        assertDoubleEquals(3.138, b.calculateDistance(c));
        assertDoubleEquals(3.138, c.calculateDistance(d));
        assertDoubleEquals(3.138, d.calculateDistance(e));
    }

    @Test
    public void testGetDistance() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        assertDoubleEquals(0.0, route.getDistance(0, 0));
        positions.add(b);
        assertDoubleEquals(0.0, route.getDistance(0, 0));
        assertDoubleEquals(3.138, route.getDistance(0, 1));
        positions.add(c);
        assertDoubleEquals(0.0, route.getDistance(0, 0));
        assertDoubleEquals(3.138, route.getDistance(0, 1));
        assertDoubleEquals(6.276, route.getDistance(0, 2));
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
        assertDoubleArrayEquals(new double[]{0.0, 3.138}, route.getDistancesFromStart(0, 1));
        assertDoubleArrayEquals(new double[]{0.0, 3.138, 6.276}, route.getDistancesFromStart(0, 2));
        assertDoubleArrayEquals(new double[]{0.0, 3.138, 6.276, 9.414, 12.552}, route.getDistancesFromStart(0, 4));
        assertDoubleArrayEquals(new double[]{3.138, 6.276, 9.414}, route.getDistancesFromStart(1, 3));
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(0, 0));
        assertDoubleArrayEquals(new double[]{3.138}, route.getDistancesFromStart(1, 1));
        assertDoubleArrayEquals(new double[]{6.2761}, route.getDistancesFromStart(2, 2));
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
        assertDoubleArrayEquals(new double[]{3.138, 6.276, 12.5521}, route.getDistancesFromStart(new int[]{1,2,4}));
        assertDoubleArrayEquals(new double[]{3.138, 6.276, 12.5521}, route.getDistancesFromStart(new int[]{4,1,2}));
        assertDoubleArrayEquals(new double[]{0.0, 12.5522}, route.getDistancesFromStart(new int[]{0,4}));
        assertDoubleArrayEquals(new double[]{3.138, 9.4141}, route.getDistancesFromStart(new int[]{1,3}));
        assertDoubleArrayEquals(new double[]{3.138, 6.276, 9.414}, route.getDistancesFromStart(new int[]{3,2,1}));
    }

    @Test
    public void testRouteLength() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        assertDoubleEquals(3.138, route.getLength());
        assertDoubleEquals(a.calculateDistance(b), route.getLength());
        positions.add(c);
        assertDoubleEquals(3.138 * 2, route.getLength());
        positions.add(d);
        assertDoubleEquals(3.138 * 3, route.getLength());
        positions.add(e);
        assertDoubleEquals(3.138 * 4, route.getLength());
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
        int[] in3mDistance = route.getPositionsWithinDistanceToPredecessor(3.0);
        assertIntArrayEquals(new int[0], in3mDistance);
        int[] in3m50Distance = route.getPositionsWithinDistanceToPredecessor(3.5);
        assertIntArrayEquals(new int[]{1, 3}, in3m50Distance);
        int[] in5mDistance = route.getPositionsWithinDistanceToPredecessor(5.0);
        assertIntArrayEquals(new int[]{1, 3}, in5mDistance);
    }

    @Test
    public void testCommentPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(new BcrPosition(i, i, i, null));
        }

        for (int i = 0; i < 10; i++) {
            assertNull(positions.get(i).getComment());
        }

        RouteComments.commentPositions(positions, false);

        for (int i = 0; i < 10; i++) {
            assertEquals("Position " + (i + 1), positions.get(i).getComment());
        }
    }

    @Test
    public void testCommentAndRenumberPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(new BcrPosition(i, i, i, null));
        }

        RouteComments.commentPositions(positions, false);

        positions.get(9).setComment("Position 7: Hamburg");
        positions.get(7).setComment("Hamburg (Position 7)");
        positions.get(3).setComment("Hamburg");
        positions.remove(8);
        positions.remove(6);
        positions.remove(4);
        positions.remove(2);
        positions.remove(1);

        RouteComments.commentPositions(positions, false);

        assertEquals("Position 1", positions.get(0).getComment());
        assertEquals("Hamburg", positions.get(1).getComment());
        assertEquals("Position 3", positions.get(2).getComment());
        assertEquals("Hamburg (Position 4)", positions.get(3).getComment());
        assertEquals("Position 5: Hamburg", positions.get(4).getComment());
    }

    @Test
    public void testNumberPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(new BcrPosition(i, i, i, "Comment"));
        }

        for (int i = 0; i < positions.size(); i++) {
            RouteComments.numberPosition(positions.get(i), i, 0, false);
        }

        for (int i = 0; i < positions.size(); i++) {
            assertEquals((i + 1) + "Comment", positions.get(i).getComment());
        }

        positions.remove(8);
        positions.remove(0);

        // check renumbering, add space
        for (int i = 0; i < positions.size(); i++) {
            RouteComments.numberPosition(positions.get(i), i, 0, true);
        }

        for (int i = 0; i < positions.size(); i++) {
            assertEquals((i + 1) + " Comment", positions.get(i).getComment());
        }

        positions.remove(5);
        positions.remove(0);

        // check renumbering, check remove space again but have 2 digits and leading zeros
        for (int i = 0; i < positions.size(); i++) {
            RouteComments.numberPosition(positions.get(i), i, 2, false);
        }

        for (int i = 0; i < positions.size(); i++) {
            assertEquals(Transfer.formatIntAsString(i + 1, 2) + "Comment", positions.get(i).getComment());
        }
    }

    @Test
    public void testGetNumberPositions() {
        assertEquals("006 Position 6", RouteComments.getNumberedPosition(new BcrPosition(1, 2, 3, " Position  9 "), 5, 3, true));
        assertEquals("006 Position 6", RouteComments.getNumberedPosition(new BcrPosition(1, 2, 3, " Position9 "), 5, 3, true));
        assertEquals("006 aPosition 6a", RouteComments.getNumberedPosition(new BcrPosition(1, 2, 3, "aPositiona9a"), 5, 3, true));
        assertEquals("006 a", RouteComments.getNumberedPosition(new BcrPosition(1, 2, 3, "09a"), 5, 3, true));
        assertEquals("006", RouteComments.getNumberedPosition(new BcrPosition(1, 2, 3, "09"), 5, 3, true));
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
    public void testPredecessor() {
        initialize();
        assertNull(route.getPredecessor(a));
        assertEquals(a, route.getPredecessor(b));
        assertEquals(b, route.getPredecessor(c));
        assertNull(route.getPredecessor(d));
    }

    @Test
    public void testGetPosition() {
        initialize();
        assertEquals(b, route.getPosition(1));
    }
}

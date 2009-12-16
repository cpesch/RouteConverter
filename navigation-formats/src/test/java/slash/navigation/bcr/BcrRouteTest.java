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

import slash.navigation.NavigationTestCase;
import slash.navigation.util.RouteComments;
import slash.common.io.Transfer;

import java.util.ArrayList;
import java.util.List;

public class BcrRouteTest extends NavigationTestCase {
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

    public void testUp() {
        initialize();
        assertPositions(a, b, c);
        route.up(2);
        assertPositions(a, c, b);
        route.up(2);
        assertPositions(a, b, c);
        route.up(1);
        assertPositions(b, a, c);
    }

    public void testDown() {
        initialize();
        assertPositions(a, b, c);
        route.down(1);
        assertPositions(a, c, b);
        route.down(1);
        assertPositions(a, b, c);
        route.down(0);
        assertPositions(b, a, c);
    }

    public void testReverse() {
        initialize();
        assertPositions(a, b, c);
        route.revert();
        assertPositions(c, b, a);
        route.revert();
        assertPositions(a, b, c);
    }

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

    public void testCalculateDistance() {
        assertEquals(3.138, a.calculateDistance(b));
        assertEquals(b.calculateDistance(a), a.calculateDistance(b));
        assertEquals(3.138, b.calculateDistance(c));
        assertEquals(3.138, c.calculateDistance(d));
        assertEquals(3.138, d.calculateDistance(e));
    }

    public void testGetDistance() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        assertEquals(0.0, route.getDistance(0, 0));
        positions.add(b);
        assertEquals(0.0, route.getDistance(0, 0));
        assertEquals(3.138, route.getDistance(0, 1));
        positions.add(c);
        assertEquals(0.0, route.getDistance(0, 0));
        assertEquals(3.138, route.getDistance(0, 1));
        assertEquals(6.276, route.getDistance(0, 2));
    }

    public void testGetDistancesFromStart() {
        BcrRoute route = new BcrRoute(new MTP0607Format(), "?", null, new ArrayList<BcrPosition>());
        List<BcrPosition> positions = route.getPositions();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(0, 0));
        assertDoubleArrayEquals(new double[]{0.0, 3.138}, route.getDistancesFromStart(0, 1));
        assertDoubleArrayEquals(new double[]{0.0, 3.138, 6.276}, route.getDistancesFromStart(0, 2));
        assertDoubleArrayEquals(new double[]{3.138, 6.276}, route.getDistancesFromStart(1, 2));
        assertDoubleArrayEquals(new double[]{0.0}, route.getDistancesFromStart(0, 0));
        assertDoubleArrayEquals(new double[]{3.138}, route.getDistancesFromStart(1, 1));
        assertDoubleArrayEquals(new double[]{6.2761}, route.getDistancesFromStart(2, 2));
    }

    public void testRouteLength() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        assertEquals(3.138, route.getLength());
        assertEquals(a.calculateDistance(b), route.getLength());
        positions.add(c);
        assertEquals(3.138 * 2, route.getLength());
        positions.add(d);
        assertEquals(3.138 * 3, route.getLength());
        positions.add(e);
        assertEquals(3.138 * 4, route.getLength());
    }

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

    public void testDuplicatesWithinDistance() {
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
        int[] in1mDistance = route.getDuplicatesWithinDistance(1.0);
        assertIntArrayEquals(new int[]{2, 3, 4, 7, 8, 11}, in1mDistance);
        int[] in5mDistance = route.getDuplicatesWithinDistance(5.0);
        assertIntArrayEquals(new int[]{2, 3, 4, 6, 7, 8, 9, 11}, in5mDistance);
    }

    public void testPositionsThatRemainingHaveDistance() {
        List<BcrPosition> positions = route.getPositions();
        positions.clear();
        positions.add(a);
        positions.add(b);
        positions.add(c);
        positions.add(d);
        positions.add(e);
        assertPositions(a, b, c, d, e);
        int[] in1mDistance = route.getPositionsThatRemainingHaveDistance(1.0);
        assertIntArrayEquals(new int[0], in1mDistance);
        int[] in3mDistance = route.getPositionsThatRemainingHaveDistance(3.0);
        assertIntArrayEquals(new int[0], in3mDistance);
        int[] in3m50Distance = route.getPositionsThatRemainingHaveDistance(3.5);
        assertIntArrayEquals(new int[]{1, 3}, in3m50Distance);
        int[] in5mDistance = route.getPositionsThatRemainingHaveDistance(5.0);
        assertIntArrayEquals(new int[]{1, 3}, in5mDistance);
    }

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

    public void testSuccessor() {
        initialize();
        assertEquals(b, route.getSuccessor(a));
        assertEquals(c, route.getSuccessor(b));
        assertNull(route.getSuccessor(c));
        assertNull(route.getSuccessor(d));
    }

    public void testPredecessor() {
        initialize();
        assertNull(route.getPredecessor(a));
        assertEquals(a, route.getPredecessor(b));
        assertEquals(b, route.getPredecessor(c));
        assertNull(route.getPredecessor(d));
    }
}

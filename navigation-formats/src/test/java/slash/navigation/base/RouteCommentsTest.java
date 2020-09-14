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
import slash.navigation.bcr.BcrPosition;
import slash.navigation.bcr.BcrRoute;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.common.NavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;
import static slash.common.io.Transfer.formatIntAsString;
import static slash.navigation.base.RouteComments.*;
import static slash.navigation.common.NumberPattern.*;

public class RouteCommentsTest {
    private BcrRoute route = new BcrRoute(new MTP0607Format(), "r", null, new ArrayList<>());

    private BcrPosition createPosition(String description) {
        return new BcrPosition(1, 2, 3, description);
    }

    @Test
    public void testCommentPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(createPosition(null));
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
            positions.add(createPosition(null));
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
    public void testIsDefaultDescription() {
        assertTrue(isDefaultDescription("Position 12"));
        assertTrue(isDefaultDescription("Waypoint 123"));
        assertTrue(isDefaultDescription("Position 1234 abc"));
        assertTrue(isDefaultDescription("abcPosition 1234abc"));
        assertTrue(isDefaultDescription("abc Position 1234 abc"));

        assertFalse(isDefaultDescription("Position"));
        assertFalse(isDefaultDescription("abc Position"));
        assertFalse(isDefaultDescription("Position abc"));
        assertFalse(isDefaultDescription("abc Position abc"));
    }

    @Test
    public void testGetDefaultDescription() {
        assertEquals("Position 1", getDefaultDescription(0));

        assertEquals("Position 2", getDefaultDescription(createPosition(null), 1));
        assertEquals("Position 2", getDefaultDescription(createPosition(""), 1));
        assertEquals("Position 2", getDefaultDescription(createPosition("Position 2"), 1));
        assertEquals("Position 2abc", getDefaultDescription(createPosition("Position 23abc"), 1));
        assertEquals("Position 2 abc", getDefaultDescription(createPosition("Position 234 abc"), 1));
        assertEquals("abcPosition 2", getDefaultDescription(createPosition("abcPosition 234"), 1));
        assertEquals("abc Position 2", getDefaultDescription(createPosition("abc Position 234"), 1));
        assertEquals("abcPosition 2abc", getDefaultDescription(createPosition("abcPosition 234abc"), 1));
        assertEquals("abc Position 2 abc", getDefaultDescription(createPosition("abc Position 234 abc"), 1));
        assertEquals("abc Position 2 abc", getDefaultDescription(createPosition(" abc Position 234 abc "), 1));
    }

    @Test
    public void testGetNumberedPosition() {
        assertEquals("Hamburg", getNumberedPosition(createPosition("1234 Hamburg"), 5, 3, Description_Only));
        assertNull(getNumberedPosition(createPosition("1234"), 5, 3, Description_Only));
        assertEquals("006", getNumberedPosition(createPosition(" Position  9 "), 5, 3, Number_Only));
        assertEquals("06", getNumberedPosition(createPosition(" Waypoint  9 "), 5, 2, Number_Only));
        assertEquals("6", getNumberedPosition(createPosition(" Waypoint  9 "), 5, 1, Number_Only));
        assertEquals("6", getNumberedPosition(createPosition(null), 5, 1, Number_Only));

        assertEquals("006Position 6", getNumberedPosition(createPosition(" Position  9 "), 5, 3, Number_Directly_Followed_By_Description));
        assertEquals("006Position 6", getNumberedPosition(createPosition(" Waypoint  9 "), 5, 3, Number_Directly_Followed_By_Description));

        assertEquals("006 Position 6", getNumberedPosition(createPosition(" Position  9 "), 5, 3, Number_Space_Then_Description));
        assertEquals("006 Position 6", getNumberedPosition(createPosition(" Waypoint  9 "), 5, 3, Number_Space_Then_Description));
        assertEquals("006 Position 6", getNumberedPosition(createPosition("9 Position 9"), 5, 3, Number_Space_Then_Description));
        assertEquals("006 Position 6", getNumberedPosition(createPosition("8Position 9"), 5, 3, Number_Space_Then_Description));
        assertEquals("006 Position 6", getNumberedPosition(createPosition(" Position7 "), 5, 3, Number_Space_Then_Description));
        assertEquals("006 aPosition 6a", getNumberedPosition(createPosition("aPositiona5a"), 5, 3, Number_Space_Then_Description));
        assertEquals("006 a", getNumberedPosition(createPosition("04a"), 5, 3, Number_Space_Then_Description));
        assertEquals("006", getNumberedPosition(createPosition(" 3 "), 5, 3, Number_Space_Then_Description));
        assertEquals("006", getNumberedPosition(createPosition("0002"), 5, 3, Number_Space_Then_Description));
    }

    @Test
    public void testNumberPositionsWithGetNumberPositions() {
        List<BcrPosition> positions = route.getPositions();
        for (int i = 0; i < 10; i++) {
            positions.add(createPosition("description"));
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
}

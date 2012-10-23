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

package slash.navigation.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.util.Positions.asPosition;
import static slash.navigation.util.Positions.contains;

public class PositionsTest {
    @Test
    public void testContains() {
        assertTrue(contains(asPosition(0.1, 0.1), asPosition(-0.1, -0.1), asPosition(0.0, 0.0)));
        assertTrue(contains(asPosition(-1.0, -1.0), asPosition(-2.0, -2.0), asPosition(-1.5, -1.5)));
        assertTrue(contains(asPosition(-1.0, 2.0), asPosition(-2.0, 1.0), asPosition(-1.5, 1.5)));
        assertTrue(contains(asPosition(2.0, 2.0), asPosition(1.0, 1.0), asPosition(1.5, 1.5)));
        assertTrue(contains(asPosition(2.0, -1.0), asPosition(1.0, -2.0), asPosition(1.5, -1.5)));
    }

    @Test
    public void testNotContains() {
        assertFalse(contains(asPosition(0.0, 0.0), asPosition(0.0, 0.0), asPosition(0.0, 0.0)));
        assertFalse(contains(asPosition(-0.1, -0.1), asPosition(0.0, 0.0), asPosition(0.0, 0.0)));
        assertFalse(contains(asPosition(0.0, 0.0), asPosition(-0.1, -0.1), asPosition(0.0, 0.0)));
        assertFalse(contains(asPosition(-0.1, -0.1), asPosition(-0.1, -0.1), asPosition(0.0, 0.0)));
    }
}

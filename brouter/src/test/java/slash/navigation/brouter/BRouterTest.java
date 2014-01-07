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
package slash.navigation.brouter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.common.TestCase.assertDoubleEquals;

public class BRouterTest {
    private BRouter bRouter = new BRouter();

    @Test
    public void testLongitude() {
        assertEquals(190032100, bRouter.asLongitude(10.0321));
        assertEquals(10.0321, bRouter.asLongitude(190032100), 0.00001);
    }

    @Test
    public void testLatitude() {
        assertEquals(143569480, bRouter.asLatitude(53.56948));
        assertDoubleEquals(53.56948, bRouter.asLatitude(143569480));
    }
}

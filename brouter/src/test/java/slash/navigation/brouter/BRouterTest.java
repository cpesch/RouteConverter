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

public class BRouterTest {
    private BRouter router = new BRouter(null);

    @Test
    public void testLongitude() {
        assertEquals(190032145, router.asLongitude(10.032145));
        assertEquals(10.032145, router.asLongitude(190032145), 0.000001);
    }

    @Test
    public void testLatitude() {
        assertEquals(143569481, router.asLatitude(53.569481));
        assertEquals(53.569481, router.asLatitude(143569481), 0.000001);
    }

    @Test
    public void createFileKey() {
        assertEquals("E0_N0.rd5", router.createFileKey(0.1, 0.1));
        assertEquals("E0_S5.rd5", router.createFileKey(0.1, -0.1));
        assertEquals("W5_N0.rd5", router.createFileKey(-0.1, 0.1));
        assertEquals("W5_S5.rd5", router.createFileKey(-0.1, -0.1));

        assertEquals("W5_N40.rd5", router.createFileKey(-4.036, 42.486));
        assertEquals("E5_N5.rd5", router.createFileKey(5.1, 9.9));
        assertEquals("E50_N50.rd5", router.createFileKey(50.1, 54.9));
        assertEquals("E175_N85.rd5", router.createFileKey(179.9, 89.9));
        assertEquals("W10_S10.rd5", router.createFileKey(-5.1, -9.9));
        assertEquals("W5_S45.rd5", router.createFileKey(-4.036, -43.431));
        assertEquals("W55_S55.rd5", router.createFileKey(-50.1, -54.9));

        assertEquals("E175_N85.rd5", router.createFileKey(179.9, 89.9));
        assertEquals("E175_S90.rd5", router.createFileKey(179.9, -89.9));
        assertEquals("W180_N85.rd5", router.createFileKey(-179.9, 89.9));
        assertEquals("W180_S90.rd5", router.createFileKey(-179.9, -89.9));
    }
}

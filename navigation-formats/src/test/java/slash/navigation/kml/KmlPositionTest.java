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
package slash.navigation.kml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class KmlPositionTest {

    private KmlPosition position(Double longitude, Double latitude, Double elevation, String description) {
        return new KmlPosition(longitude, latitude, elevation, null, null, description);
    }

    @Test
    public void testEqualsReflexiveAndAgainstNullAndOtherType() {
        KmlPosition position = position(11.0, 48.0, 100.0, "a");
        assertEquals(position, position);
        assertNotEquals(position, null);
        assertNotEquals(position, "not a position");
    }

    @Test
    public void testEqualsAndHashCodeForEqualPositions() {
        KmlPosition a = position(11.0, 48.0, 100.0, "a");
        KmlPosition b = position(11.0, 48.0, 100.0, "a");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testNotEqualWhenAnyFieldDiffers() {
        KmlPosition base = position(11.0, 48.0, 100.0, "a");
        assertNotEquals(base, position(12.0, 48.0, 100.0, "a"));   // longitude
        assertNotEquals(base, position(11.0, 49.0, 100.0, "a"));   // latitude
        assertNotEquals(base, position(11.0, 48.0, 200.0, "a"));   // elevation
        assertNotEquals(base, position(11.0, 48.0, 100.0, "b"));   // description
    }

    @Test
    public void testEqualsHandlesNullElevationAndDescription() {
        KmlPosition a = position(11.0, 48.0, null, null);
        KmlPosition b = position(11.0, 48.0, null, null);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, position(11.0, 48.0, 100.0, null));     // null vs non-null elevation
    }

    @Test
    public void testAsKmlPositionReturnsItself() {
        KmlPosition position = position(11.0, 48.0, 100.0, "a");
        assertEquals(position, position.asKmlPosition());
    }
}

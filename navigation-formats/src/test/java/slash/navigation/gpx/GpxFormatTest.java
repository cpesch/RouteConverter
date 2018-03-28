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

package slash.navigation.gpx;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.gpx.GpxFormat.parseHeading;
import static slash.navigation.gpx.GpxFormat.parseSpeed;

public class GpxFormatTest {

    @Test
    public void testExtractSpeed() {
        assertDoubleEquals(9.0, parseSpeed("9Km/h"));
        assertDoubleEquals(9.0, parseSpeed(" 9 Km/h "));
        assertDoubleEquals(99.0, parseSpeed(" 99 Km/h "));
        assertDoubleEquals(99.9, parseSpeed(" 99.9 km/h "));
        assertDoubleEquals(99.99999, parseSpeed(" 99.99999 Km/h "));
        assertDoubleEquals(9.0, parseSpeed("Speed: 9 Km/h "));
        assertDoubleEquals(9.0, parseSpeed("Geschwindigkeit: 9 Km/h "));
        assertDoubleEquals(9.0, parseSpeed("Lat.=54.144422, Long.=12.098487, Alt.=5.000000m, Speed=9Km/h, Course=270deg."));
        assertDoubleEquals(9.0, parseSpeed("1007; Speed 9.0 km/h Distance 15.41 km"));
        assertDoubleEquals(9.0, parseSpeed("egal Speed 9.0 km/h egal"));
        assertNull(parseSpeed("egal"));
    }

    @Test
    public void testExtractHeading() {
        assertDoubleEquals(270.0, parseHeading("Lat.=54.144422, Long.=12.098487, Alt.=5.000000m, Speed=9Km/h, Course=270deg."));
        assertNull(parseHeading("egal"));
    }

    @Test
    public void testExtractReason() {
        GpxPosition position = new GpxPosition(null, null, null, null, null, null);
        assertNull(position.getDescription());
        assertNull(position.getCity());
        assertNull(position.getReason());
        position.setDescription("Course 97 : Barmbek-Nord");
        assertEquals("Barmbek-Nord", position.getDescription());
        assertEquals("Barmbek-Nord", position.getCity());
        assertEquals("Course 97", position.getReason());
        position.setDescription("Course 97 : Barmbek-Nord; 14.2 Km");
        assertEquals("Barmbek-Nord; 14.2 Km", position.getDescription());
        assertEquals("Barmbek-Nord; 14.2 Km", position.getCity());
        assertEquals("Course 97", position.getReason());
    }

    @Test
    public void testExtractDescription() {
        GpxPosition position = new GpxPosition(null, null, null, null, null, null);
        assertNull(position.getDescription());
        assertNull(position.getCity());
        assertNull(position.getReason());
        position.setDescription("Bad Oldesloe; 58.0 Km");
        // TODO think about how to solve this with that much errors
        // assertEquals("Bad Oldesloe", position.getDescription());
        // assertEquals("Bad Oldesloe", position.getCity());
        // assertEquals("58.0 Km", position.getReason());
    }
}

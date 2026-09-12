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
package slash.navigation.googlemaps;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Locale;

import static java.util.Locale.ENGLISH;
import static org.junit.Assert.assertEquals;

public class GoogleElevationServiceIT {
    private final GoogleElevationService service = new GoogleElevationService();

    @Before
    public void setUp() {
        Locale.setDefault(ENGLISH);
    }

    @Test
    public void getElevationFor() throws IOException {
        assertEquals(39.3, service.getElevationFor(11.2, 59.0), 0.5);
        assertEquals(179.4086151, service.getElevationFor(11.2, 60.0), 0.5);
        assertEquals(650.1, service.getElevationFor(11.2, 61.0), 0.5);

        assertEquals(77.2, service.getElevationFor(-68.0, -54.0), 0.5);
        assertEquals(457.6, service.getElevationFor(-68.0, -55.0), 0.5);
        assertEquals(-106.956543, service.getElevationFor(-68.0, -56.0), 1.0);
        assertEquals(-109.3644409, service.getElevationFor(-68.0, -56.1), 1.0);
        assertEquals(-2883.9584961, service.getElevationFor(-68.0, -57.0), 1.0);
    }
}

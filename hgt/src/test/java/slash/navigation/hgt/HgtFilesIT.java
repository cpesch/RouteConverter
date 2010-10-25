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
package slash.navigation.hgt;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HgtFilesIT {
    private HgtFiles files = new HgtFiles();

    @Before
    public void setUp() throws Exception {
        HgtFileCache hgtFileCache = new HgtFileCache();
        hgtFileCache.clear();
    }

    @Test
    public void testElevationFor() throws IOException {
        assertEquals(40, files.getElevationFor(11.2, 59.0).intValue());
        assertEquals(190, files.getElevationFor(11.2, 60.0).intValue());
        assertNull(files.getElevationFor(11.2, 61.0));

        assertEquals(77, files.getElevationFor(-68.0, -54.0).intValue());
        assertEquals(455, files.getElevationFor(-68.0, -55.0).intValue());
        assertEquals(null, files.getElevationFor(-68.0, -56.0));
        assertEquals(null, files.getElevationFor(-68.0, -56.1));
        assertEquals(null, files.getElevationFor(-68.0, -57.0));
    }
}

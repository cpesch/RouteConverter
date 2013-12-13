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
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.download.DownloadManager;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HgtFilesIT {
    private DownloadManager downloadManager = new DownloadManager();
    private Map<String, String> mapping = new HashMap<String, String>();
    private HgtFiles files = new HgtFiles("test", "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/", mapping, "test", downloadManager);
    {
        mapping.put("N59E011", "Eurasia/N59E011.hgt.zip");
        mapping.put("N60E012", "Eurasia/N60E012.hgt.zip");
    }

    @Before
    public void setUp() throws Exception {
        files.downloadElevationFor(Arrays.asList(new LongitudeAndLatitude(11.2, 59.0), new LongitudeAndLatitude(12.0, 60.2)));
    }

    @Test
    public void testElevationFor() throws IOException {
        Double elevation1 = files.getElevationFor(11.2, 59.0);
        assertNotNull(elevation1);
        assertEquals(40, elevation1.intValue());
        Double elevation2 = files.getElevationFor(12.0, 60.2);
        assertNotNull(elevation2);
        assertEquals(211, elevation2.intValue());
        assertNull(files.getElevationFor(11.2, 61.0));
    }
}

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
import slash.navigation.download.datasources.File;
import slash.navigation.download.datasources.Fragment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HgtFilesIT {
    private Map<String, Fragment> archiveMap = new HashMap<String, Fragment>();
    private Map<String, File> fileMap = new HashMap<String, File>();
    private HgtFiles files;
    {
        archiveMap.put("N59E011", new Fragment("N59E011", "Eurasia/N59E011.hgt.zip", 2884802L, "notdefined", null));
        archiveMap.put("N60E012", new Fragment("N60E012", "Eurasia/N60E012.hgt.zip", 2884802L, "notdefined", null));
    }

    @Before
    public void setUp() throws Exception {
        files = new HgtFiles("test", "http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/", "test", archiveMap, fileMap,
                new DownloadManager(createTempFile("queueFile", ".xml")));
        files.downloadElevationDataFor(asList(new LongitudeAndLatitude(11.2, 59.0), new LongitudeAndLatitude(12.0, 60.2)));
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

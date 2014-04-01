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
package slash.navigation.graphhopper;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;

import java.io.IOException;

import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class GraphHopperIT {
    private GraphHopper router;

    @Before
    public void setUp() throws IOException {
        router = new GraphHopper();
        router.setDownloadManager(new DownloadManager(createTempFile("queueFile", ".xml")));
        DownloadFuture future = router.downloadRoutingDataFor(asList(new LongitudeAndLatitude(10.18587, 53.40451)));
        if(future.isRequiresDownload())
            future.download();
        router.initialize();

    }

    @Test
    public void testGetRouteBetween() {
        RoutingResult result = router.getRouteBetween(new SimpleNavigationPosition(10.18587, 53.40451),
                new SimpleNavigationPosition(10.06767, 53.49249));
        assertEquals(147, result.getPositions().size());
        assertEquals(13633.0, result.getDistance(), 5.0);
        assertEquals(981243, result.getTime(), 100);
    }
}

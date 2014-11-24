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

import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.TravelMode;

import java.io.IOException;

import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BRouterIT {
    private static final NavigationPosition FROM = new SimpleNavigationPosition(10.18587, 53.40451);
    private static final NavigationPosition TO = new SimpleNavigationPosition(10.06767, 53.49249);
    private static final String URI = "E10_N50.rd5";

    private BRouter router;

    @Before
    public void setUp() throws IOException {
        Downloadable downloadable = mock(Downloadable.class);
        when(downloadable.getUri()).thenReturn(URI);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getDownloadable(URI)).thenReturn(downloadable);
        when(dataSource.getBaseUrl()).thenReturn("http://h2096617.stratoserver.net/brouter/segments2/");
        when(dataSource.getDirectory()).thenReturn("test");
        router = new BRouter(dataSource, new DownloadManager(createTempFile("queueFile", ".xml")));
        DownloadFuture future = router.downloadRoutingDataFor(asList(new LongitudeAndLatitude(10.18587, 53.40451)));
        if (future.isRequiresDownload())
            future.download();
    }

    private TravelMode getTravelMode(String lookupName) {
        for (TravelMode travelMode : router.getAvailableTravelModes()) {
            if (lookupName.equals(travelMode.getName()))
                return travelMode;
        }
        throw new IllegalArgumentException(lookupName + " not found");
    }

    @Test
    public void testGetRouteBetweenByCar() {
        RoutingResult result = router.getRouteBetween(FROM, TO, getTravelMode("car-test"));
        assertEquals(162, result.getPositions().size());
        assertEquals(13810.0, result.getDistance(), 5.0);
        assertEquals(0, result.getTime());
        assertTrue(result.isValid());
    }

    @Test
    public void testGetRouteBetweenByBike() {
        RoutingResult result = router.getRouteBetween(FROM, TO, getTravelMode("trekking"));
        assertEquals(110, result.getPositions().size());
        assertEquals(13945.0, result.getDistance(), 5.0);
        assertEquals(0, result.getTime());
        assertTrue(result.isValid());
    }
}

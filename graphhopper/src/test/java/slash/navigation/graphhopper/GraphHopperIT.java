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
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Action;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.TravelMode;

import java.io.File;
import java.io.IOException;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getApplicationDirectory;

public class GraphHopperIT {
    private static final NavigationPosition FROM = new SimpleNavigationPosition(10.18587, 53.40451);
    private static final NavigationPosition TO = new SimpleNavigationPosition(10.06767, 53.49249);
    private static final String URI = "europe/germany/hamburg-latest.osm.pbf";

    private GraphHopper hopper;

    @Before
    public void setUp() throws IOException {
        Downloadable downloadable = mock(Downloadable.class);
        when(downloadable.getUri()).thenReturn(URI);
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getDownloadable(URI)).thenReturn(downloadable);
        when(dataSource.getBaseUrl()).thenReturn("http://download.geofabrik.de/");
        when(dataSource.getDirectory()).thenReturn("test");
        when(dataSource.getAction()).thenReturn(Action.Copy.name());
        slash.navigation.datasources.File file = mock(slash.navigation.datasources.File.class);
        when(file.getBoundingBox()).thenReturn(new BoundingBox(10.33637, 53.7465, 9.613465, 53.38581));
        when(file.getUri()).thenReturn(URI);
        when(dataSource.getFiles()).thenReturn(singletonList(file));
        hopper = new GraphHopper(new DownloadManager(createTempFile("queueFile", ".xml")));
        hopper.setDataSource(dataSource);
        DownloadFuture future = hopper.downloadRoutingDataFor(singletonList(new LongitudeAndLatitude(10.18587, 53.40451)));
        if(future.isRequiresDownload())
            future.download();
        else {
            hopper.setOsmPbfFile(new File(getApplicationDirectory("test"), URI));
            hopper.initializeHopper();
        }
    }

    private TravelMode getTravelMode(String lookupName) {
        for (TravelMode travelMode : hopper.getAvailableTravelModes()) {
            if (lookupName.equals(travelMode.getName()))
                return travelMode;
        }
       throw new IllegalArgumentException(lookupName + " not found");
    }

    @Test
    public void testGetRouteBetweenByCar() {
        RoutingResult result = hopper.getRouteBetween(FROM, TO, getTravelMode("Car"));
        assertEquals(172, result.getPositions().size());
        assertEquals(13633.0, result.getDistance(), 5.0);
        assertEquals(1062488, result.getTime(), 100);
        assertTrue(result.isValid());
    }

    @Test
    public void testGetRouteBetweenByBike() {
        RoutingResult result = hopper.getRouteBetween(FROM, TO, getTravelMode("Bike"));
        assertEquals(118, result.getPositions().size());
        assertEquals(13655.4, result.getDistance(), 5.0);
        assertEquals(2752840, result.getTime(), 100);
        assertTrue(result.isValid());
    }
}

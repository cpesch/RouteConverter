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
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.TravelMode;

import java.io.File;
import java.io.IOException;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static slash.common.io.Directories.getApplicationDirectory;

public class GraphHopperIT {
    private GraphHopper hopper;

    @Before
    public void setUp() throws IOException {
        hopper = new GraphHopper(null, null);
        DownloadFuture future = hopper.downloadRoutingDataFor(asList(new LongitudeAndLatitude(10.18587, 53.40451)));
        if(future.isRequiresDownload())
            future.download();
        else
            hopper.initializeHopper(new File(getApplicationDirectory("graphhopper"), "europe/germany/hamburg-latest.osm.pbf"));
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
        RoutingResult result = hopper.getRouteBetween(new SimpleNavigationPosition(10.18587, 53.40451),
                new SimpleNavigationPosition(10.06767, 53.49249), getTravelMode("Car"));
        assertEquals(149, result.getPositions().size());
        assertEquals(13633.0, result.getDistance(), 5.0);
        assertEquals(980824, result.getTime(), 100);
    }

    @Test
    public void testGetRouteBetweenByBike() {
        RoutingResult result = hopper.getRouteBetween(new SimpleNavigationPosition(10.0907221, 53.5790863),
                new SimpleNavigationPosition(10.1510401, 53.5994911), getTravelMode("Bike"));
        assertEquals(109, result.getPositions().size());
        assertEquals(5277.44, result.getDistance(), 5.0);
        assertEquals(668473.0, result.getTime(), 100);
    }
}

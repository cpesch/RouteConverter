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
import slash.navigation.download.Action;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;
import slash.navigation.routing.RoutingResult;
import slash.navigation.routing.TravelMode;

import java.io.File;
import java.io.IOException;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Externalization.extractFile;
import static slash.navigation.routing.RoutingResult.Validity.Valid;

public class BRouterIT {
    private static final NavigationPosition FROM = new SimpleNavigationPosition(10.18587, 53.40451);
    private static final NavigationPosition TO = new SimpleNavigationPosition(10.06767, 53.49249);
    private static final String CAR_PROFILE_URI = "car-eco.brf";
    private static final String TREKKING_PROFILE_URI = "trekking.brf";
    private static final String SEGMENT_URI = "E10_N50.rd5";

    private BRouter router;

    @Before
    public void setUp() throws IOException {
        Downloadable car = mock(Downloadable.class);
        when(car.getUri()).thenReturn(CAR_PROFILE_URI);
        Downloadable trekking = mock(Downloadable.class);
        when(trekking.getUri()).thenReturn(TREKKING_PROFILE_URI);
        DataSource profiles = mock(DataSource.class);
        when(profiles.getDownloadable(CAR_PROFILE_URI)).thenReturn(car);
        when(profiles.getDownloadable(TREKKING_PROFILE_URI)).thenReturn(trekking);
        when(profiles.getBaseUrl()).thenReturn("http://h2096617.stratoserver.net/brouter/profiles2/");
        when(profiles.getDirectory()).thenReturn("test");
        prepareFile(profiles.getDirectory(), CAR_PROFILE_URI);
        prepareFile(profiles.getDirectory(), TREKKING_PROFILE_URI);
        prepareFile(profiles.getDirectory(), "lookups.dat");

        Downloadable segment = mock(Downloadable.class);
        when(segment.getUri()).thenReturn(SEGMENT_URI);
        DataSource brouterSegments = mock(DataSource.class);
        when(brouterSegments.getDownloadable(SEGMENT_URI)).thenReturn(segment);
        when(brouterSegments.getBaseUrl()).thenReturn("http://h2096617.stratoserver.net/brouter/segments4/");
        when(brouterSegments.getDirectory()).thenReturn("test");
        when(brouterSegments.getAction()).thenReturn(Action.Copy.name());

        router = new BRouter(new DownloadManager(createTempFile("queueFile", ".xml")));
        router.setProfilesAndSegments(profiles, brouterSegments);
        DownloadFuture future = router.downloadRoutingDataFor(null, singletonList(new LongitudeAndLatitude(10.18587, 53.40451)));
        if (future.isRequiresDownload())
            future.download();
    }

    private void prepareFile(String directory, String fileName) throws IOException {
        File src = extractFile("slash/navigation/brouter/" + fileName);
        assertNotNull(src);
        File dest = new File(getApplicationDirectory(directory), src.getName());
        if (!dest.exists())
            assertTrue(src.renameTo(dest));
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
        RoutingResult result = router.getRouteBetween(FROM, TO, getTravelMode("car-eco"));
        assertEquals(Valid, result.getValidity());
        assertEquals(233, result.getPositions().size(), 10);
        assertEquals(13754.0, result.getDistanceAndTime().getDistance(), 25.0);
        assertEquals(500, result.getDistanceAndTime().getTimeInMillis(), 2);
    }

    @Test
    public void testGetRouteBetweenByBike() {
        RoutingResult result = router.getRouteBetween(FROM, TO, getTravelMode("trekking"));
        assertEquals(Valid, result.getValidity());
        assertEquals(153, result.getPositions().size(), 8);
        assertEquals(13899.0, result.getDistanceAndTime().getDistance(), 25.0);
        assertEquals(2332890, result.getDistanceAndTime().getTimeInMillis(), 1000);
    }
}

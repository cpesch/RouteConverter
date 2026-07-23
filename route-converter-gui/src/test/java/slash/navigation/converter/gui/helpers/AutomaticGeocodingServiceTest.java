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
package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.GeocodingService;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;
import slash.navigation.gui.models.InMemoryPreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AutomaticGeocodingServiceTest {
    @Test
    public void prefersOfflineMapsforgePoiBeforeMapAndOnlineResults() throws Exception {
        Preferences preferences = new InMemoryPreferences();
        GeocodingServiceFacade facade = new GeocodingServiceFacade(preferences);
        AutomaticGeocodingService automatic = new AutomaticGeocodingService(facade);
        GeocodingService mapsforgePoi = new TestGeocodingService("Mapsforge POI", singletonPositions("offline-poi"));
        GeocodingService mapsforgeMap = new TestGeocodingService("Mapsforge Map", singletonPositions("offline-map"));
        GeocodingService nominatim = new TestGeocodingService("Nominatim", singletonPositions("online"));
        GeocodingService geonames = new TestGeocodingService("GeoNames", singletonPositions("backup"));

        facade.addGeocodingService(automatic);
        facade.addGeocodingService(geonames);
        facade.addGeocodingService(nominatim);
        facade.addGeocodingService(mapsforgeMap);
        facade.addGeocodingService(mapsforgePoi);

        List<GeocodingResult> results = automatic.getPositionsFor("Berlin");

        assertEquals(4, results.size());
        assertEquals("Mapsforge POI", results.get(0).getGeocodingServiceName());
        assertEquals("Mapsforge Map", results.get(1).getGeocodingServiceName());
        assertEquals("Nominatim", results.get(2).getGeocodingServiceName());
        assertEquals("GeoNames", results.get(3).getGeocodingServiceName());
    }

    @Test
    public void returnsEmptyForNoMatchesEvenWhenAServiceFails() throws Exception {
        // issue #9: searching a place that does not exist (e.g. "mannheim hofgarten")
        // must not raise an error just because an incidental service (here: Google)
        // is denied/over-limit. A reachable service returning no matches is a valid
        // empty result, not a failure.
        Preferences preferences = Preferences.userRoot().node("/RouteConverter-test/" + getClass().getName() + "/" + UUID.randomUUID());
        try {
            GeocodingServiceFacade facade = new GeocodingServiceFacade(preferences);
            AutomaticGeocodingService automatic = new AutomaticGeocodingService(facade);
            facade.addGeocodingService(automatic);
            facade.addGeocodingService(new TestGeocodingService("Nominatim", emptyList()));
            facade.addGeocodingService(new FailingGeocodingService("Google Maps"));

            List<GeocodingResult> results = automatic.getPositionsFor("mannheim hofgarten");

            assertTrue(results.isEmpty());
        } finally {
            preferences.removeNode();
        }
    }

    @Test
    public void throwsWhenEveryServiceFails() throws Exception {
        // Total failure (e.g. offline) still surfaces the error rather than
        // masquerading as "no matches".
        Preferences preferences = Preferences.userRoot().node("/RouteConverter-test/" + getClass().getName() + "/" + UUID.randomUUID());
        try {
            GeocodingServiceFacade facade = new GeocodingServiceFacade(preferences);
            AutomaticGeocodingService automatic = new AutomaticGeocodingService(facade);
            facade.addGeocodingService(automatic);
            facade.addGeocodingService(new FailingGeocodingService("Nominatim"));
            facade.addGeocodingService(new FailingGeocodingService("Google Maps"));

            try {
                automatic.getPositionsFor("anything");
                fail("expected IOException when no service can complete a query");
            } catch (IOException expected) {
                // expected
            }
        } finally {
            preferences.removeNode();
        }
    }

    private List<CategorizedNavigationPosition> singletonPositions(String description) {
        List<CategorizedNavigationPosition> positions = new ArrayList<>();
        positions.add(new SimpleCategorizedNavigationPosition(1.0, 2.0, null, description, null));
        return positions;
    }

    private static class TestGeocodingService extends BaseGeocodingService {
        private final String name;
        private final List<CategorizedNavigationPosition> positions;

        private TestGeocodingService(String name, List<CategorizedNavigationPosition> positions) {
            this.name = name;
            this.positions = positions;
        }

        public String getName() {
            return name;
        }

        public boolean isDownload() {
            return false;
        }

        public boolean isOverQueryLimit() {
            return false;
        }

        public List<GeocodingResult> getPositionsFor(String address) {
            return asGeocodingResults(positions);
        }

        public String getAddressFor(NavigationPosition position) {
            return null;
        }
    }

    private static class FailingGeocodingService extends BaseGeocodingService {
        private final String name;

        private FailingGeocodingService(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean isDownload() {
            return false;
        }

        public boolean isOverQueryLimit() {
            return false;
        }

        public List<GeocodingResult> getPositionsFor(String address) throws IOException {
            throw new IOException("service unavailable");
        }

        public String getAddressFor(NavigationPosition position) throws IOException {
            throw new IOException("service unavailable");
        }
    }
}


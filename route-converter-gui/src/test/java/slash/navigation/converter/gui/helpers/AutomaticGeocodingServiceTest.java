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

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;

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
}


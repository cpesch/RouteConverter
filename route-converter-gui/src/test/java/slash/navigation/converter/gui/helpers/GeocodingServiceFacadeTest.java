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

import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.geocoding.GeocodingService;
import slash.navigation.geocoding.SimpleCategorizedNavigationPosition;
import slash.navigation.gui.models.InMemoryPreferences;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GeocodingServiceFacadeTest {
    private Preferences preferences;

    @Before
    public void setUp() {
        preferences = new InMemoryPreferences();
    }

    @Test
    public void returnsResultsAnnotatedWithTheirServiceName() throws IOException, ServiceUnavailableException {
        GeocodingServiceFacade facade = new GeocodingServiceFacade(preferences);
        GeocodingService nominatim = new TestGeocodingService("Nominatim", singletonPositions("one"));
        GeocodingService photon = new TestGeocodingService("Photon", singletonPositions("two"));
        facade.addGeocodingService(nominatim);
        facade.addGeocodingService(photon);
        facade.setPreferredGeocodingService(nominatim);
        facade.setGeocodingService(nominatim);

        List<GeocodingResult> results = facade.getPositionsFor("Berlin");

        assertEquals(1, results.size());
        assertEquals("Nominatim", results.get(0).getGeocodingServiceName());
        assertEquals("one", results.get(0).getPosition().getDescription());
    }

    @Test
    public void automaticQueriesAllGeocodingServices() throws IOException, ServiceUnavailableException {
        GeocodingServiceFacade facade = new GeocodingServiceFacade(preferences);
        AutomaticGeocodingService automatic = new AutomaticGeocodingService(facade);
        GeocodingService geonames = new TestGeocodingService("GeoNames", singletonPositions("geonames"));
        GeocodingService nominatim = new TestGeocodingService("Nominatim", singletonPositions("nominatim"));
        GeocodingService photon = new TestGeocodingService("Photon", singletonPositions("photon"));
        facade.addGeocodingService(automatic);
        facade.addGeocodingService(geonames);
        facade.addGeocodingService(nominatim);
        facade.addGeocodingService(photon);
        facade.setPreferredGeocodingService(automatic);
        facade.setGeocodingService(automatic);

        List<GeocodingResult> results = facade.getPositionsFor("Berlin");

        assertEquals(3, results.size());
        assertEquals("Nominatim", results.get(0).getGeocodingServiceName());
        assertEquals("Photon", results.get(1).getGeocodingServiceName());
        assertEquals("GeoNames", results.get(2).getGeocodingServiceName());
    }

    @Test
    public void returnsNullWhenNoGeocodingServiceReturnsResults() throws IOException, ServiceUnavailableException {
        GeocodingServiceFacade facade = new GeocodingServiceFacade(preferences);
        GeocodingService nominatim = new TestGeocodingService("Nominatim", null);
        facade.addGeocodingService(nominatim);
        facade.setPreferredGeocodingService(nominatim);
        facade.setGeocodingService(nominatim);

        List<GeocodingResult> results = facade.getPositionsFor("Nowhere");

        assertNull(results);
    }

    @Test
    public void getPositionForReturnsFirstResultFromSelectedService() throws IOException, ServiceUnavailableException {
        GeocodingServiceFacade facade = new GeocodingServiceFacade(preferences);
        GeocodingService nominatim = new TestGeocodingService("Nominatim", singletonPositions("one"));
        GeocodingService photon = new TestGeocodingService("Photon", singletonPositions("two"));
        facade.addGeocodingService(nominatim);
        facade.addGeocodingService(photon);
        facade.setPreferredGeocodingService(nominatim);
        facade.setGeocodingService(nominatim);

        NavigationPosition position = facade.getPositionFor("Berlin");

        assertEquals("one", position.getDescription());
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


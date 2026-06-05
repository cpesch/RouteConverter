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

package slash.navigation.kml;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationTestCase.*;
import static slash.navigation.base.RouteCharacteristics.Track;

public class KmlFormatIT {

    private List<KmlRoute> readNetworkLinkedKmlFile(BaseKmlFormat format, String directFileName, String networkLinkFileName) throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("kml-network-link-");
        temporaryDirectory.toFile().deleteOnExit();

        Path directSource = Path.of(TEST_PATH, directFileName);
        Path directTarget = temporaryDirectory.resolve(directFileName);
        Files.copy(directSource, directTarget, StandardCopyOption.REPLACE_EXISTING);
        directTarget.toFile().deleteOnExit();

        String href = "file:///CWD/../RouteSamples/trunk/test/" + directFileName;
        String networkLink = Files.readString(Path.of(TEST_PATH, networkLinkFileName), StandardCharsets.UTF_8)
                .replace(href, directTarget.toUri().toString());
        Path networkLinkTarget = temporaryDirectory.resolve(networkLinkFileName);
        Files.writeString(networkLinkTarget, networkLink, StandardCharsets.UTF_8);
        networkLinkTarget.toFile().deleteOnExit();

        List<KmlRoute> directRoute = readKmlFile(format, directTarget.toFile().getAbsolutePath());
        List<KmlRoute> networkLinkRoute = readKmlFile(format, networkLinkTarget.toFile().getAbsolutePath());
        assertRoutesEquals(directRoute, networkLinkRoute);
        return networkLinkRoute;
    }

    private void assertRoutesEquals(List<KmlRoute> firstRoutes, List<KmlRoute> secondRoutes) {
        for (int i = 0; i < firstRoutes.size(); i++) {
            KmlRoute first = firstRoutes.get(i);
            KmlRoute second = secondRoutes.get(i);
            assertEquals(first.getCharacteristics(), second.getCharacteristics());
            assertEquals(first.getDescription(), second.getDescription());
            assertEquals(first.getName(), second.getName());
        }
        for (int i = 0; i < firstRoutes.size(); i++) {
            KmlRoute first = firstRoutes.get(i);
            KmlRoute second = secondRoutes.get(i);
            assertPositionsEquals(first.getPositions(), second.getPositions());
        }
        assertEquals(firstRoutes, secondRoutes);
    }

    private void assertPositionsEquals(List<KmlPosition> firstPositions, List<KmlPosition> secondPositions) {
        for (int i = 0; i < firstPositions.size(); i++) {
            KmlPosition first = firstPositions.get(i);
            KmlPosition second = secondPositions.get(i);
            assertEquals(first.getDescription(), second.getDescription());
            assertEquals(first.getElevation(), second.getElevation());
            assertEquals(first.getLatitude(), second.getLatitude());
            assertEquals(first.getLongitude(), second.getLongitude());
        }
    }

    @Test
    public void testKmlVsKmz20() throws Exception {
        List<KmlRoute> kmlRoute = readKmlFile(new Kml20Format(), SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course.kml");
        List<KmlRoute> kmzRoute = readKmlFile(new Kmz20Format(), SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course.kmz");
        assertRoutesEquals(kmlRoute, kmzRoute);
    }

    @Test
    public void testKmlVsKmz21() throws Exception {
        List<KmlRoute> kmlRoute = readKmlFile(new Kml21Format(), SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course by Google Earth.kml");
        List<KmlRoute> kmzRoute = readKmlFile(new Kmz21Format(), SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course by Google Earth.kmz");
        assertEquals(kmlRoute, kmzRoute);
    }

    @Test
    public void testItnConvKml() throws Exception {
        List<KmlRoute> routes = readKmlFile(new GarbleKml21Format(), SAMPLE_PATH + "bcr_with_itnconv.kml");
        assertNotNull(routes);
        assertEquals(2, routes.size());
        for (KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(46, routes.get(0).getPositionCount());
        assertEquals(46, routes.get(1).getPositionCount());
    }

    @Test
    public void testDirectVsNetworklink20() throws Exception {
        readNetworkLinkedKmlFile(new Kml20Format(), "from20.kml", "from20nwlink.kml");
    }

    @Test
    public void testDirectVsNetworklink21() throws Exception {
        readNetworkLinkedKmlFile(new Kml21Format(), "from21.kml", "from21nwlink.kml");
    }

    @Test
    public void testDirectVsNetworklink22() throws Exception {
        readNetworkLinkedKmlFile(new Kml22Format(), "from22.kml", "from22nwlink.kml");
    }

    @Test
    public void testGoogleEarthNetworkLink() throws Exception {
        List<KmlRoute> routes = readKmlFile(new Kml22Format(), SAMPLE_PATH + "network_link_google_earth.kml");
        assertNotNull(routes);
        TestCase.assertEquals(2, routes.size());
    }

    @Test
    public void testGoogleMapsNetworkLink() throws Exception {
        List<KmlRoute> routes = readKmlFile(new Kml21Format(), SAMPLE_PATH + "network_link_google_maps.kml");
        assertNotNull(routes);
        TestCase.assertEquals(2, routes.size());
    }

    @Test
    public void testOnlyPlacemark() throws Exception {
        List<KmlRoute> routes = readKmlFile(new Kml22BetaFormat(), SAMPLE_PATH + "Home to Corfe Castle.kml");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
    }

    @Test
    public void testNoKmlRoot20() throws Exception {
        List<KmlRoute> routes = readKmlFile(new Kml20Format(), SAMPLE_PATH + "MIK-Tour - Nuerburgring 7.10.2007.kml");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1297, route.getPositionCount());
    }

    @Test
    public void testTrackExtension22() throws Exception {
        List<KmlRoute> routes = readKmlFile(new Kml22Format(), TEST_PATH + "from22track.kml");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(10, route.getPositionCount());
    }

    @Test
    public void testFlytoExtension22() throws Exception {
        List<KmlRoute> routes = readKmlFile(new Kml22Format(), TEST_PATH + "from22flyto.kml");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(54, route.getPositionCount());
    }
}
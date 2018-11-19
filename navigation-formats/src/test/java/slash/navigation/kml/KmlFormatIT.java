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
import slash.navigation.kml.binding20.Kml;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationTestCase.*;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.kml.KmlUtil.*;

public class KmlFormatIT {
    @Test
    public void testReader() throws FileNotFoundException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        Kml kml = (Kml) newUnmarshaller20().unmarshal(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test
    public void testInputStream() throws FileNotFoundException, JAXBException {
        InputStream in = new FileInputStream(TEST_PATH + "from20.kml");
        Kml kml = (Kml) newUnmarshaller20().unmarshal(in);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test
    public void testUnmarshal20() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        Kml kml = unmarshal20(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test(expected = JAXBException.class)
    public void testUnmarshal20TypeError() throws Exception {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        unmarshal21(reader);
    }

    @Test
    public void testUnmarshal21() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from21.kml");
        slash.navigation.kml.binding21.KmlType kml = unmarshal21(reader);
        assertNotNull(kml);
    }

    @Test(expected = JAXBException.class)
    public void testUnmarshal21TypeError() throws Exception {
        Reader reader = new FileReader(TEST_PATH + "from21.kml");
        unmarshal20(reader);
    }

    @Test
    public void testUnmarshal22Beta() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from22beta.kml");
        slash.navigation.kml.binding22beta.KmlType kml = unmarshal22Beta(reader);
        assertNotNull(kml);
    }

    @Test
    public void testUnmarshal22() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from22.kml");
        slash.navigation.kml.binding22.KmlType kml = unmarshal22(reader);
        assertNotNull(kml);
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
        List<KmlRoute> directRoute = readKmlFile(new Kml20Format(), TEST_PATH + "from20.kml");
        List<KmlRoute> networkLinkRoute = readKmlFile(new Kml20Format(), TEST_PATH + "from20nwlink.kml");
        assertRoutesEquals(directRoute, networkLinkRoute);
    }

    @Test
    public void testDirectVsNetworklink21() throws Exception {
        List<KmlRoute> directRoute = readKmlFile(new Kml21Format(), TEST_PATH + "from21.kml");
        List<KmlRoute> networkLinkRoute = readKmlFile(new Kml21Format(), TEST_PATH + "from21nwlink.kml");
        assertRoutesEquals(directRoute, networkLinkRoute);
    }

    @Test
    public void testDirectVsNetworklink22() throws Exception {
        List<KmlRoute> directRoute = readKmlFile(new Kml22Format(), TEST_PATH + "from22.kml");
        List<KmlRoute> networkLinkRoute = readKmlFile(new Kml22Format(), TEST_PATH + "from22nwlink.kml");
        assertRoutesEquals(directRoute, networkLinkRoute);
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
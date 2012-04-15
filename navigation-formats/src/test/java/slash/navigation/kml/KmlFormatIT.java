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

import org.junit.Test;
import slash.navigation.kml.binding20.Kml;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.RouteCharacteristics.Track;

public class KmlFormatIT {

    @Test
    public void testReader() throws FileNotFoundException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        Kml kml = (Kml) KmlUtil.newUnmarshaller20().unmarshal(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test
    public void testInputStream() throws FileNotFoundException, JAXBException {
        InputStream in = new FileInputStream(TEST_PATH + "from20.kml");
        Kml kml = (Kml) KmlUtil.newUnmarshaller20().unmarshal(in);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test
    public void testUnmarshal20() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        Kml kml = KmlUtil.unmarshal20(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    @Test
    public void testUnmarshal20TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        try {
            KmlUtil.unmarshal21(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    @Test
    public void testUnmarshal21() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from21.kml");
        slash.navigation.kml.binding21.KmlType kml = KmlUtil.unmarshal21(reader);
        assertNotNull(kml);
    }

    @Test
    public void testUnmarshal21TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from21.kml");
        try {
            KmlUtil.unmarshal20(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    @Test
    public void testUnmarshal22Beta() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from22beta.kml");
        slash.navigation.kml.binding22beta.KmlType kml = KmlUtil.unmarshal22Beta(reader);
        assertNotNull(kml);
    }

    @Test
    public void testUnmarshal22() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from22.kml");
        slash.navigation.kml.binding22.KmlType kml = KmlUtil.unmarshal22(reader);
        assertNotNull(kml);
    }

    @Test
    public void testKmlVsKmz20() throws IOException {
        List<KmlRoute> kmlRoute = new Kml20Format().read(new FileInputStream(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course.kml")));
        List<KmlRoute> kmzRoute = new Kmz20Format().read(new FileInputStream(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course.kmz")));
        assertEquals(kmlRoute, kmzRoute);
    }

    @Test
    public void testKmlVsKmz21() throws IOException {
        List<KmlRoute> kmlRoute = new Kml21Format().read(new FileInputStream(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course by Google Earth.kml")));
        List<KmlRoute> kmzRoute = new Kmz21Format().read(new FileInputStream(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course by Google Earth.kmz")));
        assertEquals(kmlRoute, kmzRoute);
    }

    @Test
    public void testNetworkLink20() throws IOException {
        List<KmlRoute> routes = new Kml20Format().read(new FileInputStream(new File(SAMPLE_PATH + "www.gps-tour.info20.kml")));
        assertNotNull(routes);
        assertEquals(6, routes.size());
        for (KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(18724, routes.get(1).getPositionCount());
        assertEquals(2658, routes.get(4).getPositionCount());
    }

    @Test
    public void testItnConvKml() throws IOException {
        List<KmlRoute> routes = new BrokenKml21Format().read(new FileInputStream(new File(SAMPLE_PATH + "bcr_with_itnconv.kml")));
        assertNotNull(routes);
        assertEquals(2, routes.size());
        for (KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(46, routes.get(0).getPositionCount());
        assertEquals(46, routes.get(1).getPositionCount());
    }

    @Test
    public void testNetworkLink21() throws IOException {
        List<KmlRoute> routes = new Kml21Format().read(new FileInputStream(new File(SAMPLE_PATH + "www.gps-tour.info21.kml")));
        assertEquals(6, routes.size());
        for (KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(18724, routes.get(1).getPositionCount());
        assertEquals(2658, routes.get(4).getPositionCount());
    }

    @Test
    public void testNetworkLink22() throws IOException {
        List<KmlRoute> routes = new Kml22Format().read(new FileInputStream(new File(SAMPLE_PATH + "www.gps-tour.info22.kml")));
        assertEquals(6, routes.size());
        for (KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(18724, routes.get(1).getPositionCount());
        assertEquals(2658, routes.get(4).getPositionCount());
    }

    @Test
    public void testOnlyPlacemark() throws IOException {
        List<KmlRoute> routes = new Kml22BetaFormat().read(new FileInputStream(new File(SAMPLE_PATH + "Home to Corfe Castle.kml")));
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
    }

    @Test
    public void testNoKmlRoot20() throws IOException {
        List<KmlRoute> routes = new Kml20Format().read(new FileInputStream(new File(SAMPLE_PATH + "MIK-Tour - Nuerburgring 7.10.2007.kml")));
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1297, route.getPositionCount());
    }

    @Test
    public void testTrackExtension22() throws IOException {
        List<KmlRoute> routes = new Kml22Format().read(new FileInputStream(new File(TEST_PATH + "from22track.kml")));
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(133, route.getPositionCount());
    }

    @Test
    public void testFlytoExtension22() throws IOException {
        List<KmlRoute> routes = new Kml22Format().read(new FileInputStream(new File(TEST_PATH + "from22flyto.kml")));
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals(54, route.getPositionCount());
    }
}
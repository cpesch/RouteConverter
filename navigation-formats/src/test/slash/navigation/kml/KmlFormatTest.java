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

import junit.framework.Assert;
import slash.navigation.NavigationTestCase;
import slash.navigation.kml.binding20.Kml;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.List;

public class KmlFormatTest extends NavigationTestCase {
    KmlFormat format = new Kml20Format();

    public void testIsPosition() {
        assertTrue(format.isPosition("5.783245,50.28655,512"));
        assertTrue(format.isPosition("-0.203733,51.47185,512"));
        assertTrue(format.isPosition("151.777,-32.88815,512"));
        assertTrue(format.isPosition("-0.203733,-32.88815,512"));
        assertTrue(format.isPosition("-0.203733,-32.88815,-0.1"));
        assertTrue(format.isPosition("+0.203733,+32.88815,+0.1"));
        assertTrue(format.isPosition("10.032004,53.569488,64296162722.124001"));
        assertTrue(format.isPosition("10.244109,53.571977,0.000000"));
        assertTrue(format.isPosition(" 10.244109,53.571977,0.000000"));
        assertTrue(format.isPosition("10.244109,53.571977,0.000000 "));
        assertTrue(format.isPosition("10.244109, 53.571977, 0.000000"));
        assertTrue(format.isPosition(" 10.244109 , 53.571977 , 0.000000 "));
        assertTrue(format.isPosition("\n132.927856\n,\n34.44434\n,\n332.75\n"));
        assertTrue(format.isPosition("\t\t\t\t8.33687,46.90742,1436"));
        assertTrue(format.isPosition(",,0"));
        assertTrue(format.isPosition("11.5709833333333,49.9467027777778"));
        assertTrue(format.isPosition("0.2E-4,-0.2E-5,0.3E-6"));
    }

    public void testParseNullPosition() {
        KmlPosition position = format.parsePosition(",,0", null);
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertNull(position.getComment());
        assertEquals(0.0, position.getElevation());
    }

    public void testParseNoElevationPosition() {
        KmlPosition position = format.parsePosition("11.5709833333333,49.9467027777778", null);
        assertEquals(11.5709833333333, position.getLongitude());
        assertEquals(49.9467027777778, position.getLatitude());
        assertNull(position.getElevation());
        assertNull(position.getComment());
    }

    public void testParseScientificNumberPosition() {
        KmlPosition position = format.parsePosition("0.1E-4,-0.2E-5,0.3E-6", null);
        assertEquals(0.00001, position.getLongitude());
        assertEquals(-0.000002, position.getLatitude());
        assertEquals(0.0000003, position.getElevation());
        assertNull(position.getComment());
    }

    public void testReader() throws FileNotFoundException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        Kml kml = (Kml) KmlUtil.newUnmarshaller20().unmarshal(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    public void testInputStream() throws FileNotFoundException, JAXBException {
        InputStream in = new FileInputStream(TEST_PATH + "from20.kml");
        Kml kml = (Kml) KmlUtil.newUnmarshaller20().unmarshal(in);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    public void testUnmarshal20() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        Kml kml = KmlUtil.unmarshal20(reader);
        assertNotNull(kml);
        assertNotNull(kml.getFolder());
        assertEquals(3, kml.getFolder().getDocumentOrFolderOrGroundOverlay().size());
    }

    public void testUnmarshal20TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from20.kml");
        try {
            KmlUtil.unmarshal21(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    public void testUnmarshal21() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from21.kml");
        slash.navigation.kml.binding21.KmlType kml = KmlUtil.unmarshal21(reader);
        assertNotNull(kml);
    }

    public void testUnmarshal21TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from21.kml");
        try {
            KmlUtil.unmarshal20(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    public void testUnmarshal22() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from22.kml");
        slash.navigation.kml.binding22.KmlType kml = KmlUtil.unmarshal22(reader);
        assertNotNull(kml);
    }

    public void testKmlVsKmz20() throws IOException {
        List<KmlRoute> kmlRoute = new Kml20Format().read(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course.kml"), null);
        List<KmlRoute> kmzRoute = new Kmz20Format().read(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course.kmz"), null);
        assertEquals(kmlRoute, kmzRoute);
    }

    public void testKmlVsKmz21() throws IOException {
        List<KmlRoute> kmlRoute = new Kml21Format().read(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course by Google Earth.kml"), null);
        List<KmlRoute> kmzRoute = new Kmz21Format().read(new File(SAMPLE_PATH + "magnalox ID13885_Hiroshima Race Course by Google Earth.kmz"), null);
        Assert.assertEquals(kmlRoute, kmzRoute);
    }

    public void testNetworkLink20() throws IOException {
        Kml20Format format = new Kml20Format();
        List<KmlRoute> routes = format.read(new File(SAMPLE_PATH + "www.gps-tour.info20.kml"), null);
        assertNotNull(routes);
        assertEquals(6, routes.size());
        for(KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(18723, routes.get(1).getPositionCount());
        assertEquals(2657, routes.get(4).getPositionCount());
    }

    public void testItnConvKml() throws IOException {
        Kml21Format format = new BrokenKml21Format();
        List<KmlRoute> routes = format.read(new File(SAMPLE_PATH + "bcr_with_itnconv.kml"), null);
        assertNotNull(routes);
        assertEquals(2, routes.size());
        for(KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(46, routes.get(0).getPositionCount());
        assertEquals(46, routes.get(1).getPositionCount());
    }

    public void testNetworkLink21() throws IOException {
        Kml21Format format = new Kml21Format();
        List<KmlRoute> routes = format.read(new File(SAMPLE_PATH + "www.gps-tour.info21.kml"), null);
        assertEquals(6, routes.size());
        for(KmlRoute route : routes) {
            assertTrue(route.getPositionCount() > 0);
        }
        assertEquals(18723, routes.get(1).getPositionCount());
        assertEquals(2657, routes.get(4).getPositionCount());
    }

    public void testOnlyPlacemark() throws IOException {
        Kml22Format format = new Kml22Format();
        List<KmlRoute> routes = format.read(new File(SAMPLE_PATH + "Home to Corfe Castle.kml"), null);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1, route.getPositionCount());
    }

    public void testNoKmlRoot20() throws IOException {
        Kml20Format format = new Kml20Format();
        List<KmlRoute> routes = format.read(new File(SAMPLE_PATH + "MIK-Tour - Nürburgring 7.10.2007.kml"), null);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        KmlRoute route = routes.get(0);
        assertEquals(1297, route.getPositionCount());
    }
}

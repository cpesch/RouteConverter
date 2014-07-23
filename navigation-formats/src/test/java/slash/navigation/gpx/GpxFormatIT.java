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

package slash.navigation.gpx;

import org.junit.Test;
import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding11.GpxType;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.NavigationTestCase.readGpxFile;

public class GpxFormatIT {

    @Test
    public void testReader() throws FileNotFoundException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from10.gpx");
        Gpx gpx = (Gpx) GpxUtil.newUnmarshaller10().unmarshal(reader);
        assertNotNull(gpx);
        assertNotNull(gpx.getWpt());
        assertEquals(3, gpx.getWpt().size());
        assertNotNull(gpx.getRte());
        assertEquals(3, gpx.getRte().size());
        assertNotNull(gpx.getTrk());
        assertEquals(3, gpx.getRte().size());
    }

    @Test
    public void testInputStream() throws FileNotFoundException, JAXBException {
        InputStream in = new FileInputStream(TEST_PATH + "from10.gpx");
        Gpx gpx = (Gpx) GpxUtil.newUnmarshaller10().unmarshal(in);
        assertNotNull(gpx);
        assertNotNull(gpx.getWpt());
        assertEquals(3, gpx.getWpt().size());
        assertNotNull(gpx.getRte());
        assertEquals(3, gpx.getRte().size());
        assertNotNull(gpx.getTrk());
        assertEquals(3, gpx.getRte().size());
    }

    @Test
    public void testUnmarshal10() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from10.gpx");
        Gpx gpx = GpxUtil.unmarshal10(reader);
        assertNotNull(gpx);
        assertNotNull(gpx.getWpt());
        assertEquals(3, gpx.getWpt().size());
        assertNotNull(gpx.getRte());
        assertEquals(3, gpx.getRte().size());
        assertNotNull(gpx.getTrk());
        assertEquals(3, gpx.getRte().size());
    }

    @Test
    public void testUnmarshal10TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from10.gpx");
        try {
            GpxUtil.unmarshal11(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    @Test
    public void testUnmarshal11() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from11.gpx");
        GpxType gpx = GpxUtil.unmarshal11(reader);
        assertNotNull(gpx);
        assertNotNull(gpx.getWpt());
        assertEquals(3, gpx.getWpt().size());
    }

    @Test
    public void testUnmarshal11TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from11.gpx");
        try {
            GpxUtil.unmarshal10(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    @Test
    public void testAkGpxReadWriteRoundtrip() throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx10Format(), SAMPLE_PATH + "ak.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(7, route.getPositionCount());
    }

    @Test
    public void testGarminGpxExtensionsv3() throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx11Format(), SAMPLE_PATH + "extensions-garmin-route.gpx");
        assertNotNull(routes);
        assertEquals(2, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(2, route.getPositionCount());
        GpxRoute track = routes.get(1);
        assertEquals(1073, track.getPositionCount());
    }

    @Test
    public void testGarminTrackPointExtensionv1() throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx11Format(), SAMPLE_PATH + "extensions-garmin-track-v1.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute track = routes.get(0);
        assertEquals(1207, track.getPositionCount());
        assertDoubleEquals(0.444479976, track.getPosition(1).getSpeed());
        assertNull(track.getPosition(1).getHeading());
        assertDoubleEquals(6.6301596, track.getPosition(1000).getSpeed());
    }

    @Test
    public void testGarminTrackPointExtensionv2() throws Exception {
        List<GpxRoute> routes = readGpxFile(new Gpx11Format(), SAMPLE_PATH + "extensions-garmin-track-v2.gpx");
        assertNotNull(routes);
        assertEquals(2, routes.size());
        GpxRoute track1 = routes.get(0);
        assertEquals(1571, track1.getPositionCount());
        assertDoubleEquals(14.832, track1.getPosition(1).getSpeed());
        assertDoubleEquals(9.88, track1.getPosition(1).getHeading());
        GpxRoute track2 = routes.get(1);
        assertEquals(896, track2.getPositionCount());
        assertDoubleEquals(9.9, track2.getPosition(1).getSpeed());
    }

    @Test
    public void testWritingNamespaces() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from11.gpx");
        GpxType gpx = GpxUtil.unmarshal11(reader);
        assertNotNull(gpx);
        StringWriter writer = new StringWriter();
        GpxUtil.marshal11(gpx, writer);
        String string = writer.toString();
        assertTrue(string.contains("<gpx"));
        assertTrue(string.contains("version=\"1.1\""));
        assertFalse(string.contains("ns1"));
        assertFalse(string.contains("ns2"));
        assertFalse(string.contains("ns3"));
        assertFalse(string.contains("ns4"));
    }
}
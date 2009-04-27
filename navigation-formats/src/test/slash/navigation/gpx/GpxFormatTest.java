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

import slash.navigation.NavigationTestCase;
import slash.navigation.gpx.binding10.Gpx;
import slash.navigation.gpx.binding11.ExtensionsType;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.binding11.ObjectFactory;
import slash.navigation.gpx.routecatalog10.UserextensionType;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.List;

public class GpxFormatTest extends NavigationTestCase {

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

    public void testUnmarshal10TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from10.gpx");
        try {
            GpxUtil.unmarshal11(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    public void testUnmarshal11() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from11.gpx");
        GpxType gpx = GpxUtil.unmarshal11(reader);
        assertNotNull(gpx);
        assertNotNull(gpx.getWpt());
        assertEquals(3, gpx.getWpt().size());
    }

    public void testUnmarshal11TypeError() throws IOException {
        Reader reader = new FileReader(TEST_PATH + "from11.gpx");
        try {
            GpxUtil.unmarshal10(reader);
            assertTrue(false);
        } catch (JAXBException e) {
        }
    }

    public void testAkGpxReadWriteRoundtrip() throws IOException {
        List<GpxRoute> routes = readSampleGpxFile(new Gpx10Format(), "ak.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(7, route.getPositionCount());
    }

    public void testGarminExtensions() throws IOException, JAXBException {
        List<GpxRoute> routes = readSampleGpxFile(new Gpx11Format(), "MS.gpx");
        assertNotNull(routes);
        assertEquals(2, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(2, route.getPositionCount());
        GpxRoute track = routes.get(1);
        assertEquals(1073, track.getPositionCount());
    }

    public void testWritingNamespaces() throws IOException, JAXBException {
        Reader reader = new FileReader(TEST_PATH + "from11.gpx");
        GpxType gpx = GpxUtil.unmarshal11(reader);
        assertNotNull(gpx);
        StringWriter writer = new StringWriter();
        GpxUtil.marshal11(gpx, writer);
        String string = writer.toString();
        assertTrue(string.contains("<gpx version"));
        assertFalse(string.contains("ns1"));
        assertFalse(string.contains("ns2"));
        assertFalse(string.contains("ns3"));
        assertFalse(string.contains("ns4"));
    }

    public void testWritingExtensions() throws IOException, JAXBException {
        slash.navigation.gpx.routecatalog10.ObjectFactory rcFactory = new slash.navigation.gpx.routecatalog10.ObjectFactory();
        UserextensionType userExtensionType = rcFactory.createUserextensionType();
        userExtensionType.setFirstname("FIRST");
        ObjectFactory gpxFactory = new ObjectFactory();
        ExtensionsType extensionsType = gpxFactory.createExtensionsType();
        extensionsType.getAny().add(userExtensionType);
        GpxType gpx = gpxFactory.createGpxType();
        gpx.setCreator("CREATOR");
        gpx.setExtensions(extensionsType);
        assertNotNull(gpx);
        StringWriter writer = new StringWriter();
        GpxUtil.marshal11(gpx, writer);
        String string = writer.toString();
        System.out.println(string);
        assertTrue(string.contains("<gpx creator=\"CREATOR\""));
        assertTrue(string.contains("<rcxx:firstname>FIRST</rcxx:firstname>"));
    }

    public void testExtractSpeed() {
        Gpx10Format format = new Gpx10Format();
        assertEquals(9.0, format.extractSpeed(" 9 Km/h "));
        assertEquals(99.0, format.extractSpeed(" 99 Km/h "));
        assertNull(format.extractSpeed("egal"));
    }

    public void testExtractReason() {
        GpxPosition position = new GpxPosition(null, null, null, null, null, null);
        assertNull(position.getComment());
        assertNull(position.getCity());
        assertNull(position.getReason());
        position.setComment("Course 97 : Barmbek-Nord");
        assertEquals("Barmbek-Nord", position.getComment());
        assertEquals("Barmbek-Nord", position.getCity());
        assertEquals("Course 97", position.getReason());
        position.setComment("Course 97 : Barmbek-Nord; 14.2 Km");
        assertEquals("Barmbek-Nord; 14.2 Km", position.getComment());
        assertEquals("Barmbek-Nord; 14.2 Km", position.getCity());
        assertEquals("Course 97", position.getReason());
    }

    public void testExtractDescription() {
        GpxPosition position = new GpxPosition(null, null, null, null, null, null);
        assertNull(position.getComment());
        assertNull(position.getCity());
        assertNull(position.getReason());
        position.setComment("Bad Oldesloe; 58.0 Km");
        // TODO think about how to solve this with that much errors
        // assertEquals("Bad Oldesloe", position.getComment());
        // assertEquals("Bad Oldesloe", position.getCity());
        // assertEquals("58.0 Km", position.getReason());
    }
}

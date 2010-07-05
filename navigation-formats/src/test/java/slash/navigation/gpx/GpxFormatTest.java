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

import slash.common.io.Transfer;
import slash.navigation.base.NavigationTestCase;
import slash.navigation.gpx.binding11.ExtensionsType;
import slash.navigation.gpx.binding11.GpxType;
import slash.navigation.gpx.routecatalog10.UserextensionType;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringWriter;

public class GpxFormatTest extends NavigationTestCase {

    public void testWritingRouteConverterExtensions() throws IOException, JAXBException {
        slash.navigation.gpx.routecatalog10.ObjectFactory rcFactory = new slash.navigation.gpx.routecatalog10.ObjectFactory();
        UserextensionType userExtensionType = rcFactory.createUserextensionType();
        userExtensionType.setFirstname("FIRST");
        slash.navigation.gpx.binding11.ObjectFactory gpxFactory = new slash.navigation.gpx.binding11.ObjectFactory();
        ExtensionsType extensionsType = gpxFactory.createExtensionsType();
        extensionsType.getAny().add(userExtensionType);
        GpxType gpx = gpxFactory.createGpxType();
        gpx.setCreator("CREATOR");
        gpx.setExtensions(extensionsType);
        assertNotNull(gpx);
        StringWriter writer = new StringWriter();
        GpxUtil.marshal11(gpx, writer);
        String string = writer.toString();
        assertTrue(string.contains("<gpx creator=\"CREATOR\""));
        assertTrue(string.contains("FIRST"));
        assertTrue(string.contains("firstname"));
        assertTrue(string.contains("<rcxx:firstname>FIRST</rcxx:firstname>"));
    }

    public void testWritingTrekBuddyExtensions() throws IOException, JAXBException {
        slash.navigation.gpx.binding11.ObjectFactory gpxFactory = new slash.navigation.gpx.binding11.ObjectFactory();
        slash.navigation.gpx.trekbuddy.ObjectFactory tbFactory = new slash.navigation.gpx.trekbuddy.ObjectFactory();
        ExtensionsType extensionsType = gpxFactory.createExtensionsType();
        extensionsType.getAny().add(tbFactory.createSpeed(Transfer.formatBigDecimal(123.45, 2)));
        GpxType gpx = gpxFactory.createGpxType();
        assertNotNull(gpx);
        gpx.setExtensions(extensionsType);
        assertNotNull(gpx);
        StringWriter writer = new StringWriter();
        GpxUtil.marshal11(gpx, writer);
        String string = writer.toString();
        assertTrue(string.contains("<nmea:speed>123.45</nmea:speed>"));
    }

    public void testExtractSpeed() {
        Gpx10Format format = new Gpx10Format();
        assertEquals(9.0, format.parseSpeed(" 9 Km/h "));
        assertEquals(99.0, format.parseSpeed(" 99 Km/h "));
        assertEquals(99.9, format.parseSpeed(" 99.9 km/h "));
        assertEquals(99.99999, format.parseSpeed(" 99.99999 Km/h "));
        assertEquals(9.0, format.parseSpeed("Speed: 9 Km/h "));
        assertEquals(9.0, format.parseSpeed("Geschwindigkeit: 9 Km/h "));
        assertNull(format.parseSpeed("egal"));
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

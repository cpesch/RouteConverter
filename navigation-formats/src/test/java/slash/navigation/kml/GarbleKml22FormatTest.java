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
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 * Guards that a Placemark name with umlauts survives reading a garbled KML that has no encoding
 * declaration and no BOM, regardless of the platform default charset (JEP 400 changed that default
 * to UTF-8 in JDK 18+, which regressed legacy Windows-ANSI files).
 */
public class GarbleKml22FormatTest {
    private static final String NAME = "Grün Ö Ä ß Straße";
    private static final String KML = "<kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document><Placemark>" +
            "<name>" + NAME + "</name>" +
            "<Point><coordinates>12.1,49.0,0</coordinates></Point>" +
            "</Placemark></Document></kml>";

    private String readName(byte[] bytes) throws Exception {
        ParserContext<KmlRoute> context = new ParserContextImpl<>();
        new GarbleKml22Format().read(new ByteArrayInputStream(bytes), context);
        List<KmlRoute> routes = context.getRoutes();
        assertEquals(1, routes.size());
        return routes.get(0).getPositions().get(0).getDescription();
    }

    @Test
    public void testReadWindows1252WithoutEncodingDeclaration() throws Exception {
        assertEquals(NAME, readName(KML.getBytes(Charset.forName("windows-1252"))));
    }

    @Test
    public void testReadUtf8WithoutEncodingDeclaration() throws Exception {
        assertEquals(NAME, readName(KML.getBytes(UTF_8)));
    }
}

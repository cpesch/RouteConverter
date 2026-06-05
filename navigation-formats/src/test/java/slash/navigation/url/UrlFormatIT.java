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

package slash.navigation.url;

import org.junit.Test;
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.gpx.Gpx11Format;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class UrlFormatIT {
    private final NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    @Test
    public void readRouteCatalogUrl() throws IOException {
        ParserResult result = parser.read("https://api.routeconverter.com/files/2ce409b0-06b3-424e-9556-5e0765714f6b");
        assertNotNull(result);
        assertEquals(1, result.getAllRoutes().size());
        assertEquals(Gpx11Format.class, result.getFormat().getClass());
    }
}

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

package slash.navigation.base;

import org.junit.Test;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static slash.common.TestCase.assertEquals;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class NavigationFormatParserTest {
    private final NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    private ParserResult read(String testFileName) throws IOException {
        File source = new File(testFileName);
        ParserResult result = parser.read(source);
        assertNotNull(result.getFormat());
        assertNotNull(result.getAllRoutes());
        assertFalse(result.getAllRoutes().isEmpty());
        assertNotNull("Cannot read route from " + source, result.getTheRoute());
        assertTrue(result.getTheRoute().getPositionCount() > 0);
        return result;
    }

    @Test
    public void testNavigationFileParserListener() throws IOException {
        final NavigationFormat[] found = new NavigationFormat[1];
        found[0] = null;
        NavigationFormatParserListener listener = new NavigationFormatParserListener() {
            public void reading(NavigationFormat<BaseRoute> format) {
                found[0] = format;
            }
        };
        try {
            parser.addNavigationFileParserListener(listener);
            read(TEST_PATH + "from.itn");
            assertEquals(TomTom5RouteFormat.class, found[0].getClass());
            found[0] = null;
            parser.removeNavigationFileParserListener(listener);
            read(TEST_PATH + "from.itn");
            assertNull(found[0]);
        } finally {
            parser.removeNavigationFileParserListener(listener);
        }
    }

    @Test
    public void testReadWithFormatList() throws IOException {
        List<NavigationFormat> formats = new ArrayList<>();
        ParserResult result1 = parser.read(new File(TEST_PATH + "from.itn"), formats);
        assertFalse(result1.isSuccessful());

        formats.add(new TomTom5RouteFormat());
        ParserResult result2 = parser.read(new File(TEST_PATH + "from.itn"), formats);
        assertTrue(result2.isSuccessful());
        assertEquals(46, result2.getTheRoute().getPositions().size());
        assertEquals(1, result2.getAllRoutes().size());
        assertEquals(result2.getFormat().getClass(), TomTom5RouteFormat.class);

        formats.add(new TomTom8RouteFormat());
        ParserResult result3 = parser.read(new File(TEST_PATH + "from.itn"), formats);
        assertTrue(result3.isSuccessful());
        assertEquals(46, result3.getTheRoute().getPositions().size());
        assertEquals(1, result3.getAllRoutes().size());
        assertEquals(result3.getFormat().getClass(), TomTom5RouteFormat.class);
    }
}


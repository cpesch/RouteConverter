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

package slash.navigation.itn;

import org.junit.Test;
import slash.common.io.Files;
import slash.navigation.base.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class TomTomRouteFormatIT {

    @Test
    public void testIsPlainRouteRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "bcr_with_itnconv.itn");
        NavigationFormatParser parser = new NavigationFormatParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Route, parser.getTheRoute().getCharacteristics());
    }

    @Test
    public void testIsTripmasterTrackRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "tripmaster2.itn");
        NavigationFormatParser parser = new NavigationFormatParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Track, parser.getTheRoute().getCharacteristics());
    }

    @Test
    public void testSinglePositionFile() throws IOException {
        File source = new File(SAMPLE_PATH + "dilsberg kommandantenhaus.itn");
        NavigationFormatParser parser = new NavigationFormatParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Route, parser.getTheRoute().getCharacteristics());
        assertEquals(1, parser.getTheRoute().getPositionCount());
    }

    @Test
    public void testIsNamedByTyre() throws IOException {
        File source = new File(SAMPLE_PATH + "itn_with_tyre.itn");
        NavigationFormatParser parser = new NavigationFormatParser();
        assertTrue(parser.read(source));
        assertEquals("Eis essen in Ratzeburg", parser.getTheRoute().getName());
    }

    private static final String UMLAUTS = "\u00e4\u00f6\u00fc\u00df";
    private static final char EURO = '\u20ac';

    @Test
    public void testTomTomRoute5() throws IOException {
        File source = new File(TEST_PATH + "from5.itn");
        NavigationFormatParser parser = new NavigationFormatParser();
        assertTrue(parser.read(source));
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
        BaseNavigationPosition first = route.getPositions().get(0);
        assertEquals("abc" + UMLAUTS + EURO, first.getComment());
    }

    @Test
    public void testTomTomRoute8() throws IOException {
        File source = new File(TEST_PATH + "from8.itn");
        NavigationFormatParser parser = new NavigationFormatParser();
        assertTrue(parser.read(source));
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
        BaseNavigationPosition first = route.getPositions().get(0);
        assertEquals("abc" + UMLAUTS + EURO, first.getComment());
    }

    @Test
    public void testTomTomRoute8FromDevice() throws IOException {
        File source = new File(TEST_PATH + "from85.itn");
        NavigationFormatParser parser = new NavigationFormatParser();
        assertTrue(parser.read(source));
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
        BaseNavigationPosition first = route.getPositions().get(0);
        assertEquals("Borkum - Anleger", first.getComment());
    }

    @Test
    public void testManfredsTourFiles() throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser();
        List<File> files = Files.collectFiles(new File(SAMPLE_PATH), ".itn");
        for (File file : files) {
            if (file.getName().startsWith("Tour")) {
                assertTrue("Cannot read route from " + file, parser.read(file));
                assertNotNull(parser.getFormat());
                assertNotNull("Cannot get route from " + file, parser.getTheRoute());
                assertNotNull(parser.getAllRoutes());
            }
        }
    }
}
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

import slash.common.io.Files;
import slash.navigation.base.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TomTomRouteFormatIT extends NavigationTestCase {

    public void testIsPlainRouteRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "bcr_with_itnconv.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Route, parser.getTheRoute().getCharacteristics());
    }

    public void testIsTripmasterTrackRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "tripmaster2.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Track, parser.getTheRoute().getCharacteristics());
    }

    public void testSinglePositionFile() throws IOException {
        File source = new File(SAMPLE_PATH + "dilsberg kommandantenhaus.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        assertEquals(RouteCharacteristics.Route, parser.getTheRoute().getCharacteristics());
        assertEquals(1, parser.getTheRoute().getPositionCount());
    }

    public void testIsNamedByTyre() throws IOException {
        File source = new File(SAMPLE_PATH + "itn_with_tyre.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        assertEquals("Eis essen in Ratzeburg", parser.getTheRoute().getName());
    }

    public void testTomTomRoute5() throws IOException {
        File source = new File(TEST_PATH + "from5.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
        BaseNavigationPosition first = route.getPositions().get(0);
        assertEquals("abcäöüß€", first.getComment());
    }

    public void testTomTomRoute8() throws IOException {
        File source = new File(TEST_PATH + "from8.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
        BaseNavigationPosition first = route.getPositions().get(0);
        assertEquals("abcäöüß€", first.getComment());
    }

    public void testTomTomRoute8FromDevice() throws IOException {
        File source = new File(TEST_PATH + "from85.itn");
        NavigationFileParser parser = new NavigationFileParser();
        assertTrue(parser.read(source));
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = parser.getTheRoute();
        BaseNavigationPosition first = route.getPositions().get(0);
        assertEquals("Borkum - Anleger", first.getComment());
    }

    public void testManfredsTourFiles() throws IOException {
        NavigationFileParser parser = new NavigationFileParser();
        List<File> files = Files.collectFiles(new File(SAMPLE_PATH), ".itn");
        for (File file : files) {
            if(file.getName().startsWith("Tour")) {
                if(!parser.read(file))
                    System.out.println("Cannot read route from " + file);
                else {
                    assertNotNull(parser.getFormat());
                    assertNotNull("Cannot get route from " + file, parser.getTheRoute());
                    assertNotNull(parser.getAllRoutes());
                }
            }
        }
    }
}
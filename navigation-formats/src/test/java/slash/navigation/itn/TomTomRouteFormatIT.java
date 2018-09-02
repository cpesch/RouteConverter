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
import slash.navigation.base.AllNavigationFormatRegistry;
import slash.navigation.base.BaseNavigationFormat;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.ParserResult;
import slash.navigation.common.NavigationPosition;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;

public class TomTomRouteFormatIT {
    private NavigationFormatParser parser = new NavigationFormatParser(new AllNavigationFormatRegistry());

    @Test
    public void testIsPlainRouteRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "bcr_with_itnconv.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(Route, result.getTheRoute().getCharacteristics());
    }

    @Test
    public void testIsTripmasterTrackRouteCharacteristics() throws IOException {
        File source = new File(SAMPLE_PATH + "tripmaster2.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(Track, result.getTheRoute().getCharacteristics());
    }

    @Test
    public void testSinglePositionFile() throws IOException {
        File source = new File(SAMPLE_PATH + "dilsberg kommandantenhaus.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(Route, result.getTheRoute().getCharacteristics());
        assertEquals(1, result.getTheRoute().getPositionCount());
    }

    @Test
    public void testIsNamedByTyre() throws IOException {
        File source = new File(SAMPLE_PATH + "itn_with_tyre.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals("Eis essen in Ratzeburg", result.getTheRoute().getName());
    }

    private static final String UMLAUTS_ae_oe_ue_sz = "\u00e4\u00f6\u00fc\u00df";
    private static final char EURO = '\u20ac';

    static void checkUmlauts(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        NavigationPosition first = route.getPositions().get(0);
        assertEquals("abc" + UMLAUTS_ae_oe_ue_sz + EURO, first.getDescription());
    }

    @Test
    public void testTomTomRoute5() throws IOException {
        File source = new File(TEST_PATH + "from5.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
        checkUmlauts(route);
    }

    @Test
    public void testTomTomRoute8() throws IOException {
        File source = new File(TEST_PATH + "from8.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
        checkUmlauts(route);
    }

    private static final String SCHEESSEL = "Schee\u00dfel";
    private static final String MOELLN = "M\u00f6lln";
    private static final String LUEBECK = "L\u00fcbeck";

    static void checkPlaceNamesWithUmlauts(BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route) {
        NavigationPosition first = route.getPositions().get(0);
        assertEquals(SCHEESSEL, first.getDescription());
        NavigationPosition second = route.getPositions().get(1);
        assertEquals(MOELLN, second.getDescription());
        NavigationPosition third = route.getPositions().get(2);
        assertEquals(LUEBECK, third.getDescription());
    }

    @Test
    public void testTomTomRider2() throws IOException {
        File source = new File(TEST_PATH + "from-rider-2.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
        checkPlaceNamesWithUmlauts(route);
    }

    @Test
    public void testUrbanRinder() throws IOException {
        File source = new File(TEST_PATH + "from-urban-rider.itn");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = result.getTheRoute();
        checkPlaceNamesWithUmlauts(route);
    }
}
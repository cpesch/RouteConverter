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
package slash.navigation.csv;

import org.junit.Test;
import slash.navigation.base.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static slash.common.io.Transfer.formatDoubleAsString;
import static slash.common.TestCase.calendar;
import static slash.navigation.common.UnitConversion.kiloMeterToNauticMiles;
import static slash.navigation.common.UnitConversion.meterToFeets;
import static slash.navigation.base.NavigationTestCase.TEST_PATH;

public class CsvFormatTest {
    private final NavigationFormatParser parser = new NavigationFormatParser(new NavigationFormatRegistry());
    // ae oe ue sz AE OE UE
    private static final String UMLAUTS = "\u00E4\u00F6\u00FC\u00DF\u00C4\u00D6\u00DC";

    private File createFlightradar24File() throws IOException {
        Path file = Files.createTempFile("from-flightradar24-", ".csv");
        file.toFile().deleteOnExit();
        String content = """
                Timestamp,UTC,Callsign,Position,Altitude,Speed,Direction
                1513280280,2017-12-14T19:38:00Z,Positionsname,"50.241125,8.4853033",%s,%s,0
                1513280340,2017-12-14T19:39:00Z,%s,"-50.2411251,88.4853034",%s,%s,0
                1513280400,2017-12-14T19:40:00Z,"#""§$%%&/","50.2411252,8.4853035",%s,%s,0
                """.formatted(
                formatDoubleAsString(meterToFeets(654.6)),
                formatDoubleAsString(kiloMeterToNauticMiles(6.1)),
                UMLAUTS,
                formatDoubleAsString(meterToFeets(654.7)),
                formatDoubleAsString(kiloMeterToNauticMiles(0.1)),
                formatDoubleAsString(meterToFeets(654.8)),
                formatDoubleAsString(kiloMeterToNauticMiles(-2.3)));
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }

    private void checkRoute(BaseRoute route) {
        assertEquals(3, route.getPositionCount());
        BaseNavigationPosition first = route.getPosition(0);
        assertEquals(8.4853033, first.getLongitude());
        assertEquals(50.241125, first.getLatitude());
        assertEquals(654.6, first.getElevation());
        assertEquals(6.1, first.getSpeed());
        assertEquals(calendar(2017, 12, 14, 19, 38, 0), first.getTime());
        assertEquals("Positionsname", first.getDescription());
        BaseNavigationPosition second = route.getPosition(1);
        assertEquals(88.4853034, second.getLongitude());
        assertEquals(-50.2411251, second.getLatitude());
        assertEquals(654.7, second.getElevation());
        assertEquals(0.1, second.getSpeed());
        assertEquals(calendar(2017, 12, 14, 19, 39, 0), second.getTime());
        assertEquals(UMLAUTS, second.getDescription());
        BaseNavigationPosition third = route.getPosition(2);
        assertEquals(8.4853035, third.getLongitude());
        assertEquals(50.2411252, third.getLatitude());
        assertEquals(654.8, third.getElevation());
        assertEquals(-2.3, third.getSpeed());
        assertEquals(calendar(2017, 12, 14, 19, 40, 0), third.getTime());
        assertEquals("#\"\u00A7$%&/", third.getDescription());
    }

    @Test
    public void testReadExcelCsv1() throws IOException {
        File source = new File(TEST_PATH + "from-excel1.csv");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(CsvSemicolonFormat.class, result.getFormat().getClass());
        checkRoute(result.getTheRoute());
    }

    @Test
    public void testReadExcelCsv2() throws IOException {
        File source = new File(TEST_PATH + "from-excel2.csv");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(CsvSemicolonFormat.class, result.getFormat().getClass());
        checkRoute(result.getTheRoute());
    }

    @Test
    public void testReadLibreCalcCsv1() throws IOException {
        File source = new File(TEST_PATH + "from-librecalc1.csv");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(CsvCommaFormat.class, result.getFormat().getClass());
        checkRoute(result.getTheRoute());
    }

    @Test
    public void testReadLibreCalcCsv2() throws IOException {
        File source = new File(TEST_PATH + "from-librecalc2.csv");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(CsvCommaFormat.class, result.getFormat().getClass());
        checkRoute(result.getTheRoute());
    }

    @Test
    public void testReadFlightmap24() throws IOException {
        File source = new File(TEST_PATH + "from-flightmap24.csv");
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(CsvCommaFormat.class, result.getFormat().getClass());
        checkRoute(result.getTheRoute());
    }

    @Test
    public void testReadFlightradar24() throws IOException {
        File source = createFlightradar24File();
        ParserResult result = parser.read(source);
        assertNotNull(result);
        assertEquals(Flightradar24Format.class, result.getFormat().getClass());
        checkRoute(result.getTheRoute());
    }
}


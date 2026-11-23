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
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.ParserResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;
import static slash.common.TestCase.calendar;

/**
 * Tests header search and the combined coordinates column of {@link CsvFormat}.
 *
 * @author Christian Pesch
 */
public class CsvFormatTest {

    @Test
    public void findsHeaderBelowATitleRowAndReadsCombinedCoordinates() throws IOException {
        CsvSemicolonFormat format = new CsvSemicolonFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();

        try (InputStream in = getClass().getResourceAsStream("title-row-combined-coordinates.csv")) {
            assertNotNull("title-row-combined-coordinates.csv fixture must be on the test classpath", in);
            format.read(in, context);
        }

        assertEquals(1, context.getRoutes().size());
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(2, positions.size());

        CsvPosition first = positions.get(0);
        assertEquals(52.5, first.getLatitude(), 0.0);
        assertEquals(13.4, first.getLongitude(), 0.0);
        assertEquals(calendar(2026, 10, 12, 17, 24, 18), first.getTime());
        assertEquals(0.0, first.getSpeed(), 0.0);

        CsvPosition second = positions.get(1);
        assertEquals(52.5002, second.getLatitude(), 0.0);
        assertEquals(13.4003, second.getLongitude(), 0.0);
        assertEquals(calendar(2026, 10, 12, 17, 28, 22), second.getTime());
        assertEquals(7.0, second.getSpeed(), 0.0);
    }

    @Test
    public void headerInFirstLineStillReadsUnchanged() throws IOException {
        String content = """
                Longitude,Latitude,Elevation,Speed,Time,Description
                8.4,50.2,650.0,10.0,14.12.17 19:38:00,Berlin
                """;

        CsvCommaFormat format = new CsvCommaFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        boolean result = format.read(new StringReader(content), context);

        assertTrue(result);
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(1, positions.size());
        CsvPosition position = positions.get(0);
        assertEquals(8.4, position.getLongitude(), 0.0);
        assertEquals(50.2, position.getLatitude(), 0.0);
        assertEquals(650.0, position.getElevation(), 0.0);
        assertEquals(10.0, position.getSpeed(), 0.0);
        assertEquals(calendar(2017, 12, 14, 19, 38, 0), position.getTime());
        assertEquals("Berlin", position.getDescription());
    }

    @Test
    public void embeddedNewlineInAQuotedFieldSurvivesTheHeaderSearch() throws IOException {
        // the title row has no comma, so it maps zero known columns and is skipped as usual
        String content = """
                Streckendetails:Tracker-1
                Longitude,Latitude,Elevation,Speed,Time,Description
                8.4,50.2,650.0,10.0,14.12.17 19:38:00,"Berlin
                multiline"
                """;

        CsvCommaFormat format = new CsvCommaFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        boolean result = format.read(new StringReader(content), context);

        assertTrue(result);
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(1, positions.size());
        assertEquals("Berlin\nmultiline", positions.get(0).getDescription());
    }

    @Test
    public void fileWithNoRecognisableHeaderInFirstTenLinesBehavesLikeToday() throws IOException {
        // neither line maps at least 2 cells to a known column, so line 1 stays the header as before
        String content = """
                Description;X
                Berlin;5
                """;

        CsvSemicolonFormat format = new CsvSemicolonFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        boolean result = format.read(new StringReader(content), context);

        assertTrue(result);
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(1, positions.size());
        assertEquals("Berlin", positions.get(0).getDescription());
    }

    @Test
    public void readsLowerCaseColumnNamesAndIsoTimes() throws IOException {
        // the shape of the Google Takeout / Fitbit "Gps Location" daily files, with synthetic values
        String content = """
                timestamp,latitude,longitude,altitude,data source
                2024-01-09T09:14:11Z,50.2,8.4,46.3,Tracker
                2024-01-09T09:14:12Z,50.2001,8.4002,46.8,Tracker
                """;

        CsvCommaFormat format = new CsvCommaFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        boolean result = format.read(new StringReader(content), context);

        assertTrue(result);
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(2, positions.size());
        CsvPosition first = positions.get(0);
        assertEquals(50.2, first.getLatitude(), 0.0);
        assertEquals(8.4, first.getLongitude(), 0.0);
        assertEquals(46.3, first.getElevation(), 0.0);
        assertEquals(calendar(2024, 1, 9, 9, 14, 11), first.getTime());
        CsvPosition second = positions.get(1);
        assertEquals(50.2001, second.getLatitude(), 0.0);
        assertEquals(8.4002, second.getLongitude(), 0.0);
        assertEquals(calendar(2024, 1, 9, 9, 14, 12), second.getTime());
    }

    @Test
    public void writesBackToTheLowerCaseColumns() throws IOException {
        String content = """
                timestamp,latitude,longitude,altitude
                2024-01-09T09:14:11Z,50.2,8.4,46.3
                """;

        CsvCommaFormat format = new CsvCommaFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertTrue(format.read(new StringReader(content), context));
        CsvPosition position = context.getRoutes().get(0).getPositions().get(0);
        position.setLatitude(51.0);
        position.setElevation(100.0);

        assertEquals("51.0", position.getRowAsMap().get("latitude"));
        assertEquals("100.0", position.getRowAsMap().get("altitude"));
        assertFalse(position.getRowAsMap().containsKey("Latitude"));
        assertFalse(position.getRowAsMap().containsKey("Elevation"));
    }

    @Test
    public void parserReadsGoogleTakeoutGpsLocationFile() throws IOException {
        File file = File.createTempFile("gps_location_2024-01-09", ".csv");
        try {
            Files.writeString(file.toPath(), """
                    timestamp,latitude,longitude,altitude,data source
                    2024-01-09T09:14:11Z,50.2,8.4,46.3,Tracker
                    2024-01-09T09:14:12Z,50.2001,8.4002,46.8,Tracker
                    2024-01-09T09:14:13Z,50.2002,8.4004,47.1,Tracker
                    """);
            ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(file);

            assertTrue(result.isSuccessful());
            assertEquals(CsvCommaFormat.class, result.getFormat().getClass());
            assertEquals(3, result.getTheRoute().getPositionCount());
        } finally {
            assertTrue(file.delete());
        }
    }
}

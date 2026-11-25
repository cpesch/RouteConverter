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

import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            assertNotNull(in, "title-row-combined-coordinates.csv fixture must be on the test classpath");
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

    @Test
    public void appliesTheHemisphereColumns() throws IOException {
        String content = """
                INDEX,LATITUDE,N/S,LONGITUDE,E/W
                1,33.8688,S,151.2093,E
                2,40.7128,N,74.0060,W
                """;

        CsvCommaFormat format = new CsvCommaFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertTrue(format.read(new StringReader(content), context));
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(-33.8688, positions.get(0).getLatitude(), 0.0);
        assertEquals(151.2093, positions.get(0).getLongitude(), 0.0);
        assertEquals(40.7128, positions.get(1).getLatitude(), 0.0);
        assertEquals(-74.0060, positions.get(1).getLongitude(), 0.0);

        positions.get(1).setLatitude(-1.5);
        assertEquals("1.5", positions.get(1).getRowAsMap().get("LATITUDE"));
        assertEquals("S", positions.get(1).getRowAsMap().get("N/S"));
        assertEquals(-1.5, positions.get(1).getLatitude(), 0.0);
    }

    @Test
    public void insertedPositionSurvivesWriteAndReread() throws IOException {
        String content = """
                timestamp,latitude,longitude,altitude
                2024-01-09T09:14:11Z,50.2,8.4,46.3
                2024-01-09T09:14:12Z,50.3,8.5,46.8
                """;

        CsvCommaFormat format = new CsvCommaFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertTrue(format.read(new StringReader(content), context));
        CsvRoute route = context.getRoutes().get(0);
        route.getPositions().add(1, new CsvPosition(8.45, 50.25, 47.0, null, null, "inserted"));

        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        format.write(route, bytes, 0, route.getPositionCount());

        ParserContextImpl<CsvRoute> reread = new ParserContextImpl<>();
        assertTrue(format.read(new StringReader(bytes.toString(java.nio.charset.StandardCharsets.UTF_8)), reread));
        List<CsvPosition> positions = reread.getRoutes().get(0).getPositions();
        assertEquals(3, positions.size());
        assertEquals(50.2, positions.get(0).getLatitude(), 0.0);
        assertEquals(8.4, positions.get(0).getLongitude(), 0.0);
        assertEquals(50.25, positions.get(1).getLatitude(), 0.0);
        assertEquals(8.45, positions.get(1).getLongitude(), 0.0);
        assertEquals(50.3, positions.get(2).getLatitude(), 0.0);
        assertEquals(8.5, positions.get(2).getLongitude(), 0.0);
    }

    @Test
    public void lowerCaseDescriptionAloneDoesNotClaimAForeignCsv() throws IOException {
        String content = """
                id,name,description,price
                1,Chair,A wooden chair,49.90
                """;

        CsvCommaFormat format = new CsvCommaFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertFalse(format.read(new StringReader(content), context));
    }
    // a reader that counts the characters the CSV parser asked for
    private static class CountingReader extends StringReader {
        int charsRead;

        CountingReader(String content) {
            super(content);
        }

        public int read(char[] buffer, int offset, int length) throws IOException {
            int count = super.read(buffer, offset, length);
            if (count > 0)
                charsRead += count;
            return count;
        }
    }

    private static String healthDataCsv(String header, int rows) {
        StringBuilder builder = new StringBuilder(header).append('\n');
        for (int i = 0; i < rows; i++)
            builder.append("2024-01-09T09:14:11Z,").append(i).append(",72,Tracker\n");
        return builder.toString();
    }

    @Test
    public void declinesAHeaderWithoutPositionColumnsWithoutParsingTheRows() throws IOException {
        String content = healthDataCsv("timestamp,sample,bpm,data source", 400_000);
        assertTrue(content.length() > 15_000_000);

        CountingReader reader = new CountingReader(content);
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertFalse(new CsvCommaFormat().read(reader, context));

        assertTrue(context.getRoutes().isEmpty());
        // the header search buffers up to its 1 MB mark limit, nothing more
        assertTrue(reader.charsRead <= (1 << 20) + 8192, "read " + reader.charsRead + " of " + content.length() + " characters");
    }

    @Test
    public void declinesTheSemicolonAndFlightradarFormatsEarlyToo() throws IOException {
        String content = healthDataCsv("timestamp;sample;bpm;data source", 400_000).replace(',', ';');

        CountingReader semicolonReader = new CountingReader(content);
        assertFalse(new CsvSemicolonFormat().read(semicolonReader, new ParserContextImpl<>()));
        assertTrue(semicolonReader.charsRead <= (1 << 20) + 8192);

        CountingReader flightradarReader = new CountingReader(healthDataCsv("timestamp,sample,bpm,data source", 400_000));
        assertFalse(new Flightradar24Format().read(flightradarReader, new ParserContextImpl<>()));
        assertTrue(flightradarReader.charsRead <= (1 << 20) + 8192);
    }

    @Test
    public void declinesAHeaderOnlyFile() throws IOException {
        assertFalse(new CsvCommaFormat().read(new StringReader("Longitude,Latitude\n"), new ParserContextImpl<>()));
    }

    @Test
    public void stillReadsDescriptionOnlyFiles() throws IOException {
        String content = """
                Description;X
                Berlin;5
                """;
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertTrue(new CsvSemicolonFormat().read(new StringReader(content), context));
        assertEquals("Berlin", context.getRoutes().get(0).getPositions().get(0).getDescription());

        ParserContextImpl<CsvRoute> german = new ParserContextImpl<>();
        assertTrue(new CsvCommaFormat().read(new StringReader("id,Beschreibung\n1,Berlin\n"), german));
        assertEquals("Berlin", german.getRoutes().get(0).getPositions().get(0).getDescription());
    }

    @Test
    public void stillReadsALonelyLatitudeColumn() throws IOException {
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertTrue(new CsvCommaFormat().read(new StringReader("id,LATITUDE\n1,50.2\n"), context));
        assertEquals(50.2, context.getRoutes().get(0).getPositions().get(0).getLatitude(), 0.0);
    }

    @Test
    public void stillReadsTheCombinedCoordinatesColumn() throws IOException {
        String content = """
                Nr;Koordinaten;Zeit
                1;"52.5,13.4";14.12.17 19:38:00
                """;
        ParserContextImpl<CsvRoute> german = new ParserContextImpl<>();
        assertTrue(new CsvSemicolonFormat().read(new StringReader(content), german));
        assertEquals(52.5, german.getRoutes().get(0).getPositions().get(0).getLatitude(), 0.0);
        assertEquals(13.4, german.getRoutes().get(0).getPositions().get(0).getLongitude(), 0.0);

        ParserContextImpl<CsvRoute> english = new ParserContextImpl<>();
        assertTrue(new CsvSemicolonFormat().read(new StringReader(content.replace("Koordinaten", "coordinates")), english));
        assertEquals(52.5, english.getRoutes().get(0).getPositions().get(0).getLatitude(), 0.0);
    }

    @Test
    public void stillReadsACaseInsensitiveAndPaddedHeader() throws IOException {
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertTrue(new CsvCommaFormat().read(new StringReader("Id, LONGITUDE ,laTitude\n1,8.4,50.2\n"), context));
        assertEquals(50.2, context.getRoutes().get(0).getPositions().get(0).getLatitude(), 0.0);
        assertEquals(8.4, context.getRoutes().get(0).getPositions().get(0).getLongitude(), 0.0);
    }

    @Test
    public void flightradarStillReadsItsPositionAndCallsignColumns() throws IOException {
        String content = """
                Timestamp,UTC,Callsign,Position,Altitude,Speed,Direction
                1700000000,2023-11-14T22:13:20Z,DLH123,"50.2,8.4",35000,450,90
                """;
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();
        assertTrue(new Flightradar24Format().read(new StringReader(content), context));
        CsvPosition position = context.getRoutes().get(0).getPositions().get(0);
        assertEquals(50.2, position.getLatitude(), 0.0);
        assertEquals(8.4, position.getLongitude(), 0.0);
        assertEquals("DLH123", position.getDescription());

        ParserContextImpl<CsvRoute> callsignOnly = new ParserContextImpl<>();
        assertTrue(new Flightradar24Format().read(new StringReader("UTC,Callsign\n2023-11-14T22:13:20Z,DLH123\n"), callsignOnly));
    }
}

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
import slash.navigation.base.ParserContextImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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
}

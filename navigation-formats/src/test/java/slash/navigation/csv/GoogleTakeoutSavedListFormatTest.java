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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests {@link GoogleTakeoutSavedListFormat}.
 *
 * @author Christian Pesch
 */
public class GoogleTakeoutSavedListFormatTest {

    // parseCoordinates

    @Test
    public void parseCoordinatesReadsDroppedPin() {
        Double[] result = GoogleTakeoutSavedListFormat.parseCoordinates("https://www.google.com/maps/search/52.516275,13.377704");
        assertNotNull(result);
        assertEquals(52.516275, result[0], 0.000001);
        assertEquals(13.377704, result[1], 0.000001);
    }

    @Test
    public void parseCoordinatesReadsDroppedPinWithPlusEncodedSpace() {
        Double[] result = GoogleTakeoutSavedListFormat.parseCoordinates("https://www.google.com/maps/search/52.516275,+13.377704");
        assertNotNull(result);
        assertEquals(52.516275, result[0], 0.000001);
        assertEquals(13.377704, result[1], 0.000001);
    }

    @Test
    public void parseCoordinatesReadsDroppedPinWithPercentEncodedComma() {
        Double[] result = GoogleTakeoutSavedListFormat.parseCoordinates("https://www.google.com/maps/search/52.516275%2C13.377704");
        assertNotNull(result);
        assertEquals(52.516275, result[0], 0.000001);
        assertEquals(13.377704, result[1], 0.000001);
    }

    @Test
    public void parseCoordinatesReadsExactPlacePin() {
        Double[] result = GoogleTakeoutSavedListFormat.parseCoordinates(
                "https://www.google.com/maps/place/Name/data=!4m2!3m1!1s0x47a84e373f035803:0x1!3d53.5493424!4d9.973844");
        assertNotNull(result);
        assertEquals(53.5493424, result[0], 0.000001);
        assertEquals(9.973844, result[1], 0.000001);
    }

    @Test
    public void parseCoordinatesReadsQParameter() {
        Double[] result = GoogleTakeoutSavedListFormat.parseCoordinates("https://www.google.com/maps?q=48.8566,2.3522");
        assertNotNull(result);
        assertEquals(48.8566, result[0], 0.000001);
        assertEquals(2.3522, result[1], 0.000001);
    }

    @Test
    public void parseCoordinatesReadsQueryParameter() {
        Double[] result = GoogleTakeoutSavedListFormat.parseCoordinates("https://www.google.com/maps?api=1&query=48.8566,2.3522");
        assertNotNull(result);
        assertEquals(48.8566, result[0], 0.000001);
        assertEquals(2.3522, result[1], 0.000001);
    }

    @Test
    public void parseCoordinatesRejectsPlaceIdOnlyUrl() {
        assertNull(GoogleTakeoutSavedListFormat.parseCoordinates(
                "https://www.google.com/maps/place/Name/data=!4m2!3m1!1s0x47a84e373f035803:0x42120465b5e3b696"));
    }

    @Test
    public void parseCoordinatesRejectsViewportCentre() {
        assertNull(GoogleTakeoutSavedListFormat.parseCoordinates("https://www.google.com/maps/@52.5,13.4,15z"));
    }

    @Test
    public void parseCoordinatesRejectsOutOfRangeLatitude() {
        assertNull(GoogleTakeoutSavedListFormat.parseCoordinates("https://www.google.com/maps/search/95.0,13.0"));
    }

    @Test
    public void parseCoordinatesRejectsNull() {
        assertNull(GoogleTakeoutSavedListFormat.parseCoordinates(null));
    }

    @Test
    public void parseCoordinatesRejectsEmpty() {
        assertNull(GoogleTakeoutSavedListFormat.parseCoordinates(""));
    }

    // reading

    private static final String FIVE_COLUMN_HEADER_CSV =
            """
            Title,Note,URL,Tags,Comment
            Pin,my note,"https://www.google.com/maps/search/52.516275,13.377704",,
            Place,some note,https://www.google.com/maps/place/Name/data=!4m2!3m1!1s0x47a84e373f035803:0x1,,
            Pin2,,https://www.google.com/maps/place/Name/data=!3d53.5493424!4d9.973844,,
            ,,https://www.google.com/maps/place/Name/data=!4m2!3m1!1s0x1:0x2,,
            ,,,,
            """;

    @Test
    public void readsFiveColumnHeaderFile() throws IOException {
        GoogleTakeoutSavedListFormat format = new GoogleTakeoutSavedListFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();

        boolean result = format.read(new StringReader(FIVE_COLUMN_HEADER_CSV), context);

        assertTrue(result);
        assertEquals(1, context.getRoutes().size());
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(3, positions.size());

        CsvPosition droppedPin = positions.get(0);
        assertEquals(52.516275, droppedPin.getLatitude(), 0.000001);
        assertEquals(13.377704, droppedPin.getLongitude(), 0.000001);
        assertEquals("Pin; my note", droppedPin.getDescription());

        CsvPosition placeIdRow = positions.get(1);
        assertNull(placeIdRow.getLatitude());
        assertNull(placeIdRow.getLongitude());
        assertEquals("Place", placeIdRow.getDescription());

        CsvPosition placePin = positions.get(2);
        assertEquals(53.5493424, placePin.getLatitude(), 0.000001);
        assertEquals(9.973844, placePin.getLongitude(), 0.000001);
        assertEquals("Pin2", placePin.getDescription());
    }

    @Test
    public void readsOldFourColumnHeader() throws IOException {
        String csv =
                """
                Title,Note,URL,Comment
                Spot,a note,https://www.google.com/maps/place/Name/data=!3d52.5!4d13.4,
                """;

        GoogleTakeoutSavedListFormat format = new GoogleTakeoutSavedListFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();

        boolean result = format.read(new StringReader(csv), context);

        assertTrue(result);
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(1, positions.size());
        assertEquals(52.5, positions.get(0).getLatitude(), 0.000001);
        assertEquals(13.4, positions.get(0).getLongitude(), 0.000001);
    }

    @Test
    public void readsHeaderWithLeadingByteOrderMark() throws IOException {
        String csv = "﻿Title,Note,URL,Comment\n" +
                "Spot,a note,https://www.google.com/maps/place/Name/data=!3d52.5!4d13.4,\n";

        GoogleTakeoutSavedListFormat format = new GoogleTakeoutSavedListFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();

        boolean result = format.read(new StringReader(csv), context);

        assertTrue(result);
        List<CsvPosition> positions = context.getRoutes().get(0).getPositions();
        assertEquals(1, positions.size());
        assertEquals(52.5, positions.get(0).getLatitude(), 0.000001);
    }

    @Test
    public void rejectsAGenericCsv() throws IOException {
        String csv =
                """
                Latitude,Longitude,Description
                52.5,13.4,Berlin
                48.8566,2.3522,Paris
                """;

        GoogleTakeoutSavedListFormat format = new GoogleTakeoutSavedListFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();

        boolean result = format.read(new StringReader(csv), context);

        assertFalse(result);
    }

    @Test
    public void isReadOnly() {
        assertFalse(new GoogleTakeoutSavedListFormat().isSupportsWriting());
    }

    // route name

    @Test
    public void routeNameComesFromFileNameWithoutExtension() throws IOException {
        GoogleTakeoutSavedListFormat format = new GoogleTakeoutSavedListFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>(new File("Want to go.csv"), null);

        format.read(new StringReader(FIVE_COLUMN_HEADER_CSV), context);

        assertEquals("Want to go", context.getRoutes().get(0).getName());
    }

    @Test
    public void routeNameStripsExtensionCaseInsensitively() throws IOException {
        GoogleTakeoutSavedListFormat format = new GoogleTakeoutSavedListFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>(new File("Liste.CSV"), null);

        format.read(new StringReader(FIVE_COLUMN_HEADER_CSV), context);

        assertEquals("Liste", context.getRoutes().get(0).getName());
    }

    @Test
    public void routeNameFallsBackToDefaultWithoutAFile() throws IOException {
        GoogleTakeoutSavedListFormat format = new GoogleTakeoutSavedListFormat();
        ParserContextImpl<CsvRoute> context = new ParserContextImpl<>();

        format.read(new StringReader(FIVE_COLUMN_HEADER_CSV), context);

        String name = context.getRoutes().get(0).getName();
        assertNotNull(name);
        assertNotEquals("Want to go", name);
    }
}

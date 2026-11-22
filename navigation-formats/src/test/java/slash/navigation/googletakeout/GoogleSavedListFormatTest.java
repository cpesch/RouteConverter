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
package slash.navigation.googletakeout;

import org.junit.After;
import org.junit.Test;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.ParserResult;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.csv.CsvCommaFormat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Tests for {@link GoogleSavedListFormat}, with synthetic CSV built in the test.
 *
 * @author Christian Pesch
 */
public class GoogleSavedListFormatTest {
    private static final double DELTA = 0.0000001;
    private static final String PLACE_URL_1 = "https://www.google.com/maps/place/Cafe+Alpha/data=!4m2!3m1!1s0x47a84e373f035901:0x42120465b5e3b70";
    private static final String PLACE_URL_2 = "https://www.google.com/maps/place/Bistro+Beta/data=!4m2!3m1!1s0x47e66e1f06e2b70f:0x40b82c3688c9460";

    private final GoogleSavedListFormat format = new GoogleSavedListFormat();
    private final List<File> temporaryFiles = new ArrayList<>();

    @After
    public void tearDown() {
        for (File file : temporaryFiles)
            //noinspection ResultOfMethodCallIgnored
            file.delete();
    }

    private List<Wgs84Route> read(String csv, File file) throws IOException {
        ParserContext<Wgs84Route> context = new ParserContextImpl<>(file, null);
        format.read(new ByteArrayInputStream(csv.getBytes(UTF_8)), context);
        return context.getRoutes();
    }

    private List<Wgs84Route> read(String csv) throws IOException {
        return read(csv, new File("Want to go.csv"));
    }

    private File temporaryFile(String content) throws IOException {
        File file = File.createTempFile("saved-list-test", ".csv");
        temporaryFiles.add(file);
        Files.write(file.toPath(), content.getBytes(UTF_8));
        return file;
    }

    private static void assertNoCoordinates(Wgs84Position position, String description) {
        assertNull(position.getLongitude());
        assertNull(position.getLatitude());
        assertFalse(position.hasCoordinates());
        assertEquals(description, position.getDescription());
    }

    private static void assertCoordinates(Wgs84Position position, double longitude, double latitude, String description) {
        assertEquals(longitude, position.getLongitude(), DELTA);
        assertEquals(latitude, position.getLatitude(), DELTA);
        assertEquals(description, position.getDescription());
    }

    @Test
    public void testReadThreeColumnList() throws IOException {
        List<Wgs84Route> routes = read("Title,Note,URL\n" +
                "Cafe Alpha,,"+ PLACE_URL_1 + "\n" +
                "Bistro Beta,Try the soup," + PLACE_URL_2 + "\n");

        assertEquals(1, routes.size());
        Wgs84Route route = routes.get(0);
        assertEquals(Waypoints, route.getCharacteristics());
        assertEquals("Want to go", route.getName());
        assertEquals(2, route.getPositionCount());
        assertNoCoordinates(route.getPosition(0), "Cafe Alpha");
        assertNoCoordinates(route.getPosition(1), "Bistro Beta; Try the soup");
        assertNull(route.getPosition(0).getTime());
        assertNull(route.getPosition(0).getElevation());
    }

    @Test
    public void testReadFiveColumnListWithDescriptionLine() throws IOException {
        List<Wgs84Route> routes = read("Places for the summer trip\n" +
                "\n" +
                "Title,Note,URL,Tags,Comment\n" +
                "Cafe Alpha,Near the station," + PLACE_URL_1 + ",Date night;Coffee,Opens at 8\n" +
                "Bistro Beta,,"+ PLACE_URL_2 + ",,\n", null);

        assertEquals(1, routes.size());
        Wgs84Route route = routes.get(0);
        assertEquals("Places for the summer trip", route.getName());
        assertEquals(2, route.getPositionCount());
        assertNoCoordinates(route.getPosition(0), "Cafe Alpha; Near the station");
        assertNoCoordinates(route.getPosition(1), "Bistro Beta");
    }

    @Test
    public void testHeaderIsCaseInsensitiveAndToleratesByteOrderMark() throws IOException {
        List<Wgs84Route> routes = read("﻿\"title\",\"note\",\"url\"\n" +
                "\"Cafe Alpha\",\"\",\"" + PLACE_URL_1 + "\"\n");

        assertEquals(1, routes.size());
        assertNoCoordinates(routes.get(0).getPosition(0), "Cafe Alpha");
    }

    @Test
    public void testQuotedFieldsWithCommasAndLineBreaks() throws IOException {
        List<Wgs84Route> routes = read("Title,Note,URL\n" +
                "\"Alpha, Beta & Co\",\"First line\nsecond line\"," + PLACE_URL_1 + "\n" +
                "\"Gamma \"\"Delta\"\"\",," + PLACE_URL_2 + "\n");

        assertEquals(1, routes.size());
        Wgs84Route route = routes.get(0);
        assertEquals(2, route.getPositionCount());
        assertNoCoordinates(route.getPosition(0), "Alpha, Beta & Co; First line second line");
        assertNoCoordinates(route.getPosition(1), "Gamma \"Delta\"");
    }

    @Test
    public void testCoordinatesFromUrlAndNote() throws IOException {
        List<Wgs84Route> routes = read("Title,Note,URL\n" +
                "Dropped pin,,\"https://www.google.com/maps/search/52.520008,13.404954\"\n" +
                "Viewpoint,,\"https://www.google.com/maps/place/Viewpoint/@48.8566,2.3522,17z/data=!4m2\"\n" +
                "Query pin,,https://maps.google.com/?q=-33.8688%2C151.2093\n" +
                "Note pin,\" 40.7128, -74.0060 \"," + PLACE_URL_1 + "\n" +
                "Unknown place,,\"https://www.google.com/maps/search/0,0\"\n");

        assertEquals(1, routes.size());
        Wgs84Route route = routes.get(0);
        assertEquals(5, route.getPositionCount());
        assertCoordinates(route.getPosition(0), 13.404954, 52.520008, "Dropped pin");
        assertCoordinates(route.getPosition(1), 2.3522, 48.8566, "Viewpoint");
        assertCoordinates(route.getPosition(2), 151.2093, -33.8688, "Query pin");
        assertCoordinates(route.getPosition(3), -74.0060, 40.7128, "Note pin; 40.7128, -74.0060");
        assertNoCoordinates(route.getPosition(4), "Unknown place");
    }

    @Test
    public void testParseUrlCoordinates() {
        assertNull(GoogleSavedListFormat.parseUrlCoordinates(null));
        assertNull(GoogleSavedListFormat.parseUrlCoordinates(PLACE_URL_1));
        assertNull(GoogleSavedListFormat.parseUrlCoordinates("https://www.google.com/maps/search/95.0,13.4"));
        assertNull(GoogleSavedListFormat.parseUrlCoordinates("https://www.google.com/maps/search/52.5,190.0"));
        assertNull(GoogleSavedListFormat.parseUrlCoordinates("https://www.google.com/maps/search/0.0,0.0"));
        assertArrayEquals(new double[]{13.4, 52.5},
                GoogleSavedListFormat.parseUrlCoordinates("https://www.google.com/maps/search/52.5,+13.4"), DELTA);
        assertArrayEquals(new double[]{-0.1276, 51.5072},
                GoogleSavedListFormat.parseUrlCoordinates("https://www.google.com/maps?ll=51.5072,-0.1276&z=12"), DELTA);
        assertArrayEquals(new double[]{32.5825, 0.0},
                GoogleSavedListFormat.parseUrlCoordinates("https://www.google.com/maps/place/0.0,32.5825"), DELTA);
    }

    @Test
    public void testParseNoteCoordinates() {
        assertNull(GoogleSavedListFormat.parseNoteCoordinates(null));
        assertNull(GoogleSavedListFormat.parseNoteCoordinates("Meet at 52.5, 13.4"));
        assertArrayEquals(new double[]{13.4, 52.5}, GoogleSavedListFormat.parseNoteCoordinates("52.5,13.4"), DELTA);
    }

    @Test
    public void testSkipsRowsWithoutTitleAndCoordinates() throws IOException {
        List<Wgs84Route> routes = read("Title,Note,URL\n" +
                ",A note only," + PLACE_URL_1 + "\n" +
                ",,\"https://www.google.com/maps/search/52.5,13.4\"\n" +
                "Cafe Alpha,," + PLACE_URL_1 + "\n");

        assertEquals(1, routes.size());
        Wgs84Route route = routes.get(0);
        assertEquals(2, route.getPositionCount());
        assertCoordinates(route.getPosition(0), 13.4, 52.5, null);
        assertNoCoordinates(route.getPosition(1), "Cafe Alpha");
    }

    @Test
    public void testOnlyHeaderGivesNoRoute() throws IOException {
        assertEquals(0, read("Title,Note,URL,Tags,Comment\n").size());
    }

    @Test
    public void testOtherCsvGivesNoRoute() throws IOException {
        assertEquals(0, read("Latitude,Longitude,Description\n52.5,13.4,Berlin\n").size());
        assertEquals(0, read("Title,URL,Latitude,Longitude\nBerlin," + PLACE_URL_1 + ",52.5,13.4\n").size());
        assertEquals(0, read("Name,Note,Link\nCafe Alpha,," + PLACE_URL_1 + "\n").size());
        assertEquals(0, read("").size());
    }

    @Test
    public void testIsHeader() {
        assertTrue(GoogleSavedListFormat.isHeader("Title,Note,URL"));
        assertTrue(GoogleSavedListFormat.isHeader("Title,Note,URL,Tags,Comment"));
        assertTrue(GoogleSavedListFormat.isHeader("\"Title\",\"Note\",\"URL\""));
        assertFalse(GoogleSavedListFormat.isHeader("Title,Note"));
        assertFalse(GoogleSavedListFormat.isHeader("Title,URL,Lat,Lng"));
        assertFalse(GoogleSavedListFormat.isHeader("Places for the summer trip"));
    }

    @Test
    public void testParserDetectsSavedList() throws IOException {
        File file = temporaryFile("Title,Note,URL,Tags,Comment\n" +
                "Cafe Alpha,," + PLACE_URL_1 + ",,\n" +
                "Bistro Beta,Try the soup," + PLACE_URL_2 + ",,\n");

        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(file);

        assertTrue(result.isSuccessful());
        assertEquals(GoogleSavedListFormat.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
    }

    @Test
    public void testParserLeavesPlainCsvToCsvFormat() throws IOException {
        File file = temporaryFile("Latitude,Longitude,Description\n52.5,13.4,Berlin\n48.1,11.6,Munich\n");

        ParserResult result = new NavigationFormatParser(new NavigationFormatRegistry()).read(file);

        assertTrue(result.isSuccessful());
        assertEquals(CsvCommaFormat.class, result.getFormat().getClass());
        assertEquals(2, result.getTheRoute().getPositionCount());
    }
}

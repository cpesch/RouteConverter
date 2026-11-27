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

package slash.navigation.garmin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class GarminPcx5FormatTest {
    private static final String HEADER = "H  SOFTWARE NAME & VERSION\n" +
            "I  PCX5 2.09\n" +
            "\n" +
            "H  R DATUM                IDX DA            DF            DX            DY            DZ\n" +
            "M  G WGS 84               121 +0.000000e+00 +0.000000e+00 +0.000000e+00 +0.000000e+00 +0.000000e+00\n" +
            "\n" +
            "H  COORDINATE SYSTEM\n" +
            "U  LAT LON DM\n";
    private static final String WAYPOINTS_HEADER = "\n" +
            "H  IDNT   LATITUDE    LONGITUDE    DATE      TIME     ALT   DESCRIPTION                              PROXIMITY     SYMBOL ;waypts\n";
    private static final String WPT = "W  WPT001 N4712.34567 E01107.12345 19-NOV-98 12:34:56   500 Brenner pass\n";

    private static final CompactCalendar T0 = utc(1970, 1, 1, 0, 0, 0);
    private final GarminPcx5Format format = new GarminPcx5Format();
    private TimeZone defaultTimeZone;

    @BeforeEach
    public void setUp() {
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    public void tearDown() {
        TimeZone.setDefault(defaultTimeZone);
    }

    private List<GpxRoute> read(byte[] bytes) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(bytes), context);
        return context.getRoutes();
    }

    private List<GpxRoute> read(String text) throws IOException {
        return read(text.getBytes(ISO_8859_1));
    }

    private static CompactCalendar utc(int year, int month, int day, int hour, int minute, int second) {
        return CompactCalendar.fromMillis(LocalDateTime.of(year, month, day, hour, minute, second).toEpochSecond(UTC) * 1000);
    }

    private static GpxPosition position(Double longitude, Double latitude, Double elevation, CompactCalendar time, String description) {
        return new GpxPosition(longitude, latitude, elevation, null, time, description);
    }

    private String write(GpxRoute route) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        return target.toString(ISO_8859_1);
    }

    private static String padded(String string, int length) {
        return String.format("%-" + length + "s", string);
    }

    // reading

    @Test
    public void readsWaypointsInDegreesAndMinutes() throws IOException {
        List<GpxRoute> routes = read(HEADER + WAYPOINTS_HEADER + WPT);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Waypoints, route.getCharacteristics());
        assertEquals(1, route.getPositionCount());
        GpxPosition position = route.getPosition(0);
        assertEquals(47 + 12.34567 / 60.0, position.getLatitude(), 1e-9);
        assertEquals(11 + 7.12345 / 60.0, position.getLongitude(), 1e-9);
        assertEquals(500.0, position.getElevation(), 0.0);
        assertEquals(utc(1998, 11, 19, 12, 34, 56), position.getTime());
        assertEquals("WPT001; Brenner pass", position.getDescription());
    }

    @Test
    public void unknownElevationIsNotRead() throws IOException {
        assertNull(read("W  A N4700.00000 E01100.00000 19-NOV-98 12:34:56 -9999 Text\n").get(0).getPosition(0).getElevation());
    }

    @Test
    public void readsSouthernAndWesternHemispheres() throws IOException {
        List<GpxRoute> routes = read("W  A S3300.00000 W07030.00000 19-NOV-98 12:34:56\n");
        GpxPosition position = routes.get(0).getPosition(0);
        assertEquals(-33.0, position.getLatitude(), 1e-9);
        assertEquals(-70.5, position.getLongitude(), 1e-9);
        assertNull(position.getElevation());
        assertEquals("A", position.getDescription());
    }

    @Test
    public void timeIsOnlySetWhenDateAndTimeParse() throws IOException {
        assertNull(read("W  A N4700.00000 E01100.00000 19-Xxx-98 12:34:56\n").get(0).getPosition(0).getTime());
        assertNull(read("W  A N4700.00000 E01100.00000 19-Nov-98 25:00:00\n").get(0).getPosition(0).getTime());
        assertNull(read("W  A N4700.00000 E01100.00000 31-Nov-98 12:00:00\n").get(0).getPosition(0).getTime());
        assertEquals(utc(1998, 11, 19, 12, 34, 56), read("W  A N4700.00000 E01100.00000 19-Nov-98 12:34:56\n").get(0).getPosition(0).getTime());
    }

    @Test
    public void twoDigitYearsBelow70AreInThisCentury() throws IOException {
        // GPSBabel 1.5.4 reads every year as 19yy
        assertEquals(utc(2007, 6, 16, 17, 54, 56), read("W  A N4700.00000 E01100.00000 16-Jun-07 17:54:56\n").get(0).getPosition(0).getTime());
        assertEquals(utc(2069, 6, 16, 17, 54, 56), read("W  A N4700.00000 E01100.00000 16-Jun-69 17:54:56\n").get(0).getPosition(0).getTime());
        assertEquals(utc(1970, 6, 16, 17, 54, 56), read("W  A N4700.00000 E01100.00000 16-Jun-70 17:54:56\n").get(0).getPosition(0).getTime());
    }

    @Test
    public void monthNamesAreReadInAnyCase() throws IOException {
        // GPSBabel 1.5.4 does not read the upper-case month names it writes
        assertEquals(utc(1998, 11, 19, 12, 34, 56), read("W  A N4700.00000 E01100.00000 19-NOV-98 12:34:56\n").get(0).getPosition(0).getTime());
        assertEquals(utc(1998, 11, 19, 12, 34, 56), read("W  A N4700.00000 E01100.00000 19-nov-98 12:34:56\n").get(0).getPosition(0).getTime());
    }

    @Test
    public void placeholderForPositionsWithoutTimeIsNotReadAsTime() throws IOException {
        assertNull(read("W  A N4700.00000 E01100.00000 01-JAN-70 00:00:00\n").get(0).getPosition(0).getTime());
    }

    @Test
    public void readsNamesWithWhitespaceOfTheWrittenLayout() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, null, "Ab1 Cd; Some pass")));
        GpxPosition position = read(write(route)).get(0).getPosition(0);
        assertEquals(47.5, position.getLatitude(), 1e-9);
        assertEquals(11.25, position.getLongitude(), 1e-9);
        assertEquals(500.0, position.getElevation(), 0.0);
        assertTrue(position.getDescription().startsWith("Ab1 Cd; Some pass"), position.getDescription());
    }

    @Test
    public void readsCoordinatesInDecimalDegrees() throws IOException {
        List<GpxRoute> routes = read("U  LAT LON DEG\nW  A N47.5 W11.25 19-NOV-98 12:34:56\n");
        GpxPosition position = routes.get(0).getPosition(0);
        assertEquals(47.5, position.getLatitude(), 1e-9);
        assertEquals(-11.25, position.getLongitude(), 1e-9);
    }

    @Test
    public void rejectsUnparseableDecimalDegrees() throws IOException {
        // pcx.cc would read this as a waypoint at 0/0
        assertTrue(read("U  LAT LON DEG\nW  A Nabc Wdef 19-NOV-98 12:34:56\n").isEmpty());
    }

    @Test
    public void readsGpsuCoordinatesAsDecimalMinutes() throws IOException {
        List<GpxRoute> routes = read("I  GPSU 1.0\nU  LAT LON DM\nW  A N4730.0 E01115.0 19-NOV-98 12:34:56\n");
        GpxPosition position = routes.get(0).getPosition(0);
        assertEquals(47.5, position.getLatitude(), 1e-9);
        assertEquals(11.25, position.getLongitude(), 1e-9);
    }

    @Test
    public void readsDescriptionFromTheCommentColumnOfTheFormatLine() throws IOException {
        String formatLine = "F  IDNT   LATITUDE    LONGITUDE    DATE      TIME     ALT   COMMENT";
        String waypoint = padded("W  WPT001 N4712.34567 E01107.12345 19-NOV-98 12:34:56   500", formatLine.indexOf("COMMENT")) + "Pass";
        List<GpxRoute> routes = read(formatLine + "\n" + waypoint + "\n");
        GpxPosition position = routes.get(0).getPosition(0);
        assertEquals(47 + 12.34567 / 60.0, position.getLatitude(), 1e-9);
        assertEquals(11 + 7.12345 / 60.0, position.getLongitude(), 1e-9);
        assertEquals("WPT001; Pass", position.getDescription());
    }

    @Test
    public void readsCoordinatesFromTheColumnsOfTheFormatLine() throws IOException {
        String formatLine = "F  IDNT   LONGITUDE    LATITUDE";
        String waypoint = "W  WPT001 E01107.12345 N4712.34567 19-NOV-98 12:34:56";
        GpxPosition position = read(formatLine + "\n" + waypoint + "\n").get(0).getPosition(0);
        assertEquals(47 + 12.34567 / 60.0, position.getLatitude(), 1e-9);
        assertEquals(11 + 7.12345 / 60.0, position.getLongitude(), 1e-9);
    }

    @Test
    public void readsRoutesAfterTheWaypoints() throws IOException {
        String text = HEADER + WAYPOINTS_HEADER +
                "R  Tour one\n" + WAYPOINTS_HEADER +
                "W  AAAAAA N4700.00000 E01100.00000 19-NOV-98 12:00:00     1\n" +
                "W  BBBBBB N4800.00000 E01200.00000 19-NOV-98 13:00:00     2\n" +
                "R  Tour two\n" + WAYPOINTS_HEADER +
                "W  CCCCCC N4900.00000 E01300.00000 19-NOV-98 14:00:00     3\n";
        List<GpxRoute> routes = read(text);
        assertEquals(3, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(3, routes.get(0).getPositionCount());
        assertEquals(Route, routes.get(1).getCharacteristics());
        assertEquals("Tour one", routes.get(1).getName());
        assertEquals(2, routes.get(1).getPositionCount());
        assertEquals("BBBBBB", routes.get(1).getPosition(1).getDescription());
        assertEquals(Route, routes.get(2).getCharacteristics());
        assertEquals("Tour two", routes.get(2).getName());
        assertEquals(1, routes.get(2).getPositionCount());
    }

    @Test
    public void readsTracksAfterTheWaypoints() throws IOException {
        String text = "H  TN My track\n" +
                "H  LATITUDE    LONGITUDE    DATE      TIME     ALT  ;track\n" +
                "T  N4700.00000 E01100.00000 19-NOV-98 12:00:00 100\n" +
                "T  N4701.00000 E01101.00000 19-NOV-98 12:00:10 101.5\n" +
                WPT;
        List<GpxRoute> routes = read(text);
        assertEquals(2, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(1, routes.get(0).getPositionCount());
        assertEquals(Track, routes.get(1).getCharacteristics());
        assertEquals("My track", routes.get(1).getName());
        assertEquals(2, routes.get(1).getPositionCount());
        assertEquals(101.5, routes.get(1).getPosition(1).getElevation(), 0.0);
        assertEquals(utc(1998, 11, 19, 12, 0, 10), routes.get(1).getPosition(1).getTime());
        assertEquals(47 + 1.0 / 60.0, routes.get(1).getPosition(1).getLatitude(), 1e-9);
    }

    @Test
    public void readsFilesWithTracksOnly() throws IOException {
        String text = "U  LAT LON DEG\nH  TN First\nT  N47.5 E11.25 19-NOV-98 12:00:00 100\n" +
                "H  TN Second\nT  N47.75 E11.5 19-NOV-98 12:00:10 101\n";
        List<GpxRoute> routes = read(text);
        assertEquals(2, routes.size());
        assertEquals(Track, routes.get(0).getCharacteristics());
        assertEquals("First", routes.get(0).getName());
        assertEquals(47.5, routes.get(0).getPosition(0).getLatitude(), 1e-9);
        assertEquals("Second", routes.get(1).getName());
    }

    @Test
    public void namesTracksWithoutHeader() throws IOException {
        List<GpxRoute> routes = read("T  N4700.00000 E01100.00000 19-NOV-98 12:00:00 100\n");
        assertEquals("Default", routes.get(0).getName());
        assertTrue(read("T  N4700.00000 E01100.00000 19-NOV-98 12:00:00\n").isEmpty());
    }

    @Test
    public void descriptionEndsBeforeProximityAndSymbol() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, null, "Position 1")));
        GpxPosition position = read(write(route)).get(0).getPosition(0);
        assertEquals("Position 1", position.getDescription());
    }

    @Test
    public void utmCoordinatesAreNotSupported() throws IOException {
        assertTrue(read("U  UTM\n" + WPT).isEmpty());
    }

    @Test
    public void allowsLeadingWhitespaceAndLineBreakVariants() throws IOException {
        assertEquals(1, read("\n  H  header\r\n\t" + WPT.replace("\n", "\r\n") + "\n").size());
    }

    @Test
    public void ignoresNonWgs84DatumLine() throws IOException {
        assertEquals(1, read("M  G European 1950          1 +0.0 +0.0 +0.0 +0.0 +0.0\n" + WPT).size());
    }

    @Test
    public void readsUtf8WhenThereIsAByteOrderMark() throws IOException {
        byte[] text = ("\ufeff" + padded("W  A N4700.00000 E01100.00000 19-NOV-98 12:34:56   500", 60) + "K\u00e4se\n").getBytes(UTF_8);
        assertEquals("A; K\u00e4se", read(text).get(0).getPosition(0).getDescription());
    }

    @Test
    public void bytesThatAreNoUtf8AreReadAsLatin1() throws IOException {
        // GPSBabel 1.5.4 delivers a replacement character
        byte[] text = padded("W  A N4700.00000 E01100.00000 19-NOV-98 12:34:56   500", 60).getBytes(ISO_8859_1);
        byte[] all = new byte[text.length + 3];
        System.arraycopy(text, 0, all, 0, text.length);
        all[text.length] = 'K';
        all[text.length + 1] = (byte) 0xFC;
        all[text.length + 2] = 'x';
        assertEquals("A; K\u00fcx", read(all).get(0).getPosition(0).getDescription());
    }

    @Test
    public void rejectsFilesThatAreNotPcx5() throws IOException {
        assertTrue(read("").isEmpty());
        assertTrue(read("\n\n").isEmpty());
        assertTrue(read(HEADER).isEmpty());
        assertTrue(read(HEADER + "X  unknown line\n" + WPT).isEmpty());
        assertTrue(read("OziExplorer Waypoint File Version 1.1\nWGS 84\n" + WPT).isEmpty());
        assertTrue(read("W  A N4700.00000 E01100.00000\n").isEmpty());
        assertTrue(read("W  A X4700.00000 E01100.00000 19-NOV-98 12:34:56\n").isEmpty());
        assertTrue(read("W  A N47abc E01100.00000 19-NOV-98 12:34:56\n").isEmpty());
        assertTrue(read(new byte[]{0, 1, 2, (byte) 0xFF, 'W', ' ', 'x'}).isEmpty());
    }

    // validity

    @Test
    public void rejectsTheTomTomOv2ClashPattern() {
        GpxPosition noCoordinates = position(null, null, 200000.0, null, null);
        GpxPosition noLongitude = position(null, 47.0, 200000.0, null, null);
        GpxPosition valid = position(11.0, 47.0, 500.0, null, null);
        assertFalse(format.isValidRoute(format.createRoute(Waypoints, null, singletonList(noCoordinates))));
        assertFalse(format.isValidRoute(format.createRoute(Waypoints, null, asList(noLongitude, noCoordinates))));
        assertFalse(format.isValidRoute(format.createRoute(Waypoints, null, List.<GpxPosition>of())));
        assertTrue(format.isValidRoute(format.createRoute(Waypoints, null, asList(noCoordinates, valid))));
    }

    // writing

    private static String waypointLine(String name, String latitude, String longitude, String time, String elevation, String description, int symbol) {
        return "W  " + padded(name, 6) + " " + latitude + " " + longitude + " " + time + " " + elevation + " " +
                padded(description, 40) + " 0.000000e+00  " + symbol + "\n";
    }

    @Test
    public void writesTheHeaderAndWaypoints() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, "ignored", asList(
                position(11.25, 47.5, 500.0, utc(2026, 11, 19, 12, 34, 56), "WPT001; Brenner pass"),
                position(-70.5, -33.0, null, T0, "A very long name; " + "d".repeat(50)),
                position(0.5, -0.5, 1234.4, utc(1999, 1, 2, 3, 4, 5), null)));
        String expected = HEADER + WAYPOINTS_HEADER +
                waypointLine("WPT001", "N4730.00000", "E01115.00000", "19-NOV-26 12:34:56", "  500", "Brenner pass", 0) +
                waypointLine("A very", "S3300.00000", "W07030.00000", "01-JAN-70 00:00:00", "-9999", "d".repeat(40), 0) +
                waypointLine("", "S30.00000", "E00030.00000", "02-JAN-99 03:04:05", " 1234", "", 0);
        assertEquals(expected, write(route));
    }

    @Test
    public void writesElevationsRoundedHalfToEven() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, null, asList(
                position(1.0, 1.0, 2.5, T0, "a"), position(1.0, 1.0, 3.5, T0, "b"), position(1.0, 1.0, -0.4, T0, "c")));
        String written = write(route);
        assertTrue(written.contains("01-JAN-70 00:00:00     2 "), written);
        assertTrue(written.contains("01-JAN-70 00:00:00     4 "), written);
        assertTrue(written.contains("01-JAN-70 00:00:00    -0 "), written);
    }

    @Test
    public void skipsPositionsWithoutCoordinatesAndHonoursTheIndices() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, null, asList(
                position(11.25, 47.5, 1.0, T0, "A"), position(null, 47.5, 2.0, T0, "B"), position(11.25, 47.5, 3.0, T0, "C")));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 1, 3);
        String written = target.toString(ISO_8859_1);
        assertFalse(written.contains(" B "));
        assertEquals(HEADER + WAYPOINTS_HEADER + waypointLine("C", "N4730.00000", "E01115.00000", "01-JAN-70 00:00:00", "    3", "C", 0), written);
    }

    @Test
    public void writesRoutesAndTracksAsWaypoints() throws IOException {
        for (GpxRoute route : asList(
                format.createRoute(Route, "A route", singletonList(position(11.25, 47.5, 500.0, T0, "P1"))),
                format.createRoute(Track, "Track", singletonList(position(11.25, 47.5, 500.0, T0, "P1"))))) {
            assertEquals(HEADER + WAYPOINTS_HEADER + waypointLine("P1", "N4730.00000", "E01115.00000", "01-JAN-70 00:00:00", "  500", "P1", 0), write(route));
        }
    }

    @Test
    public void writesTheTimeAsUtc() throws IOException {
        // GPSBabel 1.5.4 writes the local time
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
        GpxRoute route = format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, utc(2026, 1, 1, 10, 0, 0), "P1")));
        assertTrue(write(route).contains(" 01-JAN-26 10:00:00 "));
        route = format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, utc(2026, 7, 1, 10, 0, 0), "P1")));
        assertTrue(write(route).contains(" 01-JUL-26 10:00:00 "));
    }

    @Test
    public void writesTheHourOnATwentyFourHourClock() throws IOException {
        // GPSBabel 1.5.4 writes a 12 hour clock without am/pm
        for (int hour : new int[]{0, 1, 11, 12, 13, 15, 23}) {
            GpxRoute route = format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, utc(2026, 6, 5, hour, 29, 21), "P1")));
            assertTrue(write(route).contains(String.format(" 05-JUN-26 %02d:29:21 ", hour)), write(route));
        }
    }

    @Test
    public void positionsWithoutTimeAreWrittenWithAPlaceholderAndReadWithoutTime() throws IOException {
        // GPSBabel 1.5.4 writes the current time
        String written = write(format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, null, "P1"))));
        assertTrue(written.contains(" 01-JAN-70 00:00:00   500 P1 "), written);
        assertNull(read(written).get(0).getPosition(0).getTime());
    }

    @Test
    public void writesLatin1AndReplacesOtherCharacters() throws IOException {
        String written = write(format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, T0, "Z\u00fcrich\u0001; K\u00e4se \u20ac"))));
        assertTrue(written.contains("W  Z\u00fcrich N4730.00000"), written);
        assertTrue(written.contains(" K\u00e4se ? "), written);
    }

    @Test
    public void writtenFilesCanBeReadAgain() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, null, asList(
                position(11.25, 47.5, 500.0, utc(1998, 11, 19, 12, 34, 56), "WPT001; Brenner pass"),
                position(-70.5, -33.25, -12.0, utc(1998, 11, 20, 0, 0, 1), "WPT002")));
        List<GpxRoute> routes = read(write(route));
        assertEquals(1, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(2, routes.get(0).getPositionCount());
        for (int i = 0; i < 2; i++) {
            GpxPosition expected = route.getPosition(i), actual = routes.get(0).getPosition(i);
            assertEquals(expected.getLongitude(), actual.getLongitude(), 1e-6);
            assertEquals(expected.getLatitude(), actual.getLatitude(), 1e-6);
            assertEquals(expected.getElevation(), actual.getElevation(), 0.0);
            assertEquals(expected.getTime(), actual.getTime());
            // the description is everything from column 60 on, including proximity and symbol of the written line
            assertTrue(actual.getDescription().startsWith(expected.getDescription()), actual.getDescription());
        }
    }

    // contract

    @Test
    public void keepsTheContractOfTheGpsBabelBasedFormat() {
        assertEquals(".wpt", format.getExtension());
        assertEquals("Garmin PCX5 (*.wpt)", format.getName());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
        assertFalse(format.isWritingRouteCharacteristics());
    }

    @Test
    public void formatsFixedPointLikeC() {
        assertEquals("04712.34567", GarminPcx5Format.formatFixed(4712.34567, 11, 5, true));
        assertEquals("0030.00000", GarminPcx5Format.formatFixed(30.0, 10, 5, true));
        assertEquals("    2", GarminPcx5Format.formatFixed(2.5, 5, 0, false));
        assertEquals("    4", GarminPcx5Format.formatFixed(3.5, 5, 0, false));
        assertEquals("    0", GarminPcx5Format.formatFixed(0.5, 5, 0, false));
        assertEquals("-9999", GarminPcx5Format.formatFixed(-9999.0, 5, 0, false));
        assertEquals("   -0", GarminPcx5Format.formatFixed(-0.4, 5, 0, false));
        assertEquals("123456", GarminPcx5Format.formatFixed(123456.0, 5, 0, false));
    }

    @Test
    public void isRegisteredInPlaceOfTheGpsBabelFormat() {
        // the registry imports slash.navigation.babel.* and slash.navigation.garmin.GarminPcx5Format explicitly: the single-type import
        // wins (JLS 6.4.1), so addFormat(GarminPcx5Format.class) keeps its position but now means the native class
        List<String> names = new ArrayList<>();
        for (NavigationFormat<?> format : new NavigationFormatRegistry().getReadFormats())
            names.add(format.getClass().getName());
        int index = names.indexOf("slash.navigation.garmin.GarminPcx5Format");
        assertTrue(index > 0, names.toString());
        assertFalse(names.contains("slash.navigation.babel.GarminPcx5Format"));
        assertEquals("CompeGPSDataWaypointFormat", names.get(index - 1).substring(names.get(index - 1).lastIndexOf('.') + 1));
        assertEquals("GeoCachingFormat", names.get(index + 1).substring(names.get(index + 1).lastIndexOf('.') + 1));
    }

    @Test
    public void testAbbreviatesLoggedLines() {
        assertEquals("M  G WGS 84", GarminPcx5Format.abbreviateForLog("M  G WGS 84"));
        assertEquals("MsRcd?d??", GarminPcx5Format.abbreviateForLog("MsRcd\u0000d\u0001\u0002"));
        String longLine = "M" + "x".repeat(5000);
        String abbreviated = GarminPcx5Format.abbreviateForLog(longLine);
        assertEquals(83, abbreviated.length());
        assertTrue(abbreviated.endsWith("..."));
    }
}

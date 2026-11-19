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

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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

    private final GarminPcx5Format format = new GarminPcx5Format();

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
        return target.toString(US_ASCII);
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
        assertNull(read("W  A N4700.00000 E01100.00000 19-XXX-98 12:34:56\n").get(0).getPosition(0).getTime());
        assertNull(read("W  A N4700.00000 E01100.00000 19-NOV-98 25:00:00\n").get(0).getPosition(0).getTime());
        assertNull(read("W  A N4700.00000 E01100.00000 31-NOV-98 12:00:00\n").get(0).getPosition(0).getTime());
        assertEquals(utc(1998, 11, 19, 0, 0, 0), read("W  A N4700.00000 E01100.00000 19-nov-98 00:00:00\n").get(0).getPosition(0).getTime());
    }

    @Test
    public void readsCoordinatesInDecimalDegrees() throws IOException {
        List<GpxRoute> routes = read("U  LAT LON DEG\nW  A N47.5 W11.25 19-NOV-98 12:34:56\n");
        GpxPosition position = routes.get(0).getPosition(0);
        assertEquals(47.5, position.getLatitude(), 1e-9);
        assertEquals(-11.25, position.getLongitude(), 1e-9);
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
    public void ignoresTracks() throws IOException {
        String text = "H  TN Track\n" +
                "H  LATITUDE    LONGITUDE    DATE      TIME     ALT  ;track\n" +
                "T  N4700.00000 E01100.00000 19-NOV-98 12:00:00 100\n" +
                "T  N4701.00000 E01101.00000 19-NOV-98 12:00:10 101\n" +
                WPT;
        List<GpxRoute> routes = read(text);
        assertEquals(1, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(1, routes.get(0).getPositionCount());
        assertTrue(read("T  N4700.00000 E01100.00000 19-NOV-98 12:00:00 100\n").isEmpty());
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
                position(-70.5, -33.0, null, null, "A very long name; " + "d".repeat(50)),
                position(0.5, -0.5, 1234.4, utc(1999, 1, 2, 3, 4, 5), null)));
        String expected = HEADER + WAYPOINTS_HEADER +
                waypointLine("WPT001", "N4730.00000", "E01115.00000", "19-NOV-26 12:34:56", "  500", "Brenner pass", 18) +
                waypointLine("A very", "S3300.00000", "W07030.00000", "01-JAN-70 00:00:00", "-9999", "d".repeat(40), 18) +
                waypointLine("", "S30.00000", "E00030.00000", "02-JAN-99 03:04:05", " 1234", "", 18);
        assertEquals(expected, write(route));
    }

    @Test
    public void writesElevationsRoundedHalfToEven() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, null, asList(
                position(1.0, 1.0, 2.5, null, "a"), position(1.0, 1.0, 3.5, null, "b"), position(1.0, 1.0, -0.4, null, "c")));
        String written = write(route);
        assertTrue(written, written.contains("01-JAN-70 00:00:00     2 "));
        assertTrue(written, written.contains("01-JAN-70 00:00:00     4 "));
        assertTrue(written, written.contains("01-JAN-70 00:00:00    -0 "));
    }

    @Test
    public void skipsPositionsWithoutCoordinatesAndHonoursTheIndices() throws IOException {
        GpxRoute route = format.createRoute(Waypoints, null, asList(
                position(11.25, 47.5, 1.0, null, "A"), position(null, 47.5, 2.0, null, "B"), position(11.25, 47.5, 3.0, null, "C")));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 1, 3);
        String written = target.toString(US_ASCII);
        assertFalse(written.contains(" B "));
        assertEquals(HEADER + WAYPOINTS_HEADER + waypointLine("C", "N4730.00000", "E01115.00000", "01-JAN-70 00:00:00", "    3", "", 18), written);
    }

    @Test
    public void writesRoutesWithTheirNameShortenedToTwentyCharactersWithoutWhitespace() throws IOException {
        GpxRoute route = format.createRoute(Route, "A route with a rather long name", singletonList(position(11.25, 47.5, 500.0, null, "P1")));
        String expected = HEADER + "\n\nR  Aroutewitharatherlon\n" + WAYPOINTS_HEADER +
                waypointLine("P1", "N4730.00000", "E01115.00000", "01-JAN-70 00:00:00", "  500", "", 18);
        assertEquals(expected, write(route));
    }

    @Test
    public void writesTheDefaultRouteName() throws IOException {
        String written = write(format.createRoute(Route, null, singletonList(position(11.25, 47.5, 500.0, null, "P1"))));
        assertTrue(written, written.startsWith(HEADER + "\n\nR  Rte001\n" + WAYPOINTS_HEADER));
    }

    @Test
    public void writesTracksAsWaypoints() throws IOException {
        String written = write(format.createRoute(Track, "Track", singletonList(position(11.25, 47.5, 500.0, null, "P1"))));
        assertTrue(written, written.startsWith(HEADER + WAYPOINTS_HEADER + "W  P1     N4730.00000"));
    }

    @Test
    public void replacesCharactersThatAreNotAscii() throws IOException {
        String written = write(format.createRoute(Waypoints, null, singletonList(position(11.25, 47.5, 500.0, null, "Z\u00fcrich\u0001; K\u00e4se"))));
        assertTrue(written, written.contains("W  Z?rich N4730.00000"));
        assertTrue(written, written.contains(" K?se "));
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
            assertTrue(actual.getDescription(), actual.getDescription().startsWith(expected.getDescription()));
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
}

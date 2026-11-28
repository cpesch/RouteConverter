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

package slash.navigation.ozi;

import slash.navigation.base.RouteCharacteristics;
import org.junit.jupiter.api.Test;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.common.Datums;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class OziExplorerRouteFormatTest {
    private static final Charset CP1252 = Charset.forName("windows-1252");

    private static String lines(String... lines) {
        return String.join("\r\n", lines) + "\r\n";
    }

    private static String plt(String datum, String unit, String... points) {
        List<String> all = new ArrayList<>(asList("OziExplorer Track Point File Version 2.1", datum,
                "Altitude is in " + unit, "Reserved 3", "0,2,255,Test Track,0,0,2,8421376", String.valueOf(points.length)));
        all.addAll(asList(points));
        return lines(all.toArray(new String[0]));
    }

    private static String rte(String datum, String... body) {
        List<String> all = new ArrayList<>(asList("OziExplorer Route File Version 1.0", datum, "Reserved 1", "Reserved 2"));
        all.addAll(asList(body));
        return lines(all.toArray(new String[0]));
    }

    private static String wpt(String datum, String... body) {
        List<String> all = new ArrayList<>(asList("OziExplorer Waypoint File Version 1.1", datum, "Reserved 2", "Reserved 3"));
        all.addAll(asList(body));
        return lines(all.toArray(new String[0]));
    }

    private static List<GpxRoute> read(OziExplorerFormat format, String content) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(content.getBytes(CP1252)), context);
        return context.getRoutes();
    }

    private static String write(OziExplorerFormat format, GpxRoute route) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(route, out, 0, route.getPositionCount());
        return new String(out.toByteArray(), CP1252);
    }

    private static GpxRoute route(NavigationFormat<GpxRoute> format, String name, GpxPosition... positions) {
        return new GpxRoute(new Gpx10Format(), Route, name, null, new ArrayList<>(asList(positions)));
    }

    private static GpxPosition position(double longitude, double latitude, Double elevation, String description) {
        return new GpxPosition(longitude, latitude, elevation, null, null, description);
    }

    private static final String PLT = plt("WGS 84", "Feet", "47.500000,8.250000,0,100.0,40000.5000000,,");
    private static final String RTE = rte("WGS 84", "R,0,Route 1,A route,",
            "W,0,,1,Start,47.500000,8.250000,,0,1,3,0,65535,First,0,0",
            "W,0,,2,End,47.600000,8.350000,,0,1,3,0,65535,Second,0,0");
    private static final String WPT = wpt("WGS 84", "1,Alpha,47.500000,8.250000,,0,1,3,0,65535,Alpha desc,0,0,0,328,6,0,17");

    // reading: file type detection

    @Test
    public void eachClassReadsOnlyItsOwnFileType() throws IOException {
        OziExplorerFormat[] formats = {new OziExplorerRouteFormat(), new OziExplorerTrackFormat(), new OziExplorerWaypointFormat()};
        String[] contents = {RTE, PLT, WPT};
        for (int f = 0; f < formats.length; f++)
            for (int c = 0; c < contents.length; c++)
                assertEquals(f == c ? 1 : 0, read(formats[f], contents[c]).size(), "format " + f + " content " + c);
    }

    @Test
    public void nonOziContentYieldsNoRoute() throws IOException {
        assertTrue(read(new OziExplorerWaypointFormat(), "<?xml version=\"1.0\"?>\r\n<gpx/>\r\n").isEmpty());
        assertTrue(read(new OziExplorerTrackFormat(), "").isEmpty());
        assertTrue(read(new OziExplorerRouteFormat(), "OziExplorer Route File Version 1.0\r\n").isEmpty());
    }

    @Test
    public void noFormatReadsOtherOziExplorerFileTypes() throws IOException {
        // GPSBabel reads them as waypoints, with the columns of an event or point file in the wrong fields;
        // JOSM and GPXSee require "Waypoint File"
        String event = lines("OziExplorer Event File Version 1.0", "WGS 84", "Reserved 2", "Reserved 3",
                "1,Event,47.500000,8.250000,,0,1,3,0,65535,Event desc,0,0,0,328,6,0,17");
        assertTrue(read(new OziExplorerWaypointFormat(), event).isEmpty());
        String point = lines("OziExplorer Point File Version 1.0", "WGS 84", "Reserved 2", "Reserved 3",
                "1,Point,47.500000,8.250000,,0,1,3,0,65535,Point desc,0,0,0,328,6,0,17");
        assertTrue(read(new OziExplorerWaypointFormat(), point).isEmpty());
        assertEquals(1, read(new OziExplorerWaypointFormat(), event.replace("Event File", "CE Waypoint File")).size());
        assertTrue(read(new OziExplorerRouteFormat(), event).isEmpty());
        assertTrue(read(new OziExplorerTrackFormat(), event).isEmpty());
    }

    @Test
    public void unknownDatumIsReadAsWgs84() throws IOException {
        // GPSBabel declines the file
        GpxPosition position = read(new OziExplorerTrackFormat(), plt("Nowhere Datum 1999", "Feet", "47.5,8.25,0,100,,,")).get(0).getPosition(0);
        assertEquals(47.5, position.getLatitude(), 0.0);
        assertEquals(8.25, position.getLongitude(), 0.0);
    }

    @Test
    public void oziExplorerDatumNamesAreConverted() throws IOException {
        // OziExplorer's name for OSGB36, which GPSBabel does not know
        GpxPosition position = read(new OziExplorerWaypointFormat(), wpt("Ord Srvy Grt Britn",
                "1,London,51.500000,-0.100000,,0,1,3,0,65535,d,0,0,0,-777,6,0,17")).get(0).getPosition(0);
        double[] expected = Datums.toWgs84(51.5, -0.1, 0.0, Datums.lookupIndex("OSGB36"));
        assertEquals(expected[0], position.getLatitude(), 1e-12);
        assertEquals(expected[1], position.getLongitude(), 1e-12);
        assertTrue(Math.abs(expected[1] + 0.1) > 1e-4);
    }

    @Test
    public void headerLinesPaddedWithCommasAreRead() throws IOException {
        // spreadsheet exports pad every line with commas up to the widest one
        String route = lines("OziExplorer Route File Version 1.0,,,,,,,,,,,,,,,", "WGS 84,,,,,,,,,,,,,,,",
                "Reserved 1,,,,,,,,,,,,,,,", "Reserved 2,,,,,,,,,,,,,,,", "R,1,Padded,,,,,,,,,,,,,",
                "W,1,1,6,Start,47.500000,8.250000,,32,1,6,,,First,,2",
                "W,1,2,6,End,47.600000,8.350000,,32,1,6,,,Second,,2");
        List<GpxRoute> routes = read(new OziExplorerRouteFormat(), route);
        assertEquals(1, routes.size());
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals(47.6, routes.get(0).getPosition(1).getLatitude(), 0.0000001);

        String waypoints = lines("OziExplorer Waypoint File Version 1.1,,,,,,,,,,,,,,", "WGS 84 ,,,,,,,,,,,,,,",
                "Reserved 2,,,,,,,,,,,,,,", "garmin,,,,,,,,,,,,,,",
                "1,Alpha,47.500000,8.250000,,30,1,6,,,Alpha desc,,2,,-777");
        routes = read(new OziExplorerWaypointFormat(), waypoints);
        assertEquals(1, routes.size());
        assertEquals(1, routes.get(0).getPositionCount());
        assertNull(routes.get(0).getPosition(0).getElevation());
    }

    private static final String RT2 = "H1,OziExplorer CE Route2 File Version 1.0\nH2,WGS 84\nH3,Lake Tour,,0\n" +
            "W,Start,  47.500000,   8.250000,0\nW,Z\u00fcrich,47.6,8.35,1\n";

    @Test
    public void route2FileIsReadByRouteFormatOnly() throws IOException {
        List<GpxRoute> routes = read(new OziExplorerRouteFormat(), RT2);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Route, route.getCharacteristics());
        assertEquals("Lake Tour", route.getName());
        assertEquals(2, route.getPositionCount());
        assertEquals(47.5, route.getPosition(0).getLatitude(), 0.0000001);
        assertEquals(8.25, route.getPosition(0).getLongitude(), 0.0000001);
        assertEquals("Start", route.getPosition(0).getDescription());
        assertEquals("Z\u00fcrich", route.getPosition(1).getDescription());

        assertTrue(read(new OziExplorerTrackFormat(), RT2).isEmpty());
        assertTrue(read(new OziExplorerWaypointFormat(), RT2).isEmpty());
    }

    @Test
    public void route2TextWrittenAsUtf8IsRead() throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        new OziExplorerRouteFormat().read(new ByteArrayInputStream(RT2.getBytes(StandardCharsets.UTF_8)), context);
        assertEquals("Z\u00fcrich", context.getRoutes().get(0).getPosition(1).getDescription());
    }

    @Test
    public void route2FileWithUnknownDatumOrWithoutPositionsYieldsNoRoute() throws IOException {
        assertEquals(1, read(new OziExplorerRouteFormat(), RT2.replace("WGS 84", "Nowhere Datum 1999")).size());
        assertTrue(read(new OziExplorerRouteFormat(), "H1,OziExplorer CE Route2 File Version 1.0\nH2,WGS 84\nH3,Empty,,0\n").isEmpty());
        assertTrue(read(new OziExplorerRouteFormat(), "H1,OziExplorer CE Route2 File Version 1.0\nWGS 84\n").isEmpty());
    }

    @Test
    public void unknownAltitudeUnitIsFeet() throws IOException {
        // GPSBabel declines the file, the format defines feet
        GpxPosition furlongs = read(new OziExplorerTrackFormat(), plt("WGS 84", "Furlongs", "47.5,8.25,0,100,,,")).get(0).getPosition(0);
        assertEquals(30.48, furlongs.getElevation(), 1e-9);
        GpxPosition metres = read(new OziExplorerTrackFormat(), plt("WGS 84", "Metres", "47.5,8.25,0,100,,,")).get(0).getPosition(0);
        assertEquals(100.0, metres.getElevation(), 1e-9);
    }

    @Test
    public void european1950IsConvertedToWgs84() throws IOException {
        List<GpxRoute> routes = read(new OziExplorerTrackFormat(), plt("European 1950", "Meters", "47.500000,8.250000,0,100.0,,,"));
        GpxPosition position = routes.get(0).getPosition(0);
        double[] expected = Datums.toWgs84(47.5, 8.25, 0.0, Datums.lookupIndex("European 1950"));
        assertEquals(expected[0], position.getLatitude(), 1e-12);
        assertEquals(expected[1], position.getLongitude(), 1e-12);
        assertTrue(Math.abs(expected[0] - 47.5) > 1e-6);
    }

    // reading: values

    @Test
    public void trackAltitudeInFeetAndMeters() throws IOException {
        GpxPosition feet = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,100,,,")).get(0).getPosition(0);
        assertEquals(30.48, feet.getElevation(), 1e-9);
        GpxPosition meters = read(new OziExplorerTrackFormat(), plt("WGS 84", "Meters", "47.5,8.25,0,100,,,")).get(0).getPosition(0);
        assertEquals(100.0, meters.getElevation(), 1e-9);
    }

    @Test
    public void unknownAltitudeIsNull() throws IOException {
        GpxPosition position = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,-777,,,")).get(0).getPosition(0);
        assertNull(position.getElevation());
        GpxPosition waypoint = read(new OziExplorerWaypointFormat(),
                wpt("WGS 84", "1,A,47.5,8.25,,0,1,3,0,65535,d,0,0,0,-777,6,0,17")).get(0).getPosition(0);
        assertNull(waypoint.getElevation());
    }

    @Test
    public void dayEpochIs18991230() throws IOException {
        // 25569 is 1970-01-01, a fractional day adds the time of day
        GpxPosition midnight = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,25570.0000000,,")).get(0).getPosition(0);
        assertEquals(86400000L, midnight.getTime().getTimeInMillis());
        GpxPosition noon = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,25570.5000000,,")).get(0).getPosition(0);
        assertEquals(86400000L + 43200000L, noon.getTime().getTimeInMillis());
        GpxPosition none = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,,,")).get(0).getPosition(0);
        assertNull(none.getTime());
        GpxPosition epoch = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,25569.0000000,,")).get(0).getPosition(0);
        assertNull(epoch.getTime());
        GpxPosition zero = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,0,,")).get(0).getPosition(0);
        assertNull(zero.getTime());
        // GPSBabel drops times before 1970, Delphi counts them
        GpxPosition before1970 = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,25568.5000000,,")).get(0).getPosition(0);
        assertEquals(-43200000L, before1970.getTime().getTimeInMillis());
    }

    @Test
    public void absurdTimesAndAltitudesAreDropped() throws IOException {
        List<GpxRoute> routes = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet",
                "47.5,8.25,0,1e300,1e300,,", "47.6,8.35,0,100001,9e15,,", "47.7,8.45,0,-100001,2958466,,",
                "47.8,8.55,0,99999,2958465.5,,"));
        List<GpxPosition> positions = routes.get(0).getPositions();
        assertEquals(4, positions.size());
        for (int i = 0; i < 3; i++) {
            assertNull(positions.get(i).getTime(), "position " + i);
            assertNull(positions.get(i).getElevation(), "position " + i);
        }
        assertEquals(99999 * 0.3048, positions.get(3).getElevation(), 1e-6);
        assertNotNull(positions.get(3).getTime());
    }

    @Test
    public void garbledBlankAndOutOfRangeRowsAreSkipped() throws IOException {
        List<GpxRoute> tracks = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet",
                "47.5,8.25,0,100,,,", "abc,def,0,100,,,", "999,999,0,100,,,", ",,,,,,", "47.5,,0,100,,,",
                "-90,180,0,100,,,", "90.5,8.25,0,100,,,", "47.6,180.5,0,100,,,"));
        assertEquals(1, tracks.size());
        assertEquals(2, tracks.get(0).getPositionCount());
        assertEquals(-90.0, tracks.get(0).getPosition(1).getLatitude(), 0.0);

        List<GpxRoute> routes = read(new OziExplorerRouteFormat(), rte("WGS 84", "R,0,Route 1,,",
                "W,0,,1,P1", "W,0,,2,P2,48.1", "W,0,,3,P3,48.1,11.5,,0,1,3,0,65535,,0,0", ",,,,,,,,,"));
        assertEquals(1, routes.get(0).getPositionCount());
        assertEquals("P3", routes.get(0).getPosition(0).getDescription());

        List<GpxRoute> waypoints = read(new OziExplorerWaypointFormat(), wpt("WGS 84",
                "1,Alpha,47.500000,8.250000,,0,1,3,0,65535,d,0,0,0,328,6,0,17", "2,Beta,x,y,,0,1,3,0,65535,d,0,0,0,328,6,0,17",
                ",,,,,,,,,,,,,,,,,"));
        assertEquals(1, waypoints.get(0).getPositionCount());
    }

    @Test
    public void loneCarriageReturnsEndLines() throws IOException {
        String text = "OziExplorer Track Point File Version 2.1\rWGS 84\rAltitude is in Feet\rReserved 3\r" +
                "0,2,255,Mac,0,0,2,8421376\r2\r47.5,8.25,0,100,,,\r47.6,8.35,0,100,,,\r";
        List<GpxRoute> routes = read(new OziExplorerTrackFormat(), text);
        assertEquals("Mac", routes.get(0).getName());
        assertEquals(2, routes.get(0).getPositionCount());
    }

    @Test
    public void character209IsAComma() throws IOException {
        // "comma's not allowed in text fields, character 209 can be used instead and a comma will be substituted"
        List<GpxRoute> routes = read(new OziExplorerRouteFormat(), rte("WGS 84", "R,0,Route\u00D11,Desc\u00D1x,",
                "W,0,,1,P\u00D11,47.1,8.1,,0,1,3,0,65535,a\u00D1b,0,0"));
        assertEquals("Route,1", routes.get(0).getName());
        assertEquals("P,1; a,b", routes.get(0).getPosition(0).getDescription());
        List<GpxRoute> tracks = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,100,,,").replace("Test Track", "A\u00D1B"));
        assertEquals("A,B", tracks.get(0).getName());
    }

    @Test
    public void trackBreakStartsNewTrack() throws IOException {
        List<GpxRoute> routes = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet",
                "47.1,8.1,1,0,,,", "47.2,8.2,0,0,,,", "47.3,8.3,1,0,,,", "47.4,8.4,0,0,,,"));
        assertEquals(2, routes.size());
        assertEquals(Track, routes.get(0).getCharacteristics());
        assertEquals("Test Track", routes.get(0).getName());
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals(2, routes.get(1).getPositionCount());
        assertEquals(47.3, routes.get(1).getPosition(0).getLatitude(), 1e-9);
    }

    @Test
    public void routeAndWaypointValues() throws IOException {
        List<GpxRoute> routes = read(new OziExplorerRouteFormat(), RTE);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Route, route.getCharacteristics());
        assertEquals("Route 1", route.getName());
        assertEquals(asList("A route"), route.getDescription());
        assertEquals(2, route.getPositionCount());
        assertEquals("Start; First", route.getPosition(0).getDescription());
        assertEquals(8.35, route.getPosition(1).getLongitude(), 1e-9);

        GpxRoute waypoints = read(new OziExplorerWaypointFormat(), WPT).get(0);
        assertEquals(Waypoints, waypoints.getCharacteristics());
        GpxPosition position = waypoints.getPosition(0);
        assertEquals("Alpha desc", position.getDescription());
        assertEquals(328 * 0.3048, position.getElevation(), 1e-5);
    }

    // reading: sanitize and validity

    @Test
    public void sanitizeDropsLeadingRptPoint() throws IOException {
        List<GpxRoute> routes = read(new OziExplorerRouteFormat(), rte("WGS 84",
                "R,0,One,,", "W,0,,1,A,47.1,8.1,,0,1,3,0,65535,a,0,0",
                "R,1,Two,,", "W,1,,1,RPT001,0.000000,0.000000,,0,1,3,0,65535,,0,0",
                "W,1,,2,B,47.2,8.2,,0,1,3,0,65535,b,0,0"));
        assertEquals(2, routes.size());
        assertEquals(1, routes.get(0).getPositionCount());
        assertEquals(1, routes.get(1).getPositionCount());
        assertEquals("B; b", routes.get(1).getPosition(0).getDescription());
    }

    @Test
    public void routesWithOnlyEmptyPositionsAreRejected() throws IOException {
        assertTrue(read(new OziExplorerRouteFormat(), rte("WGS 84", "R,0,Empty,,")).isEmpty());
        assertTrue(read(new OziExplorerRouteFormat(), rte("WGS 84", "R,0,Zero,,",
                "W,0,,1,X,0.0,0.0,,0,1,3,0,65535,,0,0")).isEmpty());
        // a 0/0 point with an elevation is not empty
        assertEquals(1, read(new OziExplorerWaypointFormat(),
                wpt("WGS 84", "1,A,0,0,,0,1,3,0,65535,d,0,0,0,100,6,0,17")).size());
    }

    @Test
    public void isValidRouteRule() {
        OziExplorerRouteFormat format = new OziExplorerRouteFormat();
        assertFalse(format.isValidRoute(route(format, "r", position(0.0, 0.0, null, "a"), position(0.0, 0.0, 0.0, "b"))));
        assertTrue(format.isValidRoute(route(format, "r", position(0.0, 0.0, null, "a"), position(1.0, 0.0, null, "b"))));
        assertTrue(format.isValidRoute(route(format, "r", position(0.0, 0.0, 5.0, "a"))));
    }

    @Test
    public void sanitizeOnlyRemovesRptPoints() {
        OziExplorerRouteFormat format = new OziExplorerRouteFormat();
        GpxRoute kept = format.sanitizeRouteAfterReading(route(format, "r", position(0.0, 0.0, null, "Other"), position(1.0, 1.0, null, "RPT1")));
        assertEquals(2, kept.getPositionCount());
        GpxRoute removed = format.sanitizeRouteAfterReading(route(format, "r", position(0.0, 0.0, null, "RPT1"), position(1.0, 1.0, null, "x")));
        assertEquals(1, removed.getPositionCount());
    }

    @Test
    public void windows1252UmlautsAreRead() throws IOException {
        GpxRoute route = read(new OziExplorerWaypointFormat(),
                wpt("WGS 84", "1,Grüße,47.5,8.25,,0,1,3,0,65535,,0,0,0,10,6,0,17")).get(0);
        assertEquals("Grüße", route.getPosition(0).getDescription());
    }

    private static List<GpxRoute> readBytes(OziExplorerFormat format, byte[] content) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(content), context);
        return context.getRoutes();
    }

    private static byte[] utf8WithBom(String content) {
        byte[] text = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[text.length + 3];
        result[0] = (byte) 0xEF; result[1] = (byte) 0xBB; result[2] = (byte) 0xBF;
        System.arraycopy(text, 0, result, 3, text.length);
        return result;
    }

    @Test
    public void utf8WaypointFileWithByteOrderMarkIsRead() throws IOException {
        List<GpxRoute> routes = readBytes(new OziExplorerWaypointFormat(),
                utf8WithBom(wpt("WGS 84", "1,Grüße,47.5,8.25,,0,1,3,0,65535,Ελλάδα,0,0,0,10,6,0,17")));
        assertEquals(1, routes.size());
        assertEquals("Grüße; Ελλάδα", routes.get(0).getPosition(0).getDescription());
    }

    @Test
    public void utf8RouteFileWithoutByteOrderMarkIsRead() throws IOException {
        List<GpxRoute> routes = readBytes(new OziExplorerRouteFormat(), rte("WGS 84", "R,0,Straße,Weg über Köln,",
                "W,0,,1,Zürich,47.500000,8.250000,,0,1,3,0,65535,Bahnhof,0,0",
                "W,0,,2,Genève,47.600000,8.350000,,0,1,3,0,65535,Gare,0,0").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(1, routes.size());
        assertEquals("Straße", routes.get(0).getName());
        assertEquals("Zürich; Bahnhof", routes.get(0).getPosition(0).getDescription());
        assertEquals("Genève; Gare", routes.get(0).getPosition(1).getDescription());
    }

    @Test
    public void utf8TrackFileWithByteOrderMarkIsRead() throws IOException {
        List<GpxRoute> routes = readBytes(new OziExplorerTrackFormat(), utf8WithBom(
                plt("WGS 84", "Feet", "47.500000,8.250000,0,100.0,40000.5000000,,").replace("Test Track", "Größe")));
        assertEquals(1, routes.size());
        assertEquals("Größe", routes.get(0).getName());
        assertEquals(1, routes.get(0).getPositionCount());
    }

    @Test
    public void windows1252FieldNextToUtf8FieldIsReadPerField() throws IOException {
        byte[] utf8 = rte("WGS 84", "R,0,Straße,,",
                "W,0,,1,A,47.500000,8.250000,,0,1,3,0,65535,,0,0",
                "W,0,,2,B,47.600000,8.350000,,0,1,3,0,65535,,0,0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String content = new String(utf8, java.nio.charset.StandardCharsets.ISO_8859_1).replace("A,47.5", "M\u00FCnster,47.5");
        List<GpxRoute> routes = readBytes(new OziExplorerRouteFormat(), content.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        assertEquals("Straße", routes.get(0).getName());
        assertEquals("Münster", routes.get(0).getPosition(0).getDescription());
    }

    // writing

    @Test
    public void writesWaypointFile() throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, 100.0, "Alpha; first, point"), position(-8.5, -47.25, null, "Beta"))));
        String expected = lines("OziExplorer Waypoint File Version 1.1", "WGS 84", "Reserved 2", "Reserved 3",
                "1,Alpha,47.500000,8.250000,,0,1,3,0,65535,first\u00D1 point,0,0,0,328,6,0,17",
                "2,Beta,-47.250000,-8.500000,,0,1,3,0,65535,Beta,0,0,0,-777,6,0,17");
        assertEquals(expected, write(new OziExplorerWaypointFormat(), route));
    }

    @Test
    public void writesTrackFileWithoutName() throws IOException {
        // GPSBabel writes "ComplimentsOfGPSBabel" for a track without name and whole feet; GPSBabel master one decimal
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, 30.48, null), position(8.5, 47.75, null, null), position(8.75, 48.0, 10.0, null))));
        String expected = lines("OziExplorer Track Point File Version 2.1", "WGS 84", "Altitude is in Feet", "Reserved 3",
                "0,2,255,,0,0,2,8421376", "0",
                "47.500000,8.250000,1,100.0,,,",
                "47.750000,8.500000,0,-777.0,,,",
                "48.000000,8.750000,0,32.8,,,");
        String written = write(new OziExplorerTrackFormat(), route);
        assertEquals(expected, written);
        List<GpxRoute> routes = read(new OziExplorerTrackFormat(), written);
        assertNull(routes.get(0).getName());
        assertNull(routes.get(0).getPosition(1).getElevation());
        assertEquals(10.0, routes.get(0).getPosition(2).getElevation(), 0.02);
    }

    @Test
    public void writesTimeAsDaysSince18991230() throws IOException {
        GpxPosition timed = new GpxPosition(8.25, 47.5, null, null, slash.common.type.CompactCalendar.fromMillis(86400000L + 43200000L), "T");
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, "Named", null, new ArrayList<>(asList(timed)));
        String written = write(new OziExplorerTrackFormat(), route);
        assertTrue(written.contains("0,2,255,Named,0,0,2,8421376\r\n0\r\n"), written);
        assertTrue(written.endsWith("47.500000,8.250000,1,-777.0,25570.5000000,,\r\n"), written);
    }

    @Test
    public void descriptionIsCutTo40CharactersWithCommasAsCharacter209() throws IOException {
        String longDescription = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z";
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, null, longDescription))));
        String written = write(new OziExplorerWaypointFormat(), route);
        String name = longDescription.replace(',', '\u00D1');
        // only the description column is limited to 40 characters, the name is not
        assertEquals(lines("OziExplorer Waypoint File Version 1.1", "WGS 84", "Reserved 2", "Reserved 3",
                "1," + name + ",47.500000,8.250000,,0,1,3,0,65535," + name.substring(0, 40) + ",0,0,0,-777,6,0,17"), written);
        // writing leaves the route of the caller alone
        assertEquals(longDescription, route.getPosition(0).getDescription());
    }

    @Test
    public void writesWindows1252AndCrlf() throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, null, "Grüße"))));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new OziExplorerWaypointFormat().write(route, out, 0, 1);
        byte[] bytes = out.toByteArray();
        String text = new String(bytes, CP1252);
        assertTrue(text.contains("1,Grüße,47.500000,8.250000"));
        boolean umlaut = false;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == (byte) 0xFC)
                umlaut = true;
            if (bytes[i] == '\n')
                assertEquals('\r', bytes[i - 1]);
        }
        assertTrue(umlaut);
    }

    @Test
    public void allTextIsWrittenAsWindows1252() throws IOException {
        // GPSBabel 1.5.4 writes route point text as UTF-8 bytes and names as ISO-8859-1 - maintainer 2026-11-19:
        // prefer correct behaviour over GPSBabel 1.5.4 bugs
        GpxRoute route = new GpxRoute(new Gpx10Format(), Route, "Gr\u00F6\u00DFe", null, new ArrayList<>(asList(
                position(8.25, 47.5, null, "\u20AC 1"))));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new OziExplorerRouteFormat().write(route, out, 0, 1);
        String text = new String(out.toByteArray(), CP1252);
        assertTrue(text.contains("R,1,Gr\u00F6\u00DFe,,"), text);
        assertTrue(text.contains("W,1,,1,\u20AC 1,"), text);

        GpxRoute waypoints = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, null, "\u20AC \u00C4"))));
        out.reset();
        new OziExplorerWaypointFormat().write(waypoints, out, 0, 1);
        assertTrue(new String(out.toByteArray(), CP1252).contains("1,\u20AC \u00C4,47.500000"));
    }

    @Test
    public void routeTextIsReadAsWindows1252() throws IOException {
        byte[] bytes = rte("WGS 84", "R,0,One,,", "W,0,,1,A\u00FC \u20AC,47.1,8.1,,0,1,3,0,65535,,0,0").getBytes(CP1252);
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        new OziExplorerRouteFormat().read(new ByteArrayInputStream(bytes), context);
        // GPSBabel 1.5.4 delivers a replacement character for every Windows-1252 byte that is not valid UTF-8
        assertEquals("A\u00FC \u20AC", context.getRoutes().get(0).getPosition(0).getDescription());
    }

    @Test
    public void timeJustBelowAFullSecondIsRoundedToMilliseconds() throws IOException {
        // (39488.5834375 - 25569) days are 0.9999997 s below a full second in double arithmetic; GPSBabel 1.5.4
        // delivers the truncated second plus odd milliseconds there
        GpxPosition position = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,39488.5834375,,")).get(0).getPosition(0);
        assertEquals(1202652009000L, position.getTime().getTimeInMillis());
    }

    @Test
    public void packPutsAllRoutesIntoOneRteFile() throws IOException {
        GpxRoute first = new GpxRoute(new Gpx10Format(), Route, "One", asList("About one"), new ArrayList<>(asList(
                position(8.25, 47.5, 100.0, "A"), position(8.5, 47.75, null, "B; bee"))));
        GpxRoute second = new GpxRoute(new Gpx10Format(), Track, "Two", null, new ArrayList<>(asList(
                position(9.0, 48.0, null, "C"))));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new OziExplorerRouteFormat().write(asList(first, second), out);
        String expected = lines("OziExplorer Route File Version 1.0", "WGS 84", "Reserved 1", "Reserved 2",
                "R,1,One,About one,",
                "W,1,,1,A,47.500000,8.250000,,0,1,3,0,65535,,0,0",
                "W,1,,2,B,47.750000,8.500000,,0,1,3,0,65535,bee,0,0",
                "R,2,Two,,",
                "W,2,,1,C,48.000000,9.000000,,0,1,3,0,65535,,0,0");
        assertEquals(expected, new String(out.toByteArray(), CP1252));
    }

    @Test
    public void packPutsAllTracksIntoOnePltFile() throws IOException {
        GpxRoute first = new GpxRoute(new Gpx10Format(), Track, "One", null, new ArrayList<>(asList(
                position(8.25, 47.5, null, null), position(8.5, 47.75, null, null))));
        GpxRoute second = new GpxRoute(new Gpx10Format(), Track, "Two", null, new ArrayList<>(asList(
                position(9.0, 48.0, null, null))));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new OziExplorerTrackFormat().write(asList(first, second), out);
        String expected = lines("OziExplorer Track Point File Version 2.1", "WGS 84", "Altitude is in Feet", "Reserved 3",
                "0,2,255,One,0,0,2,8421376", "0",
                "47.500000,8.250000,1,-777.0,,,",
                "47.750000,8.500000,0,-777.0,,,",
                "48.000000,9.000000,1,-777.0,,,");
        assertEquals(expected, new String(out.toByteArray(), CP1252));
    }

    @Test
    public void writtenFilesCanBeReadAgain() throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Route, "Roundtrip", null, new ArrayList<>(asList(
                position(8.25, 47.5, null, "A; one"), position(8.5, 47.75, null, "B"))));
        OziExplorerRouteFormat format = new OziExplorerRouteFormat();
        List<GpxRoute> routes = read(format, write(format, route));
        assertEquals(1, routes.size());
        assertEquals("Roundtrip", routes.get(0).getName());
        assertEquals(2, routes.get(0).getPositionCount());
        // the description of a route point is written to field 14
        assertEquals("A; one", routes.get(0).getPosition(0).getDescription());
        assertNotNull(routes.get(0).getPosition(1));
    }

    @Test
    public void formatContract() {
        assertEquals(".rte", new OziExplorerRouteFormat().getExtension());
        assertEquals(".plt", new OziExplorerTrackFormat().getExtension());
        assertEquals(".wpt", new OziExplorerWaypointFormat().getExtension());
        assertEquals("OziExplorer Route (*.rte)", new OziExplorerRouteFormat().getName());
        assertEquals("OziExplorer Track (*.plt)", new OziExplorerTrackFormat().getName());
        assertEquals("OziExplorer Waypoint (*.wpt)", new OziExplorerWaypointFormat().getName());
        assertTrue(new OziExplorerRouteFormat().isSupportsMultipleRoutes());
        assertFalse(new OziExplorerTrackFormat().isSupportsMultipleRoutes());
        assertFalse(new OziExplorerWaypointFormat().isSupportsMultipleRoutes());
    }

    // writing: sanitising (audit 00083 batch 1)

    private static final String HOSTILE = "Na,me\r\nx\ty\0z\uFFFE";

    private static GpxRoute hostileRoute(RouteCharacteristics characteristics, String name, List<String> description) {
        return new GpxRoute(new Gpx10Format(), characteristics, name, description, new ArrayList<>(asList(
                position(8.25, 47.5, null, HOSTILE + "; de,sc\r\nq"), position(8.5, 47.75, null, "B"), position(9.0, 48.0, null, "C"))));
    }

    private static void assertOneRecordPerLine(String written, int lines) {
        assertEquals(lines, written.split("\r\n", -1).length - 1, written);
        assertFalse(written.contains("\n\n"));
        assertFalse(written.indexOf('\0') != -1 || written.indexOf('\uFFFE') != -1 || written.indexOf('\t') != -1);
    }

    @Test
    public void waypointTextWithCommaLineBreaksTabNulAndNoncharacterIsCleaned() throws IOException {
        OziExplorerWaypointFormat format = new OziExplorerWaypointFormat();
        String written = write(format, hostileRoute(Waypoints, null, null));
        assertOneRecordPerLine(written, 4 + 3);
        List<GpxRoute> routes = read(format, written);
        assertEquals(1, routes.size());
        assertEquals(3, routes.get(0).getPositionCount());
        assertEquals("Na,me x y z; de,sc q", routes.get(0).getPosition(0).getDescription());
    }

    @Test
    public void routeNameDescriptionAndPointTextAreCleanedAndTheDescriptionOfAPointIsWritten() throws IOException {
        OziExplorerRouteFormat format = new OziExplorerRouteFormat();
        String written = write(format, hostileRoute(Route, "Rte, with\r\ncomma", asList("line one", "line two,three")));
        assertOneRecordPerLine(written, 4 + 1 + 3);
        assertTrue(written.contains("R,1,Rte\u00D1 with comma,line one line two\u00D1three,\r\n"), written);
        // field 14 carries the description of the point
        assertTrue(written.contains(",65535,de\u00D1sc q,0,0\r\n"), written);
        List<GpxRoute> routes = read(format, written);
        assertEquals(1, routes.size());
        assertEquals("Rte, with comma", routes.get(0).getName());
        assertEquals(3, routes.get(0).getPositionCount());
        assertEquals("Na,me x y z; de,sc q", routes.get(0).getPosition(0).getDescription());
        assertEquals("B", routes.get(0).getPosition(1).getDescription());
    }

    @Test
    public void trackNameIsCleaned() throws IOException {
        OziExplorerTrackFormat format = new OziExplorerTrackFormat();
        String written = write(format, hostileRoute(Track, "My, Track\r\n" + "x\0", null));
        assertOneRecordPerLine(written, 6 + 3);
        assertTrue(written.contains("0,2,255,My\u00D1 Track x,0,0,2,8421376\r\n"), written);
        List<GpxRoute> routes = read(format, written);
        assertEquals(1, routes.size());
        assertEquals("My, Track x", routes.get(0).getName());
        assertEquals(3, routes.get(0).getPositionCount());
    }

    @Test
    public void nameAndDescriptionAreCutSeparately() throws IOException {
        String name = "N".repeat(60);
        String description = "D".repeat(50);
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, null, name + "; short"), position(8.5, 47.75, null, "ab; " + description))));
        String written = write(new OziExplorerWaypointFormat(), route);
        assertTrue(written.contains("1," + name + ",47.500000,8.250000,,0,1,3,0,65535,short,"), written);
        assertTrue(written.contains("2,ab,47.750000,8.500000,,0,1,3,0,65535," + "D".repeat(40) + ",0,0,0"), written);
    }

    @Test
    public void writingLeavesTheRouteOfTheCallerAlone() throws IOException {
        String text = "x, y; " + "z".repeat(60) + ", end";
        for (OziExplorerFormat format : asList(new OziExplorerWaypointFormat(), new OziExplorerRouteFormat(), new OziExplorerTrackFormat())) {
            GpxRoute route = new GpxRoute(new Gpx10Format(), Track, "Name, with comma", asList("a, b"), new ArrayList<>(asList(
                    position(8.25, 47.5, null, text), position(8.5, 47.75, null, text), position(9.0, 48.0, null, text))));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            format.write(route, out, 1, 2);
            assertEquals(Track, route.getCharacteristics());
            assertEquals("Name, with comma", route.getName());
            assertEquals(asList("a, b"), route.getDescription());
            for (int i = 0; i < 3; i++)
                assertEquals(text, route.getPosition(i).getDescription());

            ByteArrayOutputStream pack = new ByteArrayOutputStream();
            format.write(asList(route), pack);
            assertEquals(Track, route.getCharacteristics());
            for (int i = 0; i < 3; i++)
                assertEquals(text, route.getPosition(i).getDescription());
        }
    }
}

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
    public void waypointFormatReadsOtherOziExplorerFileTypesLikeGpsBabel() throws IOException {
        String event = lines("OziExplorer Event File Version 1.0", "WGS 84", "Reserved 2", "Reserved 3",
                "1,Event,47.500000,8.250000,,0,1,3,0,65535,Event desc,0,0,0,328,6,0,17");
        assertEquals(1, read(new OziExplorerWaypointFormat(), event).size());
        assertTrue(read(new OziExplorerRouteFormat(), event).isEmpty());
        assertTrue(read(new OziExplorerTrackFormat(), event).isEmpty());
    }

    @Test
    public void unknownDatumYieldsNoRoute() throws IOException {
        assertTrue(read(new OziExplorerTrackFormat(), plt("Nowhere Datum 1999", "Feet", "47.5,8.25,0,100,,,")).isEmpty());
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
        assertTrue(read(new OziExplorerRouteFormat(), RT2.replace("WGS 84", "Nowhere Datum 1999")).isEmpty());
        assertTrue(read(new OziExplorerRouteFormat(), "H1,OziExplorer CE Route2 File Version 1.0\nH2,WGS 84\nH3,Empty,,0\n").isEmpty());
        assertTrue(read(new OziExplorerRouteFormat(), "H1,OziExplorer CE Route2 File Version 1.0\nWGS 84\n").isEmpty());
    }

    @Test
    public void unknownAltitudeUnitYieldsNoRoute() throws IOException {
        assertTrue(read(new OziExplorerTrackFormat(), plt("WGS 84", "Furlongs", "47.5,8.25,0,100,,,")).isEmpty());
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
        GpxPosition tooEarly = read(new OziExplorerTrackFormat(), plt("WGS 84", "Feet", "47.5,8.25,0,10,25569.0000000,,")).get(0).getPosition(0);
        assertNull(tooEarly.getTime());
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

    // writing

    @Test
    public void writesWaypointFile() throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, 100.0, "Alpha; first, point"), position(-8.5, -47.25, null, "Beta"))));
        String expected = lines("OziExplorer Waypoint File Version 1.1", "WGS 84", "Reserved 2", "Reserved 3",
                "1,Alpha,47.500000,8.250000,,0,1,3,0,65535,first  point,0,0,0,328,6,0,17",
                "2,Beta,-47.250000,-8.500000,,0,1,3,0,65535,Beta,0,0,0,-777,6,0,17");
        assertEquals(expected, write(new OziExplorerWaypointFormat(), route));
    }

    @Test
    public void writesTrackFileWithDefaultName() throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, 30.48, null), position(8.5, 47.75, null, null))));
        String expected = lines("OziExplorer Track Point File Version 2.1", "WGS 84", "Altitude is in Feet", "Reserved 3",
                "0,2,255,ComplimentsOfGPSBabel,0,0,2,8421376", "0",
                "47.500000,8.250000,1,100,,,",
                "47.750000,8.500000,0,-777,,,");
        assertEquals(expected, write(new OziExplorerTrackFormat(), route));
    }

    @Test
    public void writesTimeAsDaysSince18991230() throws IOException {
        GpxPosition timed = new GpxPosition(8.25, 47.5, null, null, slash.common.type.CompactCalendar.fromMillis(86400000L + 43200000L), "T");
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, "Named", null, new ArrayList<>(asList(timed)));
        String written = write(new OziExplorerTrackFormat(), route);
        assertTrue(written.contains("0,2,255,Named,0,0,2,8421376\r\n0\r\n"), written);
        assertTrue(written.endsWith("47.500000,8.250000,1,-777,25570.5000000,,\r\n"), written);
    }

    @Test
    public void descriptionIsCutTo40CharactersWithoutCommas() throws IOException {
        String longDescription = "a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z";
        GpxRoute route = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                position(8.25, 47.5, null, longDescription))));
        String written = write(new OziExplorerWaypointFormat(), route);
        String name = "a b c d e f g h i j k l m n o p q r s t";
        assertEquals(lines("OziExplorer Waypoint File Version 1.1", "WGS 84", "Reserved 2", "Reserved 3",
                "1," + name + ",47.500000,8.250000,,0,1,3,0,65535," + name + ",0,0,0,-777,6,0,17"), written);
        // like the old GPSBabel-backed class, the route's descriptions are reduced as well
        assertEquals(name.replace(" ", " "), route.getPosition(0).getDescription().trim());
        assertEquals(40, route.getPosition(0).getDescription().length());
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
                "W,1,,2,B,47.750000,8.500000,,0,1,3,0,65535,,0,0",
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
                "47.500000,8.250000,1,-777,,,",
                "47.750000,8.500000,0,-777,,,",
                "48.000000,9.000000,1,-777,,,");
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
        // GPSBabel 1.5.4 writes no description for route points
        assertEquals("A", routes.get(0).getPosition(0).getDescription());
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
}

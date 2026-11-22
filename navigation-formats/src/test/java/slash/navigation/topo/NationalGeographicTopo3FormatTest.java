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

package slash.navigation.topo;

import org.junit.Test;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;
import slash.navigation.tef.TourExchangeFormat;
import slash.navigation.topo.TopoFileBuilder.Bytes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.topo.NationalGeographicTopo3Format.MAP_NOTES_BLOCK;
import static slash.navigation.topo.NationalGeographicTopo3Format.ROUTES_BLOCK;
import static slash.navigation.topo.NationalGeographicTopo3Format.SYMBOLS_BLOCK;
import static slash.navigation.topo.NationalGeographicTopo3Format.TEXT_LABELS_BLOCK;
import static slash.navigation.topo.NationalGeographicTopo3Format.TRACKS_BLOCK;
import static slash.navigation.topo.NationalGeographicTopo3Format.TRACK_STYLES_BLOCK;
import static slash.navigation.topo.NationalGeographicTopo3Format.WAYPOINTS_BLOCK;
import static slash.navigation.topo.TopoFileBuilder.coordinate;
import static slash.navigation.topo.TopoFileBuilder.header;
import static slash.navigation.topo.TopoFileBuilder.mapNote;
import static slash.navigation.topo.TopoFileBuilder.mapNotes;
import static slash.navigation.topo.TopoFileBuilder.route;
import static slash.navigation.topo.TopoFileBuilder.routes;
import static slash.navigation.topo.TopoFileBuilder.style;
import static slash.navigation.topo.TopoFileBuilder.styles;
import static slash.navigation.topo.TopoFileBuilder.symbols;
import static slash.navigation.topo.TopoFileBuilder.textLabel;
import static slash.navigation.topo.TopoFileBuilder.textLabels;
import static slash.navigation.topo.TopoFileBuilder.track;
import static slash.navigation.topo.TopoFileBuilder.tracks;
import static slash.navigation.topo.TopoFileBuilder.waypoint;
import static slash.navigation.topo.TopoFileBuilder.waypoints;

public class NationalGeographicTopo3FormatTest {
    private static final double DELTA = 0.0000000005;
    private static final int UNKNOWN_ALTITUDE = -3276800;
    private final NationalGeographicTopo3Format format = new NationalGeographicTopo3Format();

    private List<GpxRoute> read(byte[] data) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(data), context);
        return context.getRoutes();
    }

    private void assertNoRoutes(byte[] data) throws IOException {
        assertTrue(read(data).isEmpty());
    }

    private GpxRoute readSingle(byte[] data) throws IOException {
        List<GpxRoute> routes = read(data);
        assertEquals(1, routes.size());
        return routes.get(0);
    }

    private static void assertPosition(GpxPosition position, double longitude, double latitude, Double elevation, String description) {
        assertEquals(longitude, position.getLongitude(), DELTA);
        assertEquals(latitude, position.getLatitude(), DELTA);
        assertEquals(elevation, position.getElevation());
        assertEquals(description, position.getDescription());
        assertNull(position.getTime());
        assertNull(position.getSpeed());
        assertNull(position.getHeading());
    }

    // GPSBabel divides by 0x800000 and writes the GPX with %0.9f
    private static double topo(double degrees) {
        return Math.round((double) coordinate(degrees) / 0x800000 * 1e9) / 1e9;
    }

    private static byte[] waypointFile(Bytes... waypoints) {
        return new TopoFileBuilder().block(WAYPOINTS_BLOCK, waypoints(waypoints)).build();
    }

    // format

    @Test
    public void testFormat() {
        assertEquals("National Geographic Topo 3/4 (*.tpo)", format.getName());
        assertEquals(".tpo", format.getExtension());
        assertTrue(format.isSupportsReading());
        assertFalse(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
        assertEquals(Integer.MAX_VALUE, format.getMaximumPositionCount());
    }

    @Test
    public void testRegisteredAtOldPositionOnce() {
        List<? extends NavigationFormat<?>> formats = new NavigationFormatRegistry().getReadFormats();
        int tourExchange = -1, topo = -1, count = 0;
        for (int i = 0; i < formats.size(); i++) {
            Class<?> clazz = formats.get(i).getClass();
            if (clazz == TourExchangeFormat.class)
                tourExchange = i;
            if (clazz == NationalGeographicTopo3Format.class) {
                topo = i;
                count++;
            }
            assertFalse(clazz.getName().equals("slash.navigation.babel.NationalGeographicTopo3Format"));
        }
        assertEquals(1, count);
        assertEquals(tourExchange + 1, topo);
    }

    // header guard

    @Test
    public void testAcceptsVersion3And4Headers() throws IOException {
        Bytes waypoint = waypoint("A", 11.5, 48.25, 0, "");
        assertEquals(1, read(new TopoFileBuilder("TOPO! Ver. 3.4.3").block(WAYPOINTS_BLOCK, waypoints(waypoint)).build()).size());
        assertEquals(1, read(new TopoFileBuilder("TOPO! Ver. 4.5.0").block(WAYPOINTS_BLOCK, waypoints(waypoint)).build()).size());
    }

    @Test
    public void testRejectsVersion2AndForeignFiles() throws IOException {
        // a 2.x file starts with the track count and "CTopoRoute"
        Bytes version2 = new Bytes().u16(1).zeros(4).u16(0xffff).u16(0).u16(10).raw("CTopoRoute".getBytes()).zeros(600);
        assertNoRoutes(version2.toByteArray());
        assertNoRoutes(new TopoFileBuilder("TOPO 3.4.3").block(WAYPOINTS_BLOCK, waypoints(waypoint("A", 1, 2, 0, ""))).build());
        assertNoRoutes(new byte[0]);
        assertNoRoutes(new byte[]{9});
        assertNoRoutes("<?xml version=\"1.0\"?><gpx></gpx>".getBytes());
        byte[] random = new byte[4096];
        new Random(42).nextBytes(random);
        assertNoRoutes(random);
    }

    @Test
    public void testRejectsHeaderWithoutNulOrStartSection() throws IOException {
        byte[] noNul = "\u0010TOPO! Ver. 3.4.3".getBytes();
        assertFalse(NationalGeographicTopo3Format.isTopo3Header(noNul));
        assertNoRoutes(noNul);

        byte[] header = header("TOPO! Ver. 3.4.3", false);
        assertTrue(NationalGeographicTopo3Format.isTopo3Header(header));
        assertFalse(NationalGeographicTopo3Format.hasStartSection(header));
        assertNoRoutes(new Bytes().raw(header).i32(WAYPOINTS_BLOCK).i32(0).raw(waypoints(waypoint("A", 1, 2, 0, "")).toByteArray()).toByteArray());
    }

    @Test
    public void testStartSectionScanDoesNotBacktrack() {
        // tpo_read_until_section() restarts its match without re-examining the mismatching byte
        assertFalse(NationalGeographicTopo3Format.hasStartSection(new Bytes().u8(3).raw("TOP".getBytes()).u8(0)
                .raw("RRed Without Arrow".getBytes()).toByteArray()));
        assertTrue(NationalGeographicTopo3Format.hasStartSection(new Bytes().u8(3).raw("TOP".getBytes()).u8(0)
                .raw("R Red Without Arrow".getBytes()).toByteArray()));
    }

    // tracks

    private static final int[] MULTIPLIERS = {0, 1, 2, 3, 4, 5, 6, 7, -8, -7, -6, -5, -4, -3, -2, -1};

    private static byte[] trackFile(Bytes styles, Bytes... tracks) {
        return new TopoFileBuilder().block(TRACK_STYLES_BLOCK, styles).block(TRACKS_BLOCK, tracks(tracks)).build();
    }

    private static Bytes defaultStyles() {
        return styles(style(0x12, 0x34, 0x56, "Green Medium", 3, 0));
    }

    private static void assertTrackPoint(GpxPosition position, int lon, int lat) {
        assertPosition(position, Math.round((double) lon / 0x800000 * 1e9) / 1e9, Math.round((double) lat / 0x800000 * 1e9) / 1e9, null, null);
    }

    @Test
    public void testTrackWithScalesAndDeltas() throws IOException {
        int lon = coordinate(-122.29), lat = coordinate(37.53);
        int lon2 = coordinate(-122.28), lat2 = coordinate(37.54);
        int lon3 = coordinate(-122.27), lat3 = coordinate(37.55);
        Bytes data = new Bytes().i32(lon).i32(lat)
                .i32(0xfe).i32(0xcd)          // longitude and latitude scale
                .u8(0xf4).u8(0x04).u8(0x14)   // deltas
                .u8(0x00)                     // end of deltas, a full point follows
                .i32(lon2).i32(lat2)
                .u8(0x20).u8(0x13)            // deltas with the retained scales
                .u8(0x88)                     // a full point follows
                .i32(lon3).i32(lat3);
        GpxRoute route = readSingle(trackFile(defaultStyles(), track(1, "Freehand", data)));
        assertEquals(Track, route.getCharacteristics());
        assertEquals("Freehand", route.getName());
        assertEquals(asList("Style=Green Medium", "Width=3", "Dashed=0", "Color=#123456"), route.getDescription());

        List<GpxPosition> positions = route.getPositions();
        assertEquals(8, positions.size());
        assertTrackPoint(positions.get(0), lon, lat);
        int[] deltas = {0xf4, 0x04, 0x14};
        for (int i = 0; i < deltas.length; i++) {
            lon += 0xfe * MULTIPLIERS[deltas[i] >> 4];
            lat += 0xcd * MULTIPLIERS[deltas[i] & 0xf];
            assertTrackPoint(positions.get(1 + i), lon, lat);
        }
        assertTrackPoint(positions.get(4), lon2, lat2);
        assertTrackPoint(positions.get(5), lon2 + 0xfe * 2, lat2);
        assertTrackPoint(positions.get(6), lon2 + 0xfe * 3, lat2 + 0xcd * 3);
        assertTrackPoint(positions.get(7), lon3, lat3);
    }

    @Test
    public void testTrackWithScaleAfterEndOfDeltas() throws IOException {
        // the 2020 algorithm (D13.1): after 0x00 a scale may follow instead of a full point; the 2012
        // algorithm of GPSBabel 1.5.4 read the scale and the 0x88 tag as a full point
        int lon = coordinate(10.5), lat = coordinate(47.25);
        int lon2 = coordinate(10.6), lat2 = coordinate(47.35);
        Bytes data = new Bytes().i32(lon).i32(lat)
                .i32(0x10).i32(0x20)
                .u8(0x11)
                .u8(0x00)
                .i32(0x66)                    // only a longitude scale
                .u8(0x88).i32(lon2).i32(lat2)
                .u8(0x21);
        List<GpxPosition> positions = readSingle(trackFile(defaultStyles(), track(1, "T", data))).getPositions();
        assertEquals(4, positions.size());
        assertTrackPoint(positions.get(0), lon, lat);
        assertTrackPoint(positions.get(1), lon + 0x10, lat + 0x20);
        assertTrackPoint(positions.get(2), lon2, lat2);
        assertTrackPoint(positions.get(3), lon2 + 0x66 * 2, lat2 + 0x20);
    }

    @Test
    public void testTrackFullPointAfterFullPoint() throws IOException {
        Bytes data = new Bytes().point(10.5, 47.25).u8(0x88).point(10.75, 47.5).u8(0x88).point(11.0, 47.75);
        List<GpxPosition> positions = readSingle(trackFile(defaultStyles(), track(1, "T", data))).getPositions();
        assertEquals(3, positions.size());
        assertPosition(positions.get(0), 10.5, 47.25, null, null);
        assertPosition(positions.get(1), 10.75, 47.5, null, null);
        assertPosition(positions.get(2), 11.0, 47.75, null, null);
    }

    @Test
    public void testTrackNamesAndStyles() throws IOException {
        Bytes styles = styles(style(0xff, 0, 0, "Red, Thick=Wide,x", 4, 1), style(0, 0, 0, "", 1, 0));
        Bytes point = new Bytes().point(10.5, 47.25);
        List<GpxRoute> routes = read(trackFile(styles, track(1, "First", point), track(2, "", point),
                track(1, "Empty", new Bytes())));
        assertEquals(2, routes.size());
        assertEquals("First", routes.get(0).getName());
        // GPSBabel master replaces every comma and equals sign of the style name, 1.5.4 only within the first 3 characters
        assertEquals(asList("Style=Red_ Thick_Wide_x", "Width=4", "Dashed=1", "Color=#ff0000"), routes.get(0).getDescription());
        assertEquals("TRK 2", routes.get(1).getName());
        assertEquals(asList("Style=STYLE 1", "Width=1", "Dashed=0", "Color=#000000"), routes.get(1).getDescription());
    }

    @Test
    public void testOverlongStyleNameSkipsTracksOnly() throws IOException {
        Bytes styles = new Bytes().varint(1).u8(0).u8(0).u8(1).u8(2).u8(3).u8(0).u8(255).zeros(259);
        byte[] data = new TopoFileBuilder()
                .block(TRACK_STYLES_BLOCK, styles)
                .block(TRACKS_BLOCK, tracks(track(1, "T", new Bytes().point(10.5, 47.25))))
                .block(WAYPOINTS_BLOCK, waypoints(waypoint("W", 11.5, 48.25, 0, "")))
                .build();
        GpxRoute route = readSingle(data);
        assertEquals(Waypoints, route.getCharacteristics());
    }

    @Test
    public void testTracksWithoutStylesAreSkipped() throws IOException {
        byte[] data = new TopoFileBuilder()
                .block(TRACKS_BLOCK, tracks(track(1, "T", new Bytes().point(10.5, 47.25))))
                .block(WAYPOINTS_BLOCK, waypoints(waypoint("W", 11.5, 48.25, 0, "")))
                .build();
        assertEquals(Waypoints, readSingle(data).getCharacteristics());
    }

    @Test
    public void testInvalidTracksFailTheFile() throws IOException {
        Bytes point = new Bytes().point(10.5, 47.25);
        // style numbers start at 1
        assertNoRoutes(trackFile(defaultStyles(), track(0, "T", point)));
        assertNoRoutes(trackFile(defaultStyles(), track(2, "T", point)));
        // a delta before any scale
        assertNoRoutes(trackFile(defaultStyles(), track(1, "T", new Bytes().point(10.5, 47.25).u8(0x11))));
        // a full point cut short by the end of the track
        assertNoRoutes(trackFile(defaultStyles(), track(1, "T", new Bytes().point(10.5, 47.25).u8(0x88).u16(0x1234))));
    }

    // waypoints

    @Test
    public void testWaypoints() throws IOException {
        GpxRoute route = readSingle(waypointFile(
                waypoint("Summit", 11.5, 48.25, 52050, ""),
                waypoint("Hut", 11.625, 48.375, -150, "Closed in winter"),
                waypoint("Lake", 11.75, 48.5, UNKNOWN_ALTITUDE, ""),
                waypoint("", 11.875, 48.625, 0, ""),
                waypoint("", 12.0, 48.75, 0, "Comment only")));
        assertEquals(Waypoints, route.getCharacteristics());
        assertNull(route.getName());
        List<GpxPosition> positions = route.getPositions();
        assertEquals(5, positions.size());
        assertPosition(positions.get(0), 11.5, 48.25, 520.5, "Summit");
        assertPosition(positions.get(1), 11.625, 48.375, -1.5, "Hut; Closed in winter");
        assertPosition(positions.get(2), 11.75, 48.5, null, "Lake");
        assertPosition(positions.get(3), 11.875, 48.625, 0.0, "WPT 4");
        assertPosition(positions.get(4), 12.0, 48.75, 0.0, "WPT 5; Comment only");
    }

    @Test
    public void testCoordinatesAsGpsBabelWritesThem() throws IOException {
        GpxPosition position = readSingle(waypointFile(waypoint("A", -111.5119444, 36.8772222, 128900, ""))).getPositions().get(0);
        assertEquals(topo(-111.5119444), position.getLongitude(), 0.0);
        assertEquals(topo(36.8772222), position.getLatitude(), 0.0);
        assertEquals(1289.0, position.getElevation(), 0.0);
    }

    @Test
    public void testStringsAsTheGpxDeliveredThem() throws IOException {
        List<GpxPosition> positions = readSingle(waypointFile(
                waypoint(" Lead", 11.5, 48.1, 0, ""),
                waypoint("Trail ", 11.6, 48.2, 0, ""),
                waypoint("a\tb", 11.7, 48.3, 0, "c\r\nd"),
                waypoint("e\rf", 11.8, 48.4, 0, "g\u000bh\u0000i"),
                waypoint("M\u00fcller \u20ac", 11.9, 48.5, 0, "\u00e9t\u00e9"))).getPositions();
        // the GPX kept surrounding blanks and tabs, replaced other control characters with a space and
        // normalized line ends; RouteConverter turns line ends of descriptions into blanks
        assertEquals(" Lead", positions.get(0).getDescription());
        assertEquals("Trail ", positions.get(1).getDescription());
        assertEquals("a\tb; c d", positions.get(2).getDescription());
        assertEquals("e f; g h", positions.get(3).getDescription());
        // Windows-1252, where GPSBabel 1.5.4 decoded UTF-8 and delivered replacement characters
        assertEquals("M\u00fcller \u20ac; \u00e9t\u00e9", positions.get(4).getDescription());
    }

    @Test
    public void testStringsEndAtTheFirstNul() throws IOException {
        // GPSBabel 1.5.4 held the strings as C strings; an empty string gives no GPX element
        byte[] data = new TopoFileBuilder()
                .block(TRACK_STYLES_BLOCK, styles(style(0, 0, 0, "So\u0000lid", 1, 0)))
                .block(TRACKS_BLOCK, tracks(track(1, "Tr\u0000ack", new Bytes().point(10.5, 47.25)),
                        track(1, "\u0000", new Bytes().point(10.5, 47.25))))
                .block(WAYPOINTS_BLOCK, waypoints(waypoint("Ab\u0000cd", 11.5, 48.25, 0, "x\u0000yz"),
                        waypoint("\u0000name", 11.75, 48.5, 0, ""),
                        waypoint("Peak\u0000", 12.0, 48.75, 0, "\u0000comment")))
                .block(ROUTES_BLOCK, routes(route("Ro\u0000ute", 1), route("\u0000", 2)))
                .block(TEXT_LABELS_BLOCK, textLabels(textLabel(15.0, 51.0, "he\u0000llo")))
                .build();
        List<GpxRoute> routes = read(data);
        assertEquals(5, routes.size());
        List<GpxPosition> waypoints = routes.get(0).getPositions();
        assertEquals("Ab; x", waypoints.get(0).getDescription());
        assertNull(waypoints.get(1).getDescription());
        assertEquals("Peak", waypoints.get(2).getDescription());
        assertEquals("TXT 1; he", waypoints.get(3).getDescription());
        assertEquals("Ro", routes.get(1).getName());
        assertEquals("Ab; x", routes.get(1).getPositions().get(0).getDescription());
        assertNull(routes.get(2).getName());
        // route_add_wpt() names a route point without name after its number across all routes
        assertEquals("RPT002", routes.get(2).getPositions().get(0).getDescription());
        assertEquals("Tr", routes.get(3).getName());
        assertEquals(asList("Style=So", "Width=1", "Dashed=0", "Color=#000000"), routes.get(3).getDescription());
        assertNull(routes.get(4).getName());
    }

    @Test
    public void testAsGpxText() {
        assertEquals("a\tb\nc\nd\ne", NationalGeographicTopo3Format.asGpxText("a\tb\r\nc\rd\ne"));
        assertEquals("  x ", NationalGeographicTopo3Format.asGpxText("\u0000\u001fx\u000c"));
    }

    @Test
    public void testCoordinatesOutOfRangeAreNormalized() throws IOException {
        // Waypoint::NormalizePosition() of GPSBabel master; 1.5.4 wrapped waypoints once and left route points alone
        byte[] data = new TopoFileBuilder()
                .block(WAYPOINTS_BLOCK, waypoints(waypoint("W", 200.5, 100.25, 0, ""), waypoint("X", -180.0, -95.0, 0, ""),
                        waypoint("Y", 180.0, 10.0, 0, "")))
                .block(ROUTES_BLOCK, routes(route("R", 1)))
                .build();
        List<GpxRoute> routes = read(data);
        assertEquals(2, routes.size());
        List<GpxPosition> positions = routes.get(0).getPositions();
        assertPosition(positions.get(0), 20.5, 79.75, 0.0, "W");
        assertPosition(positions.get(1), 0.0, -85.0, 0.0, "X");
        assertPosition(positions.get(2), -180.0, 10.0, 0.0, "Y");
        assertPosition(routes.get(1).getPositions().get(0), 20.5, 79.75, 0.0, "W");
    }

    // routes

    @Test
    public void testRoutes() throws IOException {
        byte[] data = new TopoFileBuilder()
                .block(WAYPOINTS_BLOCK, waypoints(waypoint("Start", 11.5, 48.25, 50000, ""),
                        waypoint("Via", 11.625, 48.375, UNKNOWN_ALTITUDE, "Bridge"),
                        waypoint("", 11.75, 48.5, 60000, "")))
                .block(ROUTES_BLOCK, routes(route("Tour", 3, 1, 2, 1), route("", 2), route("Empty")))
                .build();
        List<GpxRoute> routes = read(data);
        assertEquals(4, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());

        GpxRoute tour = routes.get(1);
        assertEquals(Route, tour.getCharacteristics());
        assertEquals("Tour", tour.getName());
        assertNull(tour.getDescription());
        assertEquals(4, tour.getPositionCount());
        assertPosition(tour.getPositions().get(0), 11.75, 48.5, 600.0, "WPT 3");
        assertPosition(tour.getPositions().get(1), 11.5, 48.25, 500.0, "Start");
        assertPosition(tour.getPositions().get(2), 11.625, 48.375, null, "Via; Bridge");
        assertPosition(tour.getPositions().get(3), 11.5, 48.25, 500.0, "Start");

        assertEquals("RTE 2", routes.get(2).getName());
        assertEquals(1, routes.get(2).getPositionCount());
        // GPSBabel's empty <rte> is a route without positions for the GPX reader
        assertEquals("Empty", routes.get(3).getName());
        assertEquals(0, routes.get(3).getPositionCount());
    }

    @Test
    public void testRouteWithUndefinedWaypointFailsTheFile() throws IOException {
        Bytes waypoints = waypoints(waypoint("A", 11.5, 48.25, 0, ""));
        assertNoRoutes(new TopoFileBuilder().block(WAYPOINTS_BLOCK, waypoints).block(ROUTES_BLOCK, routes(route("R", 0))).build());
        assertNoRoutes(new TopoFileBuilder().block(WAYPOINTS_BLOCK, waypoints).block(ROUTES_BLOCK, routes(route("R", 2))).build());
        assertNoRoutes(new TopoFileBuilder().block(ROUTES_BLOCK, routes(route("R", 1))).build());
    }

    // map notes, symbols and text labels

    @Test
    public void testMapNotesSymbolsAndTextLabelsAsWaypoints() throws IOException {
        byte[] data = new TopoFileBuilder()
                .block(TEXT_LABELS_BLOCK, textLabels(textLabel(15.0, 51.0, "Label text"), textLabel(15.25, 51.25, "")))
                .block(SYMBOLS_BLOCK, symbols(14.0, 50.0, 14.25, 50.25))
                .block(MAP_NOTES_BLOCK, mapNotes(mapNote(13.0, 49.0, "Note text", "c:\\notes\\a.txt", "c:\\images\\a.jpg", 3),
                        mapNote(13.25, 49.25, "", "", "", 0)))
                .block(WAYPOINTS_BLOCK, waypoints(waypoint("Waypoint", 12.0, 48.0, 0, "")))
                .build();
        List<GpxPosition> positions = readSingle(data).getPositions();
        assertEquals(7, positions.size());
        assertPosition(positions.get(0), 12.0, 48.0, 0.0, "Waypoint");
        assertPosition(positions.get(1), 13.0, 49.0, null, "NOTE 1; Note text");
        assertPosition(positions.get(2), 13.25, 49.25, null, "NOTE 2");
        assertPosition(positions.get(3), 14.0, 50.0, null, "SYM 1");
        assertPosition(positions.get(4), 14.25, 50.25, null, "SYM 2");
        assertPosition(positions.get(5), 15.0, 51.0, null, "TXT 1; Label text");
        assertPosition(positions.get(6), 15.25, 51.25, null, "TXT 2");
    }

    @Test
    public void testFileWithoutKnownBlocks() throws IOException {
        assertNoRoutes(new TopoFileBuilder().build());
    }

    // truncated and hostile input

    private static byte[] completeFile() {
        return new TopoFileBuilder()
                .block(TRACK_STYLES_BLOCK, defaultStyles())
                .block(TRACKS_BLOCK, tracks(track(1, "T", new Bytes().point(10.5, 47.25).i32(0x10).i32(0x20).u8(0x11).u8(0x22))))
                .block(WAYPOINTS_BLOCK, waypoints(waypoint("A", 11.5, 48.25, 100, "Comment"), waypoint("B", 11.75, 48.5, 200, "")))
                .block(ROUTES_BLOCK, routes(route("R", 1, 2)))
                .block(MAP_NOTES_BLOCK, mapNotes(mapNote(13.0, 49.0, "Note", "a.txt", "a.jpg", 2)))
                .block(SYMBOLS_BLOCK, symbols(14.0, 50.0))
                .block(TEXT_LABELS_BLOCK, textLabels(textLabel(15.0, 51.0, "Label")))
                .build();
    }

    @Test
    public void testCompleteFile() throws IOException {
        List<GpxRoute> routes = read(completeFile());
        assertEquals(3, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(5, routes.get(0).getPositionCount());
        assertEquals(Route, routes.get(1).getCharacteristics());
        assertEquals(Track, routes.get(2).getCharacteristics());
        assertEquals(3, routes.get(2).getPositionCount());
    }

    @Test(timeout = 30000)
    public void testTruncatedFilesReturnNoRoute() throws IOException {
        byte[] data = completeFile();
        for (int length = 0; length < data.length; length++) {
            byte[] truncated = new byte[length];
            System.arraycopy(data, 0, truncated, 0, length);
            assertNoRoutes(truncated);
        }
    }

    @Test
    public void testMissingIgnoredBytesAtTheEndOfTheFile() throws IOException {
        // GPSBabel reads the bytes after a waypoint and after a track style with gbfgetc(), which returns EOF
        // at the end of the file without failing
        byte[] waypointsLast = waypointFile(waypoint("A", 11.5, 48.25, 0, ""), waypoint("B", 11.75, 48.5, 0, ""));
        for (int missing = 0; missing <= 4; missing++)
            assertEquals(2, readSingle(copyOf(waypointsLast, waypointsLast.length - missing)).getPositionCount());
        assertNoRoutes(copyOf(waypointsLast, waypointsLast.length - 5));

        byte[] stylesLast = new TopoFileBuilder()
                .block(TRACKS_BLOCK, tracks(track(1, "T", new Bytes().point(10.5, 47.25))))
                .block(TRACK_STYLES_BLOCK, defaultStyles())
                .build();
        for (int missing = 0; missing <= 2; missing++)
            assertEquals(Track, readSingle(copyOf(stylesLast, stylesLast.length - missing)).getCharacteristics());
        assertNoRoutes(copyOf(stylesLast, stylesLast.length - 3));
    }

    @Test(timeout = 10000)
    public void testOverrunningCountsAndLengths() throws IOException {
        // counts as 32 bit values
        assertNoRoutes(new TopoFileBuilder().block(WAYPOINTS_BLOCK, new Bytes().varint(0xffffffffL)).build());
        assertNoRoutes(new TopoFileBuilder().block(MAP_NOTES_BLOCK, new Bytes().varint(0x7fffffff)).build());
        assertNoRoutes(new TopoFileBuilder().block(TRACK_STYLES_BLOCK, new Bytes().varint(0x10000)).build());
        // a name longer than the file
        assertNoRoutes(new TopoFileBuilder().block(WAYPOINTS_BLOCK, new Bytes().varint(1).varint(0).varint(0).varint(0xffff).zeros(30)).build());
        // track bytes beyond the end of the file
        assertNoRoutes(trackFile(defaultStyles(), new Bytes().varint(0).varint(1).varint(0).varint(0).varint(0x7fffffff)));
        // a route with more points than bytes
        assertNoRoutes(new TopoFileBuilder().block(WAYPOINTS_BLOCK, waypoints(waypoint("A", 1, 2, 0, "")))
                .block(ROUTES_BLOCK, new Bytes().varint(1).varint(0).varint(0).varint(0).varint(0).varint(0xfffffff)).build());
    }

    @Test(timeout = 10000)
    public void testLoopingBlockChainReturnsNoRoute() throws IOException {
        // the block at 512 points to itself, and two blocks point to each other
        assertNoRoutes(new Bytes().raw(header("TOPO! Ver. 3.4.3", true)).i32(0x7f0000).i32(512).toByteArray());
        assertNoRoutes(new Bytes().raw(header("TOPO! Ver. 3.4.3", true)).i32(0x7f0000).i32(520).i32(0x7e0000).i32(512).toByteArray());
        // offsets beyond the file and beyond 2^31
        assertNoRoutes(new Bytes().raw(header("TOPO! Ver. 3.4.3", true)).i32(0x7f0000).i32(100000).toByteArray());
        assertNoRoutes(new Bytes().raw(header("TOPO! Ver. 3.4.3", true)).i32(0x7f0000).i32(0x90000000).toByteArray());
    }

    @Test(timeout = 60000)
    public void testCorruptedFilesNeverThrow() throws IOException {
        byte[] data = completeFile();
        Random random = new Random(4711);
        for (int i = 0; i < 5000; i++) {
            byte[] corrupted = data.clone();
            for (int j = 0, count = 1 + random.nextInt(4); j < count; j++) {
                int index = 512 + random.nextInt(corrupted.length - 512);
                corrupted[index] = (byte) random.nextInt(256);
            }
            read(corrupted);
        }
        for (int i = 0; i < 1000; i++) {
            byte[] tail = new byte[random.nextInt(2048)];
            random.nextBytes(tail);
            read(new Bytes().raw(header("TOPO! Ver. 3.4.3", true)).raw(tail).toByteArray());
        }
    }

    @Test
    public void testWritingIsNotSupported() {
        try {
            format.write(null, new ByteArrayOutputStream(), 0, 0);
        } catch (UnsupportedOperationException e) {
            return;
        }
        throw new AssertionError("writing must not be supported");
    }
}

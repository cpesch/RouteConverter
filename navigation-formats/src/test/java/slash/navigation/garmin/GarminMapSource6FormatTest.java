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
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class GarminMapSource6FormatTest {
    private static final double SEMICIRCLES_PER_DEGREE = 2147483648.0 / 180.0;
    private static final long T1 = 1767261600L; // 2026-01-01T10:00:00Z
    private final GarminMapSource6Format format = new GarminMapSource6Format();

    // building GDB files

    private static class Builder {
        private final LittleEndianOutput output = new LittleEndianOutput();
        private final Charset charset;

        Builder(int version) {
            this.charset = version >= 3 ? UTF_8 : ISO_8859_1;
        }

        Builder bytes(int... values) {
            for (int value : values)
                output.writeUInt8(value);
            return this;
        }

        Builder fill(int count, int value) {
            for (int i = 0; i < count; i++)
                output.writeUInt8(value);
            return this;
        }

        Builder i16(int value) {
            output.writeInt16(value);
            return this;
        }

        Builder i32(int value) {
            output.writeInt32(value);
            return this;
        }

        Builder dbl(double value) {
            output.writeDouble(value);
            return this;
        }

        Builder str(String string) {
            if (string != null)
                output.writeBytes(string.getBytes(charset));
            output.writeUInt8(0);
            return this;
        }

        Builder semi(double degrees) {
            return i32((int) (SEMICIRCLES_PER_DEGREE * degrees));
        }

        Builder raw(byte[] bytes) {
            output.writeBytes(bytes);
            return this;
        }

        Builder record(char type, Builder payload) {
            byte[] bytes = payload.toByteArray();
            i32(bytes.length);
            bytes(type);
            return raw(bytes);
        }

        byte[] toByteArray() {
            return output.toByteArray();
        }
    }

    private static Builder header(int version, String application) {
        Builder header = new Builder(version);
        header.raw("MsRcf".getBytes(ISO_8859_1)).bytes(0);
        header.i32(2).bytes('D', 'k' + version - 1, 0);
        Builder creator = new Builder(version).i16(605).str("SQA").str("Jan 01 2010").str("12:00:00");
        header.record('A', creator);
        return header.str(application);
    }

    private static Builder header(int version) {
        return header(version, "MapSource");
    }

    private static byte[] end(Builder builder) {
        return builder.i32(2).bytes('V', 0, 1).toByteArray();
    }

    // a waypoint record as MapSource writes it
    private static Builder waypoint(int version, String name, int waypointClass, double latitude, double longitude,
                                    Double altitude, String notes, String instruction, Long time) {
        Builder w = new Builder(version);
        w.str(name).i32(waypointClass).str("").fill(4, 0).fill(12, 0xFF).fill(2, 0).fill(4, 0xFF);
        w.semi(latitude).semi(longitude);
        if (altitude != null)
            w.bytes(1).dbl(altitude);
        else
            w.bytes(0);
        w.str(notes).bytes(0).i32(1).i32(0).i32(18).str("").str("").str("").bytes(0).bytes(0);
        if (version <= 2) {
            w.fill(2, 0).bytes(0).fill(3, 0).str("").str(instruction);
        } else {
            w.str("").fill(5, 0).str(instruction).i32(0);
        }
        w.i16(0).bytes(0);
        if (time != null)
            w.bytes(1).i32((int) (long) time);
        else
            w.bytes(0);
        if (version >= 3)
            w.i32(0).str("").str("");
        return w;
    }

    private static Builder routePoint(Builder r, int version, String name, int waypointClass, double[][] links) {
        r.str(name).i32(waypointClass).str("").fill(22, 0).bytes(0).bytes(3).fill(17, 0);
        r.i32(links.length);
        for (double[] link : links) {
            r.semi(link[0]).semi(link[1]);
            if (link.length > 2)
                r.bytes(1).dbl(link[2]);
            else
                r.bytes(0);
        }
        r.bytes(1);   // no interlink bounds
        if (version >= 2)
            r.fill(8, 0xFF);
        if (version >= 3)
            r.fill(2, 0);
        return r;
    }

    private List<GpxRoute> read(byte[] bytes) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(bytes), context);
        return context.getRoutes();
    }

    // header and version guard

    @Test
    public void testReadsAllVersions() throws IOException {
        for (int version = 1; version <= 3; version++) {
            Builder file = header(version);
            file.record('W', waypoint(version, "Home", 0, 48.1, 11.5, 520.0, "my notes", null, T1));
            List<GpxRoute> routes = read(end(file));
            assertEquals("version " + version, 1, routes.size());
            GpxRoute route = routes.get(0);
            assertEquals(Waypoints, route.getCharacteristics());
            GpxPosition position = route.getPosition(0);
            assertEquals(48.1, position.getLatitude(), 0.0000001);
            assertEquals(11.5, position.getLongitude(), 0.0000001);
            assertEquals(520.0, position.getElevation(), 0.0);
            assertEquals(T1 * 1000, position.getTime().getTimeInMillis());
            assertEquals("Home; my notes", position.getDescription());
        }
    }

    @Test
    public void testReadsBaseCampSignature() throws IOException {
        Builder file = header(3, "BaseCamp");
        file.record('W', waypoint(3, "Home", 0, 48.1, 11.5, null, null, null, null));
        assertEquals(1, read(end(file)).size());
    }

    @Test
    public void testDeclinesForeignAndNewerFiles() throws IOException {
        assertTrue(read(new byte[0]).isEmpty());
        assertTrue(read("<?xml version=\"1.0\"?><gpx/>".getBytes(UTF_8)).isEmpty());
        assertTrue(read("MsRcd\0\0\0\0\0".getBytes(ISO_8859_1)).isEmpty());
        // BaseCamp versions after MapSource 6.16 write file format versions GPSBabel does not know
        for (int version : new int[]{0, 4, 15}) {
            Builder file = header(version);
            file.record('W', waypoint(3, "Home", 0, 48.1, 11.5, null, null, null, null));
            assertTrue("version " + version, read(end(file)).isEmpty());
        }
        Builder file = header(3, "Other");
        file.record('W', waypoint(3, "Home", 0, 48.1, 11.5, null, null, null, null));
        assertTrue(read(end(file)).isEmpty());
    }

    @Test
    public void testDeclinesTruncatedAndHostileFiles() throws IOException {
        Builder valid = header(3);
        valid.record('W', waypoint(3, "Home", 0, 48.1, 11.5, null, null, null, null));
        byte[] complete = end(valid);
        // every truncation, including one without the end record
        for (int length = 0; length < complete.length - 7; length++)
            assertTrue("length " + length, read(Arrays.copyOf(complete, length)).isEmpty());

        // a record length overrunning the file
        assertTrue(read(end(header(3).i32(1000).bytes('W').fill(10, 0))).isEmpty());
        // a negative record length
        assertTrue(read(end(header(3).i32(-5).bytes('W'))).isEmpty());
        // a huge point count in a short track record
        Builder track = new Builder(3).str("t").bytes(0).i32(0).i32(Integer.MAX_VALUE).semi(1).semi(1);
        assertTrue(read(end(header(3).record('T', track))).isEmpty());
        // a waypoint record that is shorter than its fields
        Builder shortWaypoint = new Builder(3).str("w").i32(0);
        assertTrue(read(end(header(3).record('W', shortWaypoint))).isEmpty());
        // a header record length beyond GPSBabel's buffer
        byte[] header = header(3).toByteArray();
        byte[] hostile = header.clone();
        hostile[13] = (byte) 0xFF;
        assertTrue(read(hostile).isEmpty());
    }

    @Test
    public void testSkipsUnknownRecordsAndResyncsOnRecordLength() throws IOException {
        Builder file = header(3);
        file.record('L', new Builder(3).fill(17, 0x55));
        // trailing bytes the reader does not know
        file.record('W', waypoint(3, "One", 0, 1.0, 2.0, null, null, null, null).fill(9, 0x41));
        file.record('W', waypoint(3, "Two", 0, 3.0, 4.0, null, null, null, null));
        List<GpxRoute> routes = read(end(file));
        assertEquals(1, routes.size());
        assertEquals(2, routes.get(0).getPositionCount());
        assertEquals("Two", routes.get(0).getPosition(1).getDescription());
    }

    // reading records

    @Test
    public void testReadsWaypointFields() throws IOException {
        Builder file = header(3);
        file.record('W', waypoint(3, "Notes", 0, -33.8567844, 151.213108, 12.5, "line 1\r\nline 2", "", T1));
        file.record('W', waypoint(3, "Same", 0, 0.5, 0.5, null, null, "an instruction", null));
        file.record('W', waypoint(3, "", 0, 1.5, 1.5, null, "only notes", "", null));
        file.record('W', waypoint(3, "Hidden", 8, 2.5, 2.5, 1.0e25, null, "", 0L));
        file.record('W', waypoint(3, "Käse", 0, 3.5, 3.5, null, "Straße", "", null));
        List<GpxRoute> routes = read(end(file));
        assertEquals(1, routes.size());
        List<GpxPosition> positions = routes.get(0).getPositions();
        assertEquals(5, positions.size());

        GpxPosition notes = positions.get(0);
        assertEquals(-33.8567844, notes.getLatitude(), 0.0000001);
        assertEquals(151.213108, notes.getLongitude(), 0.0000001);
        assertEquals(12.5, notes.getElevation(), 0.0);
        assertEquals(T1 * 1000, notes.getTime().getTimeInMillis());
        // the XML exchange turns CR LF into a single line feed
        assertEquals("Notes; line 1 line 2", notes.getDescription());

        // GPSBabel uses the instruction as the description
        assertEquals("Same; an instruction", positions.get(1).getDescription());
        // without a name the empty instruction becomes the empty name
        assertEquals("only notes", positions.get(2).getDescription());
        // hidden waypoints are waypoints, too; altitudes beyond 1e24 and time 0 are unknown
        assertEquals("Hidden", positions.get(3).getDescription());
        assertNull(positions.get(3).getElevation());
        assertNull(positions.get(3).getTime());
        assertEquals("Käse; Straße", positions.get(4).getDescription());
    }

    @Test
    public void testReadsLatin1InVersion2() throws IOException {
        Builder file = header(2);
        file.record('W', waypoint(2, "Käse", 0, 3.5, 3.5, null, "Straße", "http://example.com", null));
        file.record('W', waypoint(2, "", 0, 4.5, 4.5, null, null, "", null));
        List<GpxRoute> routes = read(end(file));
        List<GpxPosition> positions = routes.get(0).getPositions();
        assertEquals("Käse; Straße", positions.get(0).getDescription());
        // without name, notes and instruction GPSBabel numbers the waypoints
        assertEquals("WPT002", positions.get(1).getDescription());
    }

    @Test
    public void testReadsRoutesWithLinksAndAutorouteData() throws IOException {
        Builder file = header(3);
        file.record('W', waypoint(3, "Start", 0, 48.0, 11.0, 500.0, "start notes", "", T1));
        file.record('W', waypoint(3, "End", 0, 48.2, 11.2, null, null, "", null));

        Builder route = new Builder(3).str("Tour").bytes(0).bytes(1).i32(3);
        routePoint(route, 3, "Start", 0, new double[][]{{48.0, 11.0, 500.0}, {48.1, 11.1}});
        // a map point that only exists in the route: its coordinates come from the first link
        routePoint(route, 3, "Junction", 8, new double[][]{{48.1, 11.1, 510.0}, {48.15, 11.15}, {48.2, 11.2}});
        // without links the route point is the waypoint of the same name
        routePoint(route, 3, "end", 0, new double[][]{});
        route.i32(0).i32(14).bytes(1).fill(6 + 1 + 4 + 1 + 4 + 5 * 8 + 8, 0x77).str("route description");
        file.record('R', route);

        List<GpxRoute> routes = read(end(file));
        assertEquals(2, routes.size());
        GpxRoute tour = routes.get(1);
        assertEquals(Route, tour.getCharacteristics());
        assertEquals("Tour", tour.getName());
        assertEquals(asList("route description"), tour.getDescription());
        assertEquals(3, tour.getPositionCount());

        GpxPosition start = tour.getPosition(0);
        assertEquals("Start; start notes", start.getDescription());
        assertEquals(500.0, start.getElevation(), 0.0);
        assertEquals(T1 * 1000, start.getTime().getTimeInMillis());

        GpxPosition junction = tour.getPosition(1);
        assertEquals("Junction", junction.getDescription());
        assertEquals(48.1, junction.getLatitude(), 0.0000001);
        assertEquals(11.1, junction.getLongitude(), 0.0000001);
        assertEquals(510.0, junction.getElevation(), 0.0);
        assertNull(junction.getTime());

        GpxPosition end = tour.getPosition(2);
        assertEquals("End", end.getDescription());
        assertEquals(48.2, end.getLatitude(), 0.0000001);
    }

    @Test
    public void testReadsVersion1Routes() throws IOException {
        Builder file = header(1);
        file.record('W', waypoint(1, "A", 0, 10.0, 20.0, null, null, "", null));
        Builder route = new Builder(1).str("").bytes(0).bytes(1).i32(1);
        routePoint(route, 1, "A", 0, new double[][]{});
        route.str("");
        file.record('R', route);
        List<GpxRoute> routes = read(end(file));
        assertEquals(2, routes.size());
        assertNull(routes.get(1).getName());
        assertEquals("A", routes.get(1).getPosition(0).getDescription());
    }

    @Test
    public void testDeclinesRoutePointsWithoutCoordinatesOrFarFromTheirWaypoint() throws IOException {
        Builder sleeping = header(3);
        Builder route = new Builder(3).str("R").bytes(0).bytes(1).i32(1);
        routePoint(route, 3, "Nowhere", 0, new double[][]{});
        sleeping.record('R', route.i32(0).i32(14).bytes(0).str(""));
        assertTrue(read(end(sleeping)).isEmpty());

        Builder mismatch = header(3);
        mismatch.record('W', waypoint(3, "A", 0, 10.0, 20.0, null, null, "", null));
        Builder far = new Builder(3).str("R").bytes(0).bytes(1).i32(1);
        routePoint(far, 3, "A", 0, new double[][]{{10.01, 20.0}});
        mismatch.record('R', far.i32(0).i32(14).bytes(0).str(""));
        assertTrue(read(end(mismatch)).isEmpty());
    }

    @Test
    public void testReadsTracks() throws IOException {
        Builder file = header(3);
        Builder track = new Builder(3).str("Walk").bytes(0).i32(0).i32(3);
        track.semi(47.0).semi(8.0).bytes(1).dbl(400.0).bytes(1).i32((int) T1).bytes(0).bytes(0);
        track.semi(47.1).semi(8.1).bytes(0).bytes(0).bytes(1).dbl(2.0).bytes(1).dbl(21.0);
        track.semi(47.2).semi(8.2).bytes(1).dbl(2.0e24).bytes(1).i32(-1).bytes(0).bytes(0);
        track.i32(0);
        file.record('T', track);
        file.record('T', new Builder(3).str("Empty").bytes(0).i32(0).i32(0).i32(0));

        List<GpxRoute> routes = read(end(file));
        assertEquals(1, routes.size());
        GpxRoute walk = routes.get(0);
        assertEquals(Track, walk.getCharacteristics());
        assertEquals("Walk", walk.getName());
        assertEquals(3, walk.getPositionCount());
        assertEquals(400.0, walk.getPosition(0).getElevation(), 0.0);
        assertEquals(T1 * 1000, walk.getPosition(0).getTime().getTimeInMillis());
        assertNull(walk.getPosition(0).getDescription());
        assertNull(walk.getPosition(1).getElevation());
        assertNull(walk.getPosition(1).getTime());
        assertNull(walk.getPosition(2).getElevation());
        // GPSBabel 1.5.4 keeps the time as an unsigned 32 bit value
        assertEquals(0xFFFFFFFFL * 1000, walk.getPosition(2).getTime().getTimeInMillis());
    }

    // writing

    private static byte[] subclass() {
        return new Builder(3).fill(4, 0).fill(12, 0xFF).fill(2, 0).fill(4, 0xFF).toByteArray();
    }

    private static Builder expectedHeader() {
        Builder header = new Builder(3);
        header.raw("MsRc".getBytes(ISO_8859_1)).i16(0x66);
        header.i32(2).bytes('D').i16(0x6d);
        header.record('A', new Builder(3).i16(605).str("RouteConverter").str("Nov 22 2026").str("00:00:00"));
        return header.str("MapSource");
    }

    private static Builder expectedWaypoint(String name, int waypointClass, int display, double latitude, double longitude,
                                            Double altitude, String comment, String instruction, Long time) {
        Builder w = new Builder(3);
        w.str(name).i32(waypointClass).str("").raw(subclass()).semi(latitude).semi(longitude);
        if (altitude != null)
            w.bytes(1).dbl(altitude);
        else
            w.bytes(0);
        w.str(comment).bytes(0).i32(display).i32(0).i32(18).str("").str("").str("").bytes(0).bytes(0);
        w.str("").fill(5, 0).str(instruction).i32(0);
        w.i16(0).bytes(0);
        if (time != null)
            w.bytes(1).i32((int) (long) time);
        else
            w.bytes(0);
        return w.i32(0).str("").str("");
    }

    private static Builder bounds(Builder b, double maxLatitude, double maxLongitude, Double maxAltitude,
                                  double minLatitude, double minLongitude, Double minAltitude) {
        b.bytes(0).semi(maxLatitude).semi(maxLongitude);
        if (maxAltitude != null)
            b.bytes(1).dbl(maxAltitude);
        else
            b.bytes(0);
        b.semi(minLatitude).semi(minLongitude);
        if (minAltitude != null)
            b.bytes(1).dbl(minAltitude);
        else
            b.bytes(0);
        return b;
    }

    private static Builder expectedRoutePointHead(Builder r, String name, int waypointClass) {
        return r.str(name).i32(waypointClass).str("").raw(subclass()).bytes(0).bytes(3).fill(3, 0).fill(4, 0xFF).fill(10, 0);
    }

    private static GpxRoute route(String name, GpxPosition... positions) {
        return new GpxRoute(new Gpx10Format(), Route, name, null, new ArrayList<>(asList(positions)));
    }

    private byte[] write(GpxRoute route) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(route, out, 0, route.getPositionCount());
        return out.toByteArray();
    }

    @Test
    public void testWritesHeaderWithRouteConverterWatermark() throws IOException {
        byte[] bytes = write(route("Empty"));
        byte[] expected = end(expectedHeader());
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testWritesWaypointsRouteAndTrackOfARoute() throws IOException {
        GpxPosition a = new GpxPosition(11.5, 48.1, 10.0, null, fromMillis(T1 * 1000), "A; note a");
        GpxPosition b = new GpxPosition(11.6, 48.2, null, null, null, "B");
        byte[] bytes = write(route("Tour", a, b));

        Builder expected = expectedHeader();
        expected.record('W', expectedWaypoint("A", 0, 1, 48.1, 11.5, 10.0, "note a", "note a", T1));
        expected.record('W', expectedWaypoint("B", 0, 1, 48.2, 11.6, null, "B", null, null));

        Builder route = new Builder(3).str("Tour").bytes(0);
        bounds(route, 48.2, 11.6, 10.0, 48.1, 11.5, 10.0).i32(2);
        expectedRoutePointHead(route, "A", 0).i32(2).semi(48.1).semi(11.5).bytes(1).dbl(10.0).semi(48.2).semi(11.6).bytes(0);
        bounds(route, 48.2, 11.6, 10.0, 48.1, 11.5, 10.0).fill(8, 0xFF).fill(2, 0);
        expectedRoutePointHead(route, "B", 0).i32(0).bytes(1).fill(8, 0xFF).fill(2, 0);
        route.i32(0).i32(14).bytes(0).str(null);
        expected.record('R', route);

        Builder track = new Builder(3).str("Tour").bytes(0).i32(0).i32(2);
        track.semi(48.1).semi(11.5).bytes(1).dbl(10.0).bytes(1).i32((int) T1).bytes(0).bytes(0);
        track.semi(48.2).semi(11.6).bytes(0).bytes(0).bytes(0).bytes(0);
        expected.record('T', track.i32(0));

        assertArrayEquals(end(expected), bytes);
    }

    @Test
    public void testWritesUnnamedPositionsAsHiddenRouteWaypoints() throws IOException {
        GpxPosition unnamed = new GpxPosition(1.0, 2.0, null, null, null, null);
        byte[] bytes = write(route(null, unnamed));

        Builder expected = expectedHeader();
        expected.record('W', expectedWaypoint("WPT001", 0, 1, 2.0, 1.0, null, "WPT001", null, null));
        // the route point has another synthesized name and becomes a map point with the display of class 8,
        // but GPSBabel's two bit wpt_flags.fmt_use stores its class as 0
        expected.record('W', expectedWaypoint("RPT001", 0, 0, 2.0, 1.0, null, null, null, null));
        Builder route = new Builder(3).str("Route0000").bytes(0);
        bounds(route, 2.0, 1.0, null, 2.0, 1.0, null).i32(1);
        expectedRoutePointHead(route, "RPT001", 0).i32(0).bytes(1).fill(8, 0xFF).fill(2, 0);
        expected.record('R', route.i32(0).i32(14).bytes(0).str(null));
        Builder track = new Builder(3).str("Track0000").bytes(0).i32(0).i32(1);
        track.semi(2.0).semi(1.0).bytes(0).bytes(0).bytes(0).bytes(0);
        expected.record('T', track.i32(0));

        assertArrayEquals(end(expected), bytes);
    }

    @Test
    public void testMakesDuplicateWaypointNamesUnique() throws IOException {
        GpxPosition first = new GpxPosition(1.0, 2.0, null, null, null, "Same");
        GpxPosition second = new GpxPosition(3.0, 4.0, null, null, null, "same");
        GpxPosition repeated = new GpxPosition(1.0, 2.0, null, null, null, "Same");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(asList(new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(first, second, repeated)))), out);

        Builder expected = expectedHeader();
        expected.record('W', expectedWaypoint("Same", 0, 1, 2.0, 1.0, null, "Same", null, null));
        expected.record('W', expectedWaypoint("same.1", 0, 1, 4.0, 3.0, null, "same", null, null));
        // the same name, position and notes are written once
        assertArrayEquals(end(expected), out.toByteArray());
    }

    @Test
    public void testWriteSkipsPositionsWithoutCoordinatesAndEmptyRoutes() throws IOException {
        GpxPosition invalid = new GpxPosition(null, 2.0, null, null, null, "X");
        assertArrayEquals(end(expectedHeader()), write(route("Nothing", invalid)));
    }

    @Test
    public void testRoundtrip() throws IOException {
        List<GpxRoute> routes = new ArrayList<>();
        routes.add(new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(asList(
                new GpxPosition(11.5, 48.1, 10.0, null, fromMillis(T1 * 1000), "Home; with notes"),
                new GpxPosition(-70.25, -33.5, -2.5, null, null, "Süd")))));
        routes.add(new GpxRoute(new Gpx10Format(), Route, "Route", null, new ArrayList<>(asList(
                new GpxPosition(11.5, 48.1, 10.0, null, fromMillis(T1 * 1000), "Home; with notes"),
                new GpxPosition(12.0, 49.0, null, null, null, "Via")))));
        routes.add(new GpxRoute(new Gpx10Format(), Track, "Track", null, new ArrayList<>(asList(
                new GpxPosition(8.0, 47.0, 400.0, null, fromMillis(T1 * 1000), null),
                new GpxPosition(8.1, 47.1, null, null, fromMillis((T1 + 60) * 1000), null)))));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(routes, out);

        List<GpxRoute> read = read(out.toByteArray());
        assertEquals(3, read.size());
        GpxRoute waypoints = read.get(0);
        // the route point "Via" is written as a waypoint, too
        assertEquals(3, waypoints.getPositionCount());
        assertEquals("Home; with notes", waypoints.getPosition(0).getDescription());
        assertEquals(48.1, waypoints.getPosition(0).getLatitude(), 0.0000001);
        assertEquals(11.5, waypoints.getPosition(0).getLongitude(), 0.0000001);
        assertEquals(10.0, waypoints.getPosition(0).getElevation(), 0.0);
        assertEquals(T1 * 1000, waypoints.getPosition(0).getTime().getTimeInMillis());
        assertEquals("Süd", waypoints.getPosition(1).getDescription());
        assertEquals(-33.5, waypoints.getPosition(1).getLatitude(), 0.0000001);
        assertEquals(-70.25, waypoints.getPosition(1).getLongitude(), 0.0000001);
        assertEquals(-2.5, waypoints.getPosition(1).getElevation(), 0.0);

        GpxRoute route = read.get(1);
        assertEquals(Route, route.getCharacteristics());
        assertEquals("Route", route.getName());
        assertEquals("Home; with notes", route.getPosition(0).getDescription());
        assertEquals("Via", route.getPosition(1).getDescription());

        GpxRoute track = read.get(2);
        assertEquals(Track, track.getCharacteristics());
        assertEquals("Track", track.getName());
        assertEquals(2, track.getPositionCount());
        assertEquals(400.0, track.getPosition(0).getElevation(), 0.0);
        assertEquals((T1 + 60) * 1000, track.getPosition(1).getTime().getTimeInMillis());
    }

    // helpers

    @Test
    public void testSemicirclesAreTruncatedLikeGpsBabel154() {
        assertEquals(1, GarminMapSource6Format.degreesToSemicircles(1.7 / SEMICIRCLES_PER_DEGREE));
        assertEquals(-1, GarminMapSource6Format.degreesToSemicircles(-1.7 / SEMICIRCLES_PER_DEGREE));
        assertEquals(Integer.MAX_VALUE, GarminMapSource6Format.degreesToSemicircles(180.0));
        assertEquals(-1073741824, GarminMapSource6Format.degreesToSemicircles(-90.0));
    }

    @Test
    public void testShortNames() {
        GarminMapSource6Format.ShortNames shortNames = new GarminMapSource6Format.ShortNames("WPT");
        assertEquals("A", shortNames.mkshort("A"));
        assertEquals("a.1", shortNames.mkshort("a"));
        assertEquals("A.2", shortNames.mkshort("A"));
        assertEquals("Walk 1", shortNames.mkshort("Walk one"));
        assertEquals("Tab", shortNames.mkshort("\tTab\r\n"));
        assertEquals("WPT", shortNames.mkshort(" "));
        assertEquals("WPT.1", shortNames.mkshort(""));
        assertEquals("Ünïcödé", shortNames.mkshort("Ünïcödé"));
        assertEquals("ÜNÏCÖDÉ", shortNames.mkshort("ÜNÏCÖDÉ"));
        String longName = "x".repeat(1100);
        assertEquals(1024, shortNames.mkshort(longName).length());
    }

    @Test
    public void testDetectionGuard() {
        assertTrue(GarminMapSource6Format.isGdb("MsRcf\0".getBytes(ISO_8859_1)));
        assertTrue(!GarminMapSource6Format.isGdb("MsRcf".getBytes(ISO_8859_1)));
        assertTrue(!GarminMapSource6Format.isGdb("MsRcfX".getBytes(ISO_8859_1)));
    }
}

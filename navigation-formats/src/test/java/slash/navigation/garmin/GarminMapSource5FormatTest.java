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
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class GarminMapSource5FormatTest {
    private static final double DELTA = 0.0000001;
    private final GarminMapSource5Format format = new GarminMapSource5Format();

    private List<GpxRoute> read(byte[] bytes) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(bytes), context);
        return context.getRoutes();
    }

    private static MpsBuilder.Point point(String name, int waypointClass, double latitude, double longitude, Double altitude) {
        return new MpsBuilder.Point(name, waypointClass, latitude, longitude, altitude);
    }

    private static MpsBuilder.TrackPoint trackPoint(double latitude, double longitude, Double altitude, Integer time) {
        return new MpsBuilder.TrackPoint(latitude, longitude, altitude, time);
    }

    // user waypoints A and B, the hidden route waypoint H, a route over A, H, B and an unknown point X and a track
    private static MpsBuilder sample(MpsBuilder builder) {
        return builder
                .waypoint("A", 0, 48.1, 11.5, 10.0, "desc A", "note A")
                .waypoint("B", 0, 48.2, 11.6, null, "desc B", "")
                .waypoint("H", 8, 48.3, 11.7, 5.5, "hidden", "")
                .route("R1", point("A", 0, 48.1, 11.5, 10.0), point("H", 8, 48.3, 11.7, 5.5),
                        point("B", 0, 48.2, 11.6, null), point("X", 8, 47.0, 10.0, 3.0))
                .track("T1", trackPoint(48.0, 11.0, 1.0, 1000000000), trackPoint(48.01, 11.01, null, null),
                        trackPoint(48.02, 11.02, 2.0, 0), trackPoint(48.03, 11.03, 2.0, -5))
                .mapSet();
    }

    private static void assertPosition(GpxPosition position, double longitude, double latitude, Double elevation, String description) {
        assertEquals(longitude, position.getLongitude(), DELTA);
        assertEquals(latitude, position.getLatitude(), DELTA);
        if (elevation == null)
            assertNull(position.getElevation());
        else
            assertEquals(elevation, position.getElevation(), DELTA);
        assertEquals(description, position.getDescription());
    }

    private void assertSample(List<GpxRoute> routes, int version) {
        assertEquals(3, routes.size());

        GpxRoute waypoints = routes.get(0);
        assertEquals(Waypoints, waypoints.getCharacteristics());
        // the hidden route waypoint H is no user waypoint
        assertEquals(2, waypoints.getPositionCount());
        // version 3 has no notes: the description is the comment
        assertPosition(waypoints.getPosition(0), 11.5, 48.1, 10.0, version == 3 ? "A; desc A" : "A; note A");
        assertPosition(waypoints.getPosition(1), 11.6, 48.2, null, "B; desc B");

        GpxRoute route = routes.get(1);
        assertEquals(Route, route.getCharacteristics());
        assertEquals("R1", route.getName());
        assertEquals(4, route.getPositionCount());
        assertPosition(route.getPosition(0), 11.5, 48.1, 10.0, version == 3 ? "A; desc A" : "A; note A");
        assertPosition(route.getPosition(1), 11.7, 48.3, 5.5, "H; hidden");
        assertPosition(route.getPosition(2), 11.6, 48.2, null, "B; desc B");
        // a route point without waypoint takes its position from the route link
        assertPosition(route.getPosition(3), 10.0, 47.0, 3.0, "X");

        GpxRoute track = routes.get(2);
        assertEquals(Track, track.getCharacteristics());
        assertEquals("T1", track.getName());
        assertEquals(4, track.getPositionCount());
        assertPosition(track.getPosition(0), 11.0, 48.0, 1.0, null);
        assertEquals(fromMillis(1000000000L * 1000), track.getPosition(0).getTime());
        // a track point without time keeps the time of the previous point
        assertEquals(fromMillis(1000000000L * 1000), track.getPosition(1).getTime());
        assertNull(track.getPosition(2).getTime());
        // the time is an unsigned 32 bit value
        assertEquals(fromMillis((0x100000000L - 5) * 1000), track.getPosition(3).getTime());
    }

    @Test
    public void testReadVersion5() throws IOException {
        assertSample(read(sample(new MpsBuilder(5)).toBytes()), 5);
    }

    @Test
    public void testReadVersion4() throws IOException {
        assertSample(read(sample(new MpsBuilder(4)).toBytes()), 4);
        assertSample(read(sample(new MpsBuilder(4, 'e')).toBytes()), 4);
        assertSample(read(sample(new MpsBuilder(4, 'h')).toBytes()), 4);
    }

    @Test
    public void testReadVersion3() throws IOException {
        assertSample(read(sample(new MpsBuilder(3)).toBytes()), 3);
    }

    @Test
    public void testVersionFlags() throws Exception {
        assertEquals(3, new GarminMapSource5Format().new Reader(versionHeader('d')).readVersion());
        assertEquals(4, new GarminMapSource5Format().new Reader(versionHeader('e')).readVersion());
        assertEquals(4, new GarminMapSource5Format().new Reader(versionHeader('h')).readVersion());
        assertEquals(5, new GarminMapSource5Format().new Reader(versionHeader('i')).readVersion());
        for (char flag : new char[]{'a', 'c', 'j', 'k', 'z'})
            assertTrue(read(new MpsBuilder(5, flag).waypoint("A", 0, 1, 2, null, "d", "n").mapSet().toBytes()).isEmpty());
    }

    private static byte[] versionHeader(char flag) {
        return new MpsBuilder(5, flag).mapSet().toBytes();
    }

    @Test
    public void testReadSemicircles() throws IOException {
        // 2^30 semicircles are 90 degrees, -2^31 are -180 degrees
        MpsBuilder builder = new MpsBuilder(5);
        byte[] payload = builder.waypointPayload(MpsBuilder.ascii("S"), 0, 0, 0, null, MpsBuilder.ascii("d"), MpsBuilder.ascii(""));
        // name "S\0" (2) + class (4) + country (1) + subclass (22)
        int offset = 2 + 4 + 1 + 22;
        payload[offset + 3] = 0x40;
        payload[offset + 7] = (byte) 0x80;
        List<GpxRoute> routes = read(builder.record('W', payload).mapSet().toBytes());
        assertEquals(90.0, routes.get(0).getPosition(0).getLatitude(), DELTA);
        assertEquals(-180.0, routes.get(0).getPosition(0).getLongitude(), DELTA);
    }

    @Test
    public void testReadWrapsUserWaypointsOnly() throws IOException {
        List<GpxRoute> routes = read(new MpsBuilder(5)
                .waypoint("A", 0, 100, 2, null, "d", "")
                .waypoint("H", 8, 100, 2, null, "d", "")
                .route("R", point("A", 0, 100, 2, null), point("H", 8, 100, 2, null))
                .mapSet().toBytes());
        // waypt_add() wraps the latitude of user waypoints only
        assertEquals(-80.0, routes.get(0).getPosition(0).getLatitude(), DELTA);
        assertEquals(-80.0, routes.get(1).getPosition(0).getLatitude(), DELTA);
        assertEquals(100.0, routes.get(1).getPosition(1).getLatitude(), DELTA);
    }

    @Test
    public void testReadRoutePointNames() throws IOException {
        List<GpxRoute> routes = read(new MpsBuilder(5)
                .route("", point("", 8, 1, 2, null), point("Q", 8, 3, 4, null))
                .route("R2", point("", 8, 5, 6, null), point("", 8, 7, 8, null))
                .mapSet().toBytes());
        assertEquals(2, routes.size());
        assertEquals(Route, routes.get(0).getCharacteristics());
        // the route points without name are numbered across all routes
        assertEquals("RPT001", routes.get(0).getPosition(0).getDescription());
        assertEquals("Q", routes.get(0).getPosition(1).getDescription());
        assertEquals("R2", routes.get(1).getName());
        assertEquals("RPT003", routes.get(1).getPosition(0).getDescription());
        assertEquals("RPT004", routes.get(1).getPosition(1).getDescription());
    }

    @Test
    public void testReadDuplicateWaypointNames() throws IOException {
        List<GpxRoute> routes = read(new MpsBuilder(5)
                .waypoint("A", 0, 1, 2, null, "first", "")
                .waypoint("A", 0, 3, 4, null, "second", "")
                .route("R", point("A", 0, 9, 9, null), point("A", 0, 8, 8, null))
                .mapSet().toBytes());
        assertEquals(2, routes.get(0).getPositionCount());
        // route points refer to the first waypoint with their name
        assertPosition(routes.get(1).getPosition(0), 2, 1, null, "A; first");
        assertPosition(routes.get(1).getPosition(1), 2, 1, null, "A; first");
    }

    @Test
    public void testReadCharsets() throws IOException {
        byte[] windows1252 = {'M', (byte) 0xFC, 'h', 'l', 'e'};
        byte[] utf8 = {'M', (byte) 0xC3, (byte) 0xBC, 'h', 'l', 'e'};
        byte[] euro = {(byte) 0x80, ' ', 'x'};
        List<GpxRoute> routes = read(new MpsBuilder(5).waypoint(windows1252, 0, 1, 2, null, utf8, euro).mapSet().toBytes());
        assertEquals("Mühle; € x", routes.get(0).getPosition(0).getDescription());
    }

    @Test
    public void testReadSkipsUnknownRecordsAndMapSegments() throws IOException {
        List<GpxRoute> routes = read(new MpsBuilder(5)
                .record('Z', new byte[]{1, 2, 3})
                .record('L', new byte[]{9, 9, 9, 9})
                .waypoint("A", 0, 1, 2, null, "d", "")
                .mapSet()
                // nothing behind the map set is read
                .waypoint("B", 0, 3, 4, null, "e", "")
                .toBytes());
        assertEquals(1, routes.size());
        assertEquals(1, routes.get(0).getPositionCount());
    }

    @Test
    public void testReadWaypointWithWrongRecordLength() throws IOException {
        MpsBuilder builder = new MpsBuilder(5);
        byte[] payload = builder.waypointPayload(MpsBuilder.ascii("A"), 0, 1, 2, null, MpsBuilder.ascii("d"), MpsBuilder.ascii(""));
        byte[] longer = Arrays.copyOf(payload, payload.length + 3);
        List<GpxRoute> routes = read(builder.record('W', longer).waypoint("B", 0, 3, 4, null, "e", "").mapSet().toBytes());
        // a waypoint that lost sync with the file is junked, the reader resyncs on the record length
        assertEquals(1, routes.get(0).getPositionCount());
        assertEquals("B; e", routes.get(0).getPosition(0).getDescription());
    }

    @Test
    public void testReadWithoutMapSet() throws IOException {
        byte[] bytes = sample(new MpsBuilder(5)).toBytes();
        // without the map set record the file ends behind the track: everything is read
        assertEquals(3, read(Arrays.copyOf(bytes, bytes.length - 7)).size());
    }

    @Test
    public void testReadEmptyTrackAndRoute() throws IOException {
        List<GpxRoute> routes = read(new MpsBuilder(5).track("T0").track("T1", trackPoint(1, 2, null, 5)).mapSet().toBytes());
        // a track without points is dropped
        assertEquals(1, routes.size());
        assertEquals("T1", routes.get(0).getName());
    }

    @Test
    public void testNoMapSourceFile() throws IOException {
        assertTrue(read(new byte[0]).isEmpty());
        assertTrue(read("<?xml version=\"1.0\"?><gpx/>".getBytes(US_ASCII)).isEmpty());
        assertTrue(read(new byte[]{'M', 's', 'R', 'c', 'f', 0, 2, 0, 0, 0, 'D', 'k', 0}).isEmpty());
        assertTrue(read(new byte[]{'M', 's', 'R', 'c', 'd'}).isEmpty());
        assertTrue(read(Arrays.copyOf(new MpsBuilder(5).toBytes(), 6)).isEmpty());
        assertFalse(GarminMapSource5Format.isMps("MsRcf".getBytes(US_ASCII)));
        assertTrue(GarminMapSource5Format.isMps(new MpsBuilder(5).toBytes()));
    }

    @Test
    public void testTruncatedInputNeverThrows() throws IOException {
        byte[] bytes = sample(new MpsBuilder(5)).toBytes();
        for (int length = 0; length < bytes.length; length++)
            read(Arrays.copyOf(bytes, length));
        // a number read beyond the end of the file is fatal in GPSBabel: no route
        assertTrue(read(Arrays.copyOf(bytes, 120)).isEmpty());
    }

    @Test
    public void testHostileInputTerminates() throws IOException {
        byte[] header = new MpsBuilder(5).toBytes();
        Random random = new Random(452);
        for (int i = 0; i < 2000; i++) {
            byte[] bytes = Arrays.copyOf(header, header.length + random.nextInt(400));
            for (int j = header.length; j < bytes.length; j++)
                bytes[j] = (byte) random.nextInt(256);
            read(bytes);
        }

        byte[] sample = sample(new MpsBuilder(5)).toBytes();
        for (int i = 0; i < 2000; i++) {
            byte[] bytes = sample.clone();
            for (int j = 0; j < 4; j++)
                bytes[header.length + random.nextInt(bytes.length - header.length)] = (byte) random.nextInt(256);
            read(bytes);
        }
    }

    @Test
    public void testRouteWithPointCountZero() throws IOException {
        MpsBuilder builder = new MpsBuilder(5).waypoint("A", 0, 1, 2, null, "d", "");
        byte[] route = builder.routePayload("R0", true, point("A", 0, 1, 2, null));
        // name "R0\0" (3) + autoname (1) + bounds flag (1) + 2 x (4 + 4 + 9): the point count
        route[39] = 0;
        List<GpxRoute> routes = read(builder.record('R', route).mapSet().toBytes());
        // GPSBabel reads route points until a link without steps: here the trailer of the only point
        assertEquals(2, routes.size());
        assertEquals(Route, routes.get(1).getCharacteristics());
        assertEquals(0, routes.get(1).getPositionCount());
    }

    @Test
    public void testHugeInterlinkStepCount() throws IOException {
        MpsBuilder builder = new MpsBuilder(5);
        byte[] route = builder.routePayload("R", false, point("A", 8, 1, 2, null), point("B", 8, 3, 4, null));
        int steps = indexOf(route, new byte[]{2, 0, 0, 0}, FIRST_POINT);
        route[steps] = (byte) 0xFF;
        route[steps + 1] = (byte) 0xFF;
        route[steps + 2] = (byte) 0xFF;
        route[steps + 3] = 0x7F;
        assertTrue(read(builder.record('R', route).mapSet().toBytes()).isEmpty());
    }

    @Test
    public void testRouteWithoutInterlinkSteps() throws IOException {
        MpsBuilder builder = new MpsBuilder(5).waypoint("A", 0, 1, 2, null, "d", "");
        byte[] route = builder.routePayload("R", false, point("A", 0, 1, 2, null), point("B", 8, 3, 4, null));
        route[indexOf(route, new byte[]{2, 0, 0, 0}, FIRST_POINT)] = 0;
        List<GpxRoute> routes = read(builder.record('R', route).mapSet().toBytes());
        // GPSBabel keeps the route it started with no points
        assertEquals(2, routes.size());
        assertEquals(Route, routes.get(1).getCharacteristics());
        assertEquals(0, routes.get(1).getPositionCount());
    }

    // the first route point of a route without bounds: name "R\0" (2) + autoname (1) + bounds flag (1) + count (4)
    private static final int FIRST_POINT = 8;

    private static int indexOf(byte[] bytes, byte[] pattern) {
        return indexOf(bytes, pattern, 0);
    }

    private static int indexOf(byte[] bytes, byte[] pattern, int from) {
        outer:
        for (int i = from; i <= bytes.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++)
                if (bytes[i + j] != pattern[j])
                    continue outer;
            return i;
        }
        throw new IllegalArgumentException("pattern not found");
    }

    // writing

    private static GpxPosition position(double longitude, double latitude, Double elevation, Long seconds, String description) {
        return new GpxPosition(longitude, latitude, elevation, null, seconds != null ? fromMillis(seconds * 1000) : null, description);
    }

    private static GpxRoute route(RouteCharacteristics characteristics, String name, GpxPosition... positions) {
        return new GpxRoute(new Gpx10Format(), characteristics, name, null, new ArrayList<>(asList(positions)));
    }

    private byte[] write(GpxRoute route) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        format.write(route, output, 0, route.getPositionCount());
        return output.toByteArray();
    }

    private byte[] write(List<GpxRoute> routes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        format.write(routes, output);
        return output.toByteArray();
    }

    // what GPSBabel 1.5.4 writes for the route of testWriteExactBytes(), record by record
    private static final String EXPECTED =
            // signature, "D" record for version 5 ('i'), "A" record with program version 5.00
            "4d7352636400" +
            "02000000446900" +
            "1b00000041f401535141004a756c20203320323030330030383a33353a333300" +
            // waypoint "A": class 0 (user), the description as comment and notes, display 1, colour 0, symbol 18
            "5c000000574100000000000000000000ffffffffffffffffffffffff0000ffffffff78563422d8822d08010000000000" +
            "002440646100000000000000000000010000000000000012000000000000000000000000000000000000000000006461" +
            "00" +
            // waypoint "B" without elevation: the name as comment
            "59000000574200000000000000000000ffffffffffffffffffffffff0000ffffffffcf8a46222eb73f08000000000000" +
            "00000042000000000000000000000100000000000000120000000000000000000000000000000000000000000000" +
            // route "Test": bounds, two route points and the link between them, trailer
            "db0000005254657374000000cf8a46222eb73f0801000000000000244078563422d8822d080100000000000024400200" +
            "00004100000000000000000000ffffffffffffffffffffffff0000ffffffff0003000000000000000000000000000000" +
            "00000200000078563422d8822d08010000000000002440cf8a46222eb73f0800000000000000000000cf8a46222eb73f" +
            "0801000000000000244078563422d8822d080100000000000024404200000000000000000000ffffffffffffffffffff" +
            "ffff0000ffffffff000300000000000000000000000000000000000000000001" +
            // track "Test": display 1, colour 0, the time of the first point, none for the second
            "4c00000054546573740001000000000200000078563422d8822d080100000000000024400100f1536500000000000000" +
            "0000cf8a46222eb73f080000000000000000000000000000000000000000000000" +
            // empty map set name with autoname
            "02000000560001";

    @Test
    public void testWriteExactBytes() throws IOException {
        byte[] bytes = write(route(Route, "Test",
                position(11.5, 48.1, 10.0, 1700000000L, "A; da"),
                position(11.6, 48.2, null, null, "B")));
        assertEquals(EXPECTED, HexFormat.of().formatHex(bytes));
    }

    @Test
    public void testWriteDefaults() throws IOException {
        byte[] bytes = write(route(Route, "R", position(11.5, 48.1, null, null, "A"), position(11.6, 48.2, null, null, "A")));
        // the "Waypoint" symbol of MapSource 5
        assertEquals(18, GarminDefaults.mapSourceWaypointIconNumber(GarminMapSource5Format.WRITE_VERSION));
        // a waypoint is written once per name: one user waypoint, the second route point refers to it
        assertEquals(1, count(bytes, 'W'));
        assertEquals(1, count(bytes, 'R'));
        assertEquals(1, count(bytes, 'T'));
    }

    @Test
    public void testWriteHiddenRouteWaypoints() throws IOException {
        byte[] bytes = write(List.of(route(Route, "R", position(11.5, 48.1, null, null, "A"), position(11.6, 48.2, null, null, null))));
        // route points that are no user waypoint are written as hidden waypoints (class 8)
        assertEquals(2, count(bytes, 'W'));
        List<GpxRoute> routes = read(bytes);
        assertEquals(1, routes.size());
        assertEquals(Route, routes.get(0).getCharacteristics());
        assertEquals("A", routes.get(0).getPosition(0).getDescription());
        assertEquals("RPT002", routes.get(0).getPosition(1).getDescription());
    }

    @Test
    public void testWriteRouteWithoutName() throws IOException {
        byte[] bytes = write(route(Route, null, position(11.5, 48.1, null, 0x12345678L, "A")));
        List<GpxRoute> routes = read(bytes);
        assertEquals("Route12345678", routes.get(1).getName());
        assertEquals("Track12345678", routes.get(2).getName());
    }

    @Test
    public void testWriteSkipsRouteWithoutPoints() throws IOException {
        byte[] bytes = write(List.of(route(Route, "Empty"), route(Track, "T", position(1, 2, null, 5L, null))));
        assertEquals(0, count(bytes, 'R'));
        assertEquals(1, count(bytes, 'T'));
    }

    @Test
    public void testWriteInvalidLatitude() {
        try {
            write(route(Route, "Bad", position(11.5, 300.0, null, null, "bad")));
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testWriteWindows1252() throws IOException {
        byte[] bytes = write(route(Waypoints, "W", position(11.5, 48.1, null, null, "Größe; € 3")));
        assertTrue(indexOf(bytes, new byte[]{'G', 'r', (byte) 0xF6, (byte) 0xDF, 'e', 0}) > 0);
        assertTrue(indexOf(bytes, new byte[]{(byte) 0x80, ' ', '3', 0}) > 0);
    }

    @Test
    public void testRoundtrip() throws IOException {
        GpxRoute route = route(Route, "Größe & <Test>",
                position(11.5, 48.1, 1.5, 1767261600L, "Ä 1"),
                position(11.501, 48.101, 2.5, 1767261660L, "ß,2; with desc"),
                position(-11.502, -48.102, null, 1767261720L, "€ 3"));
        List<GpxRoute> routes = read(write(route));
        assertEquals(3, routes.size());
        assertEquals(asList(Waypoints, Route, Track), asList(routes.get(0).getCharacteristics(),
                routes.get(1).getCharacteristics(), routes.get(2).getCharacteristics()));
        for (GpxRoute read : routes) {
            assertEquals(3, read.getPositionCount());
            for (int i = 0; i < 3; i++) {
                // semicircles are truncated: 1e-7 degrees are 1.2 semicircles
                assertEquals(route.getPosition(i).getLongitude(), read.getPosition(i).getLongitude(), 0.0000002);
                assertEquals(route.getPosition(i).getLatitude(), read.getPosition(i).getLatitude(), 0.0000002);
                assertEquals(route.getPosition(i).getElevation(), read.getPosition(i).getElevation());
            }
        }
        assertEquals("Größe & <Test>", routes.get(1).getName());
        assertEquals("Ä 1", routes.get(1).getPosition(0).getDescription());
        assertEquals("ß,2; with desc", routes.get(1).getPosition(1).getDescription());
        assertEquals("€ 3", routes.get(1).getPosition(2).getDescription());
        assertEquals(fromMillis(1767261660L * 1000), routes.get(2).getPosition(1).getTime());
        assertNotNull(routes.get(0).getPosition(0));
    }

    @Test
    public void testDegreesToSemicircles() {
        // GPSBabel 1.5.4 truncates the product
        assertEquals(573855352, GarminMapSource5Format.degreesToSemicircles(48.1));
        assertEquals(-573855352, GarminMapSource5Format.degreesToSemicircles(-48.1));
        assertEquals(Integer.MIN_VALUE, GarminMapSource5Format.degreesToSemicircles(180.0));
        assertEquals(Integer.MIN_VALUE, GarminMapSource5Format.degreesToSemicircles(Double.NaN));
        assertEquals(Integer.MIN_VALUE, GarminMapSource5Format.degreesToSemicircles(-180.0));
    }

    @Test
    public void testToTimeT() {
        assertEquals(0, GarminMapSource5Format.toTimeT(null));
        assertEquals(0, GarminMapSource5Format.toTimeT(fromMillis(999)));
        assertEquals(1, GarminMapSource5Format.toTimeT(fromMillis(1000)));
        assertEquals(0xFFFFFFFFL, GarminMapSource5Format.toTimeT(fromMillis(-1000)));
        assertEquals(0xFFFFFFFFL, GarminMapSource5Format.toTimeT(fromMillis(0x100000000L * 1000)));
    }

    // the records of this type, assuming the type letter follows a record length below 256
    private static int count(byte[] bytes, char type) {
        int result = 0;
        int position = 6;
        while (position + 5 <= bytes.length) {
            int length = (bytes[position] & 0xFF) | (bytes[position + 1] & 0xFF) << 8 |
                    (bytes[position + 2] & 0xFF) << 16 | (bytes[position + 3] & 0xFF) << 24;
            if (bytes[position + 4] == type)
                result++;
            position += 5 + length;
        }
        return result;
    }
}

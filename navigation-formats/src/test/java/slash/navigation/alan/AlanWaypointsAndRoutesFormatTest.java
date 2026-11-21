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

package slash.navigation.alan;

import org.junit.Test;
import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class AlanWaypointsAndRoutesFormatTest {
    // 2026-03-04T05:06:07Z
    private static final long NOW = 1772600767L;
    private final AlanWaypointsAndRoutesFormat format =
            new AlanWaypointsAndRoutesFormat(Clock.fixed(Instant.ofEpochSecond(NOW), UTC));

    // struct offsets computed by hand from alan.cc, independent of the constants of the format
    private static final int SIZE = 59768;
    private static final int WPT = 3008;
    private static final int RTEHDR = 43008;
    private static final int RTE = 43168;

    /** A wprdata image as wpr_write() initializes it. */
    private static ByteBuffer emptyFile(int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0, 0x5C38A600);
        for (int i = 0; i < 1000; i++)
            buffer.putShort(8 + 2 * i, (short) -1);
        buffer.putInt(RTEHDR, 0xD87F5900);
        for (int i = 0; i < 50; i++)
            buffer.putShort(RTEHDR + 8 + 2 * i, (short) -1);
        buffer.putShort(RTEHDR + 158, (short) -1);
        return buffer;
    }

    private static ByteBuffer emptyFile() {
        return emptyFile(SIZE);
    }

    private static void putLabel(ByteBuffer buffer, int offset, String label) {
        byte[] bytes = label.getBytes(UTF_8);
        for (int i = 0; i < bytes.length; i++)
            buffer.put(offset + i, bytes[i]);
    }

    private static void waypoint(ByteBuffer buffer, int hdrIdx, int wptIdx, String name, String comment,
                                 int x, int y, int date, int time) {
        buffer.putShort(8 + 2 * hdrIdx, (short) wptIdx);
        buffer.put(2008 + wptIdx, (byte) 0xff);
        int offset = WPT + 40 * wptIdx;
        putLabel(buffer, offset, name);
        putLabel(buffer, offset + 8, comment);
        buffer.putInt(offset + 20, x);
        buffer.putInt(offset + 24, y);
        buffer.putInt(offset + 28, date);
        buffer.putInt(offset + 32, time);
    }

    private static void route(ByteBuffer buffer, int hdrIdx, int rteIdx, String name, String comment, int... wptIdx) {
        buffer.putShort(RTEHDR + 8 + 2 * hdrIdx, (short) rteIdx);
        buffer.put(RTEHDR + 108 + rteIdx, (byte) 0xff);
        int offset = RTE + 332 * rteIdx;
        putLabel(buffer, offset, name);
        putLabel(buffer, offset + 8, comment);
        buffer.putShort(offset + 20, (short) wptIdx.length);
        for (int j = 0; j < wptIdx.length; j++)
            buffer.putShort(offset + 22 + 2 * j, (short) wptIdx[j]);
    }

    private static int date(int year, int month, int day) {
        return day | month << 8 | year << 16;
    }

    private List<GpxRoute> read(byte[] data) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(data), context);
        return context.getRoutes();
    }

    private static GpxPosition position(double longitude, double latitude, Long seconds, String description) {
        return new GpxPosition(longitude, latitude, null, null, seconds != null ? fromMillis(seconds * 1000) : null, description);
    }

    private byte[] write(GpxRoute route) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        return target.toByteArray();
    }

    private byte[] write(List<GpxRoute> routes) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(routes, target);
        return target.toByteArray();
    }

    private static GpxRoute route(String name, GpxPosition... positions) {
        return new GpxRoute(new Gpx10Format(), Route, name, null, new ArrayList<>(asList(positions)));
    }

    private static ByteBuffer buffer(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static String label(byte[] data, int offset, int length) {
        return new String(data, offset, length, US_ASCII);
    }

    @Test
    public void nameAndExtension() {
        assertEquals("Alan Map 500 Waypoints and Routes (*.wpr)", format.getName());
        assertEquals(".wpr", format.getExtension());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
        assertTrue(format instanceof MultipleRoutesFormat);
    }

    @Test
    public void structSizesFromAlanCc() {
        // wpthdr: uint32 id, int16 num, int16 next, int16 idx[1000], uint8 used[1000]
        assertEquals(3008, AlanWaypointsAndRoutesFormat.WPTHDR_SIZE);
        // wpt: char name[8], char comment[12], int32 x, y, date, time, int16 usecount, int8 checked, reserved
        assertEquals(40, AlanWaypointsAndRoutesFormat.WPT_SIZE);
        // rtehdr: uint32 id, int16 num, next, idx[50], uint8 used[50], int16 rteno
        assertEquals(160, AlanWaypointsAndRoutesFormat.RTEHDR_SIZE);
        // rte: char name[8], comment[12], int16 wptnum, wptidx[150], reserved, int32 date, time
        assertEquals(332, AlanWaypointsAndRoutesFormat.RTE_SIZE);
        assertEquals(SIZE, AlanWaypointsAndRoutesFormat.WPRDATA_SIZE);
        assertEquals(RTEHDR, AlanWaypointsAndRoutesFormat.RTEHDR_OFFSET);
        assertEquals(RTE, AlanWaypointsAndRoutesFormat.RTE_OFFSET);
    }

    @Test
    public void maximumPositionCountFromOldPreferenceNode() {
        Preferences preferences = Preferences.userRoot().node("slash/navigation/babel");
        String key = AlanWaypointsAndRoutesFormat.MAXIMUM_POSITION_COUNT_PREFERENCE;
        assertEquals("maximumAlanWaypointsAndRoutesPositionCount", key);
        String previous = preferences.get(key, null);
        try {
            preferences.remove(key);
            assertEquals(150, format.getMaximumPositionCount());
            preferences.putInt(key, 42);
            assertEquals(42, format.getMaximumPositionCount());
        } finally {
            if (previous != null)
                preferences.put(key, previous);
            else
                preferences.remove(key);
        }
    }

    @Test
    public void guardRejectsWrongLengthAndHeaderIds() throws IOException {
        byte[] valid = emptyFile().array();
        waypoint(ByteBuffer.wrap(valid).order(ByteOrder.LITTLE_ENDIAN), 0, 0, "A", "", 36000, -36000, date(2020, 1, 1), 0);
        assertEquals(1, read(valid).size());

        assertTrue(read(Arrays.copyOf(valid, SIZE - 1)).isEmpty());
        assertTrue(read(new byte[0]).isEmpty());

        byte[] wrongWaypointId = valid.clone();
        buffer(wrongWaypointId).putInt(0, 0x5C38A601);
        assertTrue(read(wrongWaypointId).isEmpty());

        byte[] wrongRouteId = valid.clone();
        buffer(wrongRouteId).putInt(RTEHDR, 0xD87F5901);
        assertTrue(read(wrongRouteId).isEmpty());
    }

    @Test
    public void readsLongerFileLikeGpsBabel() throws IOException {
        ByteBuffer buffer = emptyFile(0x10000);
        waypoint(buffer, 0, 0, "A", "", 36000, -36000, date(2020, 1, 1), 0);
        assertEquals(1, read(buffer.array()).size());
    }

    @Test
    public void readsWaypointsInIndexOrderAndSkipsUnused() throws IOException {
        ByteBuffer buffer = emptyFile();
        waypoint(buffer, 0, 2, "Second  ", "Comment two ", 2 * 36000, -2 * 36000, date(2020, 1, 1), 0);
        waypoint(buffer, 1, 0, "First", "            ", 36000, -36000, date(2020, 1, 1), 0);
        // header index 3 points to an unused slot
        buffer.putShort(8 + 2 * 3, (short) 5);
        // header index 4 points out of range
        buffer.putShort(8 + 2 * 4, (short) 1000);

        List<GpxRoute> routes = read(buffer.array());
        assertEquals(1, routes.size());
        GpxRoute waypoints = routes.get(0);
        assertEquals(Waypoints, waypoints.getCharacteristics());
        assertNull(waypoints.getName());
        assertEquals(2, waypoints.getPositionCount());

        GpxPosition second = waypoints.getPosition(0);
        assertEquals("Second; Comment two", second.getDescription());
        assertEquals(2.0, second.getLongitude(), 0.0);
        assertEquals(2.0, second.getLatitude(), 0.0);
        GpxPosition first = waypoints.getPosition(1);
        assertEquals("First", first.getDescription());
        assertEquals(1.0, first.getLongitude(), 0.0);
        assertEquals(1.0, first.getLatitude(), 0.0);
    }

    @Test
    public void readsLatitudeAsNegativeY() throws IOException {
        ByteBuffer buffer = emptyFile();
        waypoint(buffer, 0, 0, "A", "", -435363, -1722563, date(2020, 1, 1), 0);
        GpxPosition position = read(buffer.array()).get(0).getPosition(0);
        assertEquals(-12.093416667, position.getLongitude(), 0.0);
        assertEquals(47.848972222, position.getLatitude(), 0.0);
    }

    @Test
    public void readsTimeAndTreatsZeroAsMissing() throws IOException {
        ByteBuffer buffer = emptyFile();
        waypoint(buffer, 0, 0, "A", "", 0, 0, date(2006, 6, 5), 13 * 3600 + 29 * 60 + 25);
        waypoint(buffer, 1, 1, "B", "", 0, 0, date(1970, 1, 1), 0);
        GpxRoute waypoints = read(buffer.array()).get(0);
        assertEquals(LocalDateTime.of(2006, 6, 5, 13, 29, 25).toEpochSecond(UTC) * 1000,
                waypoints.getPosition(0).getTime().getTimeInMillis());
        assertNull(waypoints.getPosition(1).getTime());
    }

    private static long epoch(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0).toEpochSecond(UTC);
    }

    @Test
    public void unpackTimeWithLeapYears() {
        assertEquals(Long.valueOf(0), Map500.unpackTime(date(1970, 1, 1), 0));
        assertEquals(Long.valueOf(epoch(2000, 2, 29) + 61), Map500.unpackTime(date(2000, 2, 29), 61));
        assertEquals(Long.valueOf(epoch(2000, 3, 1)), Map500.unpackTime(date(2000, 3, 1), 0));
        assertEquals(Long.valueOf(epoch(1900, 3, 1)), Map500.unpackTime(date(1900, 3, 1), 0));
        assertEquals(Long.valueOf(epoch(2004, 2, 29)), Map500.unpackTime(date(2004, 2, 29), 0));
        assertEquals(Long.valueOf(epoch(2100, 3, 1)), Map500.unpackTime(date(2100, 3, 1), 0));
        assertEquals(Long.valueOf(epoch(2024, 12, 31) + 86399), Map500.unpackTime(date(2024, 12, 31), 86399));
        assertEquals(Long.valueOf(epoch(2025, 1, 1)), Map500.unpackTime(date(2025, 1, 1), 0));
        // month 0 and day 0 as the device writes them: the day before the first of December of the year before
        assertEquals(Long.valueOf(epoch(1999, 11, 30) + 72660), Map500.unpackTime(date(2000, 0, 0), 72660));
        // alan.cc reads behind its table for a month beyond December
        assertNull(Map500.unpackTime(date(2000, 14, 1), 0));
    }

    @Test
    public void packTime() {
        assertArrayEquals(new int[]{date(1970, 1, 1), 0}, Map500.packTime(0));
        assertArrayEquals(new int[]{date(2026, 3, 4), 5 * 3600 + 6 * 60 + 7}, Map500.packTime(NOW));
        assertArrayEquals(new int[]{date(2000, 2, 29), 3600}, Map500.packTime(epoch(2000, 2, 29) + 3600));
    }

    @Test
    public void readsRoutesThroughWaypointIndices() throws IOException {
        ByteBuffer buffer = emptyFile();
        waypoint(buffer, 0, 0, "A", "a", 36000, -36000, date(2020, 1, 1), 0);
        waypoint(buffer, 1, 1, "B", "b", 2 * 36000, -2 * 36000, date(2020, 1, 1), 0);
        waypoint(buffer, 2, 2, "C", "", 3 * 36000, -3 * 36000, date(2020, 1, 1), 0);
        // route header 1 points to route 0, header 0 to route 2
        route(buffer, 1, 0, "Route   ", "Description ", 2, 0, -1, 7);
        route(buffer, 0, 2, "R2", "", 1);
        // header 2 points to a route that is not used, header 3 out of range
        buffer.putShort(RTEHDR + 8 + 2 * 2, (short) 1);
        buffer.putShort(RTEHDR + 8 + 2 * 3, (short) 255);

        List<GpxRoute> routes = read(buffer.array());
        assertEquals(3, routes.size());
        assertEquals(Waypoints, routes.get(0).getCharacteristics());
        assertEquals(3, routes.get(0).getPositionCount());

        GpxRoute r2 = routes.get(1);
        assertEquals(Route, r2.getCharacteristics());
        assertEquals("R2", r2.getName());
        assertNull(r2.getDescription());
        assertEquals(1, r2.getPositionCount());
        assertEquals("B; b", r2.getPosition(0).getDescription());

        GpxRoute route = routes.get(2);
        assertEquals("Route", route.getName());
        assertEquals(List.of("Description"), route.getDescription());
        // -1 and the unused header index 7 are skipped
        assertEquals(2, route.getPositionCount());
        assertEquals("C", route.getPosition(0).getDescription());
        assertEquals("A; a", route.getPosition(1).getDescription());
    }

    @Test
    public void readsUtf8Labels() throws IOException {
        ByteBuffer buffer = emptyFile();
        waypoint(buffer, 0, 0, "Höhe", "Straße", 0, 0, date(2020, 1, 1), 0);
        assertEquals("Höhe; Straße", read(buffer.array()).get(0).getPosition(0).getDescription());
    }

    @Test
    public void str2lab() {
        assertEquals("Alpha   ", new String(Map500.str2lab("Alpha", 8, null, 0), US_ASCII));
        // only printable ASCII of the first len bytes of UTF-8 is copied
        assertEquals(" 1      ", new String(Map500.str2lab("Ä 1", 8, "W%05d", 3), US_ASCII));
        assertEquals("Gre     ", new String(Map500.str2lab("Größe & mehr", 8, "R%03d", 0), US_ASCII));
        assertEquals("Waypoint", new String(Map500.str2lab("Waypoint name", 8, null, 0), US_ASCII));
        assertEquals("ab          ", new String(Map500.str2lab("a\tb", 12, null, 0), US_ASCII));
        // fallback names when nothing printable is left
        assertEquals("W00003  ", new String(Map500.str2lab(null, 8, "W%05d", 3), US_ASCII));
        assertEquals("R007    ", new String(Map500.str2lab("äö", 8, "R%03d", 7), US_ASCII));
        assertEquals("        ", new String(Map500.str2lab("", 8, null, 0), US_ASCII));
    }

    private static void writtenWaypoint(ByteBuffer buffer, int idx, String name, String comment, int x, int y,
                                        int date, int time, int usecount, int checked) {
        waypoint(buffer, idx, idx, name, comment, x, y, date, time);
        buffer.putShort(WPT + 40 * idx + 36, (short) usecount);
        buffer.put(WPT + 40 * idx + 38, (byte) checked);
    }

    @Test
    public void writesExactBytes() throws IOException {
        long time = LocalDateTime.of(2026, 1, 1, 10, 0).toEpochSecond(UTC);
        byte[] actual = write(route("Tour",
                position(11.5, 48.1, time, "Alpha; first"),
                position(-0.25, -33.5, null, "Beta")));

        ByteBuffer expected = emptyFile();
        expected.putShort(4, (short) 2);
        expected.putShort(6, (short) 2);
        writtenWaypoint(expected, 0, "Alpha   ", "first       ", 414000, -1731600, date(2026, 1, 1), 36000, 1, 1);
        writtenWaypoint(expected, 1, "Beta    ", "Beta        ", -9000, 1206000, date(1970, 1, 1), 0, 1, 1);
        expected.putShort(RTEHDR + 4, (short) 1);
        expected.putShort(RTEHDR + 6, (short) 1);
        route(expected, 0, 0, "Tour    ", "            ", 0, 1);
        expected.putInt(RTE + 324, date(2026, 3, 4));
        expected.putInt(RTE + 328, 5 * 3600 + 6 * 60 + 7);

        assertEquals(SIZE, actual.length);
        assertArrayEquals(expected.array(), actual);
    }

    @Test
    public void writesFallbackNames() throws IOException {
        byte[] data = write(new GpxRoute(new Gpx10Format(), Route, null, null,
                new ArrayList<>(List.of(position(1.0, 2.0, null, null)))));
        ByteBuffer buffer = buffer(data);
        // GPSBabel's waypt_add() names the waypoint after the number of waypoints and uses the name as description
        assertEquals("WPT001  ", label(data, WPT, 8));
        assertEquals("WPT001      ", label(data, WPT + 8, 12));
        assertEquals(0, buffer.getShort(WPT + 36));
        assertEquals(1, buffer.get(WPT + 38));
        // route_add_wpt() names the route point after the number of route points and gives it no description,
        // so find_wpt() finds no duplicate and the route adds a second waypoint
        assertEquals("RPT001  ", label(data, WPT + 40, 8));
        assertEquals("            ", label(data, WPT + 40 + 8, 12));
        assertEquals(1, buffer.getShort(WPT + 40 + 36));
        assertEquals(0, buffer.get(WPT + 40 + 38));
        assertEquals("R000    ", label(data, RTE, 8));
        assertEquals(1, buffer.getShort(RTE + 22));
    }

    @Test
    public void countsWaypointsAndRoutePointsForFallbackNames() throws IOException {
        GpxRoute waypoints = new GpxRoute(new Gpx10Format(), Waypoints, null, null, new ArrayList<>(List.of(
                position(1.0, 1.0, null, "Named"),
                new GpxPosition((Double) null, null, null, null, null, null),
                position(1.0, 1.0, null, "Named"),
                position(2.0, 2.0, null, null),
                position(3.0, 3.0, null, "; notes only"))));
        GpxRoute first = route("First", position(4.0, 4.0, null, null));
        GpxRoute second = route("Second", position(5.0, 5.0, null, null), position(6.0, 6.0, null, "Name; notes"));
        byte[] data = write(asList(waypoints, first, second));

        // the duplicate counts, the position without coordinates does not
        assertEquals("Named   ", label(data, WPT, 8));
        assertEquals("WPT003  ", label(data, WPT + 40, 8));
        assertEquals("WPT003      ", label(data, WPT + 40 + 8, 12));
        // the notes name a waypoint without name
        assertEquals("notes on", label(data, WPT + 2 * 40, 8));
        assertEquals("notes only  ", label(data, WPT + 2 * 40 + 8, 12));
        // route points are counted across routes and have no description
        assertEquals("RPT001  ", label(data, WPT + 3 * 40, 8));
        assertEquals("RPT002  ", label(data, WPT + 4 * 40, 8));
        assertEquals("Name    ", label(data, WPT + 5 * 40, 8));
        assertEquals("            ", label(data, WPT + 5 * 40 + 8, 12));
    }

    @Test
    public void writesStr2labFallbackForRoutePointWithoutPrintableName() throws IOException {
        byte[] data = write(route("R", position(1.0, 2.0, null, "äö; notes")));
        // the waypoint keeps its notes as description, the route point matches nothing and has none
        assertEquals("W00000  ", label(data, WPT, 8));
        assertEquals("notes       ", label(data, WPT + 8, 12));
        assertEquals("W00001  ", label(data, WPT + 40, 8));
        assertEquals("            ", label(data, WPT + 40 + 8, 12));
    }

    @Test
    public void keepsTimesBefore1970() throws IOException {
        // GPSBabel 1.5.4 writes 2106-02-07T06:28:15Z for these and reads times before 1970 as ones after it
        long time = LocalDateTime.of(1969, 7, 8, 9, 10, 11).toEpochSecond(UTC);
        byte[] data = write(route("Old", position(1.0, 2.0, time, "Old")));
        assertEquals(date(1969, 7, 8), buffer(data).getInt(WPT + 28));
        assertEquals(9 * 3600 + 10 * 60 + 11, buffer(data).getInt(WPT + 32));
        assertEquals(time * 1000, read(data).get(0).getPosition(0).getTime().getTimeInMillis());
        assertEquals(Long.valueOf(time), Map500.unpackTime(date(1969, 7, 8), 9 * 3600 + 10 * 60 + 11));
    }

    @Test
    public void writesDuplicateWaypointsOnce() throws IOException {
        byte[] data = write(route("Loop",
                position(1.0, 2.0, null, "Same"),
                position(1.0, 2.0, null, "Same")));
        ByteBuffer buffer = buffer(data);
        assertEquals(1, buffer.getShort(4));
        assertEquals(2, buffer.getShort(WPT + 36));
        assertEquals(1, buffer.get(WPT + 38));
        assertEquals(2, buffer.getShort(RTE + 20));
        assertEquals(0, buffer.getShort(RTE + 22));
        assertEquals(0, buffer.getShort(RTE + 24));
    }

    @Test
    public void writesWaypointsAndRoutesOfMultipleRoutes() throws IOException {
        GpxRoute waypoints = new GpxRoute(new Gpx10Format(), Waypoints, null, null,
                new ArrayList<>(List.of(position(1.0, 1.0, null, "One"))));
        GpxRoute first = route("First", position(2.0, 2.0, null, "Two"), position(1.0, 1.0, null, "One"));
        GpxRoute second = route("Second", position(3.0, 3.0, null, "Three"));
        byte[] data = write(asList(first, waypoints, second));

        List<GpxRoute> routes = read(data);
        assertEquals(3, routes.size());
        assertEquals(3, routes.get(0).getPositionCount());
        assertEquals("One", routes.get(0).getPosition(0).getDescription());
        assertEquals("First", routes.get(1).getName());
        assertEquals(2, routes.get(1).getPositionCount());
        assertEquals("Second", routes.get(2).getName());
        // the waypoint list comes first, so "One" is a duplicate of it
        assertEquals(1, buffer(data).get(WPT + 38));
        assertEquals(1, buffer(data).getShort(WPT + 36));
    }

    @Test
    public void roundtrip() throws IOException {
        long time = LocalDateTime.of(2024, 2, 29, 23, 59, 58).toEpochSecond(UTC);
        List<GpxRoute> routes = read(write(route("Tour", position(10.5, 53.5, time, "Start; here"),
                position(-10.25, -53.25, null, "End"))));
        assertEquals(2, routes.size());
        GpxRoute route = routes.get(1);
        assertEquals("Tour", route.getName());
        assertEquals("Start; here", route.getPosition(0).getDescription());
        assertEquals(time * 1000, route.getPosition(0).getTime().getTimeInMillis());
        assertEquals(10.5, route.getPosition(0).getLongitude(), 0.0);
        assertEquals(53.5, route.getPosition(0).getLatitude(), 0.0);
        assertEquals("End", route.getPosition(1).getDescription());
        assertNull(route.getPosition(1).getTime());
        assertEquals(-53.25, route.getPosition(1).getLatitude(), 0.0);
    }

    private static List<GpxPosition> distinctPositions(int count) {
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 0; i < count; i++)
            positions.add(position(i / 100.0, 1.0, null, "P" + i));
        return positions;
    }

    private static void assertWriteFails(String message, ThrowingWrite write) {
        try {
            write.run();
            fail("IOException expected");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(message));
        }
    }

    private interface ThrowingWrite {
        void run() throws IOException;
    }

    @Test
    public void limits() throws IOException {
        write(new GpxRoute(new Gpx10Format(), Route, "R", null, distinctPositions(150)));
        assertWriteFails("150 waypoints per route",
                () -> write(new GpxRoute(new Gpx10Format(), Route, "R", null, distinctPositions(151))));

        write(List.of(new GpxRoute(new Gpx10Format(), Waypoints, null, null, distinctPositions(1000))));
        assertWriteFails("1000 waypoints",
                () -> write(List.of(new GpxRoute(new Gpx10Format(), Waypoints, null, null, distinctPositions(1001)))));

        List<GpxRoute> routes = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            routes.add(route("R" + i, position(1.0, 1.0, null, "Same")));
        write(routes);
        routes.add(route("R50", position(1.0, 1.0, null, "Same")));
        assertWriteFails("50 routes", () -> write(routes));
    }
}

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;

public class AlanTrackLogFormatTest {
    private final AlanTrackLogFormat format = new AlanTrackLogFormat();

    // offsets computed by hand from alan.cc trl_write(), independent of the constants of the format
    private static final int FILE_SIZE = 266240;
    private static final int LOGHDR = 0x40000;
    private static final int TRKLOG_SIZE = 30000;

    private static int trackLog(int track) {
        return 0x10000 * (track / 2) + TRKLOG_SIZE * (track % 2);
    }

    private static int trackHeader(int track) {
        return LOGHDR + 16 + 44 * track;
    }

    /** A trldata file as trl_write() writes it without tracks. */
    private static ByteBuffer emptyFile(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0xff);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(LOGHDR, 0x38CB1200);
        buffer.putShort(LOGHDR + 4, (short) -1);
        buffer.putShort(LOGHDR + 6, (short) -1);
        buffer.putInt(LOGHDR + 8, 0);
        buffer.putInt(LOGHDR + 12, 0);
        for (int i = 0; i < 8; i++) {
            int offset = trackHeader(i);
            buffer.putShort(offset, (short) 0);
            buffer.putShort(offset + 2, (short) 0);
            for (int j = 0; j < 12; j++)
                buffer.put(offset + 4 + j, (byte) 0);
            for (int j = 0; j < 12; j++)
                buffer.put(offset + 16 + j, (byte) ' ');
            buffer.put(offset + 28, (byte) 0);
            for (int j = 0; j < 3; j++)
                buffer.put(offset + 29 + j, (byte) 0);
            buffer.putInt(offset + 32, 0);
            buffer.putInt(offset + 36, 0);
            buffer.putInt(offset + 40, 1);
        }
        return buffer;
    }

    private static ByteBuffer emptyFile() {
        return emptyFile(FILE_SIZE);
    }

    private static void header(ByteBuffer buffer, int track, int totalpt, byte[] name, byte[] comment) {
        int offset = trackHeader(track);
        buffer.putShort(offset, (short) totalpt);
        buffer.putShort(offset + 2, (short) totalpt);
        for (int j = 0; j < name.length; j++)
            buffer.put(offset + 4 + j, name[j]);
        for (int j = 0; j < comment.length; j++)
            buffer.put(offset + 16 + j, comment[j]);
        buffer.putInt(offset + 32, 1);
    }

    private static void point(ByteBuffer buffer, int track, int index, int x, int y, int speed, int height) {
        int offset = trackLog(track);
        buffer.putInt(offset + 8 * index, x);
        buffer.putInt(offset + 8 * index + 4, y);
        buffer.putShort(offset + 20000 + 4 * index, (short) speed);
        buffer.putShort(offset + 20000 + 4 * index + 2, (short) height);
    }

    private static byte[] bytes(String string) {
        return string.getBytes(ISO_8859_1);
    }

    private List<GpxRoute> read(byte[] data) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(data), context);
        return context.getRoutes();
    }

    private byte[] write(GpxRoute route) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, route.getPositionCount());
        return target.toByteArray();
    }

    private static GpxPosition position(double longitude, double latitude, Double elevation, Double speed) {
        return new GpxPosition(longitude, latitude, elevation, speed, null, null);
    }

    private static GpxRoute track(String name, List<String> description, GpxPosition... positions) {
        return new GpxRoute(new Gpx10Format(), Track, name, description, new ArrayList<>(asList(positions)));
    }

    @Test
    public void nameAndExtension() {
        assertEquals("Alan Map 500 Tracklog (*.trl)", format.getName());
        assertEquals(".trl", format.getExtension());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
        assertFalse(format instanceof MultipleRoutesFormat);
        assertEquals(Integer.MAX_VALUE, format.getMaximumPositionCount());
    }

    @Test
    public void structSizesFromAlanCc() {
        // trkhdr: int16 totalpt, next, char name[12], comment[13], uint8 reserved[3], uint32 occupied, show, fill
        assertEquals(44, AlanTrackLogFormat.TRKHDR_SIZE);
        // loghdr: uint32 id, int16 num, next, int32 date, time, trkhdr[8]
        assertEquals(368, AlanTrackLogFormat.LOGHDR_SIZE);
        // trklog: {int32 x, y} pt[2500], {int16 speed, height} sh[2500]
        assertEquals(TRKLOG_SIZE, AlanTrackLogFormat.TRKLOG_SIZE);
        assertEquals(LOGHDR, AlanTrackLogFormat.LOGHDR_OFFSET);
        assertEquals(FILE_SIZE, AlanTrackLogFormat.TRLDATA_FILE_SIZE);
    }

    @Test
    public void guardRejectsWrongLengthAndHeaderId() throws IOException {
        ByteBuffer buffer = emptyFile();
        header(buffer, 0, 1, bytes("T"), new byte[0]);
        point(buffer, 0, 0, 36000, -36000, 0, 0);
        byte[] valid = buffer.array();
        assertEquals(1, read(valid).size());

        // trl_read() needs the tracklogs and the log header only
        assertEquals(1, read(Arrays.copyOf(valid, LOGHDR + 368)).size());
        assertTrue(read(Arrays.copyOf(valid, LOGHDR + 367)).isEmpty());
        assertTrue(read(new byte[0]).isEmpty());

        byte[] wrongId = valid.clone();
        ByteBuffer.wrap(wrongId).order(ByteOrder.LITTLE_ENDIAN).putInt(LOGHDR, 0x38CB1201);
        assertTrue(read(wrongId).isEmpty());
    }

    @Test
    public void readsOccupiedTracksOnly() throws IOException {
        ByteBuffer buffer = emptyFile();
        header(buffer, 1, 2, bytes("Tour\0xyz   "), bytes("Comment     "));
        point(buffer, 1, 0, 435363, -1722563, 2204, -7001);
        point(buffer, 1, 1, -36000, 18000, -1, 0);
        // occupied without points
        header(buffer, 4, 0, bytes("Empty"), new byte[0]);
        // points without being occupied
        point(buffer, 6, 0, 36000, 36000, 0, 0);
        buffer.putShort(trackHeader(6), (short) 1);
        header(buffer, 7, 1, new byte[0], new byte[0]);
        point(buffer, 7, 0, 0, 0, 0, 0);

        List<GpxRoute> routes = read(buffer.array());
        assertEquals(2, routes.size());

        GpxRoute track = routes.get(0);
        assertEquals(Track, track.getCharacteristics());
        assertEquals("Tour", track.getName());
        assertEquals(List.of("Comment"), track.getDescription());
        assertEquals(2, track.getPositionCount());

        GpxPosition first = track.getPosition(0);
        assertEquals(12.093416667, first.getLongitude(), 0.0);
        assertEquals(47.848972222, first.getLatitude(), 0.0);
        assertEquals(-1400.2, first.getElevation(), 0.0);
        // 2204 / 720 m/s as GPSBabel writes it to GPX with %.6f, in km/h
        assertEquals(3.061111 * 3.6, first.getSpeed(), 1e-9);
        assertNull(first.getTime());
        assertNull(first.getDescription());

        GpxPosition second = track.getPosition(1);
        assertEquals(-1.0, second.getLongitude(), 0.0);
        assertEquals(-0.5, second.getLatitude(), 0.0);
        assertEquals(0.0, second.getElevation(), 0.0);
        // a negative speed is a bad speed
        assertNull(second.getSpeed());

        GpxRoute unnamed = routes.get(1);
        assertNull(unnamed.getName());
        assertNull(unnamed.getDescription());
    }

    @Test
    public void readsAtMostMaximumPointsPerTrack() throws IOException {
        ByteBuffer buffer = emptyFile();
        header(buffer, 0, 3000, bytes("Long"), new byte[0]);
        assertEquals(2500, read(buffer.array()).get(0).getPositionCount());
    }

    @Test
    public void readsIso88591TrackNames() throws IOException {
        ByteBuffer buffer = emptyFile();
        header(buffer, 0, 1, bytes("Größe"), new byte[0]);
        point(buffer, 0, 0, 0, 0, 0, 0);
        assertEquals("Größe", read(buffer.array()).get(0).getName());
    }

    @Test
    public void writesExactBytes() throws IOException {
        byte[] actual = write(track("Größe & <Test> \"q\"", List.of("Description"),
                position(11.5, 48.1, 1.5, 20.52),
                position(-0.25, -33.5, 2.5, null),
                position(0.0, 0.0, null, 0.0)));

        ByteBuffer expected = emptyFile();
        header(expected, 0, 3, bytes("Größe & <Te"), bytes("Description "));
        // name[11] and comment[12] stay the terminators
        expected.put(trackHeader(0) + 4 + 11, (byte) 0);
        // 20.52 km/h are 5.7 m/s
        point(expected, 0, 0, 414000, -1731600, 4104, 8);
        point(expected, 0, 1, -9000, 1206000, -1, 13);
        point(expected, 0, 2, 0, 0, 0, -1);

        assertEquals(FILE_SIZE, actual.length);
        assertArrayEquals(expected.array(), actual);
    }

    @Test
    public void writesFallbackNameAndPadsComment() throws IOException {
        byte[] data = write(track(null, List.of("ab"), position(1.0, 1.0, null, null)));
        assertEquals("T000", new String(data, trackHeader(0) + 4, 4, ISO_8859_1));
        assertEquals(0, data[trackHeader(0) + 4 + 4]);
        assertEquals("ab          \0", new String(data, trackHeader(0) + 16, 13, ISO_8859_1));

        data = write(track("N", List.of("A long description"), position(1.0, 1.0, null, null)));
        assertEquals("A long descr\0", new String(data, trackHeader(0) + 16, 13, ISO_8859_1));
    }

    @Test
    public void writesOneQuestionMarkPerUtf16Unit() throws IOException {
        // like Qt's QString::toLatin1(): a character beyond the BMP is two UTF-16 units
        byte[] data = write(track("\uD83D\uDE00Tour", List.of("\u20ac \uD83D\uDE00"), position(1.0, 1.0, null, null)));
        assertEquals("??Tour", new String(data, trackHeader(0) + 4, 6, ISO_8859_1));
        assertEquals(0, data[trackHeader(0) + 4 + 6]);
        assertEquals("? ??        \0", new String(data, trackHeader(0) + 16, 13, ISO_8859_1));
    }

    @Test
    public void writesTrackWithoutPositionsAsUnoccupied() throws IOException {
        byte[] data = write(track("Empty", null));
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, buffer.getInt(trackHeader(0) + 32));
        assertTrue(read(data).isEmpty());
    }

    @Test
    public void roundtrip() throws IOException {
        List<GpxRoute> routes = read(write(new GpxRoute(new Gpx10Format(), Route, "Tour", null, new ArrayList<>(List.of(
                new GpxPosition(10.5, 53.5, 100.0, 36.0, fromMillis(1000000000000L), "ignored"),
                position(-10.25, -53.25, null, null))))));
        assertEquals(1, routes.size());
        GpxRoute track = routes.get(0);
        assertEquals(Track, track.getCharacteristics());
        assertEquals("Tour", track.getName());
        assertEquals(10.5, track.getPosition(0).getLongitude(), 0.0);
        assertEquals(53.5, track.getPosition(0).getLatitude(), 0.0);
        assertEquals(100.0, track.getPosition(0).getElevation(), 0.0);
        assertEquals(36.0, track.getPosition(0).getSpeed(), 1e-9);
        assertNull(track.getPosition(0).getTime());
        // an unknown elevation is written as 0xffff, which reads as -0.2 m
        assertEquals(-0.2, track.getPosition(1).getElevation(), 0.0);
        assertNull(track.getPosition(1).getSpeed());
    }

    @Test
    public void limits() throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 0; i < 2500; i++)
            positions.add(position(i / 1000.0, 1.0, null, null));
        write(new GpxRoute(new Gpx10Format(), Track, "T", null, positions));
        positions.add(position(3.0, 1.0, null, null));
        try {
            write(new GpxRoute(new Gpx10Format(), Track, "T", null, positions));
            fail("IOException expected");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("2500 points per track"));
        }
    }
}

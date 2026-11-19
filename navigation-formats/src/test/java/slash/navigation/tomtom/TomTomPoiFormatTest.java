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

package slash.navigation.tomtom;

import org.junit.Test;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class TomTomPoiFormatTest {
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private final TomTomPoiFormat format = new TomTomPoiFormat();

    // building blocks

    private static class Bytes {
        private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        Bytes u8(int value) {
            stream.write(value);
            return this;
        }

        Bytes i32(long value) {
            byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) value).array();
            stream.write(bytes, 0, 4);
            return this;
        }

        Bytes raw(byte[] bytes) {
            stream.write(bytes, 0, bytes.length);
            return this;
        }

        Bytes header(long size, long maxLon, long maxLat, long minLon, long minLat) {
            return u8(1).i32(size).i32(maxLon).i32(maxLat).i32(minLon).i32(minLat);
        }

        Bytes poi(int type, long lon, long lat, String text) {
            byte[] bytes = text.getBytes(CP1252);
            return u8(type).i32(bytes.length + 14).i32(lon).i32(lat).raw(bytes).u8(0);
        }

        Bytes poi(long lon, long lat, String text) {
            return poi(2, lon, lat, text);
        }

        byte[] get() {
            return stream.toByteArray();
        }
    }

    private List<GpxPosition> read(byte[] data) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(data), context);
        if (context.getRoutes().isEmpty())
            return null;
        assertEquals(1, context.getRoutes().size());
        GpxRoute route = context.getRoutes().get(0);
        assertEquals(Waypoints, route.getCharacteristics());
        return route.getPositions();
    }

    private byte[] write(List<GpxPosition> positions) throws IOException {
        GpxRoute route = format.createRoute(Waypoints, "route", positions);
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, positions.size());
        return target.toByteArray();
    }

    private static GpxPosition position(double lon, double lat, String description) {
        return new GpxPosition(lon, lat, null, null, null, description);
    }

    // reading

    @Test
    public void readsPoiRecordsOfType2() throws IOException {
        byte[] data = new Bytes().header(60, 1100000, 5100000, 1000000, 5000000)
                .poi(1000000, 5000000, "First").poi(1100000, 5100000, "Second").get();
        List<GpxPosition> positions = read(data);
        assertEquals(2, positions.size());
        assertEquals(10.0, positions.get(0).getLongitude(), 0.0);
        assertEquals(50.0, positions.get(0).getLatitude(), 0.0);
        assertEquals("First", positions.get(0).getDescription());
        assertEquals(11.0, positions.get(1).getLongitude(), 0.0);
        assertEquals(51.0, positions.get(1).getLatitude(), 0.0);
        assertEquals("Second", positions.get(1).getDescription());
    }

    @Test
    public void readsNegativeCoordinates() throws IOException {
        List<GpxPosition> positions = read(new Bytes().poi(-912345, -3376543, "West").get());
        assertEquals(-9.12345, positions.get(0).getLongitude(), 0.0);
        assertEquals(-33.76543, positions.get(0).getLatitude(), 0.0);
    }

    @Test
    public void readsType3LikeType2AndIgnoresStringsAfterTheNul() throws IOException {
        byte[] data = new Bytes().u8(3).i32(13 + 5 + 3 + 4).i32(1000000).i32(5000000)
                .raw("Name\0".getBytes(CP1252)).raw("ID\0".getBytes(CP1252)).raw("more".getBytes(CP1252)).get();
        List<GpxPosition> positions = read(data);
        assertEquals(1, positions.size());
        assertEquals("Name", positions.get(0).getDescription());
    }

    @Test
    public void skipsDeletedRecordsOfType0And100() throws IOException {
        byte[] data = new Bytes()
                .u8(0).i32(5 + 7).raw(new byte[7])
                .poi(1000000, 5000000, "Kept")
                .u8(100).i32(5).get();
        List<GpxPosition> positions = read(new Bytes().raw(data).poi(1100000, 5100000, "Also").get());
        assertEquals(2, positions.size());
        assertEquals("Kept", positions.get(0).getDescription());
        assertEquals("Also", positions.get(1).getDescription());
    }

    @Test
    public void skipsRecordsOfType8And9And24And25() throws IOException {
        byte[] data = new Bytes().header(21, 0, 0, 0, 0)
                .u8(9).u8(3).raw(new byte[9])
                .u8(25).u8(0).raw(new byte[6])
                .poi(1000000, 5000000, "Kept")
                .u8(8).u8(2).raw(new byte[8])
                .u8(24).u8(1).raw(new byte[7])
                .poi(1100000, 5100000, "Also").get();
        List<GpxPosition> positions = read(data);
        assertEquals(2, positions.size());
        assertEquals("Kept", positions.get(0).getDescription());
        assertEquals("Also", positions.get(1).getDescription());
    }

    @Test
    public void greaterSignStaysPartOfTheDescription() throws IOException {
        List<GpxPosition> positions = read(new Bytes().poi(1000000, 5000000, "Pizza Roma>0123 456").get());
        assertEquals("Pizza Roma>0123 456", positions.get(0).getDescription());
    }

    @Test
    public void decodesWindows1252() throws IOException {
        List<GpxPosition> positions = read(new Bytes().poi(1000000, 5000000, "Müller Äß €").get());
        assertEquals("Müller Äß €", positions.get(0).getDescription());
    }

    @Test
    public void stopsAtUnknownRecordTypeKeepingThePoisRead() throws IOException {
        byte[] data = new Bytes().poi(1000000, 5000000, "Kept").u8(77).u8(1).u8(2).get();
        List<GpxPosition> positions = read(data);
        assertEquals(1, positions.size());
        assertEquals("Kept", positions.get(0).getDescription());
    }

    @Test
    public void rejectsUnknownFirstRecordType() throws IOException {
        assertNull(read(new Bytes().u8(9).u8(0).raw(new byte[6]).poi(1000000, 5000000, "X").get()));
        assertNull(read("some text file\n".getBytes(CP1252)));
    }

    @Test
    public void rejectsEmptyFileAndFileWithoutPois() throws IOException {
        assertNull(read(new byte[0]));
        assertNull(read(new Bytes().header(21, 0, 0, 0, 0).get()));
    }

    @Test
    public void rejectsBadRecordSize() throws IOException {
        assertNull(read(new Bytes().poi(1000000, 5000000, "Kept").u8(2).i32(100001).i32(0).i32(0).get()));
        assertNull(read(new Bytes().poi(1000000, 5000000, "Kept").u8(2).i32(-1).i32(0).i32(0).get()));
        assertNull(read(new Bytes().poi(1000000, 5000000, "Kept").u8(2).i32(12).i32(0).i32(0).get()));
        assertNull(read(new Bytes().u8(0).i32(4).get()));
        assertNull(read(new Bytes().u8(100).i32(100006).get()));
    }

    @Test
    public void rejectsTruncatedFile() throws IOException {
        byte[] data = new Bytes().poi(1000000, 5000000, "Kept").poi(1100000, 5100000, "Truncated").get();
        for (int length : new int[]{data.length - 1, data.length - 5, 20 + 1 + 3})
            assertNull("length " + length, read(java.util.Arrays.copyOf(data, length)));
        assertNull(read(new Bytes().header(21, 0, 0, 0, 0).u8(9).u8(10).raw(new byte[5]).get()));
        assertNull(read(new Bytes().u8(0).i32(50).raw(new byte[10]).get()));
    }

    @Test
    public void rejectsOutOfRangeCoordinates() throws IOException {
        assertNull(read(new Bytes().poi(18000001, 5000000, "X").get()));
        assertNull(read(new Bytes().poi(-18000001, 5000000, "X").get()));
        assertNull(read(new Bytes().poi(1000000, 9000001, "X").get()));
        assertNull(read(new Bytes().poi(1000000, -9000001, "X").get()));
        assertEquals(1, read(new Bytes().poi(18000000, -9000000, "X").get()).size());
    }

    // writing

    @Test
    public void writeFloatAsLongTruncatesTowardZero() {
        assertEquals(100000L, TomTomPoiFormat.writeFloatAsLong(100000.0));
        assertEquals(100000L, TomTomPoiFormat.writeFloatAsLong(99999.7));
        assertEquals(99999L, TomTomPoiFormat.writeFloatAsLong(99999.4));
        assertEquals(0L, TomTomPoiFormat.writeFloatAsLong(-1.0000001));
        assertEquals(0L, TomTomPoiFormat.writeFloatAsLong(-0.7));
        assertEquals(-99999L, TomTomPoiFormat.writeFloatAsLong(-100000.4));
        // -100000.0 + 0.5 = -99999.5 truncates to -99999: negative integral values are one off
        assertEquals(-99999L, TomTomPoiFormat.writeFloatAsLong(-100000.0));
        assertEquals(-1L, TomTomPoiFormat.writeFloatAsLong(-1.6));
    }

    @Test
    public void writesOnePoi() throws IOException {
        byte[] expected = new Bytes().header(21 + 13 + 7, 1000000, 5000000, 1000000, 5000000)
                .poi(1000000, 5000000, "Berlin").get();
        assertArrayEquals(expected, write(List.of(position(10.0, 50.0, "Berlin"))));
        // header 1 + 5 * 4, record 1 + 4 + 4 + 4 + 6 + 1
        assertEquals(1 + 20 + 1 + 4 + 8 + 7, expected.length);
    }

    @Test
    public void writesNoPoiAsBoundingBoxOfNothing() throws IOException {
        byte[] expected = new Bytes().header(21, -19999999, -19999999, 20000000, 20000000).get();
        assertArrayEquals(expected, write(new ArrayList<>()));
    }

    @Test
    public void writes19PoisInOneBlockInInputOrder() throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 0; i < 19; i++)
            positions.add(position(10.0, 51.8 - i * 0.1, String.format("P%02d", i)));
        // minimum latitude is the last position, maximum the first
        Bytes expected = new Bytes().header(21 + 19 * 17, 1000000, 5180000, 1000000, 5000000);
        for (int i = 0; i < 19; i++)
            expected.poi(1000000, 5180000 - i * 10000, String.format("P%02d", i));
        assertArrayEquals(expected.get(), write(positions));
    }

    @Test
    public void writes20PoisSplitAlongLatitudeSortedAscending() throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 19; i >= 0; i--)
            positions.add(position(10.0, 50.0 + i * 0.1, String.format("P%02d", i)));

        Bytes expected = new Bytes().header(21 + 2 * 191, 1000000, 5190000, 1000000, 5000000)
                .header(191, 1000000, 5100000, 1000000, 5000000);
        for (int i = 0; i < 10; i++)
            expected.poi(1000000, 5000000 + i * 10000, String.format("P%02d", i));
        expected.header(191, 1000000, 5190000, 1000000, 5100000);
        for (int i = 10; i < 20; i++)
            expected.poi(1000000, 5000000 + i * 10000, String.format("P%02d", i));
        assertArrayEquals(expected.get(), write(positions));
    }

    @Test
    public void writes20PoisSplitAlongLongitudeWhenLongitudeExtentIsNotSmaller() throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        for (int i = 19; i >= 0; i--)
            positions.add(position(8.0 + i * 0.1, 48.0, String.format("P%02d", i)));

        Bytes expected = new Bytes().header(21 + 2 * 191, 990000, 4800000, 800000, 4800000)
                .header(191, 900000, 4800000, 800000, 4800000);
        for (int i = 0; i < 10; i++)
            expected.poi(800000 + i * 10000, 4800000, String.format("P%02d", i));
        expected.header(191, 990000, 4800000, 900000, 4800000);
        for (int i = 10; i < 20; i++)
            expected.poi(800000 + i * 10000, 4800000, String.format("P%02d", i));
        assertArrayEquals(expected.get(), write(positions));
    }

    @Test
    public void writes45PoisSplitRecursivelyInHalves() throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        // all on one meridian, latitude 50.00 .. 50.44, given in descending order
        for (int i = 44; i >= 0; i--)
            positions.add(position(10.0, 50.0 + i * 0.01, String.format("P%02d", i)));

        Bytes expected = new Bytes().header(21 + 437 + 454, 1000000, 5044000, 1000000, 5000000);
        // 22 POIs: 11 + 11
        expected.header(437, 1000000, 5022000, 1000000, 5000000);
        expected.header(208, 1000000, 5011000, 1000000, 5000000);
        for (int i = 0; i < 11; i++)
            expected.poi(1000000, 5000000 + i * 1000, String.format("P%02d", i));
        expected.header(208, 1000000, 5022000, 1000000, 5011000);
        for (int i = 11; i < 22; i++)
            expected.poi(1000000, 5000000 + i * 1000, String.format("P%02d", i));
        // 23 POIs: 11 + 12
        expected.header(454, 1000000, 5044000, 1000000, 5022000);
        expected.header(208, 1000000, 5033000, 1000000, 5022000);
        for (int i = 22; i < 33; i++)
            expected.poi(1000000, 5000000 + i * 1000, String.format("P%02d", i));
        expected.header(225, 1000000, 5044000, 1000000, 5033000);
        for (int i = 33; i < 45; i++)
            expected.poi(1000000, 5000000 + i * 1000, String.format("P%02d", i));
        assertArrayEquals(expected.get(), write(positions));
    }

    @Test
    public void ordersTiesByTheOtherAxisAndThenByText() throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        // same latitude (the extent in longitude is larger: sorted by longitude), same longitude: text decides
        for (int i = 0; i < 10; i++) {
            positions.add(position(10.0 + i * 0.1, 50.0, "b" + i));
            positions.add(position(10.0 + i * 0.1, 50.0, "a" + i));
        }
        byte[] written = write(positions);
        List<GpxPosition> read = read(written);
        assertEquals(20, read.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("a" + i, read.get(2 * i).getDescription());
            assertEquals("b" + i, read.get(2 * i + 1).getDescription());
        }
    }

    @Test
    public void roundsNegativeCoordinatesTowardZero() throws IOException {
        // -1.0000001 * 100000 = -100000.01 + 0.5 = -99999.51 truncates to -99999; 51.0 is not affected
        byte[] expected = new Bytes().header(21 + 13 + 2, -99999, 5100000, -99999, 5100000)
                .poi(-99999, 5100000, "W").get();
        assertArrayEquals(expected, write(List.of(position(-1.0000001, 51.0, "W"))));

        // -1.0 * 100000 + 0.5 truncates to 0 only below half: -0.000004 * 100000 = -0.4 + 0.5 = 0.1 -> 0
        byte[] tiny = new Bytes().header(21 + 13 + 2, 0, 5100000, 0, 5100000).poi(0, 5100000, "W").get();
        assertArrayEquals(tiny, write(List.of(position(-0.000004, 51.0, "W"))));
    }

    @Test
    public void cutsDescriptionAt255BytesButCountsAllCharactersInTheBlockSize() throws IOException {
        String description = "a".repeat(300);
        byte[] written = write(List.of(position(10.0, 50.0, description)));
        byte[] expected = new Bytes().header(21 + 13 + 301, 1000000, 5000000, 1000000, 5000000)
                .poi(1000000, 5000000, "a".repeat(255)).get();
        assertArrayEquals(expected, written);
    }

    @Test
    public void writesWindows1252() throws IOException {
        byte[] written = write(List.of(position(10.0, 50.0, "Müller Äß €")));
        // the block size counts the 11 characters and the NUL
        byte[] expected = new Bytes().header(21 + 13 + 11 + 1, 1000000, 5000000, 1000000, 5000000)
                .u8(2).i32(11 + 14).i32(1000000).i32(5000000)
                .raw(new byte[]{'M', (byte) 0xFC, 'l', 'l', 'e', 'r', ' ', (byte) 0xC4, (byte) 0xDF, ' ', (byte) 0x80}).u8(0).get();
        assertArrayEquals(expected, written);
    }

    @Test
    public void writtenFileCanBeReadAgain() throws IOException {
        List<GpxPosition> positions = new ArrayList<>();
        positions.add(position(-9.5, 38.7, "Lisboa"));
        positions.add(position(13.4, 52.5, "Berlin"));
        positions.add(position(2.35, 48.85, null));
        List<GpxPosition> read = read(write(positions));
        assertEquals(3, read.size());
        assertEquals("Lisboa", read.get(0).getDescription());
        // -950000 + 0.5 truncates to -949999, as in GPSBabel
        assertEquals(-9.49999, read.get(0).getLongitude(), 0.0);
        assertEquals(52.5, read.get(1).getLatitude(), 0.0);
        assertNull(read.get(2).getDescription());
    }

    @Test
    public void formatProperties() {
        assertEquals("Tom Tom POI (*.ov2)", format.getName());
        assertEquals(".ov2", format.getExtension());
        assertFalse(format.isSupportsMultipleRoutes());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertEquals(18, format.getMaximumFileNameLength());
    }
}

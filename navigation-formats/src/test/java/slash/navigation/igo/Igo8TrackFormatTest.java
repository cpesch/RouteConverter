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

package slash.navigation.igo;

import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpsbabel.LittleEndianOutput;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;

import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.base.RouteCharacteristics.Track;

public class Igo8TrackFormatTest {
    private final Igo8TrackFormat format = new Igo8TrackFormat();

    /** time, longitude and latitude of a point as stored in the file */
    private static long[] point(long time, int lon, int lat) {
        return new long[]{time, lon, lat};
    }

    private static int degrees(double value) {
        return (int) Math.round(value * 0x800000);
    }

    private static byte[] file(int totalFileSize, int padding, long[]... points) {
        LittleEndianOutput output = new LittleEndianOutput();
        output.writeUInt32(0x29B);
        output.writeUInt32(0x3E7);
        output.writeUInt32(3);
        output.writeUInt32(0x10);
        output.writeUInt32(1);
        output.writeBytes(new byte[256]);
        output.writeUInt32(points.length > 0 ? points[0][0] : 0);
        output.writeUInt32(0);
        output.writeUInt32(totalFileSize);
        for (long[] point : points) {
            output.writeUInt32(point[0]);
            output.writeInt32((int) point[1]);
            output.writeInt32((int) point[2]);
        }
        output.writeBytes(new byte[padding]);
        return output.toByteArray();
    }

    private static byte[] file(long[]... points) {
        return file(288 + 12 * points.length, 0, points);
    }

    private List<GpxRoute> read(byte[] data) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(data), context);
        return context.getRoutes();
    }

    private static GpxPosition position(Double longitude, Double latitude, Long seconds) {
        return new GpxPosition(longitude, latitude, null, null, seconds != null ? fromMillis(seconds * 1000) : null, null);
    }

    private byte[] write(GpxPosition... positions) throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, "name", null, new ArrayList<>(asList(positions)));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, positions.length);
        return target.toByteArray();
    }

    private static ByteBuffer buffer(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    public void nameAndExtension() {
        assertEquals("iGO8 Track (*.trk)", format.getName());
        assertEquals(".trk", format.getExtension());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertTrue(!format.isSupportsMultipleRoutes());
    }

    @Test
    public void readsPointsAsOneTrack() throws IOException {
        List<GpxRoute> routes = read(file(
                point(1200000000L, degrees(11.5), degrees(48.25)),
                point(1200000060L, degrees(11.75), degrees(48.5))));
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertNull(route.getName());
        assertEquals(2, route.getPositionCount());
        GpxPosition first = route.getPosition(0);
        assertEquals(11.5, first.getLongitude(), 0.0);
        assertEquals(48.25, first.getLatitude(), 0.0);
        assertNull(first.getElevation());
        assertEquals(1200000000L * 1000, first.getTime().getTimeInMillis());
        GpxPosition second = route.getPosition(1);
        assertEquals(11.75, second.getLongitude(), 0.0);
        assertEquals(48.5, second.getLatitude(), 0.0);
        assertEquals(1200000060L * 1000, second.getTime().getTimeInMillis());
    }

    @Test
    public void readsSignedCoordinates() throws IOException {
        GpxRoute route = read(file(point(10, degrees(-122.5), degrees(-33.75)))).get(0);
        assertEquals(-122.5, route.getPosition(0).getLongitude(), 0.0);
        assertEquals(-33.75, route.getPosition(0).getLatitude(), 0.0);
    }

    @Test
    public void zeroTimeIsNoTime() throws IOException {
        GpxRoute route = read(file(point(0, degrees(1), degrees(2)))).get(0);
        assertNull(route.getPosition(0).getTime());
    }

    @Test
    public void pointCountComesFromTotalFileSizeNotFromFileLength() throws IOException {
        // two points, total_file_size announces only the first one, followed by zero padding
        byte[] data = file(288 + 12, 0, point(5, degrees(1), degrees(2)), point(6, degrees(3), degrees(4)));
        assertEquals(1, read(data).get(0).getPositionCount());

        byte[] padded = file(288 + 12, 100, point(5, degrees(1), degrees(2)));
        assertEquals(1, read(padded).get(0).getPositionCount());
    }

    @Test
    public void rejectsFileShorterThanHeader() throws IOException {
        assertTrue(read(new byte[287]).isEmpty());
        assertTrue(read(new byte[0]).isEmpty());
    }

    @Test
    public void rejectsFileWithoutPoints() throws IOException {
        assertTrue(read(file()).isEmpty());
    }

    @Test
    public void rejectsTotalFileSizeBelowHeader() throws IOException {
        assertTrue(read(file(100, 0, point(5, degrees(1), degrees(2)))).isEmpty());
    }

    @Test
    public void rejectsTotalFileSizeBeyondFileLength() throws IOException {
        assertTrue(read(file(288 + 24, 0, point(5, degrees(1), degrees(2)))).isEmpty());
    }

    @Test
    public void rejectsDataNotMultipleOfPointSize() throws IOException {
        assertTrue(read(file(288 + 13, 20, point(5, degrees(1), degrees(2)))).isEmpty());
    }

    @Test
    public void rejectsOutOfRangeCoordinates() throws IOException {
        assertTrue(read(file(point(5, degrees(181), degrees(2)))).isEmpty());
        assertTrue(read(file(point(5, degrees(-181), degrees(2)))).isEmpty());
        assertTrue(read(file(point(5, degrees(1), degrees(91)))).isEmpty());
        assertTrue(read(file(point(5, degrees(1), degrees(-91)))).isEmpty());
        assertTrue(read(file(point(5, degrees(1), degrees(2)), point(6, degrees(200), degrees(2)))).isEmpty());
    }

    @Test
    public void acceptsCoordinatesOnTheLimits() throws IOException {
        assertEquals(1, read(file(point(5, degrees(180), degrees(90)))).size());
        assertEquals(1, read(file(point(5, degrees(-180), degrees(-90)))).size());
    }

    @Test
    public void rejectsRouteWithGreedyDistances() throws IOException {
        assertTrue(read(file(point(5, degrees(0), degrees(0)), point(6, degrees(179), degrees(0)))).isEmpty());
    }

    @Test
    public void writesHeader() throws IOException {
        byte[] data = write(position(1.0, 2.0, 100L));
        ByteBuffer buffer = buffer(data);
        assertEquals(0x29B, buffer.getInt(0));
        assertEquals(0x3E7, buffer.getInt(4));
        assertEquals(3, buffer.getInt(8));
        assertEquals(0x10, buffer.getInt(12));
        assertEquals(1, buffer.getInt(16));

        byte[] expected = new byte[256];
        byte[] title = "Title\u0000Description\u0000".getBytes(UTF_16LE);
        System.arraycopy(title, 0, expected, 0, title.length);
        byte[] actual = new byte[256];
        System.arraycopy(data, 20, actual, 0, 256);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void writesInformationBlockAndPoints() throws IOException {
        byte[] data = write(position(11.5, 48.25, 1200000000L), position(-11.75, -48.5, 1200000060L));
        assertEquals(288 + 24, data.length);
        ByteBuffer buffer = buffer(data);
        assertEquals(1200000000, buffer.getInt(276));
        assertEquals(0, buffer.getInt(280));
        assertEquals(data.length, buffer.getInt(284));

        assertEquals(1200000000, buffer.getInt(288));
        assertEquals(degrees(11.5), buffer.getInt(292));
        assertEquals(degrees(48.25), buffer.getInt(296));
        assertEquals(1200000060, buffer.getInt(300));
        assertEquals(degrees(-11.75), buffer.getInt(304));
        assertEquals(degrees(-48.5), buffer.getInt(308));
    }

    @Test
    public void inventsTimesStartingAtOne() throws IOException {
        byte[] data = write(position(1.0, 2.0, null), position(3.0, 4.0, null), position(5.0, 6.0, null));
        ByteBuffer buffer = buffer(data);
        assertEquals(1, buffer.getInt(276));
        assertEquals(288 + 36, buffer.getInt(284));
        assertEquals(1, buffer.getInt(288));
        assertEquals(2, buffer.getInt(300));
        assertEquals(3, buffer.getInt(312));
    }

    @Test
    public void inventedTimeCounterOnlyAdvancesForPositionsWithoutTime() throws IOException {
        byte[] data = write(position(1.0, 2.0, 500L), position(3.0, 4.0, null), position(5.0, 6.0, 600L), position(7.0, 8.0, null));
        ByteBuffer buffer = buffer(data);
        assertEquals(500, buffer.getInt(276));
        assertEquals(500, buffer.getInt(288));
        assertEquals(1, buffer.getInt(300));
        assertEquals(600, buffer.getInt(312));
        assertEquals(2, buffer.getInt(324));
    }

    @Test
    public void roundsCoordinatesHalfAwayFromZero() throws IOException {
        // 0.5 / 2^23 degrees lies half way between 0 and 1 units
        double half = 0.5 / 0x800000;
        byte[] data = write(position(half, -half, 1L));
        ByteBuffer buffer = buffer(data);
        assertEquals(1, buffer.getInt(292));
        assertEquals(-1, buffer.getInt(296));
    }

    @Test
    public void skipsPositionsWithoutCoordinates() throws IOException {
        byte[] data = write(position(null, 2.0, 1L), position(3.0, 4.0, 2L));
        assertEquals(288 + 12, data.length);
        ByteBuffer buffer = buffer(data);
        assertEquals(288 + 12, buffer.getInt(284));
        assertEquals(2, buffer.getInt(276));
    }

    @Test
    public void honoursStartAndEndIndex() throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, "name", null,
                new ArrayList<>(asList(position(1.0, 1.0, 1L), position(2.0, 2.0, 2L), position(3.0, 3.0, 3L))));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 1, 3);
        assertEquals(288 + 24, target.size());
        assertEquals(2, buffer(target.toByteArray()).getInt(276));
    }

    @Test
    public void writtenFileCanBeReadAgain() throws IOException {
        byte[] data = write(position(11.5, 48.25, 1200000000L), position(11.5, 48.5, 1200000060L));
        GpxRoute route = read(data).get(0);
        assertEquals(2, route.getPositionCount());
        assertEquals(11.5, route.getPosition(1).getLongitude(), 0.0);
        assertEquals(48.5, route.getPosition(1).getLatitude(), 0.0);
        assertEquals(1200000060L * 1000, route.getPosition(1).getTime().getTimeInMillis());
    }

    @Test
    public void isRegisteredInPlaceOfTheGpsBabelFormat() {
        // the registry imports slash.navigation.babel.* and slash.navigation.igo.Igo8TrackFormat explicitly: the single-type import
        // wins (JLS 6.4.1), so addFormat(Igo8TrackFormat.class) keeps its position but now means the native class
        List<String> names = new ArrayList<>();
        for (NavigationFormat<?> format : new NavigationFormatRegistry().getReadFormats())
            names.add(format.getClass().getName());
        int index = names.indexOf("slash.navigation.igo.Igo8TrackFormat");
        assertTrue(index > 0, names.toString());
        assertFalse(names.contains("slash.navigation.babel.Igo8TrackFormat"));
        assertEquals("GarminPoiFormat", names.get(index - 1).substring(names.get(index - 1).lastIndexOf('.') + 1));
        assertEquals("GarminPoiDbFormat", names.get(index + 1).substring(names.get(index + 1).lastIndexOf('.') + 1));
    }
}

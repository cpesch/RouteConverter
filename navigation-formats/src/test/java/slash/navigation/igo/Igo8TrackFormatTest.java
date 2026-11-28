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
import slash.navigation.base.UnreadableFileException;
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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;

import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    // a file with the ID block of iGO8 that cannot be read is reported as damaged, not as empty;
    // without the ID block it is declined
    private void assertUnreadable(byte[] data) throws IOException {
        assertThrows(UnreadableFileException.class, () -> read(data));
        assertTrue(read(withoutMagic(data)).isEmpty());
    }

    private static byte[] withoutMagic(byte[] data) {
        byte[] result = data.clone();
        Arrays.fill(result, 0, 8, (byte) 0);
        return result;
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
        assertUnreadable(file(100, 0, point(5, degrees(1), degrees(2))));
    }

    @Test
    public void readsTheCompletePointsOfACutFile() throws IOException {
        // GPSBabel 1.5.4 reads the points of a cut file until the end of the file
        byte[] data = file(288 + 24, 0, point(5, degrees(1), degrees(2)));
        assertEquals(1, read(data).get(0).getPositionCount());
        assertTrue(read(withoutMagic(data)).isEmpty());

        byte[] full = file(point(5, degrees(1), degrees(2)), point(6, degrees(3), degrees(4)), point(7, degrees(5), degrees(6)));
        assertEquals(2, read(Arrays.copyOf(full, full.length - 12)).get(0).getPositionCount());
        // a trailing partial point is dropped
        assertEquals(2, read(Arrays.copyOf(full, full.length - 5)).get(0).getPositionCount());
        assertEquals(1, read(Arrays.copyOf(full, 288 + 12)).get(0).getPositionCount());
        assertTrue(read(Arrays.copyOf(full, 288 + 11)).isEmpty());
    }

    @Test
    public void readsTheCompletePointsOfAnOddSizeField() throws IOException {
        // GPSBabel 1.5.4 floors the number of points
        byte[] data = file(288 + 13, 20, point(5, degrees(1), degrees(2)));
        assertEquals(1, read(data).get(0).getPositionCount());
        assertTrue(read(withoutMagic(data)).isEmpty());

        long[][] points = new long[11][];
        for (int i = 0; i < points.length; i++)
            points[i] = point(5 + i, degrees(1 + i * 0.01), degrees(2));
        assertEquals(10, read(file(288 + 12 * 10 + 5, 0, points)).get(0).getPositionCount());
    }

    @Test
    public void rejectsOutOfRangeCoordinates() throws IOException {
        assertUnreadable(file(point(5, degrees(181), degrees(2))));
        assertUnreadable(file(point(5, degrees(-181), degrees(2))));
        assertUnreadable(file(point(5, degrees(1), degrees(91))));
        assertUnreadable(file(point(5, degrees(1), degrees(-91))));
        assertUnreadable(file(point(5, degrees(1), degrees(2)), point(6, degrees(200), degrees(2))));
    }

    @Test
    public void acceptsCoordinatesOnTheLimits() throws IOException {
        assertEquals(1, read(file(point(5, degrees(180), degrees(90)))).size());
        assertEquals(1, read(file(point(5, degrees(-180), degrees(-90)))).size());
    }

    @Test
    public void rejectsRouteWithGreedyDistancesWithoutMagic() throws IOException {
        assertTrue(read(withoutMagic(file(point(5, degrees(0), degrees(0)), point(6, degrees(179), degrees(0))))).isEmpty());
    }

    @Test
    public void acceptsSparseRouteWithMagic() throws IOException {
        // the distance heuristic used to decline any two-point intercontinental route
        assertEquals(2, read(file(point(5, degrees(0), degrees(0)), point(6, degrees(179), degrees(0)))).get(0).getPositionCount());
        assertEquals(3, read(file(point(5, degrees(180), degrees(90)), point(6, degrees(-180), degrees(-90)),
                point(7, degrees(0), degrees(0)))).get(0).getPositionCount());
    }

    @Test
    public void detectsMagic() {
        assertTrue(Igo8TrackFormat.hasMagic(file(point(5, degrees(1), degrees(2)))));
        assertFalse(Igo8TrackFormat.hasMagic(withoutMagic(file(point(5, degrees(1), degrees(2))))));
        assertFalse(Igo8TrackFormat.hasMagic(new byte[]{(byte) 0x9B, 0x02, 0, 0}));
    }

    @Test
    public void headerWithMagicCutShortIsUnreadable() {
        assertThrows(UnreadableFileException.class, () -> read(Arrays.copyOf(file(point(5, degrees(1), degrees(2))), 100)));
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
        byte[] title = "name\u0000Description\u0000".getBytes(UTF_16LE);
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
    public void inventedTimeContinuesFromThePreviousTime() throws IOException {
        // GPSBabel 1.5.4 writes 1 and 2 for the positions without time, which puts the track out of order
        byte[] data = write(position(1.0, 2.0, 500L), position(3.0, 4.0, null), position(5.0, 6.0, 600L), position(7.0, 8.0, null));
        ByteBuffer buffer = buffer(data);
        assertEquals(500, buffer.getInt(276));
        assertEquals(500, buffer.getInt(288));
        assertEquals(501, buffer.getInt(300));
        assertEquals(600, buffer.getInt(312));
        assertEquals(601, buffer.getInt(324));
    }

    @Test
    public void timesOutsideTheFieldAreClamped() throws IOException {
        // GPSBabel 1.5.4 lets them wrap around: -100000000 s becomes 2102-12-07
        byte[] data = write(position(1.0, 2.0, -100000000L), position(3.0, 4.0, 4300000000L), position(5.0, 6.0, null));
        ByteBuffer buffer = buffer(data);
        assertEquals(1, buffer.getInt(288));
        assertEquals(0xFFFFFFFFL, buffer.getInt(300) & 0xFFFFFFFFL);
        assertEquals(0xFFFFFFFFL, buffer.getInt(312) & 0xFFFFFFFFL);
    }

    private byte[] writeRoute(String name, List<String> description, GpxPosition... positions) throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Track, name, description, new ArrayList<>(asList(positions)));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, positions.length);
        return target.toByteArray();
    }

    private static String header(byte[] data) {
        return new String(data, 20, 256, UTF_16LE);
    }

    @Test
    public void writesNameAndDescriptionOfTheRoute() throws IOException {
        // GPSBabel 1.5.4 writes the placeholders "Title" and "Description"
        byte[] data = writeRoute("Tour \u00fcber den Pass", asList("", "First line", "Second line"), position(1.0, 2.0, 5L));
        assertTrue(header(data).startsWith("Tour \u00fcber den Pass\u0000First line\u0000"), header(data));
        GpxRoute route = read(data).get(0);
        assertEquals("Tour \u00fcber den Pass", route.getName());

        byte[] placeholders = writeRoute(null, null, position(1.0, 2.0, 5L));
        assertTrue(header(placeholders).startsWith("Title\u0000Description\u0000"), header(placeholders));
        assertNull(read(placeholders).get(0).getName());
    }

    @Test
    public void cutsALongNameAndKeepsTheHeaderReadable() throws IOException {
        String name = "N\u0000a\tme" + "x".repeat(200);
        byte[] data = writeRoute(name, singletonList("Desc"), position(1.0, 2.0, 5L));
        assertEquals(288 + 12, data.length);
        GpxRoute route = read(data).get(0);
        assertEquals(126, route.getName().length());
        assertTrue(route.getName().startsWith("N a me"), route.getName());
    }

    @Test
    public void readsTheTitleWithoutTheRestOfAnEarlierTitle() throws IOException {
        byte[] data = file(point(5, degrees(1), degrees(2)));
        byte[] header = "Short\u0000 rest of a longer title\u0000".getBytes(UTF_16LE);
        System.arraycopy(header, 0, data, 20, header.length);
        GpxRoute route = read(data).get(0);
        assertEquals("Short", route.getName());
        assertTrue(route.getDescription() == null || route.getDescription().isEmpty());
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

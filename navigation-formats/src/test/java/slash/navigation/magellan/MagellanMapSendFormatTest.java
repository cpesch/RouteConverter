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

package slash.navigation.magellan;

import org.junit.Test;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
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
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class MagellanMapSendFormatTest {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private final MagellanMapSendFormat format = new MagellanMapSendFormat();

    // file building blocks, written from the layout in mapsend.h/mapsend.cc

    private static LittleEndianOutput header(int length, String signature, String version, int type) {
        LittleEndianOutput output = new LittleEndianOutput();
        output.writeUInt8(length);
        output.writeBytes(signature.getBytes(WINDOWS_1252));
        output.writeBytes(version.getBytes(WINDOWS_1252));
        output.writeUInt8(type);
        output.writeBytes(new byte[3]);
        return output;
    }

    private static LittleEndianOutput header(int type) {
        return header(13, "4D533330 MS", "30", type);
    }

    private static void pstr(LittleEndianOutput output, String string) {
        byte[] bytes = string.getBytes(WINDOWS_1252);
        output.writeUInt8(bytes.length);
        output.writeBytes(bytes);
    }

    private static void waypoint(LittleEndianOutput output, String name, String description, int number,
                                 double altitude, double longitude, double latitude) {
        pstr(output, name);
        pstr(output, description);
        output.writeInt32(number);
        output.writeUInt8(0); // icon
        output.writeUInt8(1); // status
        output.writeDouble(altitude);
        output.writeDouble(longitude);
        output.writeDouble(-latitude);
    }

    private static void routePoint(LittleEndianOutput output, String name, int number, double longitude, double latitude) {
        pstr(output, name);
        output.writeInt32(number);
        output.writeDouble(longitude);
        output.writeDouble(-latitude);
        output.writeUInt8(0); // icon
    }

    private static byte[] waypointFile() {
        LittleEndianOutput output = header(1);
        output.writeInt32(3);
        waypoint(output, "HOME", "Home sweet home", 1, 12.5, 11.5, 48.1);
        waypoint(output, "LAKE", "", 2, 0.0, 11.75, 48.25);
        waypoint(output, "Größe", "Größe Straße", 3, -99999999.0, -0.5, -33.125);
        output.writeInt32(1);
        pstr(output, "Lake Tour");
        output.writeInt32(7);
        output.writeInt32(3);
        routePoint(output, "Home sweet home", 1, 11.5, 48.1);
        routePoint(output, "LAKE", 2, 11.75, 48.25);
        routePoint(output, "", 3, -0.5, -33.125);
        return output.toByteArray();
    }

    private List<GpxRoute> read(byte[] data) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(data), context);
        return context.getRoutes();
    }

    private byte[] write(String routeName, GpxPosition... positions) throws IOException {
        GpxRoute route = new GpxRoute(new Gpx10Format(), Route, routeName, null, new ArrayList<>(asList(positions)));
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        format.write(route, target, 0, positions.length);
        return target.toByteArray();
    }

    private static GpxPosition position(double longitude, double latitude, Double elevation, String description) {
        return new GpxPosition(longitude, latitude, elevation, null, null, description);
    }

    @Test
    public void nameExtensionAndCapabilities() {
        assertEquals("Magellan MapSend (*.wpt)", format.getName());
        assertEquals(".wpt", format.getExtension());
        assertTrue(format.isSupportsReading());
        assertTrue(format.isSupportsWriting());
        assertFalse(format.isSupportsMultipleRoutes());
        assertFalse(format instanceof slash.navigation.base.MultipleRoutesFormat);
    }

    @Test
    public void readsWaypointFile() throws IOException {
        List<GpxRoute> routes = read(waypointFile());
        assertEquals(2, routes.size());

        GpxRoute waypoints = routes.get(0);
        assertEquals(Waypoints, waypoints.getCharacteristics());
        assertEquals(3, waypoints.getPositionCount());

        GpxPosition home = waypoints.getPosition(0);
        assertEquals(11.5, home.getLongitude(), 0.0);
        // the latitude is stored negated
        assertEquals(48.1, home.getLatitude(), 0.0);
        assertEquals(12.5, home.getElevation(), 0.0);
        assertNull(home.getTime());
        // the name is ignored for waypoints, GPSBabel cuts it to 8 characters
        assertEquals("Home sweet home", home.getDescription());

        GpxPosition lake = waypoints.getPosition(1);
        assertEquals(0.0, lake.getElevation(), 0.0);
        // a waypoint without description takes its name
        assertEquals("LAKE", lake.getDescription());

        GpxPosition third = waypoints.getPosition(2);
        assertEquals(-0.5, third.getLongitude(), 0.0);
        assertEquals(-33.125, third.getLatitude(), 0.0);
        // GPSBabel's unknown altitude
        assertNull(third.getElevation());
        // text is Windows-1252
        assertEquals("Größe Straße", third.getDescription());
    }

    @Test
    public void readsRoutesInOrderWithNameAndPositionOnly() throws IOException {
        GpxRoute route = read(waypointFile()).get(1);
        assertEquals(Route, route.getCharacteristics());
        assertEquals("Lake Tour", route.getName());
        assertEquals(3, route.getPositionCount());

        assertEquals("Home sweet home", route.getPosition(0).getDescription());
        assertEquals(11.5, route.getPosition(0).getLongitude(), 0.0);
        assertEquals(48.1, route.getPosition(0).getLatitude(), 0.0);
        // route points carry no altitude; the parser takes it from a waypoint at the same coordinates
        assertNull(route.getPosition(0).getElevation());
        assertEquals("LAKE", route.getPosition(1).getDescription());
        assertEquals(11.75, route.getPosition(1).getLongitude(), 0.0);
        // a route point without name is numbered by GPSBabel
        assertEquals("RPT003", route.getPosition(2).getDescription());
        assertEquals(-33.125, route.getPosition(2).getLatitude(), 0.0);
    }

    @Test
    public void readsWaypointFileOfOtherVersions() throws IOException {
        for (String version : new String[]{"34", "36"}) {
            LittleEndianOutput output = header(13, "4D533336 MS", version, 1);
            output.writeInt32(1);
            waypoint(output, "A", "B", 1, 1.0, 2.0, 3.0);
            output.writeInt32(0);
            List<GpxRoute> routes = read(output.toByteArray());
            assertEquals(1, routes.size());
            assertEquals("B", routes.get(0).getPosition(0).getDescription());
        }
    }

    @Test
    public void rejectsOtherFileTypes() throws IOException {
        // 0 region and 3 log are fatal to GPSBabel, 2 track is not delivered with "-r -w"
        for (int type : new int[]{0, 2, 3, 4}) {
            LittleEndianOutput output = header(type);
            output.writeInt32(1);
            waypoint(output, "A", "B", 1, 1.0, 2.0, 3.0);
            output.writeInt32(0);
            assertTrue("type " + type, read(output.toByteArray()).isEmpty());
        }
    }

    @Test
    public void rejectsTrackFile() throws IOException {
        LittleEndianOutput output = header(13, "4D533334 MS", "34", 2);
        pstr(output, "Track");
        output.writeUInt32(1);
        output.writeDouble(11.5);
        output.writeDouble(-48.1);
        output.writeInt32(500);
        output.writeInt32(1700000000);
        output.writeInt32(1);
        output.writeUInt8(0);
        assertTrue(read(output.toByteArray()).isEmpty());
    }

    @Test
    public void rejectsWrongHeader() throws IOException {
        byte[][] headers = {
                header(12, "4D533330 MS", "30", 1).toByteArray(),
                header(13, "4D533330 XX", "30", 1).toByteArray(),
                header(13, "5D533330 MS", "30", 1).toByteArray(),
                header(13, "4D53333X MS", "30", 1).toByteArray(),
                header(13, "4D533330 MS", "3a", 1).toByteArray()
        };
        for (byte[] header : headers) {
            LittleEndianOutput output = new LittleEndianOutput();
            output.writeBytes(header);
            output.writeInt32(0);
            output.writeInt32(0);
            assertTrue(read(output.toByteArray()).isEmpty());
        }
        assertTrue(read(new byte[0]).isEmpty());
        assertTrue(read(new byte[]{13, '4', 'D'}).isEmpty());
    }

    @Test
    public void rejectsCountsLargerThanTheRemainingBytes() throws IOException {
        LittleEndianOutput waypoints = header(1);
        waypoints.writeInt32(1000);
        waypoint(waypoints, "A", "B", 1, 1.0, 2.0, 3.0);
        waypoints.writeInt32(0);
        assertTrue(read(waypoints.toByteArray()).isEmpty());

        LittleEndianOutput negative = header(1);
        negative.writeInt32(-1);
        assertTrue(read(negative.toByteArray()).isEmpty());

        LittleEndianOutput routes = header(1);
        routes.writeInt32(0);
        routes.writeInt32(50);
        assertTrue(read(routes.toByteArray()).isEmpty());

        LittleEndianOutput points = header(1);
        points.writeInt32(0);
        points.writeInt32(1);
        pstr(points, "R");
        points.writeInt32(0);
        points.writeInt32(9);
        routePoint(points, "P", 1, 1.0, 2.0);
        assertTrue(read(points.toByteArray()).isEmpty());
    }

    @Test
    public void rejectsTruncatedFile() throws IOException {
        byte[] data = waypointFile();
        for (int length = 18; length < data.length; length += 7) {
            byte[] truncated = new byte[length];
            System.arraycopy(data, 0, truncated, 0, length);
            // never throws
            read(truncated);
        }
        byte[] cut = new byte[data.length - 1];
        System.arraycopy(data, 0, cut, 0, cut.length);
        assertTrue(read(cut).isEmpty());
    }

    @Test
    public void writesWaypointFileBytes() throws IOException {
        byte[] actual = write(null,
                position(11.5, 48.1, 1.5, "Position 1"),
                position(11.501, 48.101, null, "Café, Bar; A very long description beyond thirty chars"),
                position(11.502, 48.102, 3.0, null));

        LittleEndianOutput expected = header(13, "4D533330 MS", "30", 1);
        expected.writeInt32(3);
        // running numbers from 1, icon 0, status 1, latitude negated
        waypoint(expected, "Positn 1", "Position 1", 1, 1.5, 11.5, 48.1);
        // naughty characters are removed from the name, the description is cut to 30 characters,
        // the unknown altitude is written as 0
        waypoint(expected, "Café Bar", "A very long description beyond", 2, 0.0, 11.501, 48.101);
        // a position without description is numbered by GPSBabel
        waypoint(expected, "WPT003", "WPT003", 3, 3.0, 11.502, 48.102);
        expected.writeInt32(1);
        // the default route name
        pstr(expected, "Route");
        expected.writeInt32(0);
        expected.writeInt32(3);
        routePoint(expected, "Position 1", 1, 11.5, 48.1);
        routePoint(expected, "Café, Bar", 2, 11.501, 48.101);
        routePoint(expected, "RPT003", 3, 11.502, 48.102);

        assertArrayEquals(expected.toByteArray(), actual);
    }

    @Test
    public void writesRouteNameAndSkipsPositionsWithoutCoordinates() throws IOException {
        byte[] actual = write("Lake Tour",
                position(11.5, 48.1, null, "A"),
                new GpxPosition((Double) null, null, null, null, null, "B"));

        LittleEndianOutput expected = header(1);
        expected.writeInt32(1);
        waypoint(expected, "A", "A", 1, 0.0, 11.5, 48.1);
        expected.writeInt32(1);
        pstr(expected, "Lake Tour");
        expected.writeInt32(0);
        expected.writeInt32(1);
        routePoint(expected, "A", 1, 11.5, 48.1);

        assertArrayEquals(expected.toByteArray(), actual);
    }

    @Test
    public void writtenFileReadsBack() throws IOException {
        byte[] data = write("Grüße", position(11.5, 48.1, 1.5, "Home"), position(11.75, 48.25, null, "Lake; Grüne Wiese"));
        List<GpxRoute> routes = read(data);
        assertEquals(2, routes.size());
        assertEquals("Home", routes.get(0).getPosition(0).getDescription());
        assertEquals("Grüne Wiese", routes.get(0).getPosition(1).getDescription());
        assertEquals("Grüße", routes.get(1).getName());
        assertEquals("Lake", routes.get(1).getPosition(1).getDescription());
    }

    @Test
    public void shortNamesFollowMkshort() {
        MagellanMapSendFormat.ShortNames shortNames = new MagellanMapSendFormat.ShortNames(8);
        // vowels are tossed from the end, but not the leading two characters or the first letter of a word
        assertEquals("Positn 1", shortNames.mkshort("Position 1"));
        // trailing digits survive the truncation
        assertEquals("Waypnt12", shortNames.mkshort("Waypoint 12"));
        assertEquals("Th Old M", shortNames.mkshort("The Old Mill"));
        // number words at the end are replaced
        assertEquals("Hill 2", shortNames.mkshort("Hill two"));
        // leading whitespace, the bad characters "$.,'!- and repeated blanks are removed
        assertEquals("Café Bar", shortNames.mkshort("  Café, \"Bar\"!"));
        assertEquals("a b", shortNames.mkshort("a  b"));
        assertEquals("WPT", shortNames.mkshort("..."));
    }

    @Test
    public void shortNamesAreUnique() {
        MagellanMapSendFormat.ShortNames shortNames = new MagellanMapSendFormat.ShortNames(8);
        assertEquals("Home", shortNames.mkshort("Home"));
        assertEquals("home.1", shortNames.mkshort("home"));
        assertEquals("Home.2", shortNames.mkshort("Home"));
        assertEquals("ABCDEFGH", shortNames.mkshort("ABCDEFGH"));
        assertEquals("ABCDEF.1", shortNames.mkshort("ABCDEFGH"));
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    @Test
    public void registeredAtTheOldPosition() {
        List<String> names = new ArrayList<>();
        for (NavigationFormat<?> format : new NavigationFormatRegistry().getReadFormats())
            names.add(format.getClass().getName());
        int index = names.indexOf("slash.navigation.magellan.MagellanMapSendFormat");
        assertTrue(names.toString(), index > 0);
        assertFalse(names.contains("slash.navigation.babel.MagellanMapSendFormat"));
        assertEquals("NationalGeographicTopo3Format", simpleName(names.get(index - 1)));
        assertEquals("AlanTrackLogFormat", simpleName(names.get(index + 1)));
    }
}

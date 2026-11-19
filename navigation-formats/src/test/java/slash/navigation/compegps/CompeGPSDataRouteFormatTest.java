package slash.navigation.compegps;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

public class CompeGPSDataRouteFormatTest {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final String DEG = "º";
    private static final double DELTA = 0.0000001;

    private static List<GpxRoute> read(CompeGPSDataFormat format, byte[] bytes) throws IOException {
        ParserContextImpl<GpxRoute> context = new ParserContextImpl<>();
        format.read(new ByteArrayInputStream(bytes), context);
        return context.getRoutes();
    }

    private static List<GpxRoute> read(CompeGPSDataFormat format, String text) throws IOException {
        return read(format, text.getBytes(WINDOWS_1252));
    }

    private static long millis(String instant) {
        return Instant.parse(instant).toEpochMilli();
    }

    private static String write(NavigationFormat<GpxRoute> format, GpxRoute route) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(route, out, 0, route.getPositionCount());
        return new String(out.toByteArray(), ISO_8859_1);
    }

    private static final String HEADER = "G  WGS 84\nU  1\n";

    private static final String WAYPOINTS = HEADER +
            "W  Home A 48.1234567890" + DEG + "N 11.5000000000" + DEG + "E 27-MAR-62 00:00:00 520.000000 Sweet home\n" +
            "w  Waypoint,0,0.0,16777215,255,1,7,,0.0\n" +
            "W  Pub__ A 48.2000000000" + DEG + "S 11.6000000000" + DEG + "W 03-JUN-11 07:24:40 -9999.000000\n";

    private static final String TRACK = HEADER +
            "T  A 48.0000000000" + DEG + "N 11.0000000000" + DEG + "E 03-JUN-11 07:24:40 s 500.0 0.0 0.0 0.0 0 -1000.0 -1.0 -1.0 -1.0 -1.0\n" +
            "t 4294967295|Morning ride|-1|-1\n" +
            "T  A 48.1000000000" + DEG + "N 11.1000000000" + DEG + "E 03-JUN-11 07:24:50 s 501.5 0.0 0.0 0.0 0 -1000.0 -1.0 -1.0 -1.0 -1.0\n" +
            "F  1234\n";

    @Test
    public void readsWaypointsAndForcesCharacteristicsPerClass() throws IOException {
        for (CompeGPSDataFormat format : asList(new CompeGPSDataWaypointFormat(), new CompeGPSDataRouteFormat(), new CompeGPSDataTrackFormat())) {
            List<GpxRoute> routes = read(format, WAYPOINTS);
            assertEquals(1, routes.size());
            GpxRoute route = routes.get(0);
            assertEquals(format instanceof CompeGPSDataWaypointFormat ? Waypoints :
                    format instanceof CompeGPSDataRouteFormat ? Route : Track, route.getCharacteristics());
            assertEquals(2, route.getPositionCount());
        }

        GpxRoute route = read(new CompeGPSDataWaypointFormat(), WAYPOINTS).get(0);
        GpxPosition home = route.getPosition(0);
        assertEquals(48.123456789, home.getLatitude(), DELTA);
        assertEquals(11.5, home.getLongitude(), DELTA);
        assertEquals(520.0, home.getElevation(), DELTA);
        assertNull(home.getTime());
        assertEquals("Home; Sweet home", home.getDescription());

        GpxPosition pub = route.getPosition(1);
        assertEquals(-48.2, pub.getLatitude(), DELTA);
        assertEquals(-11.6, pub.getLongitude(), DELTA);
        assertNull(pub.getElevation());
        assertEquals("Pub", pub.getDescription());
        assertEquals(millis("2011-06-03T07:24:40Z"), pub.getTime().getTimeInMillis());
    }

    @Test
    public void readsRouteHeaderAndRoutePoints() throws IOException {
        String text = HEADER +
                "R  16711680,Evening round,1,-1\n" +
                "W  Start A 48.0" + DEG + "N 11.0" + DEG + "E 27-MAR-62 00:00:00 100.000000\n" +
                "W  End A 48.5" + DEG + "N 11.5" + DEG + "E 27-MAR-62 00:00:00 110.000000 Finish line\n";
        List<GpxRoute> routes = read(new CompeGPSDataRouteFormat(), text);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Route, route.getCharacteristics());
        assertEquals("Evening round", route.getName());
        assertEquals(2, route.getPositionCount());
        assertEquals("Start", route.getPosition(0).getDescription());
        assertEquals("End; Finish line", route.getPosition(1).getDescription());
    }

    @Test
    public void readsTrackWithTrackInfo() throws IOException {
        List<GpxRoute> routes = read(new CompeGPSDataTrackFormat(), TRACK);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(Track, route.getCharacteristics());
        assertEquals("Morning ride", route.getName());
        assertEquals(2, route.getPositionCount());
        GpxPosition first = route.getPosition(0);
        assertEquals(48.0, first.getLatitude(), DELTA);
        assertEquals(11.0, first.getLongitude(), DELTA);
        assertEquals(500.0, first.getElevation(), DELTA);
        assertNull(first.getDescription());
        assertEquals(millis("2011-06-03T07:24:40Z"), first.getTime().getTimeInMillis());
        assertEquals(millis("2011-06-03T07:24:50Z"), route.getPosition(1).getTime().getTimeInMillis());
    }

    @Test
    public void twoDigitYears() throws IOException {
        String text = HEADER +
                "T  A 48.0" + DEG + "N 11.0" + DEG + "E 03-JUN-98 07:24:40 s 1.0\n" +
                "T  A 48.0" + DEG + "N 11.0" + DEG + "E 03-JUN-62 07:24:40 s 1.0\n";
        GpxRoute route = read(new CompeGPSDataTrackFormat(), text).get(0);
        assertEquals(millis("1998-06-03T07:24:40Z"), route.getPosition(0).getTime().getTimeInMillis());
        assertEquals(millis("2062-06-03T07:24:40Z"), route.getPosition(1).getTime().getTimeInMillis());
    }

    private static final String TWONAV_BODY =
            "B  UTF-8\n" +
            "G  WGS 84\n" +
            "C  0 0 255 2 -1.000000\n" +
            "V  0.0 0.0 0 0 0 0 0.0\n" +
            "E  0|1|00-NUL-00 00:00:00|00:00:00|0\n" +
            "i  2 6 0.1\n" +
            "z  47.0 10.0 49.0 12.0\n" +
            "U  1\n" +
            "W  Übung A 48.0" + DEG + "N 11.0" + DEG + "E 03-JUN-2011 07:24:40.000 -9999.000000 Weiße Wand\n" +
            "T  A 48.0" + DEG + "N 11.0" + DEG + "E 03-JUN-2011 07:24:40.250 s -9999.000000 0.0 0.0 0.0 0\n" +
            "T  A 48.1" + DEG + "N 11.1" + DEG + "E 03-JUN-2011 07:24:41.000 s 345.500000 0.0 0.0 0.0 0\n";

    @Test
    public void twoNavDialectWithBomIsUtf8() throws IOException {
        byte[] body = TWONAV_BODY.getBytes(UTF_8);
        byte[] bytes = new byte[body.length + 3];
        bytes[0] = (byte) 0xEF;
        bytes[1] = (byte) 0xBB;
        bytes[2] = (byte) 0xBF;
        System.arraycopy(body, 0, bytes, 3, body.length);
        // the BOM wins even if there is no "B  UTF-8" line
        byte[] withoutB = TWONAV_BODY.replace("B  UTF-8\n", "").getBytes(UTF_8);
        byte[] bomOnly = new byte[withoutB.length + 3];
        System.arraycopy(bytes, 0, bomOnly, 0, 3);
        System.arraycopy(withoutB, 0, bomOnly, 3, withoutB.length);

        for (byte[] content : asList(bytes, bomOnly)) {
            List<GpxRoute> routes = read(new CompeGPSDataWaypointFormat(), content);
            assertEquals(2, routes.size());
            assertEquals("Übung; Weiße Wand", routes.get(0).getPosition(0).getDescription());
        }
    }

    @Test
    public void twoNavDialectWithBLineIsUtf8WithoutBom() throws IOException {
        List<GpxRoute> routes = read(new CompeGPSDataWaypointFormat(), TWONAV_BODY.getBytes(UTF_8));
        assertEquals(2, routes.size());
        GpxPosition position = routes.get(0).getPosition(0);
        assertEquals("Übung; Weiße Wand", position.getDescription());
        assertNull(position.getElevation());
        assertEquals(millis("2011-06-03T07:24:40Z"), position.getTime().getTimeInMillis());
    }

    @Test
    public void withoutBomAndBLineWindows1252IsUsed() throws IOException {
        String text = TWONAV_BODY.replace("B  UTF-8\n", "");
        List<GpxRoute> routes = read(new CompeGPSDataWaypointFormat(), text.getBytes(WINDOWS_1252));
        assertEquals("Übung; Weiße Wand", routes.get(0).getPosition(0).getDescription());
    }

    @Test
    public void twoNavDialectTimesWithMillisecondsAndUnknownAltitude() throws IOException {
        List<GpxRoute> routes = read(new CompeGPSDataTrackFormat(), TWONAV_BODY.getBytes(UTF_8));
        assertEquals(2, routes.size());
        GpxRoute route = routes.get(1);
        assertEquals(2, route.getPositionCount());
        assertNull(route.getPosition(0).getElevation());
        assertEquals(millis("2011-06-03T07:24:40.250Z"), route.getPosition(0).getTime().getTimeInMillis());
        assertEquals(345.5, route.getPosition(1).getElevation(), DELTA);
        assertEquals(millis("2011-06-03T07:24:41Z"), route.getPosition(1).getTime().getTimeInMillis());
    }

    @Test
    public void nonWgs84DatumIsConvertedToWgs84() throws IOException {
        String text = "G  European 1950\nU  1\n" +
                "W  Munich A 48.0" + DEG + "N 11.0" + DEG + "E 27-MAR-62 00:00:00 500.000000\n" +
                "T  A 48.0" + DEG + "N 11.0" + DEG + "E 03-JUN-11 07:24:40 s 500.0\n";
        GpxPosition waypoint = read(new CompeGPSDataWaypointFormat(), text).get(0).getPosition(0);
        assertEquals(47.999154, waypoint.getLatitude(), 0.000001);
        assertEquals(10.998960, waypoint.getLongitude(), 0.000001);
        assertEquals(500.0, waypoint.getElevation(), DELTA);
        GpxPosition trackpoint = read(new CompeGPSDataTrackFormat(), text).get(1).getPosition(0);
        assertEquals(47.999154, trackpoint.getLatitude(), 0.000001);
        assertEquals(10.998960, trackpoint.getLongitude(), 0.000001);
    }

    @Test
    public void utmAndUnknownDatumAndInvalidFilesYieldNoRoute() throws IOException {
        CompeGPSDataFormat format = new CompeGPSDataWaypointFormat();
        assertTrue(read(format, "G  WGS 84\nU  0\nW  A A 123456 5000000 27-MAR-62 00:00:00 1.0\n").isEmpty());
        assertTrue(read(format, "G  No Such Datum\nU  1\nW  A A 48.0" + DEG + "N 11.0" + DEG + "E 27-MAR-62 00:00:00 1.0\n").isEmpty());
        assertTrue(read(format, "U  1\nW  A A 48.0" + DEG + "N 11.0" + DEG + "E 27-MAR-62 00:00:00 1.0\n").isEmpty());
        assertTrue(read(format, "U  1\nG  WGS 84\nW  A A 48.0" + DEG + "N 11.0" + DEG + "E 27-MAR-62 00:00:00 1.0\n").isEmpty());
        assertTrue(read(format, HEADER).isEmpty());
        assertTrue(read(format, "this is not a CompeGPS file\nat all\n").isEmpty());
        assertTrue(read(format, "").isEmpty());
    }

    @Test
    public void routeWithoutValidCoordinatesIsDropped() throws IOException {
        String text = HEADER + "T  A foo bar 03-JUN-11 07:24:40 s 1.0\n";
        assertTrue(read(new CompeGPSDataTrackFormat(), text).isEmpty());
        assertFalse(read(new CompeGPSDataTrackFormat(), TRACK).isEmpty());
    }

    @Test
    public void formatContract() {
        CompeGPSDataFormat route = new CompeGPSDataRouteFormat();
        assertEquals(".rte", route.getExtension());
        assertEquals("CompeGPS Data Route (*.rte)", route.getName());
        assertEquals(".trk", new CompeGPSDataTrackFormat().getExtension());
        assertEquals("CompeGPS Data Track (*.trk)", new CompeGPSDataTrackFormat().getName());
        assertEquals(".wpt", new CompeGPSDataWaypointFormat().getExtension());
        assertEquals("CompeGPS Data Waypoint (*.wpt)", new CompeGPSDataWaypointFormat().getName());
        assertFalse(route.isSupportsMultipleRoutes());
        assertTrue(route.isSupportsReading());
        assertTrue(route.isSupportsWriting());
    }

    private static GpxPosition position(double longitude, double latitude, Double elevation, String time, String description) {
        CompactCalendar calendar = time != null ? CompactCalendar.fromMillis(millis(time)) : null;
        return new GpxPosition(longitude, latitude, elevation, null, calendar, description);
    }

    @Test
    public void writesWaypoints() throws IOException {
        CompeGPSDataFormat format = new CompeGPSDataWaypointFormat();
        List<GpxPosition> positions = new ArrayList<>();
        positions.add(position(11.5, 48.1234567, 520.0, null, "Home; Sweet home"));
        positions.add(position(-11.6, -48.2, null, null, "Two Words"));
        String expected = "G  WGS 84\nU  1\n" +
                "W  Home A 48.1234567000ºN 11.5000000000ºE 27-MAR-62 00:00:00 520.000000 Sweet home\n" +
                "w  Waypoint,0,0.0,16777215,255,1,7,,0.0\n" +
                "W  Two_Words A 48.2000000000ºS 11.6000000000ºW 27-MAR-62 00:00:00 0.000000\n" +
                "w  Waypoint,0,0.0,16777215,255,1,7,,0.0\n";
        assertEquals(expected, write(format, format.createRoute(Waypoints, "ignored", positions)));
    }

    @Test
    public void writesRoute() throws IOException {
        CompeGPSDataFormat format = new CompeGPSDataRouteFormat();
        List<GpxPosition> positions = new ArrayList<>();
        positions.add(position(11.0, 48.0, 100.0, null, "Start"));
        String expected = "G  WGS 84\nU  1\n" +
                "R  16711680,My route x,1,-1\n" +
                "W  Start A 48.0000000000ºN 11.0000000000ºE 27-MAR-62 00:00:00 100.000000\n" +
                "w  Waypoint,0,0.0,16777215,255,1,7,,0.0\n";
        assertEquals(expected, write(format, format.createRoute(Route, "My route, x", positions)));
        assertTrue(write(format, format.createRoute(Route, null, positions)).contains("R  16711680, ,1,-1\n"));
    }

    @Test
    public void writesTrack() throws IOException {
        CompeGPSDataFormat format = new CompeGPSDataTrackFormat();
        List<GpxPosition> positions = new ArrayList<>();
        positions.add(position(11.0, 48.0, 400.5, "2011-06-03T07:24:40Z", null));
        positions.add(position(11.1, 48.1, null, null, null));
        String expected = "G  WGS 84\nU  1\n" +
                "T  A 48.0000000000ºN 11.0000000000ºE 03-JUN-11 07:24:40 s 400.5 0.0 0.0 0.0 0 -1000.0 -1.0 -1.0 -1.0 -1.0\n" +
                "t 4294967295|Morningride|-1|-1\n" +
                "T  A 48.1000000000ºN 11.1000000000ºE 01-JAN-70 00:00:00 s -99999999.0 0.0 0.0 0.0 0 -1000.0 -1.0 -1.0 -1.0 -1.0\n" +
                "F  1234\n";
        assertEquals(expected, write(format, format.createRoute(Track, "Morning|ride", positions)));
    }

    @Test
    public void writtenFilesCanBeReadBack() throws IOException {
        CompeGPSDataFormat format = new CompeGPSDataTrackFormat();
        List<GpxPosition> positions = new ArrayList<>();
        positions.add(position(11.0, 48.0, 400.5, "2011-06-03T07:24:40Z", null));
        positions.add(position(11.1, 48.1, 410.0, "2011-06-03T07:24:50Z", null));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        format.write(format.createRoute(Track, "Round trip", positions), out, 0, 2);
        GpxRoute route = read(format, out.toByteArray()).get(0);
        assertEquals("Round trip", route.getName());
        assertEquals(2, route.getPositionCount());
        assertEquals(400.5, route.getPosition(0).getElevation(), DELTA);
        assertEquals(millis("2011-06-03T07:24:50Z"), route.getPosition(1).getTime().getTimeInMillis());
    }
}

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

package slash.navigation.itn;

import slash.navigation.NavigationTestCase;
import slash.navigation.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

public class TripmasterTest extends NavigationTestCase {

    private void checkTripmaster1dot4ItnPosition(ItnPosition position) {
        assertEquals("Abstand 6", position.getReason());
        assertEquals("Bahrenfeld", position.getCity());
        assertEquals(34.0, position.getElevation());
        assertEquals(calendar(1970, 1, 1, 11, 32, 26).getTime(), position.getTime().getTime());
    }

    public void testTripmaster1dot4ItnPositionByParsingFile() {
        ItnPosition position = new ItnPosition(0, 0, "Abstand 6 - 11:32:26 - 34 m - Bahrenfeld");
        checkTripmaster1dot4ItnPosition(position);
    }

    public void testTripmaster1dot4ItnPositionByConvertingFromOtherFormat() {
        ItnPosition position = new ItnPosition(null, null, null, null, "Abstand 6 - 11:32:26 - 34 m - Bahrenfeld");
        checkTripmaster1dot4ItnPosition(position);
    }

    public void testTripmasterItnMiddlePosition() {
        ItnPosition position1 = new ItnPosition(0, 0, "Start : Weil Der Stadt - 27/12/2006 - 13:35:13 - 430.5 m - 0.0 km");
        assertEquals("Start : 27/12/2006 - 13:35:13", position1.getReason());
        assertEquals("Weil Der Stadt", position1.getCity());
        assertEquals(430.5, position1.getElevation());
        assertEquals(calendar(2006, 12, 27, 13, 35, 13), position1.getTime());

        ItnPosition position2 = new ItnPosition(0, 0, "Ende : Herrenberg - 14:03:45 - 437.4 m - 25.5 km");
        assertEquals("Ende : 14:03:45", position2.getReason());
        assertEquals("Herrenberg", position2.getCity());
        assertEquals(437.4, position2.getElevation());
        String cal1 = DateFormat.getDateTimeInstance().format(position2.getTime().getTime());
        Calendar expected = calendar(1970, 1, 1, 14, 3, 45);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(calendar(1970, 1, 1, 14, 3, 45), position2.getTime());

        ItnPosition position3 = new ItnPosition(0, 0, "13:39:33 - Distanz 2 : Weil Der Stadt - 408.3 m - 2.0 km - 39 km/h");
        assertEquals("Distanz 2", position3.getReason());
        assertEquals("Weil Der Stadt", position3.getCity());
        assertEquals(408.3, position3.getElevation());
        assertEquals(calendar(1970, 1, 1, 13, 39, 33), position3.getTime());
    }

    public void testTripmasterItnLongPosition() {
        ItnPosition position1 = new ItnPosition(0, 0, "18:51:59 - Dur. 0:05:55 : Hohenfelde (Hamburg) - 41.0 m - 0.2 Km - 0 Km/h - 6");
        assertEquals("Dur. 0:05:55", position1.getReason());
        assertEquals("Hohenfelde (Hamburg)", position1.getCity());
        assertEquals(41.0, position1.getElevation());
        assertEquals(calendar(1970, 1, 1, 18, 51, 59), position1.getTime());

        ItnPosition position2 = new ItnPosition(0, 0, "18:51:36 - Start : 21/07/2007 18:51:36 : Hohenfelde (Hamburg) - 41.0 m - 0.2 Km - 0 Km/h - 6");
        assertEquals("Start : 21/07/2007 18:51:36", position2.getReason());
        assertEquals("Hohenfelde (Hamburg)", position2.getCity());
        assertEquals(41.0, position2.getElevation());
        assertEquals(calendar(2007, 7, 21, 18, 51, 36), position2.getTime());
    }


    private void checkTripmasterItnComment(String expectedComment, String expectedReason, String comment) {
        ItnPosition position = new ItnPosition(0.0, 0.0, 0.0, null, comment);
        assertEquals(expectedReason, position.getReason());
        assertEquals(expectedComment, position.getCity());
        assertEquals(expectedComment, position.getComment());
    }

    public void testTripmasterItnShortComments() {
        checkTripmasterItnComment(null, "Waypoint", "13:35:13 - 430.5 m");
        checkTripmasterItnComment(null, "Waypoint", "23:33:44 - -2.5 m");
    }

    public void testTripmasterItnMiddleComments() {
        checkTripmasterItnComment("Weil Der Stadt", "Start : 27/12/2006 - 13:35:13", "Start : Weil Der Stadt - 27/12/2006 - 13:35:13 - 430.5 m - 0.0 km");
        checkTripmasterItnComment("Weil Der Stadt", "Kurs 83", "13:35:50 - Kurs 83 : Weil Der Stadt - 411.4 m - 0.0 km");
        checkTripmasterItnComment("Weil Der Stadt", "Wpt", "13:36:13 - Wpt : Weil Der Stadt - 408.5 m - 0.1 km");
        checkTripmasterItnComment("Herrenberg", "Ende : 14:03:45", "Ende : Herrenberg - 14:03:45 - 437.4 m - 25.5 km");
    }

    public void testTripmasterItnLongComments() {
        checkTripmasterItnComment("Altona-Altstadt", "Start : 31/08/2007 19:57:24", "19:57:24 - Start : 31/08/2007 19:57:24 : Altona-Altstadt - 18.2 m - 0.0 Km - 0 Km/h - 9");
        checkTripmasterItnComment("Altona-Altstadt", "Dist. 8", "19:57:24 - Dist. 8 : Altona-Altstadt - 25.5 m - 8.0 Km - 50 Km/h - 9");
        checkTripmasterItnComment("Altona-Altstadt", "Dur. 0:23:18", "19:57:24 - Dur. 0:23:18 : Altona-Altstadt - 25.7 m - 8.3 Km - 50 Km/h - 10");
        checkTripmasterItnComment("Altona-Altstadt", "Course 327", "19:57:24 - Course 327 : Altona-Altstadt - 27.5 m - 8.4 Km - 25 Km/h - 11");
        checkTripmasterItnComment("Altona-Altstadt", "Finish : 31/08/2007 20:15:11", "20:15:11 - Finish : 31/08/2007 20:15:11 : Altona-Altstadt - 24.6 m - 10.3 Km - 7 Km/h - 7");

        checkTripmasterItnComment("Abtsgmünd", "Start : 30/09/2007 09:34:55", "09:34:55 - Start : 30/09/2007 09:34:55 : Abtsgmünd - 369.2 m - 0.0 km - 0 km/h - 6");
        checkTripmasterItnComment("Abtsgmünd", "Kurs 193", "09:36:43 - Kurs 193 : Abtsgmünd - 371.1 m - 0.1 km - 18 km/h - 6");
        checkTripmasterItnComment("Neuschmiede", "Distanz 2", "09:39:18 - Distanz 2 : Neuschmiede - 379.5 m - 2.0 km - 67 km/h - 6");
        checkTripmasterItnComment("Hüttlingen DE", "Dauer 0:10:04", "09:44:58 - Dauer 0:10:04 : Hüttlingen DE - 407.8 m - 8.6 km - 71 km/h - 7");
        checkTripmasterItnComment("Weil Der Stadt", "Kurs 83", "13:35:50 - Kurs 83 : Weil Der Stadt - 411.4 m - 0.0 km - 5 km/h");
        checkTripmasterItnComment("Weil Der Stadt", "Wpt", "13:36:13 - Wpt : Weil Der Stadt - 408.5 m - 0.1 km - 25 km/h");
        checkTripmasterItnComment("Acigné", "Cape 155", "08:52:25 - Cape 155 : Acigné - 39.4 m - 2.1 km - 54 Km/h");
    }

    private void checkTripmasterGpxComment(String expectedComment, String expectedReason, String comment) {
        GpxPosition position = new GpxPosition(0.0, 0.0, 0.0, null, comment);
        assertEquals(expectedReason, position.getReason());
        assertEquals(expectedComment, position.getCity());
        assertEquals(expectedComment, position.getComment());
    }

    public void testTripmaster1dot4GpxComments() {
        checkTripmasterGpxComment("Deven (Gross Plasten)", "Richtung 358", "Richtung 358 - Deven (Gross Plasten)");
        checkTripmasterGpxComment("Gross Gievitz", "Abstand 10", "Abstand 10 - Gross Gievitz");
        checkTripmasterGpxComment("Malchin", "Punkt", "Punkt - Malchin");
    }

    public void testTripmasterGpxComments() {
        checkTripmasterGpxComment("Blunk", "Dur. 2:11:13", "Dur. 2:11:13 : Blunk");
        checkTripmasterGpxComment("Blunk", "Dist. 107", "Dist. 107 : Blunk");
        checkTripmasterGpxComment("Blunk", "Course 35", "Course 35 : Blunk");
    }

    private void readFiles(String extension, int routeCount, boolean expectElevation, boolean expectTime, RouteCharacteristics... characteristics) throws IOException {
        readFiles("tripmaster", extension, routeCount, expectElevation, expectTime, characteristics);
    }

    public void testAllTripmasterGpxTracks() throws IOException {
        readFiles(".gpx", 1, true, true, RouteCharacteristics.Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes.gpx"), 3, true, true, RouteCharacteristics.Waypoints, RouteCharacteristics.Route, RouteCharacteristics.Track);
    }

    public void testAllTripmasterItnTracks() throws IOException {
        readFiles(".itn", 1, true, true, RouteCharacteristics.Track);
    }

    public void testAllTripmasterKmlTracks() throws IOException {
        readFiles(".kml", 1, true, true, RouteCharacteristics.Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes.kml"), 1, true, false, RouteCharacteristics.Track);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes-2.kml"), 2, true, false, RouteCharacteristics.Waypoints, RouteCharacteristics.Waypoints);
        readFile(new File(SAMPLE_PATH, "tripmastr-with-3-routes-3.kml"), 3, true, false, RouteCharacteristics.Track, RouteCharacteristics.Waypoints, RouteCharacteristics.Waypoints);
    }

    public void testTripmaster1dot4GpxTrack() throws IOException {
        List<GpxRoute> routes = readSampleGpxFile(new Gpx10Format(), "tripmaster1.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(881, route.getPositionCount());
        GpxPosition position1 = route.getPositions().get(441);
        assertEquals(53.9783, position1.getLatitude());
        assertEquals(11.148, position1.getLongitude());
        assertEquals(22.6, position1.getElevation());
        assertEquals("Klütz", position1.getComment());
        assertEquals("Klütz", position1.getCity());
        assertEquals("Richtung 248", position1.getReason());
        Calendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        Calendar expected = calendar(2007, 6, 23, 14, 57, 14);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        GpxPosition position2 = route.getPositions().get(442);
        assertEquals(53.978, position2.getLatitude());
        assertEquals(11.1451, position2.getLongitude());
        assertEquals(18.0, position2.getElevation());
        assertEquals("Klütz", position2.getComment());
        assertEquals("Klütz", position2.getCity());
        assertEquals("Punkt", position2.getReason());

        GpxPosition position3 = route.getPositions().get(443);
        assertEquals(53.9778, position3.getLatitude());
        assertEquals(11.1386, position3.getLongitude());
        assertEquals(20.3, position3.getElevation());
        assertEquals("Klütz", position3.getComment());
        assertEquals("Klütz", position3.getCity());
        assertEquals("Abstand 211", position3.getReason());
    }

    public void testTripmasterGpxTrack() throws IOException {
        List<GpxRoute> routes = readSampleGpxFile(new Gpx10Format(), "tripmaster2.gpx");
        assertNotNull(routes);
        assertEquals(1, routes.size());
        GpxRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(735, route.getPositionCount());
        GpxPosition position1 = route.getPositions().get(441);
        assertEquals(53.79967, position1.getLatitude());
        assertEquals(10.36535, position1.getLongitude());
        assertEquals(17.9, position1.getElevation());
        assertEquals("Bad Oldesloe; 170.1 Km", position1.getComment());
        assertEquals("Bad Oldesloe; 170.1 Km", position1.getCity());
        assertEquals("Course 184", position1.getReason());
        Calendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        Calendar expected = calendar(2007, 7, 15, 15, 2, 53);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        GpxPosition position2 = route.getPositions().get(442);
        assertEquals(53.79544, position2.getLatitude());
        assertEquals(10.35700, position2.getLongitude());
        assertEquals(3.9, position2.getElevation());
        assertEquals("Bad Oldesloe; 170.9 Km", position2.getComment());
        assertEquals("Bad Oldesloe; 170.9 Km", position2.getCity());
        assertEquals("Dist. 171", position2.getReason());

        GpxPosition position3 = route.getPositions().get(443);
        assertEquals(53.79446, position3.getLatitude());
        assertEquals(10.35603, position3.getLongitude());
        assertEquals(5.6, position3.getElevation());
        assertEquals("Bad Oldesloe; 171.0 Km", position3.getComment());
        assertEquals("Bad Oldesloe; 171.0 Km", position3.getCity());
        assertEquals("Dur. 3:49:31", position3.getReason());
    }

    public void testTripmaster1dot4ItnTrack() throws IOException {
        File file = new File(SAMPLE_PATH + "tripmaster1.itn");
        List<ItnRoute> routes = readSampleItnFile("tripmaster1.itn", true);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        ItnRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(369, route.getPositionCount());
        ItnPosition position1 = route.getPositions().get(85);
        assertEquals(53.65066, position1.getLatitude());
        assertEquals(9.56348, position1.getLongitude());
        assertEquals(-5.4, position1.getElevation());
        assertEquals("Hohenhorst (Haselau)", position1.getComment());
        assertEquals("Hohenhorst (Haselau)", position1.getCity());
        assertEquals("Richtung 248", position1.getReason());
        Calendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        Calendar expected = calendar(file, 12, 12, 27);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        ItnPosition position2 = route.getPositions().get(86);
        assertEquals(53.65074, position2.getLatitude());
        assertEquals(9.56224, position2.getLongitude());
        assertEquals(-3.3, position2.getElevation());
        assertEquals("Hohenhorst (Haselau)", position2.getComment());
        assertEquals("Hohenhorst (Haselau)", position2.getCity());
        assertEquals("Punkt", position2.getReason());

        ItnPosition position3 = route.getPositions().get(97);
        assertEquals(53.6691, position3.getLatitude());
        assertEquals(9.57994, position3.getLongitude());
        assertEquals(-1.6, position3.getElevation());
        assertEquals("Audeich (Haselau)", position3.getComment());
        assertEquals("Audeich (Haselau)", position3.getCity());
        assertEquals("Abstand 46", position3.getReason());
    }

    public void testTripmasterItnTrack() throws IOException {
        List<ItnRoute> routes = readSampleItnFile("tripmaster3.itn", true);
        assertNotNull(routes);
        assertEquals(1, routes.size());
        ItnRoute route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals(6, route.getPositionCount());
        ItnPosition position1 = route.getPositions().get(0);
        assertEquals(53.56963, position1.getLatitude());
        assertEquals(10.0294, position1.getLongitude());
        assertEquals(41.0, position1.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position1.getComment());
        assertEquals("Hohenfelde (Hamburg)", position1.getCity());
        assertEquals("Start : 21/07/2007 18:51:36", position1.getReason());
        Calendar actual = position1.getTime();
        String cal1 = DateFormat.getDateTimeInstance().format(actual.getTime());
        Calendar expected = calendar(2007, 7, 21, 18, 51, 36);
        String cal2 = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTime(), actual.getTime());

        ItnPosition position2 = route.getPositions().get(1);
        assertEquals(53.56963, position2.getLatitude());
        assertEquals(10.0294, position2.getLongitude());
        assertEquals(41.0, position2.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position2.getComment());
        assertEquals("Hohenfelde (Hamburg)", position2.getCity());
        assertNull(position2.getReason());

        ItnPosition position3 = route.getPositions().get(2);
        assertEquals(53.56963, position3.getLatitude());
        assertEquals(10.0294, position3.getLongitude());
        assertEquals(41.0, position3.getElevation());
        assertEquals("Hohenfelde (Hamburg)", position3.getComment());
        assertEquals("Hohenfelde (Hamburg)", position3.getCity());
        assertEquals("Dur. 0:05:55", position3.getReason());
    }
}

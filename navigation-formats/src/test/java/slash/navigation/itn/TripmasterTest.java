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

import slash.common.type.CompactCalendar;
import slash.navigation.base.NavigationTestCase;
import slash.navigation.gpx.GpxPosition;

import java.text.DateFormat;

public class TripmasterTest extends NavigationTestCase {

    private void checkTripmaster14Position(TomTomPosition position) {
        assertEquals("Richtung 316", position.getReason());
        assertEquals("Bahrenfeld", position.getCity());
        assertEquals(34.0, position.getElevation());
        assertEquals(316.0, position.getHeading());
        assertEquals(calendar(1970, 1, 1, 11, 32, 26).getTime(), position.getTime().getTime());
    }

    public void testTripmaster14Position() {
        TomTomPosition position = new TomTomPosition(0, 0, "Richtung 316 - 11:32:26 - 34 m - Bahrenfeld");
        checkTripmaster14Position(position);
    }

    public void testTripmaster14PositionByConvertingFromOtherFormat() {
        TomTomPosition position = new TomTomPosition(null, null, null, null, null, "Richtung 316 - 11:32:26 - 34 m - Bahrenfeld");
        checkTripmaster14Position(position);
    }

    public void testTripmaster18ShortPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "09:02:43 - 47.5 m");
        assertEquals("Waypoint", position.getReason());
        assertEquals(47.5, position.getElevation());
        assertEquals(calendar(1970, 1, 1, 9, 2, 43), position.getTime());
    }

    public void testTripmaster25ShortStartPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "15:01:20 - Start : 26/02/2010 15:01:20 - 10.2 m");
        assertEquals("Start : 26/02/2010 15:01:20", position.getReason());
        assertEquals(10.2, position.getElevation());
        assertEquals(calendar(2010, 2, 26, 15, 1, 20), position.getTime());
    }

    public void testTripmaster25ShortIntermediatePosition() {
        TomTomPosition position1 = new TomTomPosition(0, 0, "15:05:00 - Kurs 173 - 10.4 m");
        assertEquals("Kurs 173", position1.getReason());
        assertEquals(173.0, position1.getHeading());
        assertEquals(10.4, position1.getElevation());
        assertEquals(calendar(1970, 1, 1, 15, 5, 0), position1.getTime());

        TomTomPosition position2 = new TomTomPosition(0, 0, "15:06:20 - Distanz 6 - 11.3 m");
        assertEquals("Distanz 6", position2.getReason());
        assertEquals(11.3, position2.getElevation());
        assertEquals(calendar(1970, 1, 1, 15, 6, 20), position2.getTime());

        TomTomPosition position3 = new TomTomPosition(0, 0, "15:08:43 - Dauer 0:07:33 - 23.5 m");
        assertEquals("Dauer 0:07:33", position3.getReason());
        assertEquals(23.5, position3.getElevation());
        assertEquals(calendar(1970, 1, 1, 15, 8, 43), position3.getTime());
    }

    public void testTripmaster25ShortEndPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "16:22:22 - Ende : 26/02/2010 16:22:22 - 9.8 m");
        assertEquals("Ende : 26/02/2010 16:22:22", position.getReason());
        assertEquals(9.8, position.getElevation());
        assertEquals(calendar(2010, 2, 26, 16, 22, 22), position.getTime());
    }

    public void testTripmasterMiddlePosition() {
        TomTomPosition position1 = new TomTomPosition(0, 0, "Start : Noyal-Sur-Vilaine - 23/11/2006 - 08:50:26 - 37.2 m - 0.4 Km");
        assertEquals("Start : 23/11/2006 - 08:50:26", position1.getReason());
        assertEquals("Noyal-Sur-Vilaine", position1.getCity());
        assertEquals(37.2, position1.getElevation());
        assertNull(position1.getSpeed());
        assertEquals(calendar(2006, 11, 23, 8, 50, 26), position1.getTime());

        TomTomPosition position2a = new TomTomPosition(0, 0, "Finish : Cesson-Sévigné - 09:03:23 - 51.9 m - 8.6 Km");
        assertEquals("Finish : 09:03:23", position2a.getReason());
        assertEquals("Cesson-Sévigné", position2a.getCity());
        assertEquals(51.9, position2a.getElevation());
        assertNull(position2a.getSpeed());
        String actualStr = DateFormat.getDateTimeInstance().format(position2a.getTime().getTime());
        CompactCalendar expected = calendar(1970, 1, 1, 9, 3, 23);
        String expectedStr = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(expectedStr, actualStr);
        assertEquals(calendar(1970, 1, 1, 9, 3, 23), position2a.getTime());

        TomTomPosition position2b = new TomTomPosition(0, 0, "Ende : Herrenberg - 14:03:45 - 437.4 m - 25.5 km");
        assertEquals("Ende : 14:03:45", position2b.getReason());
        assertEquals("Herrenberg", position2b.getCity());
        assertEquals(437.4, position2b.getElevation());
        assertNull(position2b.getSpeed());
        actualStr = DateFormat.getDateTimeInstance().format(position2b.getTime().getTime());
        expected = calendar(1970, 1, 1, 14, 3, 45);
        expectedStr = DateFormat.getDateTimeInstance().format(expected.getTime());
        assertEquals(expectedStr, actualStr);
        assertEquals(calendar(1970, 1, 1, 14, 3, 45), position2b.getTime());

        TomTomPosition position3a = new TomTomPosition(0, 0, "13:39:33 - Distanz 2 : Weil Der Stadt - 408.3 m - 2.0 km - 39 km/h");
        assertEquals("Distanz 2", position3a.getReason());
        assertEquals("Weil Der Stadt", position3a.getCity());
        assertEquals(39.0, position3a.getSpeed());
        assertEquals(408.3, position3a.getElevation());
        assertEquals(calendar(1970, 1, 1, 13, 39, 33), position3a.getTime());

        TomTomPosition position4 = new TomTomPosition(0, 0, "09:01:31 - Cape 125: Cesson-Sévigné - 62.0 m - 7.1 Km");
        assertEquals("Cape 125", position4.getReason());
        assertEquals("Cesson-Sévigné", position4.getCity());
        assertEquals(62.0, position4.getElevation());
        assertNull(position4.getSpeed());
        assertEquals(calendar(1970, 1, 1, 9, 1, 31), position4.getTime());

        TomTomPosition position5 = new TomTomPosition(0, 0, "18:51:45 - Hohenfelde (Hamburg) - 42.0 m - 0.2 Km - 2 Km/h - 5");
        assertEquals("Hohenfelde (Hamburg)", position5.getReason());
        assertEquals("Hohenfelde (Hamburg)", position5.getCity());
        assertEquals(42.0, position5.getElevation());
        assertEquals(2.0, position5.getSpeed());
        assertEquals(calendar(1970, 1, 1, 18, 51, 45), position5.getTime());
    }

    public void testTripmasterLongIntermediatePosition() {
        TomTomPosition position1 = new TomTomPosition(0, 0, "18:51:59 - Dur. 0:05:55 : Hohenfelde (Hamburg) - 41.0 m - 0.2 Km - 5 Km/h - 6");
        assertEquals("Dur. 0:05:55", position1.getReason());
        assertEquals("Hohenfelde (Hamburg)", position1.getCity());
        assertEquals(41.0, position1.getElevation());
        assertEquals(5.0, position1.getSpeed());
        assertEquals(calendar(1970, 1, 1, 18, 51, 59), position1.getTime());

        TomTomPosition position2 = new TomTomPosition(0, 0, "08:51:25 - Km 1.4: Acigné - 26.5 m - 1.4 km - 69 Km/h");
        assertEquals("Km 1.4", position2.getReason());
        assertEquals("Acigné", position2.getCity());
        assertEquals(26.5, position2.getElevation());
        assertEquals(69.0, position2.getSpeed());
        assertEquals(calendar(1970, 1, 1, 8, 51, 25), position2.getTime());
    }

    public void testTripmaster18LongStartPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "18:51:36 - Start : 21/07/2007 18:51:36 : Hohenfelde (Hamburg) - 1241.231 m - 0.2 Km - 12 Km/h - 6");
        assertEquals("Start : 21/07/2007 18:51:36", position.getReason());
        assertEquals("Hohenfelde (Hamburg)", position.getCity());
        assertEquals(1241.231, position.getElevation());
        assertEquals(12.0, position.getSpeed());
        assertEquals(calendar(2007, 7, 21, 18, 51, 36), position.getTime());
    }

    public void testTripmaster22LongStartPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "08:45:54 - Start : 04/04/2009 08:45:54 - 12.7 m - 0.0 Km - 5 Km/h - 11");
        assertEquals("Start : 04/04/2009 08:45:54", position.getReason());
        assertEquals("Start : 04/04/2009 08:45:54", position.getCity());
        assertEquals(12.7, position.getElevation());
        assertEquals(5.0, position.getSpeed());
        assertEquals(calendar(2009, 4, 4, 8, 45, 54), position.getTime());
    }

    public void testTripmaster31StartPosition() {
        TomTomPosition position1 = new TomTomPosition(0, 0, "10:05:16 - Start : 24/03/2012 10:05:16 - 19.3 m - 0.1 km");
        assertEquals("Start : 24/03/2012 10:05:16", position1.getReason());
        assertNull(position1.getCity());
        assertEquals(19.3, position1.getElevation());
        assertNull(position1.getSpeed());
        assertEquals(calendar(2012, 3, 24, 10, 5, 16), position1.getTime());

        TomTomPosition position2 = new TomTomPosition(0, 0, "08:41:40 - Start : 25/03/2012 08:41:40 - 16.7 m - 0.1 km - 0 Km");
        assertEquals("Start : 25/03/2012 08:41:40", position2.getReason());
        assertNull(position2.getCity());
        assertEquals(16.7, position2.getElevation());
        assertNull(position2.getSpeed());
        assertEquals(calendar(2012, 3, 25, 8, 41, 40), position2.getTime());
    }

    public void testTripmaster31EndPosition() {
        TomTomPosition position = new TomTomPosition(0, 0, "18:30:48 - Ende : 24/03/2012 18:30:48 - 25.0 m - 275.5 km");
        assertEquals("Ende : 24/03/2012 18:30:48", position.getReason());
        assertNull(position.getCity());
        assertEquals(25.0, position.getElevation());
        assertNull(position.getSpeed());
        assertEquals(calendar(2012, 3, 24, 18, 30, 48), position.getTime());
    }

    private void checkTripmasterDescription(String expectedDescription, String expectedReason, String description) {
        TomTomPosition position = new TomTomPosition(0.0, 0.0, 0.0, null, null, description);
        assertEquals(expectedReason, position.getReason());
        assertEquals(expectedDescription, position.getCity());
        assertEquals(expectedDescription, position.getDescription());
    }

    public void testTripmasterShortDescriptions() {
        checkTripmasterDescription(null, "Waypoint", "13:35:13 - 430.5 m");
        checkTripmasterDescription(null, "Waypoint", "23:33:44 - -2.5 m");
    }

    public void testTripmasterMiddleDescriptions() {
        checkTripmasterDescription("Weil Der Stadt", "Start : 27/12/2006 - 13:35:13", "Start : Weil Der Stadt - 27/12/2006 - 13:35:13 - 430.5 m - 0.0 km");
        checkTripmasterDescription("Weil Der Stadt", "Kurs 83", "13:35:50 - Kurs 83 : Weil Der Stadt - 411.4 m - 0.0 km");
        checkTripmasterDescription("Weil Der Stadt", "Wpt", "13:36:13 - Wpt : Weil Der Stadt - 408.5 m - 0.1 km");
        checkTripmasterDescription("Herrenberg", "Ende : 14:03:45", "Ende : Herrenberg - 14:03:45 - 437.4 m - 25.5 km");
    }

    public void testTripmasterLongDescriptions() {
        checkTripmasterDescription("Altona-Altstadt", "Start : 31/08/2007 19:57:24", "19:57:24 - Start : 31/08/2007 19:57:24 : Altona-Altstadt - 18.2 m - 0.0 Km - 0 Km/h - 9");
        checkTripmasterDescription("Altona-Altstadt", "Dist. 8", "19:57:24 - Dist. 8 : Altona-Altstadt - 25.5 m - 8.0 Km - 50 Km/h - 9");
        checkTripmasterDescription("Altona-Altstadt", "Dur. 0:23:18", "19:57:24 - Dur. 0:23:18 : Altona-Altstadt - 25.7 m - 8.3 Km - 50 Km/h - 10");
        checkTripmasterDescription("Altona-Altstadt", "Course 327", "19:57:24 - Course 327 : Altona-Altstadt - 27.5 m - 8.4 Km - 25 Km/h - 11");
        checkTripmasterDescription("Altona-Altstadt", "Finish : 31/08/2007 20:15:11", "20:15:11 - Finish : 31/08/2007 20:15:11 : Altona-Altstadt - 24.6 m - 10.3 Km - 7 Km/h - 7");

        checkTripmasterDescription("Abtsgm\u00fcnd", "Start : 30/09/2007 09:34:55", "09:34:55 - Start : 30/09/2007 09:34:55 : Abtsgm\u00fcnd - 369.2 m - 0.0 km - 0 km/h - 6");
        checkTripmasterDescription("Abtsgm\u00fcnd", "Kurs 193", "09:36:43 - Kurs 193 : Abtsgm\u00fcnd - 371.1 m - 0.1 km - 18 km/h - 6");
        checkTripmasterDescription("Neuschmiede", "Distanz 2", "09:39:18 - Distanz 2 : Neuschmiede - 379.5 m - 2.0 km - 67 km/h - 6");
        checkTripmasterDescription("H\u00fcttlingen DE", "Dauer 0:10:04", "09:44:58 - Dauer 0:10:04 : H\u00fcttlingen DE - 407.8 m - 8.6 km - 71 km/h - 7");
        checkTripmasterDescription("Weil Der Stadt", "Kurs 83", "13:35:50 - Kurs 83 : Weil Der Stadt - 411.4 m - 0.0 km - 5 km/h");
        checkTripmasterDescription("Weil Der Stadt", "Wpt", "13:36:13 - Wpt : Weil Der Stadt - 408.5 m - 0.1 km - 25 km/h");
        checkTripmasterDescription("Acigné", "Cape 155", "08:52:25 - Cape 155 : Acigné - 39.4 m - 2.1 km - 54 Km/h");
    }

    private void checkTripmasterGpxDescription(String expectedDescription, String expectedReason, String description) {
        GpxPosition position = new GpxPosition(0.0, 0.0, 0.0, null, null, description);
        assertEquals(expectedReason, position.getReason());
        assertEquals(expectedDescription, position.getCity());
        assertEquals(expectedDescription, position.getDescription());
    }

    public void testTripmaster14GpxDescriptions() {
        checkTripmasterGpxDescription("Deven (Gross Plasten)", "Richtung 358", "Richtung 358 - Deven (Gross Plasten)");
        checkTripmasterGpxDescription("Gross Gievitz", "Abstand 10", "Abstand 10 - Gross Gievitz");
        checkTripmasterGpxDescription("Malchin", "Punkt", "Punkt - Malchin");
    }

    public void testTripmasterGpxDescriptions() {
        checkTripmasterGpxDescription("Blunk", "Dur. 2:11:13", "Dur. 2:11:13 : Blunk");
        checkTripmasterGpxDescription("Blunk", "Dist. 107", "Dist. 107 : Blunk");
        checkTripmasterGpxDescription("Blunk", "Course 35", "Course 35 : Blunk");
    }
}

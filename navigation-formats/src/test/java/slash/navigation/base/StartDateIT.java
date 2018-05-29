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

package slash.navigation.base;

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.navigation.common.NavigationPosition;
import slash.navigation.gopal.GoPalTrackFormat;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.itn.TomTomRoute;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmea.NmeaRoute;

import java.io.File;
import java.io.FileInputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.JULY;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.common.TestCase.calendar;
import static slash.common.type.CompactCalendar.fromCalendar;
import static slash.navigation.base.NavigationTestCase.SAMPLE_PATH;
import static slash.navigation.base.NavigationTestCase.readSampleGopalTrackFile;
import static slash.navigation.base.NavigationTestCase.readSampleNmeaFile;
import static slash.navigation.base.NavigationTestCase.readSampleTomTomRouteFile;

public class StartDateIT {

    private void checkPosition(NavigationPosition position, CompactCalendar expectedDate) {
        CompactCalendar actual = position.getTime();
        DateFormat format = DateFormat.getDateTimeInstance();
        format.setTimeZone(CompactCalendar.UTC);
        String cal1 = format.format(actual.getTime());
        String cal2 = format.format(expectedDate.getTime());
        assertEquals(cal2, cal1);
        assertEquals(expectedDate.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expectedDate.getTime(), actual.getTime());
    }

    private void checkPositions(List<? extends BaseRoute> routes) {
        Calendar expectedDate = Calendar.getInstance();
        expectedDate.set(2007, JULY, 21);
        checkPositionsWithDate(routes, expectedDate);
    }

    @SuppressWarnings({"unchecked"})
    private void checkPositionsWithDate(List<? extends BaseRoute> routes, Calendar expectedDate) {
        int year = expectedDate.get(YEAR);
        int month = expectedDate.get(MONTH) + 1;
        int day = expectedDate.get(DAY_OF_MONTH);

        assertNotNull(routes);
        assertEquals(1, routes.size());
        BaseRoute<BaseNavigationPosition, BaseNavigationFormat> route = routes.get(0);
        assertEquals(3, route.getPositionCount());
        NavigationPosition position1 = route.getPositions().get(0);
        checkPosition(position1, calendar(year, month, day, 18, 51, 36));
        NavigationPosition position2 = route.getPositions().get(1);
        checkPosition(position2, calendar(year, month, day, 18, 51, 45));
        NavigationPosition position3 = route.getPositions().get(2);
        checkPosition(position3, calendar(year, month, day, 18, 51, 59));
    }

    @Test
    public void testFileStartDateForTomTomRouteWithDate() throws Exception {
        List<TomTomRoute> routes = readSampleTomTomRouteFile("startdate-with-date.itn", true);
        checkPositions(routes);
    }

    @Test
    public void testCurrentStartDateForTomTomRouteWithDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-with-date.itn");
        CompactCalendar startDate = CompactCalendar.now();
        ParserContext<TomTomRoute> context = new ParserContextImpl<>(source, startDate);
        new TomTom5RouteFormat().read(new FileInputStream(source), context);
        checkPositions(context.getRoutes());
    }

    @Test
    public void testNullStartDateForTomTomRouteWithDate() throws Exception {
        List<TomTomRoute> routes = readSampleTomTomRouteFile("startdate-with-date.itn", false);
        checkPositions(routes);
    }

    @Test
    public void testFileStartDateForTomTomRouteWithoutDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-without-date.itn");
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(source.lastModified());
        ParserContext<TomTomRoute> context = new ParserContextImpl<>(source, fromCalendar(startDate));
        new TomTom8RouteFormat().read(new FileInputStream(source), context);
        List<TomTomRoute> routes = context.getRoutes();
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testCurrentStartDateForTomTomRouteWithoutDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-without-date.itn");
        Calendar startDate = Calendar.getInstance();
        ParserContext<TomTomRoute> context = new ParserContextImpl<>(source, fromCalendar(startDate));
        new TomTom5RouteFormat().read(new FileInputStream(source), context);
        List<TomTomRoute> routes = context.getRoutes();
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testNullStartDateForTomTomRouteWithoutDate() throws Exception {
        List<TomTomRoute> routes = readSampleTomTomRouteFile("startdate-without-date.itn", false);
        Calendar startDate = Calendar.getInstance();
        startDate.set(1970, JANUARY, 1);
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testFileStartDateForNmeaWithDate() throws Exception {
        List<NmeaRoute> routes = readSampleNmeaFile("startdate-with-date.nmea", true);
        checkPositions(routes);
    }

    @Test
    public void testCurrentStartDateForNmeaWithDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-with-date.nmea");
        Calendar startDate = Calendar.getInstance();
        ParserContext<NmeaRoute> context = new ParserContextImpl<>(source, fromCalendar(startDate));
        new NmeaFormat().read(new FileInputStream(source), context);
        List<NmeaRoute> routes = context.getRoutes();
        checkPositions(routes);
    }

    @Test
    public void testNullStartDateForNmeaWithDate() throws Exception {
        List<NmeaRoute> routes = readSampleNmeaFile("startdate-with-date.nmea", false);
        checkPositions(routes);
    }

    @Test
    public void testFileStartDateForNmeaWithoutDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-without-date.nmea");
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(source.lastModified());
        ParserContext<NmeaRoute> context = new ParserContextImpl<>(source, fromCalendar(startDate));
        new NmeaFormat().read(new FileInputStream(source), context);
        List<NmeaRoute> routes = context.getRoutes();
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testCurrentStartDateForNmeaWithoutDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-without-date.nmea");
        Calendar startDate = Calendar.getInstance();
        ParserContext<NmeaRoute> context = new ParserContextImpl<>(source, fromCalendar(startDate));
        new NmeaFormat().read(new FileInputStream(source), context);
        List<NmeaRoute> routes = context.getRoutes();
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testNullStartDateForNmeaWithoutDate() throws Exception {
        List<NmeaRoute> routes = readSampleNmeaFile("startdate-without-date.nmea", false);
        Calendar startDate = Calendar.getInstance();
        startDate.set(1970, JANUARY, 1);
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testFileStartDateForGopalTrackWithoutDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-without-date.trk");
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(source.lastModified());
        ParserContext<SimpleRoute> context = new ParserContextImpl<>(source, fromCalendar(startDate));
        new GoPalTrackFormat().read(new FileInputStream(source), context);
        List<SimpleRoute> routes = context.getRoutes();
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testCurrentStartDateForGopalTrackWithoutDate() throws Exception {
        File source = new File(SAMPLE_PATH + "startdate-without-date.trk");
        Calendar startDate = Calendar.getInstance();
        ParserContext<SimpleRoute> context = new ParserContextImpl<>(source, fromCalendar(startDate));
        new GoPalTrackFormat().read(new FileInputStream(source), context);
        List<SimpleRoute> routes = context.getRoutes();
        checkPositionsWithDate(routes, startDate);
    }

    @Test
    public void testNullStartDateForGopalTrackWithoutDate() throws Exception {
        List<SimpleRoute> routes = readSampleGopalTrackFile("startdate-without-date.trk", false);
        Calendar startDate = Calendar.getInstance();
        startDate.set(1970, JANUARY, 1);
        checkPositionsWithDate(routes, startDate);
    }
}

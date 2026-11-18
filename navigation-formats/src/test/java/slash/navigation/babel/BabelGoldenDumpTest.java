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

package slash.navigation.babel;

import org.junit.Test;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static slash.common.type.CompactCalendar.fromMillis;

public class BabelGoldenDumpTest {
    // 2007-06-12T09:22:45.000Z
    private static final long TIME_MILLIS = 1181640165000L;

    @Test
    public void dumpsAllFieldsAndNulls() {
        GpxPosition full = new GpxPosition(11.5, 48.1, 123.4, 5.5, fromMillis(TIME_MILLIS), "pos desc");
        full.setHeading(90.0);
        full.setHdop(1.2);
        full.setVdop(2.3);
        full.setPdop(3.4);
        full.setSatellites(7);

        GpxPosition empty = new GpxPosition(null, null, null, null, null, null);

        GpxRoute route = new GpxRoute(new Gpx10Format(), RouteCharacteristics.Route, "R",
                asList("Line 1", "Line2"), asList(full, empty));

        List<BaseRoute<?, ?>> routes = singletonList(route);

        String expected = "{\n" +
                "  \"routes\" : [\n" +
                "    {\n" +
                "      \"characteristics\" : \"Route\",\n" +
                "      \"name\" : \"R\",\n" +
                "      \"description\" : [\n" +
                "        \"Line 1\",\n" +
                "        \"Line2\"\n" +
                "      ],\n" +
                "      \"positions\" : [\n" +
                "        {\n" +
                "          \"longitude\" : \"11.5000000\",\n" +
                "          \"latitude\" : \"48.1000000\",\n" +
                "          \"elevation\" : \"123.4000000\",\n" +
                "          \"time\" : \"2007-06-12T09:22:45.000Z\",\n" +
                "          \"speed\" : \"5.5000000\",\n" +
                "          \"heading\" : \"90.0000000\",\n" +
                "          \"hdop\" : \"1.2000000\",\n" +
                "          \"vdop\" : \"2.3000000\",\n" +
                "          \"pdop\" : \"3.4000000\",\n" +
                "          \"satellites\" : 7,\n" +
                "          \"description\" : \"pos desc\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"longitude\" : null,\n" +
                "          \"latitude\" : null,\n" +
                "          \"elevation\" : null,\n" +
                "          \"time\" : null,\n" +
                "          \"speed\" : null,\n" +
                "          \"heading\" : null,\n" +
                "          \"hdop\" : null,\n" +
                "          \"vdop\" : null,\n" +
                "          \"pdop\" : null,\n" +
                "          \"satellites\" : null,\n" +
                "          \"description\" : null\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        assertEquals(expected, BabelGoldenDump.toJson(routes));
    }

    @Test
    public void dumpsNullDescriptionAsJsonNull() {
        GpxRoute route = new GpxRoute(new Gpx10Format(), RouteCharacteristics.Route, "R", null, singletonList(
                new GpxPosition(1.0, 2.0, null, null, null, null)));

        String json = BabelGoldenDump.toJson(singletonList(route));

        assertEquals("{\n" +
                "  \"routes\" : [\n" +
                "    {\n" +
                "      \"characteristics\" : \"Route\",\n" +
                "      \"name\" : \"R\",\n" +
                "      \"description\" : null,\n" +
                "      \"positions\" : [\n" +
                "        {\n" +
                "          \"longitude\" : \"1.0000000\",\n" +
                "          \"latitude\" : \"2.0000000\",\n" +
                "          \"elevation\" : null,\n" +
                "          \"time\" : null,\n" +
                "          \"speed\" : null,\n" +
                "          \"heading\" : null,\n" +
                "          \"hdop\" : null,\n" +
                "          \"vdop\" : null,\n" +
                "          \"pdop\" : null,\n" +
                "          \"satellites\" : null,\n" +
                "          \"description\" : null\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}", json);
    }
}

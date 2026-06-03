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

package slash.navigation.fpl;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.common.TestCase.assertDoubleEquals;

public class GarminFlightPlanUtilTest {
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <flight-plan xmlns="http://www8.garmin.com/xmlschemas/FlightPlan/v1">
              <created>2026-06-03T09:30:00Z</created>
              <waypoint-table>
                <waypoint>
                  <identifier>LOWI</identifier>
                  <type>AIRPORT</type>
                  <country-code>AT</country-code>
                  <lat>47.2602</lat>
                  <lon>11.3439</lon>
                  <comment>INNSBRUCK</comment>
                  <elevation>1906.0</elevation>
                </waypoint>
              </waypoint-table>
              <route>
                <route-name>TEST ROUTE</route-name>
                <route-description>ROUTE DESCRIPTION</route-description>
                <flight-plan-index>1</flight-plan-index>
                <route-point>
                  <waypoint-identifier>LOWI</waypoint-identifier>
                  <waypoint-type>AIRPORT</waypoint-type>
                  <waypoint-country-code>AT</waypoint-country-code>
                </route-point>
              </route>
            </flight-plan>
            """;

    @Test
    public void testMarshalAndUnmarshalRoundTrip() throws Exception {
        Object flightPlan = GarminFlightPlanUtil.unmarshal(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(flightPlan);
        assertEquals("2026-06-03T09:30:00Z", invoke(flightPlan, "getCreated").toString());
        Object route = invoke(flightPlan, "getRoute");
        Object waypointTable = invoke(flightPlan, "getWaypointTable");
        assertEquals("TEST ROUTE", invoke(route, "getRouteName"));
        assertEquals("ROUTE DESCRIPTION", invoke(route, "getRouteDescription"));
        List<?> waypoints = invokeList(waypointTable, "getWaypoint");
        List<?> routePoints = invokeList(route, "getRoutePoint");
        assertEquals(1, waypoints.size());
        assertEquals(1, routePoints.size());

        Object waypoint = waypoints.get(0);
        assertEquals("LOWI", invoke(waypoint, "getIdentifier"));
        assertEquals("AIRPORT", invoke(waypoint, "getType"));
        assertEquals("AT", invoke(waypoint, "getCountryCode"));
        assertDoubleEquals(47.2602, ((Number) invoke(waypoint, "getLat")).doubleValue());
        assertDoubleEquals(11.3439, ((Number) invoke(waypoint, "getLon")).doubleValue());

        Object routePoint = routePoints.get(0);
        assertEquals("LOWI", invoke(routePoint, "getWaypointIdentifier"));
        assertEquals("AIRPORT", invoke(routePoint, "getWaypointType"));
        assertEquals("AT", invoke(routePoint, "getWaypointCountryCode"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Method marshal = GarminFlightPlanUtil.class.getDeclaredMethod("marshal", Class.forName("slash.navigation.fpl.binding.FlightPlan"), java.io.OutputStream.class);
        marshal.setAccessible(true);
        marshal.invoke(null, flightPlan, outputStream);
        String marshalled = outputStream.toString(StandardCharsets.UTF_8);

        assertTrue(marshalled.contains("<flight-plan"));
        assertTrue(marshalled.contains("http://www8.garmin.com/xmlschemas/FlightPlan/v1"));

        Object roundTripped = GarminFlightPlanUtil.unmarshal(new ByteArrayInputStream(marshalled.getBytes(StandardCharsets.UTF_8)));
        Object roundTrippedRoute = invoke(roundTripped, "getRoute");
        assertEquals("TEST ROUTE", invoke(roundTrippedRoute, "getRouteName"));
        assertEquals("LOWI", invoke(invokeList(roundTrippedRoute, "getRoutePoint").get(0), "getWaypointIdentifier"));
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        return target.getClass().getMethod(methodName).invoke(target);
    }

    @SuppressWarnings("unchecked")
    private static List<?> invokeList(Object target, String methodName) throws Exception {
        return (List<?>) invoke(target, methodName);
    }
}


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

package slash.navigation.rtz;

import org.junit.Test;
import slash.navigation.rtz.binding10.LegType;
import slash.navigation.rtz.binding10.ObjectFactory;
import slash.navigation.rtz.binding10.Route;
import slash.navigation.rtz.binding10.ScheduleElementType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link RtzUtil} unmarshal/marshal round-trips.
 */
public class RtzUtilTest {

    private static final String RTZ_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<route xmlns=\"http://www.cirm.org/RTZ/1/0\" version=\"1.0\">" +
            "<routeInfo routeName=\"Hamburg-Cuxhaven\"/>" +
            "<waypoints>" +
            "<waypoint id=\"1\" name=\"Elbe 1\"><position lat=\"53.9950\" lon=\"8.7133\"/></waypoint>" +
            "<waypoint id=\"2\"><position lat=\"53.8700\" lon=\"9.0500\"/></waypoint>" +
            "</waypoints>" +
            "</route>";

    // --- unmarshal ---

    @Test
    public void testUnmarshalReturnsNonNull() throws IOException {
        Route route = RtzUtil.unmarshal(new ByteArrayInputStream(RTZ_XML.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(route);
    }

    @Test
    public void testUnmarshalRouteName() throws IOException {
        Route route = RtzUtil.unmarshal(new ByteArrayInputStream(RTZ_XML.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(route.getRouteInfo());
        assertEquals("Hamburg-Cuxhaven", route.getRouteInfo().getRouteName());
    }

    @Test
    public void testUnmarshalVersion() throws IOException {
        Route route = RtzUtil.unmarshal(new ByteArrayInputStream(RTZ_XML.getBytes(StandardCharsets.UTF_8)));
        assertEquals("1.0", route.getVersion());
    }

    @Test
    public void testUnmarshalWaypointCount() throws IOException {
        Route route = RtzUtil.unmarshal(new ByteArrayInputStream(RTZ_XML.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, route.getWaypoints().getWaypoint().size());
    }

    @Test
    public void testUnmarshalFirstWaypoint() throws IOException {
        Route route = RtzUtil.unmarshal(new ByteArrayInputStream(RTZ_XML.getBytes(StandardCharsets.UTF_8)));
        Route.Waypoints.Waypoint waypoint = route.getWaypoints().getWaypoint().get(0);
        assertEquals(1, (int) waypoint.getId());
        assertEquals("Elbe 1", waypoint.getName());
        assertEquals(53.995, waypoint.getPosition().getLat(), 0.000001);
        assertEquals(8.7133, waypoint.getPosition().getLon(), 0.000001);
    }

    @Test
    public void testUnmarshalSecondWaypointHasNoName() throws IOException {
        Route route = RtzUtil.unmarshal(new ByteArrayInputStream(RTZ_XML.getBytes(StandardCharsets.UTF_8)));
        assertNull(route.getWaypoints().getWaypoint().get(1).getName());
    }

    // --- marshal ---

    @Test
    public void testMarshalWritesUtf8AndNamespace() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RtzUtil.marshal(buildTwoWaypointRouteWithSchedule(), out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue(xml.contains("encoding=\"UTF-8\""));
        assertTrue(xml.contains(RtzUtil.RTZ_10_NAMESPACE_URI));
        assertTrue(xml.contains("routeName=\"Hamburg-Cuxhaven\""));
    }

    @Test
    public void testMarshalWritesSequentialIdsDefaultWaypointAndLegs() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RtzUtil.marshal(buildTwoWaypointRouteWithSchedule(), out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertTrue(xml.contains("version=\"1.0\""));
        assertTrue(xml.contains("radius=\"0.5\""));
        // one leg for the defaultWaypoint, one per waypoint
        assertEquals(3, countOccurrences(xml, "geometryType=\"Loxodrome\""));
        assertTrue(xml.contains("id=\"1\""));
        assertTrue(xml.contains("id=\"2\""));
        assertTrue(xml.contains("<schedules>"));
        assertTrue(xml.contains("eta="));
    }

    // --- round-trip ---

    @Test
    public void testRoundTrip() throws Exception {
        Route original = buildTwoWaypointRouteWithSchedule();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RtzUtil.marshal(original, out);

        Route roundtripped = RtzUtil.unmarshal(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(roundtripped.getRouteInfo());
        assertEquals("Hamburg-Cuxhaven", roundtripped.getRouteInfo().getRouteName());
        assertEquals("1.0", roundtripped.getVersion());
        assertEquals(2, roundtripped.getWaypoints().getWaypoint().size());
        assertEquals(0.5, roundtripped.getWaypoints().getDefaultWaypoint().getRadius(), 0.000001);

        Route.Waypoints.Waypoint first = roundtripped.getWaypoints().getWaypoint().get(0);
        assertEquals(1, (int) first.getId());
        assertEquals("Elbe 1", first.getName());
        assertEquals(53.995, first.getPosition().getLat(), 0.000001);
        assertEquals(8.7133, first.getPosition().getLon(), 0.000001);
        assertEquals("Loxodrome", first.getLeg().getGeometryType());

        Route.Waypoints.Waypoint second = roundtripped.getWaypoints().getWaypoint().get(1);
        assertEquals(2, (int) second.getId());
        assertNull(second.getName());

        assertNotNull(roundtripped.getSchedules());
        Route.Schedules.Schedule schedule = roundtripped.getSchedules().getSchedule().get(0);
        assertEquals("Default", schedule.getName());
        assertNotNull(schedule.getManual());
        assertEquals(1, schedule.getManual().getScheduleElement().size());
        ScheduleElementType scheduleElement = schedule.getManual().getScheduleElement().get(0);
        assertEquals(1, (int) scheduleElement.getWaypointId());
        assertEquals(xmlTime("2026-09-10T09:00:00Z"), scheduleElement.getEta());
    }

    private static Route buildTwoWaypointRouteWithSchedule() throws DatatypeConfigurationException {
        ObjectFactory objectFactory = new ObjectFactory();

        Route route = objectFactory.createRoute();
        route.setVersion("1.0");

        Route.RouteInfo routeInfo = objectFactory.createRouteRouteInfo();
        routeInfo.setRouteName("Hamburg-Cuxhaven");
        route.setRouteInfo(routeInfo);

        Route.Waypoints waypoints = objectFactory.createRouteWaypoints();
        Route.Waypoints.DefaultWaypoint defaultWaypoint = objectFactory.createRouteWaypointsDefaultWaypoint();
        defaultWaypoint.setRadius(0.5);
        LegType defaultLeg = new LegType();
        defaultLeg.setGeometryType("Loxodrome");
        defaultWaypoint.setLeg(defaultLeg);
        waypoints.setDefaultWaypoint(defaultWaypoint);

        Route.Waypoints.Waypoint first = objectFactory.createRouteWaypointsWaypoint();
        first.setId(1);
        first.setName("Elbe 1");
        setPositionAndLeg(objectFactory, first, 53.995, 8.7133);
        waypoints.getWaypoint().add(first);

        Route.Waypoints.Waypoint second = objectFactory.createRouteWaypointsWaypoint();
        second.setId(2);
        setPositionAndLeg(objectFactory, second, 53.87, 9.05);
        waypoints.getWaypoint().add(second);
        route.setWaypoints(waypoints);

        Route.Schedules schedules = objectFactory.createRouteSchedules();
        Route.Schedules.Schedule schedule = objectFactory.createRouteSchedulesSchedule();
        schedule.setId(1);
        schedule.setName("Default");
        Route.Schedules.Schedule.Manual manual = objectFactory.createRouteSchedulesScheduleManual();
        ScheduleElementType scheduleElement = new ScheduleElementType();
        scheduleElement.setWaypointId(1);
        scheduleElement.setEta(xmlTime("2026-09-10T09:00:00Z"));
        manual.getScheduleElement().add(scheduleElement);
        schedule.setManual(manual);
        schedules.getSchedule().add(schedule);
        route.setSchedules(schedules);
        return route;
    }

    private static void setPositionAndLeg(ObjectFactory objectFactory, Route.Waypoints.Waypoint waypoint,
                                          double lat, double lon) {
        Route.Waypoints.Waypoint.Position position = objectFactory.createRouteWaypointsWaypointPosition();
        position.setLat(lat);
        position.setLon(lon);
        waypoint.setPosition(position);
        LegType leg = new LegType();
        leg.setGeometryType("Loxodrome");
        waypoint.setLeg(leg);
    }

    private static XMLGregorianCalendar xmlTime(String lexicalRepresentation) throws DatatypeConfigurationException {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(lexicalRepresentation);
    }

    private static int countOccurrences(String string, String searched) {
        int count = 0;
        for (int index = string.indexOf(searched); index != -1; index = string.indexOf(searched, index + 1))
            count++;
        return count;
    }
}

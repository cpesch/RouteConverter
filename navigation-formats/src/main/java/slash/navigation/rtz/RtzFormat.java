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

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.rtz.binding10.LegType;
import slash.navigation.rtz.binding10.ObjectFactory;
import slash.navigation.rtz.binding10.Route;
import slash.navigation.rtz.binding10.ScheduleElementType;

import jakarta.xml.bind.JAXBException;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static slash.common.io.Transfer.formatXMLTime;
import static slash.common.io.Transfer.parseXMLTime;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.common.NavigationConversion.formatBigDecimal;
import static slash.navigation.rtz.RtzUtil.marshal;
import static slash.navigation.rtz.RtzUtil.unmarshal;

/**
 * Reads and writes RTZ Route Exchange (IEC 61174 / CIRM, *.rtz) files as used
 * by ECDIS. Reads version 1.0 and 1.1, writes version 1.0.
 *
 * @author Christian Pesch
 */

public class RtzFormat extends XmlNavigationFormat<RtzRoute> {
    private static final String GEOMETRY_TYPE_LOXODROME = "Loxodrome";
    private static final String DEFAULT_ROUTE_NAME = "RouteConverter route";
    private static final double DEFAULT_WAYPOINT_RADIUS = 0.5;
    private static final int MAXIMUM_POSITION_FRACTION_DIGITS = 6;

    public String getName() {
        return "RTZ Route Exchange (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".rtz";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> RtzRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new RtzRoute(name, (List<Wgs84Position>) positions);
    }

    private Map<Integer, XMLGregorianCalendar> getEtasByWaypointId(Route route) {
        Map<Integer, XMLGregorianCalendar> result = new HashMap<>();
        Route.Schedules schedules = route.getSchedules();
        if (schedules == null || schedules.getSchedule().isEmpty())
            return result;

        // the times of the first schedule are used
        Route.Schedules.Schedule schedule = schedules.getSchedule().get(0);
        List<ScheduleElementType> scheduleElements = new ArrayList<>();
        if (schedule.getManual() != null)
            scheduleElements = schedule.getManual().getScheduleElement();
        else if (schedule.getCalculated() != null)
            scheduleElements = schedule.getCalculated().getScheduleElement();

        for (ScheduleElementType scheduleElement : scheduleElements) {
            if (scheduleElement.getEta() != null)
                result.put(scheduleElement.getWaypointId(), scheduleElement.getEta());
        }
        return result;
    }

    private RtzRoute process(Route route) {
        List<Wgs84Position> positions = new ArrayList<>();
        Route.Waypoints waypoints = route.getWaypoints();
        if (waypoints != null) {
            Map<Integer, XMLGregorianCalendar> etasByWaypointId = getEtasByWaypointId(route);
            for (Route.Waypoints.Waypoint waypoint : waypoints.getWaypoint()) {
                Route.Waypoints.Waypoint.Position position = waypoint.getPosition();
                if (position == null)
                    continue;
                positions.add(new Wgs84Position(position.getLon(), position.getLat(), null, null,
                        parseXMLTime(etasByWaypointId.get(waypoint.getId())), trim(waypoint.getName())));
            }
        }

        Route.RouteInfo routeInfo = route.getRouteInfo();
        return new RtzRoute(routeInfo != null ? asRouteName(routeInfo.getRouteName()) : null, positions);
    }

    public void read(InputStream source, ParserContext<RtzRoute> context) throws IOException {
        Route route = unmarshal(source);
        context.appendRoute(process(route));
    }

    private Route createRtz(RtzRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();

        Route result = objectFactory.createRoute();
        result.setVersion("1.0");

        Route.RouteInfo routeInfo = objectFactory.createRouteRouteInfo();
        String name = trim(route.getName());
        routeInfo.setRouteName(name != null ? name : DEFAULT_ROUTE_NAME);
        result.setRouteInfo(routeInfo);

        Route.Waypoints waypoints = objectFactory.createRouteWaypoints();
        Route.Waypoints.DefaultWaypoint defaultWaypoint = objectFactory.createRouteWaypointsDefaultWaypoint();
        defaultWaypoint.setRadius(DEFAULT_WAYPOINT_RADIUS);
        LegType defaultLeg = new LegType();
        defaultLeg.setGeometryType(GEOMETRY_TYPE_LOXODROME);
        defaultWaypoint.setLeg(defaultLeg);
        waypoints.setDefaultWaypoint(defaultWaypoint);

        List<ScheduleElementType> scheduleElements = new ArrayList<>();
        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            int id = i - startIndex + 1;

            Route.Waypoints.Waypoint waypoint = objectFactory.createRouteWaypointsWaypoint();
            waypoint.setId(id);
            waypoint.setName(position.getDescription());
            Route.Waypoints.Waypoint.Position rtzPosition = objectFactory.createRouteWaypointsWaypointPosition();
            rtzPosition.setLat(formatBigDecimal(position.getLatitude(), MAXIMUM_POSITION_FRACTION_DIGITS).doubleValue());
            rtzPosition.setLon(formatBigDecimal(position.getLongitude(), MAXIMUM_POSITION_FRACTION_DIGITS).doubleValue());
            waypoint.setPosition(rtzPosition);
            LegType leg = new LegType();
            leg.setGeometryType(GEOMETRY_TYPE_LOXODROME);
            waypoint.setLeg(leg);
            waypoints.getWaypoint().add(waypoint);

            if (position.getTime() != null) {
                ScheduleElementType scheduleElement = new ScheduleElementType();
                scheduleElement.setWaypointId(id);
                scheduleElement.setEta(formatXMLTime(position.getTime()));
                scheduleElements.add(scheduleElement);
            }
        }
        result.setWaypoints(waypoints);

        if (!scheduleElements.isEmpty()) {
            Route.Schedules schedules = objectFactory.createRouteSchedules();
            Route.Schedules.Schedule schedule = objectFactory.createRouteSchedulesSchedule();
            schedule.setId(1);
            schedule.setName("Default");
            Route.Schedules.Schedule.Manual manual = objectFactory.createRouteSchedulesScheduleManual();
            manual.getScheduleElement().addAll(scheduleElements);
            schedule.setManual(manual);
            schedules.getSchedule().add(schedule);
            result.setSchedules(schedules);
        }
        return result;
    }

    public void write(RtzRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal(createRtz(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}

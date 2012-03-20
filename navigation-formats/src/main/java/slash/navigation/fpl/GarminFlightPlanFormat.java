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

import slash.common.io.CompactCalendar;
import slash.navigation.fpl.binding.FlightPlan;
import slash.navigation.fpl.binding.ObjectFactory;
import slash.navigation.gpx.GpxFormat;
import slash.navigation.gpx.GpxPosition;
import slash.navigation.gpx.GpxRoute;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;
import static slash.common.io.Transfer.formatElevation;
import static slash.common.io.Transfer.formatPosition;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.fpl.GarminFlightPlanUtil.marshal;
import static slash.navigation.fpl.GarminFlightPlanUtil.unmarshal;

/**
 * Reads and writes Garmin Flight Plan (.fpl) files.
 *
 * @author Christian Pesch
 */

public class GarminFlightPlanFormat extends GpxFormat {
    private static final Logger log = Logger.getLogger(GarminFlightPlanFormat.class.getName());

    public String getExtension() {
        return ".fpl";
    }

    public String getName() {
        return "Garmin Flight Plan (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    private FlightPlan.WaypointTable.Waypoint find(FlightPlan.WaypointTable waypointTable, String waypointIdentifier) {
        List<FlightPlan.WaypointTable.Waypoint> waypoints = waypointTable.getWaypoint();
        for (FlightPlan.WaypointTable.Waypoint waypoint : waypoints) {
            if (waypoint.getIdentifier().equals(waypointIdentifier))
                return waypoint;
        }
        return null;
    }

    private GpxPosition process(FlightPlan.Route.RoutePoint routePoint, FlightPlan.WaypointTable.Waypoint waypoint) {
        if (waypoint == null) {
            String type = trim(routePoint.getWaypointType());
            String countryCode = trim(routePoint.getWaypointCountryCode());
            return new GpxPosition(
                    null, null, null, null, null,
                    trim(routePoint.getWaypointIdentifier()) +
                            (type != null ? " type=" + type : "") +
                            (countryCode != null ? " countryCode=" + countryCode : "")
            );
        }

        // TODO preserve/use type, countryCode, comment
        return new GpxPosition(
                waypoint.getLon(),
                waypoint.getLat(),
                waypoint.getElevation(),
                null,
                null,
                null,
                trim(waypoint.getIdentifier()),
                null,
                null,
                null,
                null,
                null
        );
    }

    private GpxRoute process(FlightPlan flightPlan) {
        List<GpxPosition> positions = new ArrayList<GpxPosition>();

        List<FlightPlan.Route.RoutePoint> routePoints = flightPlan.getRoute().getRoutePoint();
        for (FlightPlan.Route.RoutePoint routePoint : routePoints) {
            FlightPlan.WaypointTable.Waypoint waypoint = find(flightPlan.getWaypointTable(), routePoint.getWaypointIdentifier());
            positions.add(process(routePoint, waypoint));
        }

        return positions.size() > 0 ? new GpxRoute(this, Track, flightPlan.getRoute().getRouteName(), asDescription(flightPlan.getRoute().getRouteDescription()), positions, flightPlan) : null;
    }

    public List<GpxRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            FlightPlan flightPlan = unmarshal(source);
            GpxRoute result = process(flightPlan);
            return result != null ? asList(result) : null;
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }

    private FlightPlan createFpl(GpxRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();

        FlightPlan flightPlan = objectFactory.createFlightPlan();
        FlightPlan.Route flightPlanRoute = objectFactory.createFlightPlanRoute();
        flightPlanRoute.setRouteName(route.getName());
        flightPlanRoute.setRouteDescription(asDescription(route.getDescription()));
        flightPlanRoute.setFlightPlanIndex((short)1);
        FlightPlan.WaypointTable waypointTable = objectFactory.createFlightPlanWaypointTable();

        List<GpxPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GpxPosition position = positions.get(i);

            FlightPlan.Route.RoutePoint routePoint = objectFactory.createFlightPlanRouteRoutePoint();
            // TODO use countryCode, type from position
            routePoint.setWaypointIdentifier(position.getComment());
            routePoint.setWaypointType("USER WAYPOINT");
            flightPlanRoute.getRoutePoint().add(routePoint);
            FlightPlan.WaypointTable.Waypoint waypoint = objectFactory.createFlightPlanWaypointTableWaypoint();
            // TODO use comment, countryCode, type from position
            waypoint.setElevation(formatElevation(position.getElevation()));
            waypoint.setIdentifier(position.getComment());
            waypoint.setLat(formatPosition(position.getLatitude()));
            waypoint.setLon(formatPosition(position.getLongitude()));
            waypointTable.getWaypoint().add(waypoint);
        }

        flightPlan.setRoute(flightPlanRoute);
        flightPlan.setWaypointTable(waypointTable);
        return flightPlan;
    }

    public void write(GpxRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal(createFpl(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void write(List<GpxRoute> routes, OutputStream target) throws IOException {
        throw new UnsupportedOperationException();
    }
}
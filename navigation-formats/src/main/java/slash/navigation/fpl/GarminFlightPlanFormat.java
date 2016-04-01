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

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.WaypointType;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.fpl.binding.FlightPlan;
import slash.navigation.fpl.binding.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Transfer.trim;
import static slash.navigation.base.WaypointType.UserWaypoint;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.fpl.GarminFlightPlanUtil.marshal;
import static slash.navigation.fpl.GarminFlightPlanUtil.unmarshal;

/**
 * Reads and writes Garmin Flight Plan (.fpl) files.
 *
 * @author Christian Pesch
 */

public class GarminFlightPlanFormat extends XmlNavigationFormat<GarminFlightPlanRoute> {

    public String getName() {
        return "Garmin Flight Plan (*" + getExtension() + ")";
    }

    public String getExtension() {
        return ".fpl";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> GarminFlightPlanRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GarminFlightPlanRoute(name, null, (List<GarminFlightPlanPosition>) positions);
    }

    private FlightPlan.WaypointTable.Waypoint find(FlightPlan.WaypointTable waypointTable, String waypointIdentifier) {
        List<FlightPlan.WaypointTable.Waypoint> waypoints = waypointTable.getWaypoint();
        for (FlightPlan.WaypointTable.Waypoint waypoint : waypoints) {
            if (waypoint.getIdentifier().equals(waypointIdentifier))
                return waypoint;
        }
        return null;
    }

    private WaypointType parseWaypointType(String string) {
        WaypointType type = WaypointType.fromValue(string);
        return type != null ? type : UserWaypoint;
    }

    private CountryCode parseCountryCode(String string) {
        CountryCode code = CountryCode.fromValue(string);
        return code != null ? code : null;
    }

    private GarminFlightPlanPosition process(FlightPlan.Route.RoutePoint routePoint, FlightPlan.WaypointTable.Waypoint waypoint) {
        if (waypoint == null) {
            WaypointType type = parseWaypointType(routePoint.getWaypointType());
            String identifier = trim(routePoint.getWaypointIdentifier());
            CountryCode countryCode = parseCountryCode(routePoint.getWaypointCountryCode());
            return new GarminFlightPlanPosition(null, null, null, null,
                    type,
                    identifier,
                    countryCode
            );
        }

        return new GarminFlightPlanPosition(
                waypoint.getLon(),
                waypoint.getLat(),
                waypoint.getElevation(),
                trim(waypoint.getComment()),
                parseWaypointType(waypoint.getType()),
                trim(waypoint.getIdentifier()),
                parseCountryCode(waypoint.getCountryCode())
        );
    }

    private GarminFlightPlanRoute process(FlightPlan flightPlan) {
        List<GarminFlightPlanPosition> positions = new ArrayList<>();

        List<FlightPlan.Route.RoutePoint> routePoints = flightPlan.getRoute().getRoutePoint();
        for (FlightPlan.Route.RoutePoint routePoint : routePoints) {
            FlightPlan.WaypointTable.Waypoint waypoint = find(flightPlan.getWaypointTable(), routePoint.getWaypointIdentifier());
            positions.add(process(routePoint, waypoint));
        }

        return new GarminFlightPlanRoute(flightPlan.getRoute().getRouteName(), asDescription(flightPlan.getRoute().getRouteDescription()), positions);
    }

    public void read(InputStream source, ParserContext<GarminFlightPlanRoute> context) throws Exception {
        FlightPlan flightPlan = unmarshal(source);
        context.appendRoute(process(flightPlan));
    }

    private FlightPlan createFpl(GarminFlightPlanRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();

        FlightPlan flightPlan = objectFactory.createFlightPlan();
        FlightPlan.Route flightPlanRoute = objectFactory.createFlightPlanRoute();
        flightPlanRoute.setRouteName(asRouteName(route.getName()));
        flightPlanRoute.setRouteDescription(asDescription(route.getDescription()));
        flightPlanRoute.setFlightPlanIndex((short) 1);
        FlightPlan.WaypointTable waypointTable = objectFactory.createFlightPlanWaypointTable();

        List<GarminFlightPlanPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GarminFlightPlanPosition position = positions.get(i);

            FlightPlan.Route.RoutePoint routePoint = objectFactory.createFlightPlanRouteRoutePoint();
            String countryCode = "";
            if (position.getCountryCode() != null && position.getWaypointType() != null && !position.getWaypointType().equals(UserWaypoint))
                countryCode = position.getCountryCode().value();
            routePoint.setWaypointCountryCode(countryCode);
            routePoint.setWaypointIdentifier(position.getIdentifier());
            if (position.getWaypointType() != null)
                routePoint.setWaypointType(position.getWaypointType().value());
            flightPlanRoute.getRoutePoint().add(routePoint);

            FlightPlan.WaypointTable.Waypoint waypoint = objectFactory.createFlightPlanWaypointTableWaypoint();
            waypoint.setComment(position.getDescription());
            waypoint.setCountryCode(countryCode);
            waypoint.setElevation(formatElevation(position.getElevation()));
            waypoint.setIdentifier(position.getIdentifier());
            waypoint.setLat(formatPosition(position.getLatitude()));
            waypoint.setLon(formatPosition(position.getLongitude()));
            if (position.getWaypointType() != null)
                waypoint.setType(position.getWaypointType().value());
            waypointTable.getWaypoint().add(waypoint);
        }

        flightPlan.setRoute(flightPlanRoute);
        flightPlan.setWaypointTable(waypointTable);
        return flightPlan;
    }

    public void write(GarminFlightPlanRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal(createFpl(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void write(List<GarminFlightPlanRoute> routes, OutputStream target) {
        throw new UnsupportedOperationException();
    }
}
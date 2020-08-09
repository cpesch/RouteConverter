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

import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;
import slash.navigation.fpl.binding.FlightPlan;
import slash.navigation.fpl.binding.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import static java.lang.Math.min;
import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteComments.createRouteName;
import static slash.navigation.base.WaypointType.Airport;
import static slash.navigation.base.WaypointType.UserWaypoint;
import static slash.navigation.common.NavigationConversion.formatElevation;
import static slash.navigation.common.NavigationConversion.formatPosition;
import static slash.navigation.fpl.CountryCode.None;
import static slash.navigation.fpl.CountryCode.United_States;
import static slash.navigation.fpl.GarminFlightPlanUtil.marshal;
import static slash.navigation.fpl.GarminFlightPlanUtil.unmarshal;

/**
 * Reads and writes Garmin Flight Plan (.fpl) files.
 *
 * @author Christian Pesch
 */

public class GarminFlightPlanFormat extends XmlNavigationFormat<GarminFlightPlanRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(GarminFlightPlanFormat.class);
    private static final int AIRPORT_IDENTIFIER_LENGTH = 4;
    private static final int COUNTRY_CODE_IDENTIFIER_LENGTH = 2;

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

    private static int getMaximumIdentifierLength() {
        return preferences.getInt("maximumIdentifierLength", 6);
    }

    public static boolean hasValidIdentifier(String identifier) {
        return identifier != null &&
                identifier.equals(toLettersAndNumbers(identifier)) &&
                identifier.equals(identifier.toUpperCase()) &&
                identifier.length() <= getMaximumIdentifierLength();
    }

    private static boolean hasValidIdentifier(GarminFlightPlanPosition position, String identifier, List<GarminFlightPlanPosition> positions) {
        if(!hasValidIdentifier(identifier))
            return false;

        for(GarminFlightPlanPosition p : positions) {
            if(p.getIdentifier().equals(identifier) &&
                    !(p.getLongitude().equals(position.getLongitude()) && p.getLatitude().equals(position.getLatitude())))
                return false;
        }
        return true;
    }

    static String createValidIdentifier(String identifier) {
        if (identifier != null) {
            identifier = toLettersAndNumbers(identifier).toUpperCase();
            identifier = identifier.substring(0, min(identifier.length(), getMaximumIdentifierLength()));
        }
        return identifier;
    }

    public static String createValidIdentifier(GarminFlightPlanPosition position, List<GarminFlightPlanPosition> positions) {
        String identifier = createValidIdentifier(position.getIdentifier());
        int count = positions.indexOf(position);
        if(count < 0)
            count = 0;
        String result = identifier;

        // for the same coordinates only one identifier can be present
        while(!hasValidIdentifier(position, result, positions)) {
            result = createValidIdentifier(count + identifier);
            count++;
        }
        return result;
    }

    private static int getMaximumDescriptionLength() {
        return preferences.getInt("maximumDescriptionLength", 25);
    }

    public static boolean hasValidDescription(String description) {
        return description != null &&
                description.equals(toLettersAndNumbersAndSpaces(description)) &&
                description.equals(description.toUpperCase()) &&
                description.length() <= getMaximumDescriptionLength();
    }

    public static String createValidDescription(String description) {
        if (description != null) {
            description = toLettersAndNumbersAndSpaces(description).toUpperCase();
            description = description.substring(0, min(description.length(), getMaximumDescriptionLength()));
        }
        return description;
    }

    public static CountryCode createValidCountryCode(GarminFlightPlanPosition position) {
        if(position.getWaypointType().equals(UserWaypoint))
            return None;

        String identifier = position.getIdentifier();
        if (identifier != null) {
            // extra rule for the United States
            if (identifier.length() > 0 && identifier.charAt(0) == 'K')
                return United_States;

            if (identifier.length() >= COUNTRY_CODE_IDENTIFIER_LENGTH) {
                // find first two characters from identifier in country codes
                CountryCode countryCode = CountryCode.fromValue(identifier.substring(0, COUNTRY_CODE_IDENTIFIER_LENGTH));
                if (countryCode != null)
                    return countryCode;
            }
        }
        return None;
    }

    public static WaypointType createValidWaypointType(GarminFlightPlanPosition position) {
        String identifier = position.getIdentifier();
        // identifier with four characters are always airports
        if (identifier != null && identifier.length() == AIRPORT_IDENTIFIER_LENGTH)
            return Airport;
        return position.getWaypointType() != null ? position.getWaypointType() : UserWaypoint;
    }

    public int getMaximumRouteNameLength() {
        return preferences.getInt("maximumRouteNameLength", 25);
    }

    private String createValidRouteName(BaseRoute<GarminFlightPlanPosition, GarminFlightPlanFormat> route) {
        String name = trim(route.getName());
        if(name == null)
            name = createRouteName(route.getPositions());
        return asRouteName(toLettersAndNumbersAndSpaces(name.toUpperCase()));
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
        return CountryCode.fromValue(string);
    }

    private GarminFlightPlanPosition process(FlightPlan.Route.RoutePoint routePoint, FlightPlan.WaypointTable.Waypoint waypoint) {
        if (waypoint == null) {
            WaypointType type = parseWaypointType(routePoint.getWaypointType());
            String identifier = trim(routePoint.getWaypointIdentifier());
            CountryCode countryCode = parseCountryCode(routePoint.getWaypointCountryCode());
            return new GarminFlightPlanPosition(null, null, null, identifier, null,
                    type, countryCode
            );
        }

        return new GarminFlightPlanPosition(
                waypoint.getLon(),
                waypoint.getLat(),
                waypoint.getElevation(),
                trim(waypoint.getIdentifier()), trim(waypoint.getComment()),
                parseWaypointType(waypoint.getType()),
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

    public void read(InputStream source, ParserContext<GarminFlightPlanRoute> context) throws IOException {
        FlightPlan flightPlan = unmarshal(source);
        context.appendRoute(process(flightPlan));
    }

    private FlightPlan createFpl(GarminFlightPlanRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();

        FlightPlan flightPlan = objectFactory.createFlightPlan();
        FlightPlan.Route flightPlanRoute = objectFactory.createFlightPlanRoute();
        flightPlanRoute.setRouteName(createValidRouteName(route));
        flightPlanRoute.setRouteDescription(asDescription(route.getDescription()));
        flightPlanRoute.setFlightPlanIndex((short) 1);
        FlightPlan.WaypointTable waypointTable = objectFactory.createFlightPlanWaypointTable();

        Set<String> writtenIdentifiers = new HashSet<>();
        List<GarminFlightPlanPosition> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            GarminFlightPlanPosition position = positions.get(i);

            // always write country code for Garmin G1000 https://forum.routeconverter.com/thread-3057.html
            String countryCode = preferences.getBoolean("writeEmptyCountryCode", true) ? "" : null;
            if (position.getCountryCode() != null && position.getWaypointType() != null && !position.getWaypointType().equals(UserWaypoint))
                countryCode = position.getCountryCode().value();

            String identifier = createValidIdentifier(position, positions);
            if(!writtenIdentifiers.contains(identifier)) {
                FlightPlan.WaypointTable.Waypoint waypoint = objectFactory.createFlightPlanWaypointTableWaypoint();
                waypoint.setComment(createValidDescription(position.getDescription()));
                if (countryCode != null)
                    waypoint.setCountryCode(countryCode);
                waypoint.setElevation(formatElevation(position.getElevation()));
                waypoint.setIdentifier(identifier);
                waypoint.setLat(formatPosition(position.getLatitude()));
                waypoint.setLon(formatPosition(position.getLongitude()));
                if (position.getWaypointType() != null)
                    waypoint.setType(position.getWaypointType().value());
                waypointTable.getWaypoint().add(waypoint);

                writtenIdentifiers.add(identifier);
            }

            FlightPlan.Route.RoutePoint routePoint = objectFactory.createFlightPlanRouteRoutePoint();
            if (countryCode != null)
                routePoint.setWaypointCountryCode(countryCode);
            routePoint.setWaypointIdentifier(identifier);
            if (position.getWaypointType() != null)
                routePoint.setWaypointType(position.getWaypointType().value());
            flightPlanRoute.getRoutePoint().add(routePoint);
        }

        flightPlan.setWaypointTable(waypointTable);
        flightPlan.setRoute(flightPlanRoute);
        return flightPlan;
    }

    public void write(GarminFlightPlanRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal(createFpl(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void write(List<GarminFlightPlanRoute> routes, OutputStream target) {
        throw new UnsupportedOperationException();
    }
}

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

package slash.navigation.msfs;

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.msfs.binding.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Locale.US;
import static slash.common.io.Transfer.trim;
import static slash.navigation.common.UnitConversion.*;
import static slash.navigation.msfs.MSFSFlightPlanUtil.marshal;
import static slash.navigation.msfs.MSFSFlightPlanUtil.unmarshal;

/**
 * Reads and writes Microsoft Flight Simulator 2020 Flight Plan (.pln) files.
 *
 * @author Christian Pesch
 */

public class MSFSFlightPlanFormat extends XmlNavigationFormat<MSFSFlightPlanRoute> {
    private static final Logger log = Logger.getLogger(MSFSFlightPlanFormat.class.getName());

    private static final DecimalFormat ELEVATION_FORMAT = new DecimalFormat("+000000.00");
    static {
        ELEVATION_FORMAT.setPositivePrefix("+");
        ELEVATION_FORMAT.setNegativePrefix("-");
        ELEVATION_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(US));
    }

    public String getExtension() {
        return ".pln";
    }

    public String getName() {
        return "Microsoft Flight Simulator 2020 Flight Plan (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> MSFSFlightPlanRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new MSFSFlightPlanRoute(name, null, (List<Wgs84Position>) positions);
    }

    Double parseElevation(String elevation) {
        try {
            Number number = ELEVATION_FORMAT.parse(elevation);
            return number.doubleValue();
        } catch (ParseException e) {
            log.severe("Could not parse elevation '" + elevation + "'");
        }
        return null;
    }

    private Wgs84Position process(ATCWaypoint atcWaypoint) {
        String description = trim(atcWaypoint.getId());
        Double latitude = null, longitude = null, elevation = null;

        String worldPosition = atcWaypoint.getWorldPosition();
        if(worldPosition != null) {
            String[] split = worldPosition.split(",");
            if (split.length == 3) {
                latitude = ddmmss2latitude(split[0]);
                longitude = ddmmss2longitude(split[1]);
                elevation = parseElevation(split[2]);
            }
        }
        return new Wgs84Position(longitude, latitude, elevation, null, null, description, atcWaypoint);
    }

    private MSFSFlightPlanRoute process(SimBaseDocument simBaseDocument) {
        List<Wgs84Position> positions = new ArrayList<>();
        String name = null, description = null;

        FlightPlanFlightPlan flightPlan = simBaseDocument.getFlightPlanFlightPlan();
        if (flightPlan != null) {
            name = flightPlan.getTitle();
            description = flightPlan.getDescr();

            List<ATCWaypoint> atcWaypoints = flightPlan.getATCWaypoint();
            if(atcWaypoints != null) {
                for(ATCWaypoint atcWaypoint : atcWaypoints) {
                    positions.add(process(atcWaypoint));
                }
            }
        }
        return new MSFSFlightPlanRoute(name, asDescription(description), positions, simBaseDocument);
    }

    String formatElevation(Double elevation) {
        if(elevation == null)
            elevation = 0.0;
        return ELEVATION_FORMAT.format(elevation);
    }

    private SimBaseDocument createSimBaseDocument(MSFSFlightPlanRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        SimBaseDocument simBaseDocument = route.getSimBaseDocument();
        if (simBaseDocument == null)
            simBaseDocument = objectFactory.createSimBaseDocument();

        FlightPlanFlightPlan flightPlan = simBaseDocument.getFlightPlanFlightPlan();
        if (flightPlan == null)
            flightPlan = objectFactory.createFlightPlanFlightPlan();

        flightPlan.setTitle(asRouteName(route.getName()));
        flightPlan.setDescr(asDescription(route.getDescription()));

        Wgs84Position first = route.getPositionCount() >= startIndex ? route.getPosition(startIndex) : null;
        Wgs84Position last = route.getPositionCount() >= endIndex ? route.getPosition(endIndex - 1) : null;

        if (first != null)
            flightPlan.setDepartureID(first.getDescription());
        if (last != null)
            flightPlan.setDestinationID(last.getDescription());

        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);

            ATCWaypoint atcWaypoint = position.getOrigin(ATCWaypoint.class);
            if(atcWaypoint == null)
                atcWaypoint = objectFactory.createATCWaypoint();
            atcWaypoint.setId(position.getDescription());

            if(atcWaypoint.getATCWaypointType() == null)
                atcWaypoint.setATCWaypointType("Airport");

            String longitude = longitude2ddmmss(position.getLongitude());
            String latitude = latitude2ddmmss(position.getLatitude());
            String elevation = formatElevation(position.getElevation());
            atcWaypoint.setWorldPosition(latitude + "," + longitude + "," + elevation);

            ICAO icao = atcWaypoint.getICAO();
            if (icao == null)
                icao = objectFactory.createICAO();
            icao.setICAOIdent(position.getDescription());
            atcWaypoint.setICAO(icao);

            flightPlan.getATCWaypoint().add(atcWaypoint);
        }
        simBaseDocument.setFlightPlanFlightPlan(flightPlan);
        return simBaseDocument;
    }

    public void read(InputStream source, ParserContext<MSFSFlightPlanRoute> context) throws IOException {
        SimBaseDocument simBaseDocument = unmarshal(source);
        context.appendRoute(process(simBaseDocument));
    }

    public void write(MSFSFlightPlanRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal(createSimBaseDocument(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}

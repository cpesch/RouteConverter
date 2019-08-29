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

package slash.navigation.klicktel;

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.klicktel.binding.KDRoute;
import slash.navigation.klicktel.binding.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static slash.common.io.Transfer.*;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.klicktel.KlickTelUtil.unmarshal;

/**
 * Reads and writes klickTel Routenplaner 2009 (.krt) files.
 *
 * @author Christian Pesch
 */

public class KlickTelRouteFormat extends XmlNavigationFormat<KlickTelRoute> {

    public String getExtension() {
        return ".krt";
    }

    public String getName() {
        return "klickTel Routenplaner 2009 (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> KlickTelRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new KlickTelRoute(name, (List<Wgs84Position>) positions);
    }

    private KlickTelRoute process(KDRoute route) {
        List<Wgs84Position> positions = new ArrayList<>();
        for (KDRoute.Stations.Station station : route.getStations().getStation()) {
            KDRoute.Stations.Station.Point point = station.getPoint();
            String description = (station.getCountryShortcut() != null ? station.getCountryShortcut() + " " : "") +
                    (station.getPostalCode() != null ? station.getPostalCode() + " " : "") +
                    (station.getCity() != null ? station.getCity() : "") +
                    (station.getStreet() != null ? ", " + station.getStreet() : "") +
                    (station.getHouseNumber() != null ? " " + station.getHouseNumber() : "");
            positions.add(asWgs84Position(parseDouble(point.getLongitude()), parseDouble(point.getLatitude()), trim(description)));
        }
        return new KlickTelRoute(null, route.getRouteOptions(), positions);
    }

    public void read(InputStream source, ParserContext<KlickTelRoute> context) throws IOException {
        KDRoute KDRoute = unmarshal(source);
        context.appendRoute(process(KDRoute));
    }

    private String formatPosition(Double aDouble) {
        return formatDoubleAsString(aDouble, 8).replace('.', ',');
    }

    private KDRoute createKlicktel(KlickTelRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        KDRoute kdRoute = objectFactory.createKDRoute();
        kdRoute.setRouteOptions(route.getOptions());
        KDRoute.Stations stations = objectFactory.createKDRouteStations();
        kdRoute.setStations(stations);
        for (Wgs84Position position : route.getPositions()) {
            KDRoute.Stations.Station.Point point = objectFactory.createKDRouteStationsStationPoint();
            point.setLongitude(formatPosition(position.getLongitude()));
            point.setLatitude(formatPosition(position.getLatitude()));
            KDRoute.Stations.Station station = objectFactory.createKDRouteStationsStation();
            station.setCity(position.getDescription());

            // TODO write decomposed description
            // <Street>Raiffeisenstr.</Street>
            // <HouseNumber>33</HouseNumber>
            // <PostalCode>47665</PostalCode>
            // <City>Sonsbeck</City>
            // <CountryShortcut>D</CountryShortcut>

            // TODO as in GopalRouteFormat?
            // station.setCountryShortcut();
            // station.setDistrict();
            // station.setHouseNumber();
            // station.setPostalCode();
            // station.setStreet();

            // TODO start point?
            // station.setLocationType(4096);
            station.setPoint(point);
            stations.getStation().add(station);
        }
        return kdRoute;
    }

    public void write(KlickTelRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            KlickTelUtil.marshal(createKlicktel(route), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}
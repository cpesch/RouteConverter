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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.Wgs84Position;
import slash.navigation.XmlNavigationFormat;
import slash.navigation.util.Conversion;
import slash.navigation.klicktel.binding.KDRoute;
import slash.navigation.klicktel.binding.ObjectFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

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

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public <P extends BaseNavigationPosition> KlickTelRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new KlickTelRoute(name, (List<Wgs84Position>) positions);
    }

    private KlickTelRoute process(KDRoute route) {
        List<Wgs84Position> positions = new ArrayList<Wgs84Position>();
        for (KDRoute.Stations.Station station : route.getStations().getStation()) {
            KDRoute.Stations.Station.Point point = station.getPoint();
            String comment = (station.getCountryShortcut() != null ? station.getCountryShortcut() + " " : "") +
                    (station.getPostalCode() != null ? station.getPostalCode() + " " : "") +
                    (station.getCity() != null ? station.getCity() : "") +
                    (station.getStreet() != null ? ", " + station.getStreet() : "") +
                    (station.getHouseNumber() != null ? " " + station.getHouseNumber() : "");
            positions.add(new Wgs84Position(Conversion.parseDouble(point.getLongitude()),
                    Conversion.parseDouble(point.getLatitude()),
                    null, null, Conversion.trim(comment)));
        }
        return new KlickTelRoute(null, route.getRouteOptions(), positions);
    }

    public List<KlickTelRoute> read(InputStream source, Calendar startDate) throws IOException {
        try {
            KDRoute KDRoute = KlickTelUtil.unmarshal(source);
            return Arrays.asList(process(KDRoute));
        } catch (JAXBException e) {
            return null;
        }
    }

    private String formatAsDouble(Double aDouble) {
        return Conversion.formatDoubleAsString(aDouble, 8).replace('.', ',');
    }

    private KDRoute createKlicktel(KlickTelRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        KDRoute kdRoute = objectFactory.createKDRoute();
        kdRoute.setRouteOptions(route.getOptions());
        KDRoute.Stations stations = objectFactory.createKDRouteStations();
        kdRoute.setStations(stations);
        for (Wgs84Position position : route.getPositions()) {
            KDRoute.Stations.Station.Point point = objectFactory.createKDRouteStationsStationPoint();
            point.setLongitude(formatAsDouble(position.getLongitude()));
            point.setLatitude(formatAsDouble(position.getLatitude()));
            KDRoute.Stations.Station station = objectFactory.createKDRouteStationsStation();
            station.setCity(position.getComment());

            // TODO write decomposed comment
            // <Street>Raiffeisenstr.</Street>
            // <HouseNumber>33</HouseNumber>
            // <PostalCode>47665</PostalCode>
            // <City>Sonsbeck</City>
            // <CountryShortcut>D</CountryShortcut>

            // TODO as in GopalRouteFormat? station.setCountryShortcut();
            // TODO as in GopalRouteFormat? station.setDistrict();
            // TODO as in GopalRouteFormat? station.setHouseNumber();
            // TODO as in GopalRouteFormat? station.setPostalCode();
            // TODO as in GopalRouteFormat? station.setStreet();
            // TODO start point? station.setLocationType(4096);
            station.setPoint(point);
            stations.getStation().add(station);
        }
        return kdRoute;
    }

    public void write(KlickTelRoute route, File target, int startIndex, int endIndex, boolean numberPositionNames) throws IOException {
        try {
            KlickTelUtil.marshal(createKlicktel(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
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

package slash.navigation.gopal;

import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.NavigationPosition;
import slash.navigation.gopal.binding3.ObjectFactory;
import slash.navigation.gopal.binding3.Tour;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static slash.navigation.gopal.GoPalUtil.marshal3;
import static slash.navigation.gopal.GoPalUtil.unmarshal3;

/**
 * Reads and writes GoPal 3 Route (.xml) files.
 *
 * @author Christian Pesch
 */

public class GoPal3RouteFormat extends GoPalRouteFormat<GoPalRoute> {
    private static final String VERSION_PREFIX = "v3";

    public String getName() {
        return "GoPal 3 Route (*" + getExtension() + ")";
    }

    protected String getVersion() {
        return VERSION_PREFIX;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> GoPalRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GoPalRoute(this, name, (List<GoPalPosition>) positions);
    }

    public static String createGoPalFileName(String fileName) {
        fileName = fileName.toUpperCase();
        fileName = fileName.replaceAll("[^\\w.]", " ");
        return fileName;
    }

    private GoPalRoute process(Tour tour) {
        List<GoPalPosition> positions = new ArrayList<>();
        for (Tour.Dest dest : tour.getDest()) {
            Short country = dest.getCountry() != 0 ? dest.getCountry() : null;
            positions.add(new GoPalPosition(dest.getLongitude(), dest.getLatitude(), country, null, dest.getZip(), dest.getCity(), null, dest.getStreet(), null, dest.getHouse()));
        }
        return new GoPalRoute(this, null, tour.getOptions(), positions);
    }

    public void read(InputStream source, ParserContext<GoPalRoute> context) throws IOException {
        Tour tour = unmarshal3(source);
        context.appendRoute(process(tour));
    }

    private Tour.Options createOptions(GoPalRoute route) {
        Tour.Options options = route.getOptions(Tour.Options.class);
        if (options == null) {
            options = new ObjectFactory().createTourOptions();
            options.setType((short) preferences.getInt(VERSION_PREFIX + "type", 3)); // Fahrzeugtyp: 0=PKW 1=Fussgaenger 2=Fahrrad 3=Motorrad
            options.setMode((short) preferences.getInt(VERSION_PREFIX + "mode", 2)); // Art der Route: 0=kurz 1=schnell 2=Oekonomisch
            options.setFerries((short) preferences.getInt(VERSION_PREFIX + "ferries", 1)); // Faehren: 0=meiden 1=verwenden
            options.setMotorWays((short) preferences.getInt(VERSION_PREFIX + "motorWays", 0)); // Autobahn: 0=meiden 1=verwenden
            options.setTollRoad((short) preferences.getInt(VERSION_PREFIX + "tollRoad", 1)); // Mautstrassen: 0=meiden 1=verwenden
            options.setTunnels((short) preferences.getInt(VERSION_PREFIX + "tunnels", 1)); // Tunnel: 0=meiden 1=verwenden
            options.setTTIMode((short) preferences.getInt(VERSION_PREFIX + "ttiMode", 0)); // Stauumfahrung: 0=automatisch 1=manuell 2=keine
            options.setVehicleSpeedMotorway((short) preferences.getInt(VERSION_PREFIX + "vehicleSpeedMotorway", 33)); // Km/h
            options.setVehicleSpeedNonMotorway((short) preferences.getInt(VERSION_PREFIX + "vehicleSpeedNonMotorway", 27)); // Km/h
            options.setVehicleSpeedInPedestrianArea((short) preferences.getInt(VERSION_PREFIX + "vehicleSpeedInPedestrianArea", 2));
            options.setPedestrianSpeed((short) preferences.getInt(VERSION_PREFIX + "pedestrianSpeed", 1)); // Km/h
            options.setCyclistSpeed((short) preferences.getInt(VERSION_PREFIX + "cyclistSpeed", 4)); // Km/h
        }
        return options;
    }

    private Tour createGoPal(GoPalRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        Tour tour = objectFactory.createTour();
        tour.setOptions(createOptions(route));
        for (int i = startIndex; i < endIndex; i++) {
            GoPalPosition position = route.getPosition(i);
            Tour.Dest dest = objectFactory.createTourDest();
            if (position.getX() != null)
                dest.setLongitude(position.getX());
            if (position.getY() != null)
                dest.setLatitude(position.getY());
            dest.setCity(position.getCity());
            if (position.getCountry() != null)
                dest.setCountry(position.getCountry());
            if (position.getHouseNumber() != null)
                dest.setHouse(position.getHouseNumber());
            dest.setStreet(position.getStreet());
            dest.setZip(position.getZipCode());
            if (i == startIndex)
                dest.setStartPos((short) 1);
            tour.getDest().add(dest);
        }
        return tour;
    }

    public void write(GoPalRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            marshal3(createGoPal(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}

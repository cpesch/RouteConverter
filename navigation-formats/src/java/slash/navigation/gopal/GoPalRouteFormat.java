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

import slash.navigation.BaseNavigationPosition;
import slash.navigation.RouteCharacteristics;
import slash.navigation.XmlNavigationFormat;
import slash.common.io.CompactCalendar;
import slash.navigation.gopal.binding3.ObjectFactory;
import slash.navigation.gopal.binding3.Tour;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads and writes GoPal Route (.xml) files.
 *
 * @author Christian Pesch
 */

public class GoPalRouteFormat extends XmlNavigationFormat<GoPalRoute> {

    public String getExtension() {
        return ".xml";
    }

    public String getName() {
        return "GoPal Route (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public <P extends BaseNavigationPosition> GoPalRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new GoPalRoute(name, (List<GoPalPosition>) positions);
    }

    private GoPalRoute process(Tour tour) {
        List<GoPalPosition> positions = new ArrayList<GoPalPosition>();
        for (Tour.Dest dest : tour.getDest()) {
            Short country = dest.getCountry() != 0 ? dest.getCountry() : null;
            positions.add(new GoPalPosition(dest.getLongitude(), dest.getLatitude(), country, dest.getZip(), dest.getCity(), dest.getStreet(), dest.getHouse()));
        }
        return new GoPalRoute(null, tour.getOptions(), positions);
    }

    public List<GoPalRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            Tour tour = GoPalUtil.unmarshal(source);
            return Arrays.asList(process(tour));
        } catch (JAXBException e) {
            return null;
        }
    }


    private Tour createGoPal(GoPalRoute route) {
        ObjectFactory objectFactory = new ObjectFactory();
        Tour tour = objectFactory.createTour();
        for (GoPalPosition position : route.getPositions()) {
            Tour.Dest dest = objectFactory.createTourDest();
            if (position.getX() != null)
                dest.setLongitude(position.getX());
            if (position.getY() != null)
                dest.setLatitude(position.getY());
            dest.setCity(position.getCity());
            if (position.getCountry() != null)
                dest.setCountry(position.getCountry());
            if (position.getHouseNo() != null)
                dest.setHouse(position.getHouseNo());
            dest.setStreet(position.getStreet());
            dest.setZip(position.getZipCode());
            tour.getDest().add(dest);
            tour.setOptions(route.getOptions());
        }
        return tour;
    }

    public void write(GoPalRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            GoPalUtil.marshal(createGoPal(route), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

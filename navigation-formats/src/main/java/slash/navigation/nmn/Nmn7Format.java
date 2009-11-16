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

package slash.navigation.nmn;

import slash.navigation.RouteCharacteristics;
import slash.navigation.Wgs84Position;
import slash.navigation.nmn.binding7.ObjectFactory;
import slash.navigation.nmn.binding7.Route;
import slash.common.io.CompactCalendar;
import slash.common.io.Transfer;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.logging.Logger;

/**
 * Reads and writes Navigon Mobile Navigator 7 (.freshroute) files.
 *
 * @author Christian Pesch
 */

public class Nmn7Format extends NmnFormat {
    private static final Logger log = Logger.getLogger(Nmn7Format.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(Nmn7Format.class);

    public String getExtension() {
        return ".freshroute";
    }

    public String getName() {
        return "Navigon Mobile Navigator 7 (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumNmn7PositionCount", 48 /* ApplicationSettings.xml: <RouteTargets>50</RouteTargets> */);
    }

    protected boolean isPosition(String line) {
        throw new UnsupportedOperationException();
    }

    protected Wgs84Position parsePosition(String line, CompactCalendar startDate) {
        throw new UnsupportedOperationException();
    }

    private NmnRoute process(Route route) {
        List<NmnPosition> positions = new ArrayList<NmnPosition>();
        for (Route.Point point : route.getPoint()) {
            positions.add(new NmnPosition(Transfer.formatDouble(point.getX()), Transfer.formatDouble(point.getY()), (Double)null, null, null, point.getName()));
        }
        return new NmnRoute(this, RouteCharacteristics.Route, route.getName(), positions);
    }

    public List<NmnRoute> read(InputStream source, CompactCalendar startDate) throws IOException {
        try {
            Route route = Nmn7Util.unmarshal(source);
            return Arrays.asList(process(route));
        } catch (JAXBException e) {
            log.fine("Error reading " + source + ": " + e.getMessage());
            return null;
        }
    }


    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        throw new UnsupportedOperationException();
    }

    private Route createNmn(NmnRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        Route result = objectFactory.createRoute();
        result.setName(route.getName());
        for (int i = startIndex; i < endIndex; i++) {
            NmnPosition position = route.getPosition(i);
            Route.Point point = objectFactory.createRoutePoint();
            point.setX(Transfer.formatDouble(position.getLongitude(), 7));
            point.setY(Transfer.formatDouble(position.getLatitude(), 7));
            point.setName(position.getComment());
            result.getPoint().add(point);
        }
        return result;
    }

    public void write(NmnRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            Nmn7Util.marshal(createNmn(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
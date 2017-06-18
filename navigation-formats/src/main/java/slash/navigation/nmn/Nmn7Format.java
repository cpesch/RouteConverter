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

import slash.navigation.base.ParserContext;
import slash.navigation.base.Wgs84Position;
import slash.navigation.nmn.binding7.ObjectFactory;
import slash.navigation.nmn.binding7.Route;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.formatDouble;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.common.NavigationConversion.formatBigDecimal;
import static slash.navigation.nmn.Nmn7Util.unmarshal;

/**
 * Reads and writes Navigon Mobile Navigator 7 (.freshroute) files.
 *
 * @author Christian Pesch
 */

public class Nmn7Format extends NmnFormat {
    private static final Preferences preferences = Preferences.userNodeForPackage(Nmn7Format.class);

    public String getExtension() {
        return ".freshroute";
    }

    public String getName() {
        return "Navigon Mobile Navigator 7 (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumNavigon7PositionCount", 48 /* ApplicationSettings.xml: <RouteTargets>50</RouteTargets> */);
    }

    protected boolean isPosition(String line) {
        throw new UnsupportedOperationException();
    }

    protected Wgs84Position parsePosition(String line, ParserContext context) {
        throw new UnsupportedOperationException();
    }

    private NmnRoute process(Route route) {
        List<NmnPosition> positions = new ArrayList<>();
        for (Route.Point point : route.getPoint()) {
            positions.add(new NmnPosition(formatDouble(point.getX()), formatDouble(point.getY()), (Double) null, null, null, point.getName()));
        }
        return new NmnRoute(this, Route, route.getName(), positions);
    }

    public void read(InputStream source, ParserContext<NmnRoute> context) throws Exception {
        Route route = unmarshal(source);
        context.appendRoute(process(route));
    }


    protected void writePosition(Wgs84Position position, PrintWriter writer, int index, boolean firstPosition) {
        throw new UnsupportedOperationException();
    }

    private Route createNmn(NmnRoute route, int startIndex, int endIndex) {
        ObjectFactory objectFactory = new ObjectFactory();
        Route result = objectFactory.createRoute();
        result.setName(asRouteName(route.getName()));
        for (int i = startIndex; i < endIndex; i++) {
            NmnPosition position = route.getPosition(i);
            Route.Point point = objectFactory.createRoutePoint();
            point.setX(formatBigDecimal(position.getLongitude(), 7));
            point.setY(formatBigDecimal(position.getLatitude(), 7));
            point.setName(position.getDescription());
            result.getPoint().add(point);
        }
        return result;
    }

    public void write(NmnRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        try {
            Nmn7Util.marshal(createNmn(route, startIndex, endIndex), target);
        } catch (JAXBException e) {
            throw new IOException("Cannot marshall " + route + ": " + e, e);
        }
    }
}
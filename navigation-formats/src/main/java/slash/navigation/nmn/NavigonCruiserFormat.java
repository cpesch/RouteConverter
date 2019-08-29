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
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.XmlNavigationFormat;
import slash.navigation.common.NavigationPosition;
import slash.navigation.nmn.bindingcruiser.Root;
import slash.navigation.nmn.bindingcruiser.Route;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static slash.common.io.Transfer.*;
import static slash.navigation.nmn.NavigonCruiserUtil.marshal;
import static slash.navigation.nmn.NavigonCruiserUtil.unmarshal;

/**
 * Reads and writes NavigonCruiser (.cruiser) files.
 *
 * @author Christian Pesch
 */

public class NavigonCruiserFormat extends XmlNavigationFormat<NavigonCruiserRoute> {
    private static final Preferences preferences = Preferences.userNodeForPackage(NavigonCruiserFormat.class);

    public String getExtension() {
        return ".cruiser";
    }

    public String getName() {
        return "Navigon Cruiser (*" + getExtension() + ")";
    }

    public boolean isSupportsMultipleRoutes() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return false;
    }

    public int getMaximumPositionCount() {
        return preferences.getInt("maximumNavigonCruiserPositionCount", 100);
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> NavigonCruiserRoute createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new NavigonCruiserRoute(name, (List<Wgs84Position>) positions);
    }

    private NavigonCruiserRoute process(Route route) {
        String routeName = trim(route.getName());
        List<Wgs84Position> positions = new ArrayList<>();
        for(String coord : route.getCoords()) {
            String[] parts = coord.split(",");
            if (parts.length != 2)
                throw new IllegalArgumentException(coord + " are not valid coordinates");
            Double latitude = parseDouble(parts[0]);
            Double longitude = parseDouble(parts[1]);
            positions.add(new Wgs84Position(longitude, latitude, null, null, null, null));
        }
        return new NavigonCruiserRoute(routeName, positions);
    }


    public void read(InputStream source, ParserContext<NavigonCruiserRoute> context) throws IOException {
        Root root = unmarshal(source);
        context.appendRoute(process(root.getRoute()));
    }


    private Root createRoute(NavigonCruiserRoute route) {
        Route result = new Route();
        for (Wgs84Position position : route.getPositions()) {
            result.getCoords().add(formatDoubleAsString(position.getLatitude(), 5) + "," +
                    formatDoubleAsString(position.getLongitude(), 5));
        }
        result.setCreator(GENERATED_BY);
        result.setName(route.getName());

        return new Root(result);
    }

    public void write(NavigonCruiserRoute route, OutputStream target, int startIndex, int endIndex) throws IOException {
        marshal(createRoute(route), target);
    }
}

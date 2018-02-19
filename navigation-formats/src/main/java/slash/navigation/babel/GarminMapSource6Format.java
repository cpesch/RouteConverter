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

package slash.navigation.babel;

import slash.navigation.base.MultipleRoutesFormat;
import slash.navigation.gpx.GpxRoute;

/**
 * Reads and writes Garmin MapSource 6.x (.gdb) files.
 *
 * @author Christian Pesch
 */

public class GarminMapSource6Format extends BabelFormat implements MultipleRoutesFormat<GpxRoute> {
    public String getExtension() {
        return ".gdb";
    }

    public String getName() {
        return "Garmin MapSource 6.x (*" + getExtension() + ")";
    }

    protected String getFormatName() {
        return "gdb";
    }

    protected String getFormatOptions(GpxRoute route) {
        return ",ver=3";
    }

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    public boolean isWritingRouteCharacteristics() {
        return true; // just guesses 
    }

    protected boolean isStreamingCapable() {
        return true;
    }

    /*
    private String makeUnique(String key, Set<String> keys) {
        String unique = key;
        int number = 2;
        System.out.println("");
        while (keys.contains(unique)) {
            unique = key + "@" + number;
            number++;
        }
        return unique;
    }

    private List<GpxPosition> makeDescriptionsUnique(List<GpxPosition> positions, Set<String> names) {
        List<GpxPosition> result = new ArrayList<>();
        for(GpxPosition position : positions) {
            String name = makeUnique(asName(position.getDescription()), names);
            names.add(name);
            String description = asDescription(name, asDesc(position.getDescription()));
            result.add(new GpxPosition(position.getLongitude(), position.getLatitude(), position.getElevation(), position.getSpeed(), position.getTime(), description));
        }
        return result;
    }

    protected List<GpxRoute> modifyBeforeWriting(List<GpxRoute> routes) {
        List<GpxRoute> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for(GpxRoute route : routes) {
            result.add(new GpxRoute(route.getFormat(), route.getCharacteristics(), route.getName(), route.getDescription(), makeDescriptionsUnique(route.getPositions(), names)));
        }
        return result;
    }
    */
}

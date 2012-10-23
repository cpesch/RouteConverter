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

package slash.navigation.simple;

import slash.common.type.CompactCalendar;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.SimpleFormat;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static slash.common.io.InputOutput.readBytes;
import static slash.navigation.base.RouteCharacteristics.Route;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;
import static slash.navigation.util.Positions.center;
import static slash.navigation.util.Positions.northEast;
import static slash.navigation.util.Positions.southWest;

/**
 * Writes a Web Page (*.html).
 *
 * @author Christian Pesch
 */

public class WebPageFormat extends SimpleFormat<Wgs84Route> {

    public String getExtension() {
        return ".html";
    }

    public String getName() {
        return "Web Page (*" + getExtension() + ")";
    }

    public boolean isSupportsReading() {
        return false;
    }

    public boolean isWritingRouteCharacteristics() {
        return true;
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings({"unchecked"})
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, CompactCalendar startDate, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) throws IOException {
        String template = new String(readBytes(getClass().getResourceAsStream("webpage.html")));
        List<Wgs84Position> positions = route.getPositions();

        StringBuilder routeBuffer = new StringBuilder();
        if (route.getCharacteristics() == Route) {
            for (Wgs84Position position : positions) {
                routeBuffer.append("new google.maps.LatLng(").append(position.getLatitude()).append(",").
                        append(position.getLongitude()).append("),");
            }
        }

        StringBuilder trackBuffer = new StringBuilder();
        if (route.getCharacteristics() == Track) {
            for (Wgs84Position position : positions) {
                trackBuffer.append("new google.maps.LatLng(").append(position.getLatitude()).append(",").
                        append(position.getLongitude()).append("),");
            }
        }

        StringBuilder waypointsBuffer = new StringBuilder();
        if (route.getCharacteristics() == Waypoints) {
            for (Wgs84Position position : positions) {
                waypointsBuffer.append("new google.maps.Marker({position:new google.maps.LatLng(").
                        append(position.getLatitude()).append(",").append(position.getLongitude()).append("), title: \")").
                        append(position.getComment()).append("\", clickable:false, icon:markerIcon}),");
            }
        }

        NavigationPosition southWest = southWest(positions);
        String southWestBuffer = "new google.maps.LatLng(" + southWest.getLatitude() + "," + southWest.getLongitude() + ")";
        NavigationPosition northEast = northEast(positions);
        String northEastBuffer = "new google.maps.LatLng(" + northEast.getLatitude() + "," + northEast.getLongitude() + ")";

        NavigationPosition center = center(positions);
        String centerBuffer = "new google.maps.LatLng(" + center.getLatitude() + "," + center.getLongitude() + ")";

        String output = template.replaceAll("INSERT_ROUTENAME", route.getName()).
                replaceAll("INSERT_TRACK", routeBuffer.toString()).
                replaceAll("INSERT_ROUTE", trackBuffer.toString()).
                replaceAll("INSERT_WAYPOINTS", waypointsBuffer.toString()).
                replaceAll("INSERT_SOUTHWEST", southWestBuffer).
                replaceAll("INSERT_NORTHEAST", northEastBuffer).
                replaceAll("INSERT_CENTER", centerBuffer);
        writer.println(output);
    }
}
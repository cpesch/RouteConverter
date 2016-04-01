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

import slash.navigation.base.*;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import static slash.common.io.InputOutput.readBytes;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.navigation.base.RouteCharacteristics.*;

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

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF8_ENCODING, startIndex, endIndex);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) throws IOException {
        String template = new String(readBytes(getClass().getResourceAsStream("webpage.html")), UTF8_ENCODING);
        List<Wgs84Position> positions = route.getPositions();

        StringBuilder routeBuffer = new StringBuilder();
        if (route.getCharacteristics() == Route) {
            for (int i = 0; i < positions.size(); i++) {
                Wgs84Position position = positions.get(i);
                routeBuffer.append("new google.maps.LatLng(").append(position.getLatitude()).append(",").
                        append(position.getLongitude()).append(")");
                if (i < positions.size() - 1)
                    routeBuffer.append(",");
            }
        }

        StringBuilder trackBuffer = new StringBuilder();
        if (route.getCharacteristics() == Track) {
            for (int i = 0; i < positions.size(); i++) {
                Wgs84Position position = positions.get(i);
                trackBuffer.append("new google.maps.LatLng(").append(position.getLatitude()).append(",").
                        append(position.getLongitude()).append(")");
                if (i < positions.size() - 1)
                    trackBuffer.append(",");
            }
        }

        StringBuilder waypointsBuffer = new StringBuilder();
        if (route.getCharacteristics() == Waypoints) {
            for (int i = 0; i < positions.size(); i++) {
                Wgs84Position position = positions.get(i);
                waypointsBuffer.append("new google.maps.Marker({position:new google.maps.LatLng(").
                        append(position.getLatitude()).append(",").append(position.getLongitude()).append("), title: \")").
                        append(position.getDescription()).append("\", clickable:false, icon:markerIcon})");
                if (i < positions.size() - 1)
                    waypointsBuffer.append(",");
            }
        }

        BoundingBox boundingBox = new BoundingBox(positions);
        String southWestBuffer = "new google.maps.LatLng(" + boundingBox.getSouthWest().getLatitude() + "," + boundingBox.getSouthWest().getLongitude() + ")";
        String northEastBuffer = "new google.maps.LatLng(" + boundingBox.getNorthEast().getLatitude() + "," + boundingBox.getNorthEast().getLongitude() + ")";
        String centerBuffer = "new google.maps.LatLng(" + boundingBox.getCenter().getLatitude() + "," + boundingBox.getCenter().getLongitude() + ")";

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
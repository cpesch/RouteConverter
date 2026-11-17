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

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.ExtendedSensorNavigationPosition;
import slash.navigation.base.Wgs84Position;
import slash.navigation.common.NavigationPosition;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.String.format;
import static java.util.Locale.ROOT;

/**
 * Dumps {@link BaseRoute}s read by a GPSBabel-backed format into a canonical JSON
 * representation, so a later native port can be compared against what GPSBabel delivered
 * (spec 00083 H1).
 *
 * @author Christian Pesch
 */

public class BabelGoldenDump {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", ROOT).withZone(ZoneOffset.UTC);

    private BabelGoldenDump() {
    }

    public static String toJson(List<BaseRoute<?, ?>> routes) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode routesNode = root.putArray("routes");
        for (BaseRoute<?, ?> route : routes)
            routesNode.add(toRouteNode(mapper, route));

        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        ObjectWriter writer = mapper.writer(printer);
        try {
            return writer.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot write golden JSON", e);
        }
    }

    private static ObjectNode toRouteNode(ObjectMapper mapper, BaseRoute<?, ?> route) {
        ObjectNode node = mapper.createObjectNode();
        node.put("characteristics", route.getCharacteristics().name());
        node.put("name", route.getName());
        putStringArray(mapper, node, "description", route.getDescription());

        ArrayNode positionsNode = node.putArray("positions");
        for (NavigationPosition position : route.getPositions())
            positionsNode.add(toPositionNode(mapper, position));
        return node;
    }

    private static ObjectNode toPositionNode(ObjectMapper mapper, NavigationPosition position) {
        ObjectNode node = mapper.createObjectNode();
        putDouble(node, "longitude", position.getLongitude());
        putDouble(node, "latitude", position.getLatitude());
        putDouble(node, "elevation", position.getElevation());
        putTime(node, "time", position.getTime());
        putDouble(node, "speed", position.getSpeed());

        Double heading = position instanceof ExtendedSensorNavigationPosition extended ? extended.getHeading() : null;
        putDouble(node, "heading", heading);

        Double hdop = null, vdop = null, pdop = null;
        Integer satellites = null;
        if (position instanceof Wgs84Position wgs84Position) {
            hdop = wgs84Position.getHdop();
            vdop = wgs84Position.getVdop();
            pdop = wgs84Position.getPdop();
            satellites = wgs84Position.getSatellites();
        }
        putDouble(node, "hdop", hdop);
        putDouble(node, "vdop", vdop);
        putDouble(node, "pdop", pdop);
        if (satellites != null)
            node.put("satellites", satellites);
        else
            node.putNull("satellites");

        node.put("description", position.getDescription());
        return node;
    }

    private static void putStringArray(ObjectMapper mapper, ObjectNode node, String field, List<String> values) {
        if (values == null) {
            node.putNull(field);
            return;
        }
        ArrayNode array = mapper.createArrayNode();
        for (String value : values)
            array.add(value);
        node.set(field, array);
    }

    private static void putDouble(ObjectNode node, String field, Double value) {
        if (value == null)
            node.putNull(field);
        else
            node.put(field, format(ROOT, "%.7f", value));
    }

    private static void putTime(ObjectNode node, String field, CompactCalendar time) {
        if (time == null)
            node.putNull(field);
        else
            node.put(field, TIME_FORMAT.format(time.getCalendar().toInstant()));
    }
}

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
package slash.navigation.geojson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import slash.common.type.CompactCalendar;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static slash.common.io.Transfer.*;
import static slash.common.type.ISO8601.formatDate;
import static slash.common.type.ISO8601.parseDate;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads and writes GeoJSON (.json) files.
 *
 * @author Christian Pesch
 */

public class GeoJsonFormat extends SimpleFormat<Wgs84Route> {
    private static final Logger log = Logger.getLogger(GeoJsonFormat.class.getName());
    private static final Preferences preferences = Preferences.userNodeForPackage(GeoJsonFormat.class);
    private static final String ADDRESS = "address";
    private static final String DATE = "date";
    private static final String LOCATION = "location";
    private static final String NAME = "name";

    public String getExtension() {
        return ".json";
    }

    public String getName() {
        return "GeoJSON (*" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        read(source, UTF8_ENCODING, context);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        JsonNode root = new ObjectMapper().readTree(reader);
        if (root != null && "FeatureCollection".equals(root.path("type").asText())) {
            List<Wgs84Position> positions = process(root.path("features"));
            if (!positions.isEmpty()) {
                context.appendRoute(new Wgs84Route(this, Waypoints, "FeatureCollection", positions));
            }
        } else
            log.warning("Reading GeoJSON object " + root + " is not supported.");
    }

    private CompactCalendar parseTime(String string) {
        Calendar time = parseDate(string);
        return time != null ? CompactCalendar.fromCalendar(time) : null;
    }

    private List<Wgs84Position> process(JsonNode features) {
        List<Wgs84Position> result = new ArrayList<>();
        if (!features.isArray())
            return result;

        for (JsonNode feature : features) {
            JsonNode geometry = feature.path("geometry");
            if (!"Point".equals(geometry.path("type").asText())) {
                log.warning("Reading Geometry object " + geometry + " is not supported.");
                continue;
            }

            JsonNode coordinates = geometry.path("coordinates");
            if (!coordinates.isArray() || coordinates.size() < 2 ||
                    !coordinates.get(0).isNumber() || !coordinates.get(1).isNumber()) {
                log.warning("Reading coordinates " + coordinates + " is not supported.");
                continue;
            }

            JsonNode properties = feature.path("properties");
            String name = trim(text(properties.get(NAME)));
            String address = trim(text(properties.get(ADDRESS)));

            // Bewertungen.json and GespeicherteOrte.json use a location object
            JsonNode location = properties.get(LOCATION);
            if (location != null && location.isObject()) {
                name = trim(text(location.get(NAME)));
                address = trim(text(location.get(ADDRESS)));
            }

            Double elevation = coordinates.size() > 2 && coordinates.get(2).isNumber() ? coordinates.get(2).doubleValue() : null;
            if (isEmpty(elevation))
                elevation = null;

            CompactCalendar time = parseTime(trim(text(properties.get(DATE))));

            result.add(new Wgs84Position(coordinates.get(0).doubleValue(),
                    coordinates.get(1).doubleValue(), elevation,
                    null, time, asDescription(name, address)));
        }
        return result;
    }

    private String text(JsonNode node) {
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private ObjectNode createFeature(ObjectMapper mapper, Wgs84Position position) {
        ObjectNode feature = mapper.createObjectNode();
        feature.put("type", "Feature");

        ObjectNode properties = mapper.createObjectNode();
        if (position.getDescription() != null) {
            String name = asName(position.getDescription());
            if (name != null)
                properties.put(NAME, name);
            String desc = asDesc(position.getDescription());
            if (desc != null)
                properties.put(ADDRESS, desc);
        }
        if (position.getTime() != null) {
            String date = formatDate(position.getTime());
            properties.put(DATE, date);
        }
        feature.set("properties", properties);

        ObjectNode geometry = mapper.createObjectNode();
        geometry.put("type", "Point");
        ArrayNode coordinates = geometry.putArray("coordinates");
        if (position.getLongitude() != null)
            coordinates.add(position.getLongitude());
        if (position.getLatitude() != null)
            coordinates.add(position.getLatitude());
        if (position.getElevation() != null)
            coordinates.add(position.getElevation());
        feature.set("geometry", geometry);

        return feature;
    }

    public void write(Wgs84Route route, OutputStream target, int startIndex, int endIndex) throws IOException {
        write(route, target, UTF8_ENCODING, startIndex, endIndex);
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode featureCollection = mapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");
        ArrayNode features = featureCollection.putArray("features");
        List<Wgs84Position> positions = route.getPositions();
        for (int i = startIndex; i < endIndex; i++) {
            Wgs84Position position = positions.get(i);
            features.add(createFeature(mapper, position));
        }
        if (preferences.getBoolean("prettyPrintXml", true))
            mapper.enable(INDENT_OUTPUT);
        mapper.writeValue(writer, featureCollection);
    }
}

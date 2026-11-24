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
package slash.navigation.googletakeout;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.*;

import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads the commute routes that Google Takeout exports as
 * {@code Maps/Commute routes/Commute routes.json} (German {@code Routen für Pendelstrecke}):
 * {@code trips[].place_visit[].place.lat_lng} holds the source and destination of each trip,
 * {@code trips[].transition[].route.travel_mode} how it is travelled. The places become one
 * waypoint list, each place once.
 *
 * @author Christian Pesch
 */

public class GoogleCommuteRoutesFormat extends SimpleFormat<Wgs84Route> {
    static final String ROUTE_NAME = "Commute routes";

    public String getExtension() {
        return ".json";
    }

    public String getName() {
        return "Google Maps Commute Routes (" + getExtension() + ")";
    }

    public int getMaximumPositionCount() {
        return UNLIMITED_MAXIMUM_POSITION_COUNT;
    }

    public boolean isSupportsReading() {
        return true;
    }

    public boolean isSupportsWriting() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        read(source, UTF8_ENCODING, context);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        // a leading U+FEFF (files re-saved by Windows editors) makes Jackson throw
        reader.mark(1);
        if (reader.read() != BYTE_ORDER_MARK)
            reader.reset();

        JsonNode trips = findTrips(reader);
        if (trips == null || !trips.isArray() || !hasPlaceVisits(trips))
            throw new IllegalArgumentException("No Google Takeout commute routes");

        List<Wgs84Position> positions = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int tripNumber = 0;
        for (JsonNode trip : trips) {
            tripNumber++;
            String travelModes = travelModes(trip.path("transition"));
            for (JsonNode visit : trip.path("place_visit")) {
                JsonNode latLng = visit.path("place").path("lat_lng");
                Double latitude = asDegrees(latLng.path("latitude"));
                Double longitude = asDegrees(latLng.path("longitude"));
                if (latitude == null || longitude == null || !isValid(latitude, longitude))
                    continue;
                if (!seen.add(latitude + "," + longitude))
                    continue;
                positions.add(new Wgs84Position(longitude, latitude, null, null, null,
                        describe(visit, tripNumber, travelModes)));
            }
        }

        if (positions.isEmpty())
            throw new IllegalArgumentException("No Google Takeout commute route has coordinates");
        context.appendRoute(new Wgs84Route(this, Waypoints, ROUTE_NAME, positions));
    }

    // streams over the top-level fields and builds a tree of the trips only: other JSON, such as a
    // Records.json of several hundred MB, is skipped without being loaded into memory
    private static JsonNode findTrips(BufferedReader reader) throws IOException {
        try (JsonParser parser = new ObjectMapper().getFactory().createParser(reader)) {
            if (parser.nextToken() != JsonToken.START_OBJECT)
                return null;
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String fieldName = parser.currentName();
                JsonToken value = parser.nextToken();
                if ("trips".equals(fieldName) && value == JsonToken.START_ARRAY)
                    return parser.readValueAsTree();
                parser.skipChildren();
            }
            return null;
        }
    }

    private static boolean hasPlaceVisits(JsonNode trips) {
        for (JsonNode trip : trips)
            if (trip.path("place_visit").isArray())
                return true;
        return false;
    }

    static Double asDegrees(JsonNode node) {
        if (node == null || !node.isNumber())
            return null;
        double value = node.asDouble();
        // tolerate E7 integers as Takeout writes elsewhere (latE7, lngE7); 181 stays 181 and is invalid
        if (node.isIntegralNumber() && Math.abs(value) > 1000)
            value = value / 1e7;
        return value;
    }

    private static boolean isValid(double latitude, double longitude) {
        return latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0 &&
                !(latitude == 0.0 && longitude == 0.0);
    }

    private static String travelModes(JsonNode transitions) {
        Set<String> modes = new LinkedHashSet<>();
        for (JsonNode transition : transitions) {
            String mode = trim(transition.path("route").path("travel_mode").asText(null));
            if (mode != null)
                modes.add(mode.toLowerCase(Locale.ROOT));
        }
        return modes.isEmpty() ? null : String.join(", ", modes);
    }

    private static String describe(JsonNode visit, int tripNumber, String travelModes) {
        String type = trim(visit.path("place").path("semantic_type").asText(null));
        String id = trim(visit.path("id").asText(null));
        String role = "SOURCE_ID".equals(id) ? "Start" : "DESTINATION_ID".equals(id) ? "Destination" : null;
        StringBuilder buffer = new StringBuilder();
        buffer.append(role != null ? role : "Place").append(" of commute ").append(tripNumber);
        if (type != null)
            buffer.append(" (").append(type.toLowerCase(Locale.ROOT).replace('_', ' ')).append(")");
        if (travelModes != null)
            buffer.append(" by ").append(travelModes);
        return buffer.toString();
    }

    public void write(Wgs84Route route, PrintWriter writer, int startIndex, int endIndex) {
        throw new UnsupportedOperationException("GoogleCommuteRoutesFormat is read-only");
    }
}

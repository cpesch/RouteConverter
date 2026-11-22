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

package slash.navigation.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;
import slash.navigation.rest.Get;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static slash.common.io.Transfer.trim;
import static slash.navigation.base.RouteCalculations.asWgs84Position;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Fetches and parses a shared Google Maps list (a "placelist") from Google's
 * undocumented {@code preview/entitylist/getlist} web-app endpoint.
 *
 * @author Christian Pesch
 */

class GoogleMapsListClient {
    private static final Logger log = Logger.getLogger(GoogleMapsListClient.class.getName());
    private static final String XSSI_PREFIX = ")]}'";
    private static final Pattern DATA_LIST_ID_PATTERN = Pattern.compile("!11m\\d+!2s([\\w-]+)");
    private static final Pattern PLACELIST_LIST_ID_PATTERN = Pattern.compile("/placelists/list/([\\w-]+)");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GoogleMapsListClient() {
    }

    static String extractListId(String url) {
        Matcher dataMatcher = DATA_LIST_ID_PATTERN.matcher(url);
        if (dataMatcher.find())
            return dataMatcher.group(1);
        Matcher placelistMatcher = PLACELIST_LIST_ID_PATTERN.matcher(url);
        if (placelistMatcher.find())
            return placelistMatcher.group(1);
        return null;
    }

    static String createRequestUrl(String listId) {
        return "https://www.google.com/maps/preview/entitylist/getlist?authuser=0&hl=en&gl=us&pb=!1m4!1s" + listId +
                "!2e1!3m1!1e1!2e2!3e2!4i500!16b1";
    }

    // returns the element at index if node is an array with an element there, null otherwise;
    // distinguishes "out of bounds" from "present and JSON null" the same way, since neither is usable here
    private static JsonNode at(JsonNode node, int index) {
        if (node == null || !node.isArray() || index < 0 || index >= node.size())
            return null;
        return node.get(index);
    }

    private static Double numberAt(JsonNode array, int index) {
        JsonNode node = at(array, index);
        return node != null && node.isNumber() ? node.asDouble() : null;
    }

    private static String textAt(JsonNode array, int index) {
        JsonNode node = at(array, index);
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private static Wgs84Position parsePlace(JsonNode place) {
        JsonNode details = at(place, 1);
        JsonNode coordinate = at(details, 5);
        Double latitude = numberAt(coordinate, 2);
        Double longitude = numberAt(coordinate, 3);
        if (latitude == null || longitude == null)
            return null;
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
            return null;
        return asWgs84Position(longitude, latitude, trim(textAt(place, 2)));
    }

    static Wgs84Route parseListResponse(String body, GoogleMapsUrlFormat format) {
        if (body == null)
            return null;

        String json = body;
        if (json.startsWith(XSSI_PREFIX)) {
            int newline = json.indexOf('\n');
            if (newline == -1)
                return null;
            json = json.substring(newline + 1);
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (IOException e) {
            return null;
        }
        if (root == null || !root.isArray() || root.isEmpty())
            return null;

        JsonNode list = at(root, 0);
        JsonNode places = at(list, 8);
        if (places == null || !places.isArray())
            return null;

        List<Wgs84Position> positions = new ArrayList<>();
        for (JsonNode place : places) {
            Wgs84Position position = parsePlace(place);
            if (position != null)
                positions.add(position);
        }
        if (positions.isEmpty())
            return null;

        JsonNode total = at(list, 12);
        if (total != null && total.isNumber() && total.asInt() > places.size())
            log.warning("Google Maps list has " + total.asInt() + " places, imported " + positions.size());

        return format.createRoute(Waypoints, trim(textAt(list, 4)), positions);
    }

    static Wgs84Route fetchList(String listId, GoogleMapsUrlFormat format) {
        try {
            Get get = new Get(createRequestUrl(listId));
            String body = get.executeAsString();
            if (!get.isSuccessful()) {
                log.warning("Cannot fetch Google Maps list " + listId + ": status " + get.getStatusCode());
                return null;
            }
            Wgs84Route route = parseListResponse(body, format);
            if (route == null)
                log.warning("Cannot fetch Google Maps list " + listId + ": could not parse response");
            return route;
        } catch (IOException e) {
            log.warning("Cannot fetch Google Maps list " + listId + ": " + e.getMessage());
            return null;
        }
    }
}

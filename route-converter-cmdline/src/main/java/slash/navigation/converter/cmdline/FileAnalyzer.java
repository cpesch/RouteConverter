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

package slash.navigation.converter.cmdline;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.BaseRoute;
import slash.navigation.base.NavigationFormatParser;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.base.ParserResult;
import slash.navigation.common.NavigationPosition;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.Math.round;

/**
 * Reads a file with the full RouteConverter parser and aggregates the metadata
 * described in {@code src/main/doc/analyze-json.md} (specs/00055) into a single
 * line of JSON. Aggregation runs over every position list of the file: length,
 * duration, elevation gain/loss are summed, the bounding box is unioned and the
 * start time is the earliest position timestamp.
 *
 * @author Christian Pesch
 */
public class FileAnalyzer {
    private final NavigationFormatRegistry registry;
    private final RouteLengthComputer lengthComputer;

    public FileAnalyzer(NavigationFormatRegistry registry, RouteLengthComputer lengthComputer) {
        this.registry = registry;
        this.lengthComputer = lengthComputer;
    }

    /**
     * @return one line of JSON per the analyze contract
     * @throws IOException if the file cannot be read or parsed
     */
    public String analyze(File source) throws IOException {
        NavigationFormatParser parser = new NavigationFormatParser(registry);
        ParserResult result = parser.read(source);
        if (!result.isSuccessful())
            throw new IOException("Could not read '" + source.getAbsolutePath() + "'");

        List<BaseRoute> routes = result.getAllRoutes();

        int positions = 0;
        double lengthMeters = 0;
        boolean anyLength = false;
        String aggregateKind = null;

        long durationMillis = 0;
        boolean anyTime = false;

        double elevationGain = 0, elevationLoss = 0;
        boolean anyElevation = false;

        boolean anyCoordinate = false;
        double north = -90, south = 90, east = -180, west = 180;
        Long startMillis = null;

        for (BaseRoute route : routes) {
            positions += route.getPositionCount();

            @SuppressWarnings("unchecked")
            RouteLengthComputer.LengthResult length = lengthComputer.computeLength(route);
            if (length != null) {
                lengthMeters += length.meters();
                anyLength = true;
                aggregateKind = combineKind(aggregateKind, length.kind());
            }

            @SuppressWarnings("unchecked")
            List<BaseNavigationPosition> positionList = ((BaseRoute<BaseNavigationPosition, ?>) route).getPositions();
            for (NavigationPosition position : positionList) {
                Double longitude = position.getLongitude();
                Double latitude = position.getLatitude();
                if (longitude != null && latitude != null) {
                    anyCoordinate = true;
                    if (latitude > north) north = latitude;
                    if (latitude < south) south = latitude;
                    if (longitude > east) east = longitude;
                    if (longitude < west) west = longitude;
                }
                if (position.getElevation() != null)
                    anyElevation = true;
                CompactCalendar time = position.getTime();
                if (time != null) {
                    anyTime = true;
                    long millis = time.getTimeInMillis();
                    if (startMillis == null || millis < startMillis)
                        startMillis = millis;
                }
            }

            if (route.getPositionCount() >= 2) {
                durationMillis += route.getTime();
                elevationGain += route.getElevationAscend(0, route.getPositionCount() - 1);
                elevationLoss += route.getElevationDescend(0, route.getPositionCount() - 1);
            }
        }

        // A file the parser accepts but with no positions carries no usable
        // geometry (empty/binary/garbage sniffed as a permissive text format).
        // Treat it as broken so the caller marks it broken rather than creating
        // an empty metadata row / rescuing a non-route (specs/00055, 00056).
        if (positions == 0)
            throw new IOException("No positions found in '" + source.getAbsolutePath() + "'");

        StringBuilder json = new StringBuilder();
        json.append('{');
        appendNumber(json, "size", source.length());
        json.append(',');
        appendString(json, "format", result.getFormat().getName());
        json.append(',');
        appendNumber(json, "positionLists", routes.size());
        json.append(',');
        appendNumber(json, "positions", positions);
        json.append(',');
        json.append("\"bbox\":");
        if (anyCoordinate) {
            json.append('{');
            appendRawNumber(json, "north", north);
            json.append(',');
            appendRawNumber(json, "south", south);
            json.append(',');
            appendRawNumber(json, "east", east);
            json.append(',');
            appendRawNumber(json, "west", west);
            json.append('}');
        } else {
            json.append("null");
        }
        json.append(',');
        if (anyLength) {
            appendNumber(json, "lengthM", round(lengthMeters));
        } else {
            appendNull(json, "lengthM");
        }
        json.append(',');
        appendString(json, "lengthKind", aggregateKind != null ? aggregateKind : "track");
        json.append(',');
        if (anyTime) {
            appendNumber(json, "durationS", round(durationMillis / 1000.0));
        } else {
            appendNull(json, "durationS");
        }
        json.append(',');
        if (anyElevation) {
            appendNumber(json, "elevationGainM", round(elevationGain));
            json.append(',');
            appendNumber(json, "elevationLossM", round(elevationLoss));
        } else {
            appendNull(json, "elevationGainM");
            json.append(',');
            appendNull(json, "elevationLossM");
        }
        json.append(',');
        if (startMillis != null) {
            appendString(json, "startTime", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(startMillis)));
        } else {
            appendNull(json, "startTime");
        }
        json.append('}');
        return json.toString();
    }

    /**
     * File-level length kind: report the least-certain kind present so the label
     * never over-promises. beeline (straight-line) beats routed beats track.
     */
    private static String combineKind(String current, String next) {
        if (current == null)
            return next;
        if ("beeline".equals(current) || "beeline".equals(next))
            return "beeline";
        if ("routed".equals(current) || "routed".equals(next))
            return "routed";
        return "track";
    }

    private static void appendString(StringBuilder builder, String key, String value) {
        builder.append('"').append(key).append("\":\"").append(escape(value)).append('"');
    }

    private static void appendNumber(StringBuilder builder, String key, long value) {
        builder.append('"').append(key).append("\":").append(value);
    }

    private static void appendRawNumber(StringBuilder builder, String key, double value) {
        builder.append('"').append(key).append("\":").append(value);
    }

    private static void appendNull(StringBuilder builder, String key) {
        builder.append('"').append(key).append("\":null");
    }

    private static String escape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t': result.append("\\t"); break;
                default:
                    if (c < 0x20)
                        result.append(String.format("\\u%04x", (int) c));
                    else
                        result.append(c);
            }
        }
        return result.toString();
    }
}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        // A file the parser accepts but with no positions carries no usable
        // geometry (empty/binary/garbage sniffed as a permissive text format).
        // Treat it as broken so the caller marks it broken rather than creating
        // an empty metadata row / rescuing a non-route (specs/00055, 00056).
        int total = 0;
        for (BaseRoute route : routes)
            total += route.getPositionCount();
        if (total == 0)
            throw new IOException("No positions found in '" + source.getAbsolutePath() + "'");

        return toJson(routes, source.length(), result.getFormat().getName(),
                result.getFormat().getExtension(), lengthComputer);
    }

    /**
     * JSON emission seam: aggregates the metadata across every position list and
     * renders the single-line JSON of the analyze contract. Package-visible and
     * decoupled from parsing so the emission (bbox union, length-kind roll-up,
     * null handling) can be unit-tested with crafted routes (specs/00055).
     */
    static String toJson(List<BaseRoute> routes, long size, String format, String extension,
                         RouteLengthComputer lengthComputer) throws JsonProcessingException {
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

        String firstName = null;

        for (BaseRoute route : routes) {
            positions += route.getPositionCount();

            if (firstName == null) {
                String name = route.getName();
                if (name != null && !name.trim().isEmpty())
                    firstName = name.trim();
            }

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
                // A non-finite coordinate (NaN/Infinity from a malformed source)
                // is treated as absent: it never enters the bbox union, so the
                // emitted bbox doubles stay valid JSON. If no position carries a
                // finite coordinate pair the bbox is null (specs/00055).
                if (longitude != null && latitude != null
                        && Double.isFinite(longitude) && Double.isFinite(latitude)) {
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

        // Insertion order defines the JSON field order (LinkedHashMap); the
        // single-line, no-whitespace layout and null emission are ObjectMapper
        // defaults, so the output matches the analyze-json.md contract.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("size", size);
        payload.put("format", format);
        payload.put("positionLists", routes.size());
        payload.put("positions", positions);
        if (anyCoordinate) {
            Map<String, Object> bbox = new LinkedHashMap<>();
            bbox.put("north", north);
            bbox.put("south", south);
            bbox.put("east", east);
            bbox.put("west", west);
            payload.put("bbox", bbox);
        } else {
            payload.put("bbox", null);
        }
        payload.put("lengthM", anyLength ? round(lengthMeters) : null);
        // lengthKind tracks lengthM: when nothing was computable (lengthM null)
        // the kind is null too, rather than falsely defaulting to "track"
        // (specs/00055). rc-site tolerates null: data.get('lengthKind') or ''.
        payload.put("lengthKind", aggregateKind);
        payload.put("durationS", anyTime ? round(durationMillis / 1000.0) : null);
        payload.put("elevationGainM", anyElevation ? round(elevationGain) : null);
        payload.put("elevationLossM", anyElevation ? round(elevationLoss) : null);
        payload.put("startTime", startMillis != null
                ? DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(startMillis)) : null);
        payload.put("firstName", firstName);
        payload.put("extension", extension);
        return OBJECT_MAPPER.writeValueAsString(payload);
    }

    /**
     * File-level length kind: report the least-certain kind present so the label
     * never over-promises. straight-line beats routed beats track.
     */
    private static String combineKind(String current, String next) {
        if (current == null)
            return next;
        if ("straight-line".equals(current) || "straight-line".equals(next))
            return "straight-line";
        if ("routed".equals(current) || "routed".equals(next))
            return "routed";
        return "track";
    }
}

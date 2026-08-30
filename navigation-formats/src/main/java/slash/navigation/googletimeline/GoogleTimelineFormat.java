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
package slash.navigation.googletimeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import slash.common.type.CompactCalendar;
import slash.common.type.ISO8601;
import slash.navigation.base.*;
import slash.navigation.common.NavigationPosition;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.YEAR;
import static java.util.Collections.sort;
import static slash.common.io.Transfer.UTF8_ENCODING;
import static slash.navigation.base.RouteCharacteristics.Track;
import static slash.navigation.base.RouteCharacteristics.Waypoints;

/**
 * Reads Google Maps Timeline on-device exports (location-history.json / Timeline.json).
 * Supports both iOS (bare array) and Android (semanticSegments wrapper) shapes.
 *
 * @author Christian Pesch
 */
public class GoogleTimelineFormat extends SimpleFormat<Wgs84Route> {
    private static final Logger log = Logger.getLogger(GoogleTimelineFormat.class.getName());

    private static final String GEO_PREFIX = "geo:";
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("(-?\\d+\\.\\d+)[°,]\\s*(-?\\d+\\.\\d+)[°]?");

    public String getExtension() {
        return ".json";
    }

    public String getName() {
        return "Google Timeline (*" + getExtension() + ")";
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

    public boolean isSupportsMultipleRoutes() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public <P extends NavigationPosition> Wgs84Route createRoute(RouteCharacteristics characteristics, String name, List<P> positions) {
        return new Wgs84Route(this, characteristics, name, (List<Wgs84Position>) positions);
    }

    public void read(BufferedReader reader, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        // Convert BufferedReader to InputStream to reuse the read(InputStream, String, ParserContext) implementation
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream, encoding);
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            outputStreamWriter.write(buffer, 0, read);
        }
        outputStreamWriter.flush();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        read(byteArrayInputStream, encoding, context);
    }

    public void read(InputStream source, ParserContext<Wgs84Route> context) throws IOException {
        read(source, UTF8_ENCODING, context);
    }

    public void read(InputStream source, String encoding, ParserContext<Wgs84Route> context) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(source, encoding));
        JsonNode root = new ObjectMapper().readTree(reader);

        if (root == null) {
            return;
        }

        ArrayNode segments = null;
        boolean isIOS = false;
        boolean isAndroid = false;

        // Detect format
        if (root.isArray()) {
            // iOS shape: bare array
            segments = (ArrayNode) root;
            isIOS = true;
            log.fine("Detected iOS Google Timeline format (bare array)");
        } else if (root.isObject() && root.has("semanticSegments") && root.get("semanticSegments").isArray()) {
            // Android shape: object with semanticSegments array
            segments = (ArrayNode) root.get("semanticSegments");
            isAndroid = true;
            log.fine("Detected Android Google Timeline format (semanticSegments wrapper)");
        } else {
            log.warning("GoogleTimelineFormat: unsupported root node type " + root.getNodeType() + ", not an array or semanticSegments object");
            return;
        }

        // Validate format
        if (segments == null || segments.size() == 0) {
            log.fine("GoogleTimelineFormat: empty segments array");
            return;
        }

        JsonNode first = segments.get(0);
        if (first == null || !first.has("startTime") ||
            (!first.has("visit") && !first.has("activity") && !first.has("timelinePath"))) {
            log.warning("GoogleTimelineFormat: first segment lacks required fields (startTime and one of visit/activity/timelinePath)");
            return;
        }

        // Parse all segments
        List<JsonNode> pathSegments = new ArrayList<>();
        List<JsonNode> visitSegments = new ArrayList<>();
        List<Activity> activities = new ArrayList<>();

        for (JsonNode segment : segments) {
            if (segment == null) continue;

            if (segment.has("timelinePath")) {
                pathSegments.add(segment);
            } else if (segment.has("visit")) {
                visitSegments.add(segment);
            } else if (segment.has("activity")) {
                activities.add(parseActivity(segment, isAndroid));
            }
        }

        // Process path points grouped by calendar day
        Map<CompactCalendar, List<Wgs84Position>> pointsByDay = new TreeMap<>();
        for (JsonNode segment : pathSegments) {
            List<Wgs84Position> points = parseTimelinePath(segment, activities, isIOS, isAndroid);
            for (Wgs84Position point : points) {
                if (point.getTime() == null) continue;

                CompactCalendar day = getStartOfDay(point.getTime());
                pointsByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(point);
            }
        }

        // Emit day tracks in ascending date order
        for (Map.Entry<CompactCalendar, List<Wgs84Position>> entry : pointsByDay.entrySet()) {
            List<Wgs84Position> dayPoints = entry.getValue();
            // Sort by timestamp, stable for ties
            sort(dayPoints, (a, b) -> {
                if (a.getTime() == null && b.getTime() == null) return 0;
                if (a.getTime() == null) return 1;
                if (b.getTime() == null) return -1;
                return a.getTime().compareTo(b.getTime());
            });

            String routeName = formatDateName(entry.getKey());
            context.appendRoute(new Wgs84Route(this, Track, routeName, dayPoints));
        }

        // Process visits into a single Waypoints route
        List<Wgs84Position> visitPositions = new ArrayList<>();
        for (JsonNode segment : visitSegments) {
            Wgs84Position position = parseVisit(segment, isIOS, isAndroid);
            if (position != null) {
                visitPositions.add(position);
            }
        }

        if (!visitPositions.isEmpty()) {
            context.appendRoute(new Wgs84Route(this, Waypoints, "Visits", visitPositions));
        }
    }

    /**
     * Parse coordinate string from either "geo:lat,lng" or "lat°, lng°" format.
     * Returns {longitude, latitude} or null if unparseable.
     */
    static Double[] parseCoordinates(String coordinateString) {
        if (coordinateString == null || coordinateString.trim().isEmpty()) {
            return null;
        }

        String cleaned = coordinateString.trim();

        // Handle geo: prefix
        if (cleaned.startsWith(GEO_PREFIX)) {
            cleaned = cleaned.substring(GEO_PREFIX.length());
        }

        Matcher matcher = COORDINATE_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            try {
                double latitude = Double.parseDouble(matcher.group(1));
                double longitude = Double.parseDouble(matcher.group(2));
                return new Double[]{longitude, latitude}; // Wgs84Position takes longitude first
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Calculate point time from segment start time and minute offset (iOS) or absolute time (Android).
     */
    static CompactCalendar pointTime(CompactCalendar segmentStart, String minuteOffset) {
        if (segmentStart == null) {
            return null;
        }

        // Android: minuteOffset is null, use segmentStart directly
        if (minuteOffset == null) {
            return segmentStart;
        }

        // iOS: add minute offset to segment start
        try {
            int minutes = Integer.parseInt(minuteOffset);
            CompactCalendar result = (CompactCalendar) segmentStart.clone();
            result.add(Calendar.MINUTE, minutes);
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Find the activity type active at a given time point.
     */
    static String activityTypeAt(List<Activity> activities, CompactCalendar time) {
        if (activities == null || activities.isEmpty() || time == null) {
            return null;
        }

        Activity bestMatch = null;
        for (Activity activity : activities) {
            if (activity.startTime == null || activity.endTime == null) {
                continue;
            }

            // Check if point is within activity window [startTime, endTime] inclusive
            boolean inWindow = !time.before(activity.startTime) && !time.after(activity.endTime);
            if (inWindow) {
                if (bestMatch == null) {
                    bestMatch = activity;
                } else {
                    // Resolve conflicts by probability, then by startTime, then by file order
                    int probabilityCompare = Double.compare(activity.probability, bestMatch.probability);
                    if (probabilityCompare > 0) {
                        bestMatch = activity;
                    } else if (probabilityCompare == 0) {
                        if (activity.startTime != null && bestMatch.startTime != null &&
                            activity.startTime.before(bestMatch.startTime)) {
                            bestMatch = activity;
                        }
                        // If still equal, earlier in file wins (current bestMatch is earlier)
                    }
                }
            }
        }

        return bestMatch != null ? bestMatch.type : null;
    }

    private CompactCalendar getStartOfDay(CompactCalendar time) {
        CompactCalendar day = (CompactCalendar) time.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }

    private String formatDateName(CompactCalendar day) {
        Calendar calendar = day.getCalendar();
        int year = calendar.get(YEAR);
        int month = calendar.get(MONTH) + 1; // Calendar.MONTH is 0-based
        int dayOfMonth = calendar.get(DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", year, month, dayOfMonth);
    }

    private Activity parseActivity(JsonNode segment, boolean isAndroid) {
        JsonNode activityNode = segment.path("activity");
        if (activityNode == null || activityNode.isMissingNode()) {
            return null;
        }

        JsonNode topCandidate = activityNode.path("topCandidate");
        if (topCandidate == null || topCandidate.isMissingNode()) {
            return null;
        }

        String type = topCandidate.path("type").asText();
        double probability = topCandidate.path("probability").asDouble(0.0);

        CompactCalendar startTime = parseTime(segment.path("startTime").asText(null));
        CompactCalendar endTime = parseTime(segment.path("endTime").asText(null));

        return new Activity(type, probability, startTime, endTime);
    }

    private List<Wgs84Position> parseTimelinePath(JsonNode segment, List<Activity> activities,
                                                   boolean isIOS, boolean isAndroid) {
        List<Wgs84Position> positions = new ArrayList<>();

        JsonNode timelinePathNode = segment.path("timelinePath");
        if (timelinePathNode == null || timelinePathNode.isMissingNode()) {
            return positions;
        }

        CompactCalendar segmentStart = parseTime(segment.path("startTime").asText(null));
        if (segmentStart == null) {
            return positions;
        }

        JsonNode distanceNode = segment.path("distanceMeters");
        // distance can be string (iOS) or number (Android), we don't use it

        JsonNode pointsArray = timelinePathNode.has("points") ? timelinePathNode.path("points") :
                               (timelinePathNode.isArray() ? timelinePathNode : null);
        if (pointsArray == null || !pointsArray.isArray()) {
            return positions;
        }

        for (JsonNode point : pointsArray) {
            if (point == null || !point.isObject()) {
                continue;
            }

            Wgs84Position position = parsePathPoint(point, segmentStart, activities, isIOS);
            if (position != null) {
                positions.add(position);
            }
        }

        return positions;
    }

    private Wgs84Position parsePathPoint(JsonNode point, CompactCalendar segmentStart,
                                        List<Activity> activities, boolean isIOS) {
        JsonNode locationNode = point.path("location");
        if (locationNode == null || locationNode.isMissingNode()) {
            return null;
        }

        String locationText = null;
        if (locationNode.isTextual()) {
            locationText = locationNode.asText();
        } else if (locationNode.isObject() && locationNode.has("latLng")) {
            locationText = locationNode.path("latLng").asText(null);
        }

        if (locationText == null) {
            return null;
        }

        Double[] coords = parseCoordinates(locationText);
        if (coords == null) {
            log.fine("Failed to parse coordinates: " + locationText);
            return null;
        }

        CompactCalendar time;
        if (isIOS) {
            // iOS: use durationMinutesOffsetFromStartTime
            String minuteOffset = point.path("durationMinutesOffsetFromStartTime").asText(null);
            time = pointTime(segmentStart, minuteOffset);
        } else {
            // Android: use absolute time
            time = parseTime(point.path("time").asText(null));
        }

        if (time == null) {
            return null;
        }

        // Determine activity type for this point
        String description = activityTypeAt(activities, time);

        return new Wgs84Position(coords[0], coords[1], null, null, time, description);
    }

    private Wgs84Position parseVisit(JsonNode segment, boolean isIOS, boolean isAndroid) {
        JsonNode visitNode = segment.path("visit");
        if (visitNode == null || visitNode.isMissingNode()) {
            return null;
        }

        JsonNode topCandidate = visitNode.path("topCandidate");
        if (topCandidate == null || topCandidate.isMissingNode()) {
            return null;
        }

        JsonNode locationNode = topCandidate.path("placeLocation");
        if (locationNode == null || locationNode.isMissingNode()) {
            return null;
        }

        String locationText = null;
        if (locationNode.isTextual()) {
            locationText = locationNode.asText();
        } else if (locationNode.isObject() && locationNode.has("latLng")) {
            locationText = locationNode.path("latLng").asText(null);
        }

        if (locationText == null || locationText.isEmpty()) {
            log.fine("Visit has missing or empty placeLocation");
            return null;
        }

        Double[] coords = parseCoordinates(locationText);
        if (coords == null) {
            log.fine("Failed to parse visit coordinates: " + locationText);
            return null;
        }

        CompactCalendar time = parseTime(segment.path("startTime").asText(null));
        String semanticType = topCandidate.path("semanticType").asText(null);
        String description = (semanticType != null && !semanticType.isEmpty()) ? semanticType : null;

        return new Wgs84Position(coords[0], coords[1], null, null, time, description);
    }

    private CompactCalendar parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }

        Calendar calendar = ISO8601.parseDate(timeString);
        return calendar != null ? CompactCalendar.fromCalendar(calendar) : null;
    }

    private static class Activity {
        final String type;
        final double probability;
        final CompactCalendar startTime;
        final CompactCalendar endTime;

        Activity(String type, double probability, CompactCalendar startTime, CompactCalendar endTime) {
            this.type = type;
            this.probability = probability;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}

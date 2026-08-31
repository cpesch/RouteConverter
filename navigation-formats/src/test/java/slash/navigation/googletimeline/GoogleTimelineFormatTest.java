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

import org.junit.Test;
import slash.common.type.CompactCalendar;
import slash.common.type.ISO8601;
import slash.navigation.base.ParserContext;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.*;
import static slash.common.io.Transfer.UTF8_ENCODING;

/**
 * Tests for Google Maps Timeline Format reader.
 *
 * @author Christian Pesch
 */
public class GoogleTimelineFormatTest {

    private static final String IOS_SAME_DAY_JSON =
            "[" +
            "  {" +
            "    \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}," +
            "        {\"location\": \"geo:52.5210,13.4060\", \"durationMinutesOffsetFromStartTime\": \"30\"}," +
            "        {\"location\": \"geo:52.5220,13.4070\", \"durationMinutesOffsetFromStartTime\": \"60\"}" +
            "      ]" +
            "    }" +
            "  }," +
            "  {" +
            "    \"startTime\": \"2024-12-23T14:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T16:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:48.8566,2.3522\", \"durationMinutesOffsetFromStartTime\": \"0\"}," +
            "        {\"location\": \"geo:48.8576,2.3532\", \"durationMinutesOffsetFromStartTime\": \"45\"}" +
            "      ]" +
            "    }" +
            "  }" +
            "]";

    private static final String IOS_DIFFERENT_DAYS_JSON =
            "[" +
            "  {" +
            "    \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}," +
            "        {\"location\": \"geo:52.5210,13.4060\", \"durationMinutesOffsetFromStartTime\": \"30\"}" +
            "      ]" +
            "    }" +
            "  }," +
            "  {" +
            "    \"startTime\": \"2024-12-24T15:00:00Z\"," +
            "    \"endTime\": \"2024-12-24T17:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:48.8566,2.3522\", \"durationMinutesOffsetFromStartTime\": \"0\"}" +
            "      ]" +
            "    }" +
            "  }" +
            "]";

    private static final String IOS_WITH_VISIT_JSON =
            "[" +
            "  {" +
            "    \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}," +
            "        {\"location\": \"geo:52.5210,13.4060\", \"durationMinutesOffsetFromStartTime\": \"30\"}" +
            "      ]" +
            "    }" +
            "  }," +
            "  {" +
            "    \"startTime\": \"2024-12-23T14:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T16:00:00Z\"," +
            "    \"visit\": {" +
            "      \"topCandidate\": {" +
            "        \"placeLocation\": \"geo:53.569885,10.029150\"," +
            "        \"semanticType\": \"Home\"" +
            "      }" +
            "    }" +
            "  }" +
            "]";

    private static final String IOS_WITH_ACTIVITY_JSON =
            "[" +
            "  {" +
            "    \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "    \"activity\": {" +
            "      \"topCandidate\": {" +
            "        \"type\": \"walking\"," +
            "        \"probability\": 0.85" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"startTime\": \"2024-12-23T12:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T14:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}," +
            "        {\"location\": \"geo:52.5210,13.4060\", \"durationMinutesOffsetFromStartTime\": \"30\"}," +
            "        {\"location\": \"geo:52.5220,13.4070\", \"durationMinutesOffsetFromStartTime\": \"60\"}" +
            "      ]" +
            "    }" +
            "  }" +
            "]";

    private static final String IOS_SAME_MINUTE_JSON =
            "[" +
            "  {" +
            "    \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"30\"}," +
            "        {\"location\": \"geo:52.5210,13.4060\", \"durationMinutesOffsetFromStartTime\": \"30\"}" +
            "      ]" +
            "    }" +
            "  }" +
            "]";

    private static final String ANDROID_JSON =
            "{" +
            "  \"semanticSegments\": [" +
            "    {" +
            "      \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "      \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "      \"timelinePath\": {" +
            "        \"points\": [" +
            "          {\"location\": {\"latLng\": \"52.5200°, 13.4050°\"}, \"time\": \"2024-12-23T10:00:00Z\"}," +
            "          {\"location\": {\"latLng\": \"52.5210°, 13.4060°\"}, \"time\": \"2024-12-23T10:30:00Z\"}" +
            "        ]" +
            "      }" +
            "    }," +
            "    {" +
            "      \"startTime\": \"2024-12-23T14:00:00Z\"," +
            "      \"endTime\": \"2024-12-23T16:00:00Z\"," +
            "      \"visit\": {" +
            "        \"topCandidate\": {" +
            "          \"placeLocation\": {\"latLng\": \"53.569885°, 10.029150°\"}," +
            "          \"semanticType\": \"Work\"" +
            "        }" +
            "      }" +
            "    }" +
            "  ]" +
            "}";

    private static final String VISIT_MISSING_LOCATION_JSON =
            "[" +
            "  {" +
            "    \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "    \"visit\": {" +
            "      \"topCandidate\": {" +
            "        \"semanticType\": \"Home\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"startTime\": \"2024-12-23T14:00:00Z\"," +
            "    \"endTime\": \"2024-12-23T16:00:00Z\"," +
            "    \"timelinePath\": {" +
            "      \"points\": [" +
            "        {\"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}" +
            "      ]" +
            "    }" +
            "  }" +
            "]";

    private static final String GEOJSON_FEATURE_COLLECTION =
            "{" +
            "  \"type\": \"FeatureCollection\"," +
            "  \"features\": [" +
            "    {" +
            "      \"type\": \"Feature\"," +
            "      \"geometry\": {\"type\": \"Point\", \"coordinates\": [13.4050, 52.5200]}," +
            "      \"properties\": {\"name\": \"Berlin\"}" +
            "    }" +
            "  ]" +
            "}";

    private static final String EMPTY_ARRAY_JSON = "[]";

    private static final String INVALID_JSON = "{\"invalid\": \"format\"}";

    @Test
    public void testParseCoordinatesGeoPrefix() {
        Double[] result = GoogleTimelineFormat.parseCoordinates("geo:53.569885,10.029150");
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(10.029150, result[0], 0.000001); // longitude first
        assertEquals(53.569885, result[1], 0.000001);  // latitude second
    }

    @Test
    public void testParseCoordinatesDegreeSign() {
        Double[] result = GoogleTimelineFormat.parseCoordinates("53.569885°, 10.029150°");
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(10.029150, result[0], 0.000001); // longitude first
        assertEquals(53.569885, result[1], 0.000001);  // latitude second
    }

    @Test
    public void testParseCoordinatesNegativeValues() {
        Double[] result = GoogleTimelineFormat.parseCoordinates("geo:45.948396,-1.372823");
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(-1.372823, result[0], 0.000001); // longitude first
        assertEquals(45.948396, result[1], 0.000001);  // latitude second
    }

    @Test
    public void testParseCoordinatesNullInput() {
        assertNull(GoogleTimelineFormat.parseCoordinates(null));
    }

    @Test
    public void testParseCoordinatesEmptyString() {
        assertNull(GoogleTimelineFormat.parseCoordinates(""));
    }

    @Test
    public void testParseCoordinatesGarbage() {
        assertNull(GoogleTimelineFormat.parseCoordinates("garbage"));
    }

    @Test
    public void testParseCoordinatesIncompleteGeo() {
        assertNull(GoogleTimelineFormat.parseCoordinates("geo:1"));
    }

    @Test
    public void testPointTimeZeroOffset() {
        CompactCalendar start = parseTime("2024-12-23T10:00:00Z");
        CompactCalendar result = GoogleTimelineFormat.pointTime(start, "0");
        assertNotNull(result);
        assertEquals(start.getTime(), result.getTime());
    }

    @Test
    public void testPointTimeFortySevenMinutes() {
        CompactCalendar start = parseTime("2024-12-23T10:00:00Z");
        CompactCalendar result = GoogleTimelineFormat.pointTime(start, "47");
        assertNotNull(result);
        long expected = start.getTime() + (47L * 60 * 1000);
        assertEquals(expected, result.getTime());
    }

    @Test
    public void testPointTimeMaxOffset() {
        CompactCalendar start = parseTime("2024-12-23T10:00:00Z");
        CompactCalendar result = GoogleTimelineFormat.pointTime(start, "121");
        assertNotNull(result);
        long expected = start.getTime() + (121L * 60 * 1000);
        assertEquals(expected, result.getTime());
    }

    @Test
    public void testPointTimeNullOffset() {
        CompactCalendar start = parseTime("2024-12-23T10:00:00Z");
        CompactCalendar result = GoogleTimelineFormat.pointTime(start, null);
        assertNotNull(result);
        assertEquals(start.getTime(), result.getTime());
    }

    @Test
    public void testPointTimeNonNumericOffset() {
        CompactCalendar start = parseTime("2024-12-23T10:00:00Z");
        CompactCalendar result = GoogleTimelineFormat.pointTime(start, "invalid");
        assertNull(result);
    }

    @Test
    public void testPointTimeNullStart() {
        CompactCalendar result = GoogleTimelineFormat.pointTime(null, "30");
        assertNull(result);
    }

    @Test
    public void testActivityTypeAtPointInsideWindow() {
        // Test through end-to-end read with activity data
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        String json =
            "[" +
            "  {\"startTime\": \"2024-12-23T10:00:00Z\", \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "   \"activity\": {\"topCandidate\": {\"type\": \"walking\", \"probability\": 0.8}}}," +
            "  {\"startTime\": \"2024-12-23T10:30:00Z\", \"endTime\": \"2024-12-23T11:00:00Z\"," +
            "   \"timelinePath\": {\"points\": [{" +
            "     \"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}]}}}" +
            "]";

        InputStream input = new ByteArrayInputStream(json.getBytes(UTF8_ENCODING));
        format.read(input, context);

        assertEquals(1, context.getRoutes().size());
        Wgs84Route route = context.getRoutes().get(0);
        assertEquals(1, route.getPositions().size());
        assertEquals("walking", route.getPositions().get(0).getDescription());
    }

    @Test
    public void testReadIOSSameDay() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(IOS_SAME_DAY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size()); // one route for same day

        Wgs84Route route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals("2024-12-23", route.getName());

        List<Wgs84Position> positions = route.getPositions();
        assertEquals(5, positions.size()); // 3 + 2 points from two segments

        // Check they're sorted by time
        assertNotNull(positions.get(0).getTime());
        assertNotNull(positions.get(1).getTime());
        assertTrue(positions.get(1).getTime().after(positions.get(0).getTime()));
    }

    @Test
    public void testReadIOSDifferentDays() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(IOS_DIFFERENT_DAYS_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // two routes for different days

        assertEquals(RouteCharacteristics.Track, routes.get(0).getCharacteristics());
        assertEquals(RouteCharacteristics.Track, routes.get(1).getCharacteristics());
        assertEquals("2024-12-23", routes.get(0).getName());
        assertEquals("2024-12-24", routes.get(1).getName());

        assertEquals(2, routes.get(0).getPositions().size());
        assertEquals(1, routes.get(1).getPositions().size());
    }

    @Test
    public void testReadIOSWithVisit() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(IOS_WITH_VISIT_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // 1 track + 1 visits route

        Wgs84Route track = routes.get(0);
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());
        assertEquals("2024-12-23", track.getName());
        assertEquals(2, track.getPositions().size());

        Wgs84Route visits = routes.get(1);
        assertEquals(RouteCharacteristics.Waypoints, visits.getCharacteristics());
        assertEquals("Visits", visits.getName());
        assertEquals(1, visits.getPositions().size());

        Wgs84Position visitPosition = visits.getPositions().get(0);
        assertEquals("Home", visitPosition.getDescription());
        assertEquals(10.029150, visitPosition.getLongitude(), 0.000001);
        assertEquals(53.569885, visitPosition.getLatitude(), 0.000001);
    }

    @Test
    public void testReadIOSWithActivity() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(IOS_WITH_ACTIVITY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());

        Wgs84Route track = routes.get(0);
        assertEquals(3, track.getPositions().size());

        // All points should have "walking" description
        for (Wgs84Position pos : track.getPositions()) {
            assertEquals("walking", pos.getDescription());
        }
    }

    @Test
    public void testReadIOSSameMinute() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(IOS_SAME_MINUTE_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());

        Wgs84Route track = routes.get(0);
        assertEquals(2, track.getPositions().size());

        // Both points should have identical timestamps
        Wgs84Position pos1 = track.getPositions().get(0);
        Wgs84Position pos2 = track.getPositions().get(1);
        assertNotNull(pos1.getTime());
        assertNotNull(pos2.getTime());
        assertEquals(pos1.getTime().getTime(), pos2.getTime().getTime());

        // But different coordinates
        assertNotEquals(pos1.getLongitude(), pos2.getLongitude(), 0.000001);
        assertNotEquals(pos1.getLatitude(), pos2.getLatitude(), 0.000001);
    }

    @Test
    public void testReadAndroidFormat() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(ANDROID_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // 1 track + 1 visits route

        Wgs84Route track = routes.get(0);
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());
        assertEquals("2024-12-23", track.getName());
        assertEquals(2, track.getPositions().size());

        Wgs84Route visits = routes.get(1);
        assertEquals(RouteCharacteristics.Waypoints, visits.getCharacteristics());
        assertEquals("Visits", visits.getName());
        assertEquals(1, visits.getPositions().size());

        Wgs84Position visitPosition = visits.getPositions().get(0);
        assertEquals("Work", visitPosition.getDescription());
        assertEquals(10.029150, visitPosition.getLongitude(), 0.000001);
        assertEquals(53.569885, visitPosition.getLatitude(), 0.000001);
    }

    @Test
    public void testReadVisitMissingLocation() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(VISIT_MISSING_LOCATION_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size()); // only the track, visit is skipped

        Wgs84Route track = routes.get(0);
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());
        assertEquals(1, track.getPositions().size()); // only the path point
    }

    @Test
    public void testReadGeoJsonFeatureCollection() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(GEOJSON_FEATURE_COLLECTION.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(0, routes.size()); // should not steal GeoJSON files
    }

    @Test
    public void testReadEmptyArray() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(EMPTY_ARRAY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(0, routes.size());
    }

    @Test
    public void testReadInvalidFormat() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(INVALID_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(0, routes.size());
    }

    @Test
    public void testFormatIdentity() {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        assertEquals(".json", format.getExtension());
        assertEquals("Google Timeline (*.json)", format.getName());
        assertEquals(Integer.MAX_VALUE, format.getMaximumPositionCount());
        assertTrue(format.isSupportsReading());
        assertFalse(format.isSupportsWriting());
        assertTrue(format.isSupportsMultipleRoutes());
    }

    private CompactCalendar parseTime(String timeString) {
        Calendar calendar = ISO8601.parseDate(timeString);
        return calendar != null ? CompactCalendar.fromCalendar(calendar) : null;
    }

    private ParserContext<Wgs84Route> createParserContext() {
        return new ParserContext<Wgs84Route>() {
            private final List<Wgs84Route> routes = new ArrayList<>();

            public List<Wgs84Route> getRoutes() {
                return routes;
            }

            public void appendRoute(Wgs84Route route) {
                routes.add(route);
            }

            public java.io.File getFile() {
                return new java.io.File("test.json");
            }
        };
    }
}

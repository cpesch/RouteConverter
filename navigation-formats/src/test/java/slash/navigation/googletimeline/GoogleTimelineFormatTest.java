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

import org.junit.jupiter.api.Test;
import slash.common.type.CompactCalendar;
import slash.common.type.ISO8601;
import slash.navigation.base.ParserContext;
import slash.navigation.base.ParserContextImpl;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Calendar;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final String ANDROID_WITH_TIMELINE_MEMORY_JSON =
            "{" +
            "  \"semanticSegments\": [" +
            "    {" +
            "      \"startTime\": \"2024-12-23T08:00:00Z\"," +
            "      \"endTime\": \"2024-12-23T08:05:00Z\"," +
            "      \"timelineMemory\": {\"trip\": {}}" +
            "    }," +
            "    {" +
            "      \"startTime\": \"2024-12-23T09:00:00Z\"," +
            "      \"endTime\": \"2024-12-23T09:30:00Z\"," +
            "      \"visit\": {" +
            "        \"topCandidate\": {" +
            "          \"placeLocation\": {\"latLng\": \"53.569885°, 10.029150°\"}," +
            "          \"semanticType\": \"Home\"" +
            "        }" +
            "      }" +
            "    }," +
            "    {" +
            "      \"startTime\": \"2024-12-23T10:00:00Z\"," +
            "      \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "      \"activity\": {" +
            "        \"topCandidate\": {\"type\": \"walking\", \"probability\": 0.85}" +
            "      }" +
            "    }," +
            "    {" +
            "      \"startTime\": \"2024-12-23T12:00:00Z\"," +
            "      \"endTime\": \"2024-12-23T14:00:00Z\"," +
            "      \"timelinePath\": {" +
            "        \"points\": [" +
            "          {\"location\": {\"latLng\": \"52.5200°, 13.4050°\"}, \"time\": \"2024-12-23T12:00:00Z\"}," +
            "          {\"location\": {\"latLng\": \"52.5210°, 13.4060°\"}, \"time\": \"2024-12-23T12:30:00Z\"}," +
            "          {\"location\": {\"latLng\": \"52.5220°, 13.4070°\"}, \"time\": \"2024-12-23T13:00:00Z\"}" +
            "        ]" +
            "      }" +
            "    }" +
            "  ]" +
            "}";

    private static final String RECORDS_JSON =
            "{" +
            "  \"locations\": [" +
            "    {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"timestamp\": \"2024-12-23T10:00:00Z\"}," +
            "    {\"latitudeE7\": 525300000, \"longitudeE7\": 134150000, \"timestampMs\": \"1734951600000\"}," +
            "    {\"latitudeE7\": 488566000, \"longitudeE7\": 23522000, \"altitude\": 35.5, \"timestamp\": \"2025-03-15T09:00:00Z\"}," +
            "    {\"latitudeE7\": 488576000, \"longitudeE7\": 23532000, \"timestamp\": \"2025-03-15T10:00:00Z\"}," +
            "    {\"latitudeE7\": 950000000, \"longitudeE7\": 23522000, \"timestamp\": \"2025-03-15T11:00:00Z\"}" +
            "  ]" +
            "}";

    private static final String SEMANTIC_LOCATION_HISTORY_RAW_PATH_JSON =
            "{" +
            "  \"timelineObjects\": [" +
            "    {\"placeVisit\": {" +
            "      \"location\": {\"latitudeE7\": 535698850, \"longitudeE7\": 100291500, \"name\": \"Home\"}," +
            "      \"duration\": {\"startTimestamp\": \"2024-12-23T09:00:00Z\", \"endTimestamp\": \"2024-12-23T09:30:00Z\"}" +
            "    }}," +
            "    {\"activitySegment\": {" +
            "      \"activityType\": \"WALKING\"," +
            "      \"startLocation\": {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000}," +
            "      \"endLocation\": {\"latitudeE7\": 525300000, \"longitudeE7\": 134150000}," +
            "      \"duration\": {\"startTimestamp\": \"2024-12-23T10:00:00Z\", \"endTimestamp\": \"2024-12-23T11:00:00Z\"}," +
            "      \"simplifiedRawPath\": {\"points\": [" +
            "        {\"latE7\": 525220000, \"lngE7\": 134070000, \"timestamp\": \"2024-12-23T10:20:00Z\"}," +
            "        {\"latE7\": 525240000, \"lngE7\": 134090000, \"timestamp\": \"2024-12-23T10:40:00Z\"}" +
            "      ]}" +
            "    }}" +
            "  ]" +
            "}";

    private static final String SEMANTIC_LOCATION_HISTORY_WAYPOINT_PATH_JSON =
            "{" +
            "  \"timelineObjects\": [" +
            "    {\"activitySegment\": {" +
            "      \"activityType\": \"WALKING\"," +
            "      \"startLocation\": {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000}," +
            "      \"endLocation\": {\"latitudeE7\": 525300000, \"longitudeE7\": 134150000}," +
            "      \"duration\": {\"startTimestamp\": \"2024-12-23T10:00:00Z\", \"endTimestamp\": \"2024-12-23T11:00:00Z\"}," +
            "      \"waypointPath\": {\"waypoints\": [" +
            "        {\"latE7\": 525220000, \"lngE7\": 134070000}," +
            "        {\"latE7\": 525240000, \"lngE7\": 134090000}" +
            "      ]}" +
            "    }}" +
            "  ]" +
            "}";

    private static final String UNKNOWN_OBJECT_JSON = "{\"foo\": []}";

    private static final String EMPTY_SEMANTIC_SEGMENTS_JSON = "{\"semanticSegments\": []}";

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

    private static final String RECORDS_LOCATIONS_NOT_FIRST_KEY_JSON =
            "{" +
            "  \"foo\": 1," +
            "  \"locations\": [" +
            "    {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"timestamp\": \"2024-12-23T10:00:00Z\"}," +
            "    {\"latitudeE7\": 525300000, \"longitudeE7\": 134150000, \"timestamp\": \"2024-12-23T11:00:00Z\"}" +
            "  ]" +
            "}";

    private static final String RECORDS_ACCURACY_JSON =
            "{" +
            "  \"locations\": [" +
            "    {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"timestamp\": \"2024-12-23T10:00:00Z\", \"accuracy\": 200}," +
            "    {\"latitudeE7\": 525210000, \"longitudeE7\": 134060000, \"timestamp\": \"2024-12-23T11:00:00Z\", \"accuracy\": 201}," +
            "    {\"latitudeE7\": 525220000, \"longitudeE7\": 134070000, \"timestamp\": \"2024-12-23T12:00:00Z\"}" +
            "  ]" +
            "}";

    private static final String RECORDS_VELOCITY_HEADING_JSON =
            "{" +
            "  \"locations\": [" +
            "    {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"timestamp\": \"2024-12-23T10:00:00Z\"," +
            "     \"velocity\": 10, \"heading\": 270}" +
            "  ]" +
            "}";

    private static final String RECORDS_TWO_DEVICES_JSON =
            "{" +
            "  \"locations\": [" +
            "    {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"timestamp\": \"2022-01-12T08:00:00Z\", \"deviceTag\": \"111\"}," +
            "    {\"latitudeE7\": 525210000, \"longitudeE7\": 134060000, \"timestamp\": \"2022-01-12T09:00:00Z\", \"deviceTag\": \"222\"}," +
            "    {\"latitudeE7\": 525220000, \"longitudeE7\": 134070000, \"timestamp\": \"2022-01-13T08:00:00Z\", \"deviceTag\": \"222\"}" +
            "  ]" +
            "}";

    private static final String RECORDS_DEVICE_NUMBERING_FILTERED_FIRST_JSON =
            "{" +
            "  \"locations\": [" +
            "    {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"timestamp\": \"2022-01-12T08:00:00Z\", \"deviceTag\": \"111\", \"accuracy\": 9999}," +
            "    {\"latitudeE7\": 525210000, \"longitudeE7\": 134060000, \"timestamp\": \"2022-01-12T09:00:00Z\", \"deviceTag\": \"222\"}," +
            "    {\"latitudeE7\": 525220000, \"longitudeE7\": 134070000, \"timestamp\": \"2022-01-12T10:00:00Z\", \"deviceTag\": \"111\"}" +
            "  ]" +
            "}";

    private static final String RECORDS_NO_ADJACENT_DAY_MERGE_JSON =
            "{" +
            "  \"locations\": [" +
            "    {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"timestamp\": \"2022-01-12T23:59:00Z\"}," +
            "    {\"latitudeE7\": 525200100, \"longitudeE7\": 134050100, \"timestamp\": \"2022-01-13T00:01:00Z\"}" +
            "  ]" +
            "}";

    private static final String SEMANTIC_TWO_SEGMENTS_SAME_DAY_JSON =
            "{" +
            "  \"timelineObjects\": [" +
            "    {\"activitySegment\": {" +
            "      \"activityType\": \"CYCLING\"," +
            "      \"startLocation\": {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000}," +
            "      \"endLocation\": {\"latitudeE7\": 525300000, \"longitudeE7\": 134150000}," +
            "      \"duration\": {\"startTimestamp\": \"2021-01-05T10:12:00Z\", \"endTimestamp\": \"2021-01-05T11:00:00Z\"}" +
            "    }}," +
            "    {\"activitySegment\": {" +
            "      \"activityType\": \"WALKING\"," +
            "      \"startLocation\": {\"latitudeE7\": 525400000, \"longitudeE7\": 134250000}," +
            "      \"endLocation\": {\"latitudeE7\": 525500000, \"longitudeE7\": 134350000}," +
            "      \"duration\": {\"startTimestamp\": \"2021-01-05T14:00:00Z\", \"endTimestamp\": \"2021-01-05T14:30:00Z\"}" +
            "    }}" +
            "  ]" +
            "}";

    private static final String PLACE_VISIT_ADDRESS_ONLY_JSON =
            "{" +
            "  \"timelineObjects\": [" +
            "    {\"placeVisit\": {" +
            "      \"location\": {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"address\": \"Unter den Linden 1, Berlin\"}," +
            "      \"duration\": {\"startTimestamp\": \"2024-12-23T09:00:00Z\"}" +
            "    }}" +
            "  ]" +
            "}";

    private static final String PLACE_VISIT_SEMANTIC_TYPE_ONLY_JSON =
            "{" +
            "  \"timelineObjects\": [" +
            "    {\"placeVisit\": {" +
            "      \"location\": {\"latitudeE7\": 525200000, \"longitudeE7\": 134050000, \"semanticType\": \"TYPE_HOME\"}," +
            "      \"duration\": {\"startTimestamp\": \"2024-12-23T09:00:00Z\"}" +
            "    }}" +
            "  ]" +
            "}";

    private static final String PLACE_VISIT_CENTER_COORDINATES_JSON =
            "{" +
            "  \"timelineObjects\": [" +
            "    {\"placeVisit\": {" +
            "      \"location\": {\"name\": \"Mystery place\"}," +
            "      \"centerLatE7\": 525200000, \"centerLngE7\": 134050000," +
            "      \"duration\": {\"startTimestamp\": \"2024-12-23T09:00:00Z\"}" +
            "    }}" +
            "  ]" +
            "}";

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
        long expected = start.getTimeInMillis() + (47L * 60 * 1000);
        assertEquals(expected, result.getTimeInMillis());
    }

    @Test
    public void testPointTimeMaxOffset() {
        CompactCalendar start = parseTime("2024-12-23T10:00:00Z");
        CompactCalendar result = GoogleTimelineFormat.pointTime(start, "121");
        assertNotNull(result);
        long expected = start.getTimeInMillis() + (121L * 60 * 1000);
        assertEquals(expected, result.getTimeInMillis());
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
    public void testActivityTypeAtPointInsideWindow() throws Exception {
        // Test through end-to-end read with activity data
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        String json =
            "[" +
            "  {\"startTime\": \"2024-12-23T10:00:00Z\", \"endTime\": \"2024-12-23T12:00:00Z\"," +
            "   \"activity\": {\"topCandidate\": {\"type\": \"walking\", \"probability\": 0.8}}}," +
            "  {\"startTime\": \"2024-12-23T10:30:00Z\", \"endTime\": \"2024-12-23T11:00:00Z\"," +
            "   \"timelinePath\": {\"points\": [{" +
            "     \"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}]}}" +
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
    public void testReadAndroidSkipsUnknownFirstSegment() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(ANDROID_WITH_TIMELINE_MEMORY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // 1 track + 1 visits route

        Wgs84Route track = routes.get(0);
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());
        assertEquals(3, track.getPositions().size());
        for (Wgs84Position pos : track.getPositions()) {
            assertEquals("walking", pos.getDescription());
        }

        Wgs84Route visits = routes.get(1);
        assertEquals(RouteCharacteristics.Waypoints, visits.getCharacteristics());
        assertEquals(1, visits.getPositions().size());
        assertEquals("Home", visits.getPositions().get(0).getDescription());
    }

    @Test
    public void testReadIOSWithByteOrderMark() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        String prefixed = "﻿" + IOS_SAME_DAY_JSON;
        InputStream input = new ByteArrayInputStream(prefixed.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());

        Wgs84Route route = routes.get(0);
        assertEquals(RouteCharacteristics.Track, route.getCharacteristics());
        assertEquals("2024-12-23", route.getName());
        assertEquals(5, route.getPositions().size());
    }

    @Test
    public void testReadTakeoutRecords() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(RECORDS_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // out-of-range entry skipped, two far-apart days

        Wgs84Route firstDay = routes.get(0);
        assertEquals(RouteCharacteristics.Track, firstDay.getCharacteristics());
        assertEquals("2024-12-23", firstDay.getName());
        assertEquals(2, firstDay.getPositions().size());

        Wgs84Route secondDay = routes.get(1);
        assertEquals(RouteCharacteristics.Track, secondDay.getCharacteristics());
        assertEquals("2025-03-15", secondDay.getName());
        assertEquals(2, secondDay.getPositions().size());
        assertEquals(35.5, secondDay.getPositions().get(0).getElevation(), 0.001);
    }

    @Test
    public void testReadSemanticLocationHistoryWithSimplifiedRawPath() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(SEMANTIC_LOCATION_HISTORY_RAW_PATH_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // 1 track + 1 visits route

        Wgs84Route track = routes.get(0);
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());
        assertEquals("2024-12-23 10:00 WALKING", track.getName());
        assertEquals(4, track.getPositions().size()); // start, 2 raw points, end
        for (Wgs84Position pos : track.getPositions()) {
            assertEquals("WALKING", pos.getDescription());
        }

        Wgs84Route visits = routes.get(1);
        assertEquals(RouteCharacteristics.Waypoints, visits.getCharacteristics());
        assertEquals(1, visits.getPositions().size());
        assertEquals("Home", visits.getPositions().get(0).getDescription());
    }

    @Test
    public void testReadSemanticLocationHistoryWithWaypointPath() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(SEMANTIC_LOCATION_HISTORY_WAYPOINT_PATH_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size()); // no placeVisit, just the track

        Wgs84Route track = routes.get(0);
        assertEquals(RouteCharacteristics.Track, track.getCharacteristics());
        assertEquals("2024-12-23 10:00 WALKING", track.getName());
        assertEquals(4, track.getPositions().size()); // start, 2 waypoints, end
    }

    @Test
    public void testReadUnknownObjectShape() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(UNKNOWN_OBJECT_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        assertEquals(0, context.getRoutes().size());
    }

    @Test
    public void testReadEmptySemanticSegments() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(EMPTY_SEMANTIC_SEGMENTS_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        assertEquals(0, context.getRoutes().size());
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
    public void testReadTakeoutRecordsStreamingLargeFile() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        int count = 3000;
        StringBuilder json = new StringBuilder();
        json.append("{\"locations\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) json.append(",");
            String timestamp = i < count / 2 ? "2022-01-12T10:00:00Z" : "2022-01-13T10:00:00Z";
            json.append("{\"latitudeE7\":525200000,\"longitudeE7\":134050000,\"timestamp\":\"")
                    .append(timestamp).append("\",\"deviceTag\":\"1\"}");
        }
        json.append("]}");
        assertTrue(json.length() > (1 << 16), "fixture must exceed the streaming probe's 64 KiB mark");

        InputStream input = new ByteArrayInputStream(json.toString().getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // two UTC days, one device
        int total = routes.get(0).getPositions().size() + routes.get(1).getPositions().size();
        assertEquals(count, total);
    }

    @Test
    public void testReadLargeIOSFileStillUsesReadTree() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        StringBuilder json = new StringBuilder();
        json.append("[");
        int count = 400;
        for (int i = 0; i < count; i++) {
            if (i > 0) json.append(",");
            json.append("{\"startTime\": \"2024-12-23T10:00:00Z\", \"endTime\": \"2024-12-23T10:05:00Z\",")
                    .append(" \"timelinePath\": {\"points\": [")
                    .append("{\"location\": \"geo:52.5200,13.4050\", \"durationMinutesOffsetFromStartTime\": \"0\"}")
                    .append("]}}");
        }
        json.append("]");
        assertTrue(json.length() > (1 << 16), "fixture must exceed the streaming probe's 64 KiB mark");

        InputStream input = new ByteArrayInputStream(json.toString().getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertTrue(routes.size() > 0);
    }

    @Test
    public void testReadTakeoutRecordsLocationsNotFirstKey() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(RECORDS_LOCATIONS_NOT_FIRST_KEY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());
        assertEquals(2, routes.get(0).getPositions().size());
    }

    @Test
    public void testE7ToDegreesUnsignedOverflowLatitude() {
        assertEquals(-0.0000001, GoogleTimelineFormat.e7ToDegrees(4294967295L, true), 0.00000001);
    }

    @Test
    public void testE7ToDegreesOutOfRangeLatitudeAfterOverflowFix() {
        assertNull(GoogleTimelineFormat.e7ToDegrees(950000000L, true));
    }

    @Test
    public void testE7ToDegreesNegativeLatitudeWithinRange() {
        assertEquals(-90.0, GoogleTimelineFormat.e7ToDegrees(-900000000L, true), 0.00000001);
    }

    @Test
    public void testE7ToDegreesUnsignedOverflowLongitude() {
        assertEquals(-180.0, GoogleTimelineFormat.e7ToDegrees(2494967296L, false), 0.00000001);
    }

    @Test
    public void testE7ToDegreesOutOfRangeLongitudeAfterOverflowFix() {
        assertNull(GoogleTimelineFormat.e7ToDegrees(1800000001L, false));
    }

    @Test
    public void testReadTakeoutRecordsAccuracyFilter() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(RECORDS_ACCURACY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());
        assertEquals(2, routes.get(0).getPositions().size()); // accuracy 201 skipped, 200 and missing kept
    }

    @Test
    public void testReadTakeoutRecordsVelocityAndHeading() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(RECORDS_VELOCITY_HEADING_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        Wgs84Position position = context.getRoutes().get(0).getPositions().get(0);
        assertEquals(36.0, position.getSpeed(), 0.001); // 10 m/s * 3.6
        assertEquals(270.0, position.getHeading(), 0.001);
    }

    @Test
    public void testReadTakeoutRecordsDeviceNumbering() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(RECORDS_TWO_DEVICES_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(3, routes.size());
        assertEquals("2022-01-12 (device 1)", routes.get(0).getName());
        assertEquals("2022-01-12 (device 2)", routes.get(1).getName());
        assertEquals("2022-01-13", routes.get(2).getName()); // single device that day, plain name
    }

    @Test
    public void testReadTakeoutRecordsDeviceNumberingSurvivesFilteredFirstRecord() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(RECORDS_DEVICE_NUMBERING_FILTERED_FIRST_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size());
        // deviceTag "111" appears first in the file (even though that record is dropped by
        // the accuracy filter), so it must still claim device number 1; "222", whose only
        // record survives, is numbered 2 - not the other way around.
        assertEquals("2022-01-12 (device 1)", routes.get(0).getName());
        assertEquals(52.522, routes.get(0).getPositions().get(0).getLatitude(), 0.0001);
        assertEquals("2022-01-12 (device 2)", routes.get(1).getName());
        assertEquals(52.521, routes.get(1).getPositions().get(0).getLatitude(), 0.0001);
    }

    @Test
    public void testReadTakeoutRecordsNoAdjacentDayMerge() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(RECORDS_NO_ADJACENT_DAY_MERGE_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // Records.json never merges across days, even at (almost) the same place
        assertEquals("2022-01-12", routes.get(0).getName());
        assertEquals("2022-01-13", routes.get(1).getName());
    }

    @Test
    public void testReadSemanticLocationHistoryTwoSegmentsSameDay() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(SEMANTIC_TWO_SEGMENTS_SAME_DAY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(2, routes.size()); // two activitySegment tracks, no placeVisit, so no Visits route
        assertEquals("2021-01-05 10:12 CYCLING", routes.get(0).getName());
        assertEquals("2021-01-05 14:00 WALKING", routes.get(1).getName());
    }

    @Test
    public void testCreateSegmentNameActivityTypeOnly() {
        assertEquals("WALKING", GoogleTimelineFormat.createSegmentName(null, "WALKING"));
    }

    @Test
    public void testCreateSegmentNameNeitherStartNorActivityType() {
        assertEquals("Activity", GoogleTimelineFormat.createSegmentName(null, null));
    }

    @Test
    public void testCreateSegmentNameStartTimeOnly() {
        CompactCalendar start = parseTime("2021-01-05T10:12:00Z");
        assertEquals("2021-01-05 10:12", GoogleTimelineFormat.createSegmentName(start, null));
    }

    @Test
    public void testCreateSegmentNameFromOldStyleMillis() {
        CompactCalendar start = CompactCalendar.fromMillis(1609841520000L); // 2021-01-05T10:12:00Z
        assertTrue(GoogleTimelineFormat.createSegmentName(start, null).startsWith("2021-01-05 10:12"));
    }

    @Test
    public void testPlaceVisitFallsBackToAddress() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(PLACE_VISIT_ADDRESS_ONLY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());
        assertEquals("Unter den Linden 1, Berlin", routes.get(0).getPositions().get(0).getDescription());
    }

    @Test
    public void testPlaceVisitFallsBackToSemanticType() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(PLACE_VISIT_SEMANTIC_TYPE_ONLY_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());
        assertEquals("TYPE_HOME", routes.get(0).getPositions().get(0).getDescription());
    }

    @Test
    public void testPlaceVisitFallsBackToCenterCoordinates() throws Exception {
        GoogleTimelineFormat format = new GoogleTimelineFormat();
        ParserContext<Wgs84Route> context = createParserContext();

        InputStream input = new ByteArrayInputStream(PLACE_VISIT_CENTER_COORDINATES_JSON.getBytes(UTF8_ENCODING));
        format.read(input, context);

        List<Wgs84Route> routes = context.getRoutes();
        assertEquals(1, routes.size());
        Wgs84Position position = routes.get(0).getPositions().get(0);
        assertEquals("Mystery place", position.getDescription());
        assertEquals(13.4050, position.getLongitude(), 0.000001);
        assertEquals(52.5200, position.getLatitude(), 0.000001);
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
        return new ParserContextImpl<>(new java.io.File("test.json"), null);
    }
}

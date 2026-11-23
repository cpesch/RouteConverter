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

import org.junit.jupiter.api.Test;
import slash.navigation.base.Wgs84Position;
import slash.navigation.base.Wgs84Route;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static slash.common.TestCase.assertDoubleEquals;
import static slash.navigation.url.GoogleMapsListClient.createRequestUrl;
import static slash.navigation.url.GoogleMapsListClient.extractListId;
import static slash.navigation.url.GoogleMapsListClient.parseListResponse;

public class GoogleMapsListClientTest {
    // verbatim from the live probe (2026-11-22, maps.app.goo.gl/RokrZ6ZxUJTnt5hu8), trimmed to the
    // used fields; the list owner's name/avatar (the first array entry) has been removed
    private static final String LIVE_LIST_2026 = ")]}'\n" +
            "[[[\"oq0an0dTHsvvN0Dsi8-gA5R1E49WAQ\",1,null,1,1],4,[2,1,\"https://www.google.com/maps/placelists/list/oq0an0dTHsvvN0Dsi8-gA5R1E49WAQ\"],null,\"Get away\",\"\",null,null,[[null,[null,null,\"Essaouira, Morocco\",null,\"Essaouira, Morocco\",[null,null,31.5084926,-9.7595041],[\"985613555935513807\",\"6287616717684047152\"]],\"Essaouira\",\"\"],[null,[null,null,\"Royal Tunbridge Wells, Tunbridge Wells\",null,\"Royal Tunbridge Wells, Tunbridge Wells\",[null,null,51.132377,0.263695],[\"5178920352067746009\",\"8309275710371309412\"]],\"Royal Tunbridge Wells\",\"\"],[null,[null,null,\"Rye\",null,\"Rye\",[null,null,50.949708,0.7372599999999999],[\"5178823072017633721\",\"2096826071303977536\"]],\"Rye\",\"\"],[null,[null,null,\"\",null,\"Lübbenau, Germany\",[null,null,51.902993599999995,13.967148499999999],[\"5118308034030562929\",\"-299090031316690501\"]],\"Spreewald Biosphere Reserve\",\"\"],[null,[null,null,\"Spreewald, 15907 Lübben, Germany\",null,\"Spreewald, 15907 Lübben, Germany\",[null,null,51.9140185,13.9263099],[\"5118339161121282955\",\"4261247488879010892\"]],\"Spreewald\",\"\"]],[],null,null,5]]";

    private final GoogleMapsUrlFormat format = new GoogleMapsUrlFormat();

    @Test
    public void testParseListResponse() {
        Wgs84Route route = parseListResponse(LIVE_LIST_2026, format);
        assertNotNull(route);
        assertEquals("Get away", route.getName());
        List<Wgs84Position> positions = route.getPositions();
        assertEquals(5, positions.size());

        Wgs84Position first = positions.get(0);
        assertEquals("Essaouira", first.getDescription());
        assertDoubleEquals(31.5084926, first.getLatitude());
        assertDoubleEquals(-9.7595041, first.getLongitude());

        Wgs84Position fourth = positions.get(3);
        assertEquals("Spreewald Biosphere Reserve", fourth.getDescription());
        assertDoubleEquals(51.902993599999995, fourth.getLatitude());
        assertDoubleEquals(13.967148499999999, fourth.getLongitude());
    }

    @Test
    public void testParseListResponseWithoutXssiPrefix() {
        String withoutPrefix = LIVE_LIST_2026.substring(LIVE_LIST_2026.indexOf('\n') + 1);
        Wgs84Route route = parseListResponse(withoutPrefix, format);
        assertNotNull(route);
        assertEquals("Get away", route.getName());
        assertEquals(5, route.getPositions().size());
    }

    @Test
    public void testParseListResponseInvalidOrEmptyReturnsNull() {
        assertNull(parseListResponse(")]}'\n[null,null,[4,\"x\"]]", format));
        assertNull(parseListResponse("", format));
        assertNull(parseListResponse("garbage", format));
        assertNull(parseListResponse(")]}'\n[]", format));
    }

    @Test
    public void testParseListResponseSkipsPlacesWithMissingOrOutOfRangeCoordinates() {
        String withBadPlaces = ")]}'\n" +
                "[[null,4,null,null,\"List\",\"\",null,null,[" +
                "[null,[null,null,\"\",null,\"\",null,null],\"No coordinate\",\"\"]," +
                "[null,[null,null,\"\",null,\"\",[null,null,95.0,13.0],null],\"Out of range\",\"\"]," +
                "[null,[null,null,\"\",null,\"\",[null,null,31.5084926,-9.7595041],null],\"Essaouira\",\"\"]" +
                "],[],null,null,3]]";
        Wgs84Route route = parseListResponse(withBadPlaces, format);
        assertNotNull(route);
        List<Wgs84Position> positions = route.getPositions();
        assertEquals(1, positions.size());
        assertEquals("Essaouira", positions.get(0).getDescription());
    }

    @Test
    public void testParseListResponseCountMismatchLogsWarningButKeepsAllParsedPlaces() {
        String withMismatchedTotal = LIVE_LIST_2026.replaceFirst("null,null,5]]$", "null,null,7]]");
        Wgs84Route route = parseListResponse(withMismatchedTotal, format);
        assertNotNull(route);
        assertEquals(5, route.getPositions().size());
    }

    @Test
    public void testParseListResponseEmptyNameFallsBackToDefaultRouteName() {
        String withEmptyName = LIVE_LIST_2026.replace("\"Get away\"", "\"\"");
        Wgs84Route route = parseListResponse(withEmptyName, format);
        assertNotNull(route);
        assertEquals("Essaouira to Spreewald", route.getName());
    }

    @Test
    public void testExtractListId() {
        assertEquals("oq0an0dTHsvvN0Dsi8-gA5R1E49WAQ", extractListId(
                "https://www.google.com/maps/@/data=!3m1!4b1!4m3!11m2!2soq0an0dTHsvvN0Dsi8-gA5R1E49WAQ!3e3?entry=tts"));
        assertEquals("oq0an0dTHsvvN0Dsi8-gA5R1E49WAQ", extractListId(
                "https://www.google.com/maps/placelists/list/oq0an0dTHsvvN0Dsi8-gA5R1E49WAQ?token=redacted"));
        assertNull(extractListId("https://www.google.com/maps/dir/Hamburg/Hannover"));
        assertNull(extractListId("https://www.google.com/maps/place/Berlin/@52.5,13.4,10z"));
    }

    @Test
    public void testCreateRequestUrl() {
        assertEquals("https://www.google.com/maps/preview/entitylist/getlist?authuser=0&hl=en&gl=us&pb=!1m4!1sabc!2e1!3m1!1e1!2e2!3e2!4i500!16b1",
                createRequestUrl("abc"));
    }
}

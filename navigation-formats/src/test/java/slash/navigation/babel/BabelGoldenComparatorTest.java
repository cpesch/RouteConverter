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

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BabelGoldenComparatorTest {
    private static String route(String longitude) {
        return "{\"routes\":[{\"characteristics\":\"Route\",\"name\":\"n\",\"description\":null," +
                "\"positions\":[{\"longitude\":" + longitude + ",\"latitude\":\"48.1000000\"," +
                "\"elevation\":\"100.0000000\",\"time\":\"2007-06-12T09:22:45.000Z\",\"speed\":\"5.0000000\"," +
                "\"heading\":\"90.0000000\",\"hdop\":\"1.0000000\",\"vdop\":\"1.0000000\",\"pdop\":\"1.0000000\"," +
                "\"satellites\":7,\"description\":\"d\"}]}]}";
    }

    @Test
    public void withinToleranceIsNotADifference() {
        String expected = route("\"11.5000000\"");
        String actual = route("\"11.5000005\"");
        assertTrue(BabelGoldenComparator.compare(expected, actual, emptyList()).isEmpty());
    }

    @Test
    public void outsideToleranceIsADifference() {
        String expected = route("\"11.5000000\"");
        String actual = route("\"11.5000020\"");
        List<String> differences = BabelGoldenComparator.compare(expected, actual, emptyList());
        assertEquals(1, differences.size());
        assertTrue(differences.get(0).startsWith("route[0].position[0].longitude:"), differences.get(0));
    }

    @Test
    public void routeCountMismatchIsADifference() {
        String expected = "{\"routes\":[]}";
        String actual = route("\"11.5000000\"");
        List<String> differences = BabelGoldenComparator.compare(expected, actual, emptyList());
        assertEquals(1, differences.size());
        assertTrue(differences.get(0).startsWith("routes.size:"));
    }

    @Test
    public void positionCountMismatchIsADifference() {
        String expected = "{\"routes\":[{\"characteristics\":\"Route\",\"name\":\"n\",\"description\":null,\"positions\":[]}]}";
        String actual = route("\"11.5000000\"");
        List<String> differences = BabelGoldenComparator.compare(expected, actual, emptyList());
        assertEquals(1, differences.size());
        assertTrue(differences.get(0).startsWith("route[0].positions.size:"));
    }

    @Test
    public void characteristicsMismatchIsADifference() {
        String expected = route("\"11.5000000\"").replace("\"Route\"", "\"Track\"");
        String actual = route("\"11.5000000\"");
        List<String> differences = BabelGoldenComparator.compare(expected, actual, emptyList());
        assertEquals(1, differences.size());
        assertTrue(differences.get(0).startsWith("route[0].characteristics:"));
    }

    @Test
    public void descriptionListsAreComparedAfterTrimming() {
        String expected = "{\"routes\":[{\"characteristics\":\"Route\",\"name\":\"n\",\"description\":[\" a \",\"b\"],\"positions\":[]}]}";
        String actual = "{\"routes\":[{\"characteristics\":\"Route\",\"name\":\"n\",\"description\":[\"a\",\" b\"],\"positions\":[]}]}";
        assertTrue(BabelGoldenComparator.compare(expected, actual, emptyList()).isEmpty());
    }

    @Test
    public void allowlistGlobAcceptsOneDifferenceAndNotAnother() {
        String expected = route("\"11.5000000\"");
        String actual = route("\"11.5000020\"").replace("\"d\"}", "\"other\"}");
        List<String> allowlistForLongitude = of("sample route[*].position[*].longitude accepted test-only");
        List<String> differences = BabelGoldenComparator.compare(expected, actual, allowlistForLongitude);
        assertEquals(1, differences.size());
        assertTrue(differences.get(0).startsWith("route[0].position[0].description:"), differences.get(0));
    }

    @Test
    public void commentLinesInAllowlistAreIgnored() {
        String expected = route("\"11.5000000\"");
        String actual = route("\"11.5000020\"");
        List<String> allowlistWithOnlyComments = of("# sample route[*].position[*].longitude accepted test-only");
        List<String> differences = BabelGoldenComparator.compare(expected, actual, allowlistWithOnlyComments);
        assertEquals(1, differences.size());
    }
}

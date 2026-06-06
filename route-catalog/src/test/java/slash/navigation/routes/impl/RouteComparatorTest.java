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

package slash.navigation.routes.impl;

import org.junit.Test;
import slash.navigation.routes.Category;
import slash.navigation.routes.Route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RouteComparator}.
 *
 * @author Christian Pesch
 */
public class RouteComparatorTest {

    private static final RouteComparator COMPARATOR = new RouteComparator();

    private static String descriptionOf(Route route) {
        try {
            return route.getDescription();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Route route(String description, String name) {
        return new Route() {
            public String getHref() { return "http://example.com/" + name; }
            public String getName() { return name; }
            public String getDescription() { return description; }
            public String getCreator() { return "tester"; }
            public String getUrl() { return getHref(); }
            public void update(Category parent, String description) {}
            public void delete() {}
        };
    }

    @Test
    public void sortsAlphabeticallyByDescriptionThenName() {
        Route r1 = route("Alpha", "file1.gpx");
        Route r2 = route("Beta", "file2.gpx");

        assertTrue(COMPARATOR.compare(r1, r2) < 0);
        assertTrue(COMPARATOR.compare(r2, r1) > 0);
    }

    @Test
    public void sameDescriptionAndNameComparesEqual() {
        Route r1 = route("Alpha", "file.gpx");
        Route r2 = route("Alpha", "file.gpx");
        assertEquals(0, COMPARATOR.compare(r1, r2));
    }

    @Test
    public void sortsByNameWhenDescriptionEqual() {
        Route r1 = route("Alpha", "a.gpx");
        Route r2 = route("Alpha", "z.gpx");
        assertTrue(COMPARATOR.compare(r1, r2) < 0);
    }

    @Test
    public void sortingListProducesAlphabeticalOrder() {
        Route r1 = route("Zebra", "z.gpx");
        Route r2 = route("Apple", "a.gpx");
        Route r3 = route("Mango", "m.gpx");

        List<Route> routes = new ArrayList<>();
        routes.add(r1);
        routes.add(r2);
        routes.add(r3);
        routes.sort(COMPARATOR);

        assertEquals("Apple", descriptionOf(routes.get(0)));
        assertEquals("Mango", descriptionOf(routes.get(1)));
        assertEquals("Zebra", descriptionOf(routes.get(2)));
    }
}


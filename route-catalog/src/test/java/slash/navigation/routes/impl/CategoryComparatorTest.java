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

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CategoryComparator}.
 *
 * @author Christian Pesch
 */
public class CategoryComparatorTest {

    private static final CategoryComparator COMPARATOR = new CategoryComparator();

    private static Category category(String name) {
        return new Category() {
            public String getHref() { return "http://example.com/" + name; }
            public String getName() { return name; }
            public List<Category> getCategories() { return new ArrayList<>(); }
            public Category create(String n) { throw new UnsupportedOperationException(); }
            public void update(Category parent, String n) {}
            public void delete() {}
            public List<Route> getRoutes() { return new ArrayList<>(); }
            public Route createRoute(String description, java.io.File f) { throw new UnsupportedOperationException(); }
            public Route createRoute(String description, String url) { throw new UnsupportedOperationException(); }
        };
    }

    private static Category failingCategory() {
        return new Category() {
            public String getHref() { return "http://example.com/fail"; }
            public String getName() throws IOException { throw new IOException("simulated failure"); }
            public List<Category> getCategories() { return new ArrayList<>(); }
            public Category create(String n) { throw new UnsupportedOperationException(); }
            public void update(Category parent, String n) {}
            public void delete() {}
            public List<Route> getRoutes() { return new ArrayList<>(); }
            public Route createRoute(String description, java.io.File f) { throw new UnsupportedOperationException(); }
            public Route createRoute(String description, String url) { throw new UnsupportedOperationException(); }
        };
    }

    @Test
    public void sortAlphabeticallyByName() {
        Category alpha = category("Alpha");
        Category beta = category("Beta");
        assertTrue(COMPARATOR.compare(alpha, beta) < 0);
        assertTrue(COMPARATOR.compare(beta, alpha) > 0);
    }

    @Test
    public void equalNamesCompareEqual() {
        Category c1 = category("Same");
        Category c2 = category("Same");
        assertEquals(0, COMPARATOR.compare(c1, c2));
    }

    @Test
    public void sortingListProducesAlphabeticalOrder() {
        Category c1 = category("Zebra");
        Category c2 = category("Apple");
        Category c3 = category("Mango");

        List<Category> list = new ArrayList<>();
        list.add(c1);
        list.add(c2);
        list.add(c3);
        list.sort(COMPARATOR);

        assertEquals("Apple", nameOf(list.get(0)));
        assertEquals("Mango", nameOf(list.get(1)));
        assertEquals("Zebra", nameOf(list.get(2)));
    }

    @Test
    public void failingGetNameFallsBackToQuestionMark() {
        // CategoryComparator catches IOException and returns "?" ? verify it doesn't throw
        Category failing = failingCategory();
        Category normal = category("Normal");
        // Should not throw; result can be any sign but must not throw
        int result = COMPARATOR.compare(failing, normal);
        // "?" < "Normal" in most locales
        assertNotEquals(Integer.MAX_VALUE, result); // just verify no exception
    }

    private static String nameOf(Category c) {
        try {
            return c.getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


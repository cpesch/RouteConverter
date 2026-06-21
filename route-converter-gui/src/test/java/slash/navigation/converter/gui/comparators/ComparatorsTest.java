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

package slash.navigation.converter.gui.comparators;

import org.junit.Test;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.common.type.CompactCalendar.fromMillis;

/**
 * Unit tests for {@link DateTimeComparator} and {@link DescriptionComparator}.
 *
 * @author Christian Pesch
 */
public class ComparatorsTest {

    // ---- DateTimeComparator ----

    private static final DateTimeComparator DATE_TIME = new DateTimeComparator();

    private static SimpleNavigationPosition posAt(long millis) {
        return new SimpleNavigationPosition(0.0, 0.0, fromMillis(millis));
    }

    private static SimpleNavigationPosition posNoTime() {
        return new SimpleNavigationPosition(0.0, 0.0);
    }

    @Test
    public void dateTimeEarlierComesFirst() {
        NavigationPosition early = posAt(1_000_000L);
        NavigationPosition late = posAt(2_000_000L);
        assertTrue(DATE_TIME.compare(early, late) < 0);
        assertTrue(DATE_TIME.compare(late, early) > 0);
    }

    @Test
    public void dateTimeSameTimestampIsEqual() {
        NavigationPosition p1 = posAt(1_000_000L);
        NavigationPosition p2 = posAt(1_000_000L);
        assertEquals(0, DATE_TIME.compare(p1, p2));
    }

    @Test
    public void dateTimeNoTimeSortsBeforeHasTime() {
        // p1 has no time ? compare returns -1
        assertTrue(DATE_TIME.compare(posNoTime(), posAt(1_000_000L)) < 0);
    }

    @Test
    public void dateTimeHasTimeSortsAfterNoTime() {
        // p2 has no time ? compare returns 1
        assertTrue(DATE_TIME.compare(posAt(1_000_000L), posNoTime()) > 0);
    }

    @Test
    public void dateTimeSortingListProducesChronologicalOrder() {
        NavigationPosition p3 = posAt(3_000_000L);
        NavigationPosition p1 = posAt(1_000_000L);
        NavigationPosition p2 = posAt(2_000_000L);

        List<NavigationPosition> list = new ArrayList<>(List.of(p3, p1, p2));
        list.sort(DATE_TIME);

        assertEquals(p1, list.get(0));
        assertEquals(p2, list.get(1));
        assertEquals(p3, list.get(2));
    }

    // ---- DescriptionComparator ----

    private static final DescriptionComparator DESCRIPTION = new DescriptionComparator();

    private static SimpleNavigationPosition posDesc(String description) {
        return new SimpleNavigationPosition(0.0, 0.0, 0.0, description);
    }

    @Test
    public void descriptionAlphabeticalOrder() {
        NavigationPosition apple = posDesc("Apple");
        NavigationPosition zebra = posDesc("Zebra");
        assertTrue(DESCRIPTION.compare(apple, zebra) < 0);
        assertTrue(DESCRIPTION.compare(zebra, apple) > 0);
    }

    @Test
    public void descriptionSameIsEqual() {
        assertEquals(0, DESCRIPTION.compare(posDesc("Same"), posDesc("Same")));
    }

    @Test
    public void descriptionCaseInsensitive() {
        NavigationPosition lower = posDesc("apple");
        NavigationPosition upper = posDesc("Apple");
        assertEquals(0, DESCRIPTION.compare(lower, upper));
    }

    @Test
    public void descriptionNullSortsFirst() {
        NavigationPosition nullDesc = posDesc(null);
        NavigationPosition hasDesc = posDesc("Something");
        assertTrue(DESCRIPTION.compare(nullDesc, hasDesc) < 0);
        assertTrue(DESCRIPTION.compare(hasDesc, nullDesc) > 0);
    }

    @Test
    public void descriptionSortingListProducesAlphabeticalOrder() {
        NavigationPosition z = posDesc("Zebra");
        NavigationPosition a = posDesc("Apple");
        NavigationPosition m = posDesc("Mango");

        List<NavigationPosition> list = new ArrayList<>(List.of(z, a, m));
        list.sort(DESCRIPTION);

        assertEquals("Apple", list.get(0).getDescription());
        assertEquals("Mango", list.get(1).getDescription());
        assertEquals("Zebra", list.get(2).getDescription());
    }
}


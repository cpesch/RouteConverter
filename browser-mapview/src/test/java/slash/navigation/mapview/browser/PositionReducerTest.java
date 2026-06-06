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

package slash.navigation.mapview.browser;

import org.junit.Test;
import slash.navigation.base.RouteCharacteristics;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static slash.navigation.base.RouteCharacteristics.*;

public class PositionReducerTest {
    private final PositionReducer reducer = new PositionReducer(new PositionReducer.Callback() {
        public int getZoom() {
            throw new UnsupportedOperationException();
        }
        public NavigationPosition getNorthEastBounds() {
            return new SimpleNavigationPosition(1.0, 1.0);
        }

        public NavigationPosition getSouthWestBounds() {
            return new SimpleNavigationPosition(-1.0, -1.0);
        }

    });

    private void filterEveryNthPosition(int positionCount, int maximumPositionCount) {
        List<NavigationPosition> positions = new ArrayList<>();
        NavigationPosition first = new SimpleNavigationPosition(0.0, 0.0);
        positions.add(first);

        for (int i = 1; i < positionCount - 1; i++)
            positions.add(new SimpleNavigationPosition((double) i, 0.0));

        NavigationPosition last = new SimpleNavigationPosition((double) positionCount - 1, 0.0);
        positions.add(last);

        List<NavigationPosition> result = reducer.filterEveryNthPosition(positions, maximumPositionCount);

        assertEquals(maximumPositionCount, result.size());
        assertEquals(first, result.get(0));
        double increment = (positionCount - 1) / (double) (maximumPositionCount - 1);
        for (int i = 1; i < maximumPositionCount - 1; i++) {
            assertEquals(new SimpleNavigationPosition((double) (int) (increment * i + 1.0), 0.0), result.get(i));
        }
        assertEquals(last, result.get(maximumPositionCount - 1));
    }

    @Test
    public void testFilterEveryNthPositionEvenIncrement() {
        filterEveryNthPosition(7, 4);
        filterEveryNthPosition(10, 4);
    }

    @Test
    public void testFilterEveryNthPosition() {
        filterEveryNthPosition(1001, 17);
    }

    @Test
    public void testFilterVisiblePosition() {
        List<NavigationPosition> positions = new ArrayList<>();
        NavigationPosition one = new SimpleNavigationPosition(0.0, 0.0);
        positions.add(one);
        NavigationPosition two = new SimpleNavigationPosition(0.1, 0.1);
        positions.add(two);
        NavigationPosition threeNotVisible = new SimpleNavigationPosition(45.0, 45.0);
        positions.add(threeNotVisible);
        NavigationPosition fourNotVisible = new SimpleNavigationPosition(45.1, 45.1);
        positions.add(fourNotVisible);
        NavigationPosition fiveNotVisible = new SimpleNavigationPosition(45.1, 45.1);
        positions.add(fiveNotVisible);
        NavigationPosition six = new SimpleNavigationPosition(0.2, 0.2);
        positions.add(six);

        List<NavigationPosition> result = reducer.filterVisiblePositions(positions, 1.0, false);
        assertEquals(5, result.size());
        assertEquals(one, result.get(0));
        assertEquals(two, result.get(1));
        assertEquals(threeNotVisible, result.get(2));
        assertEquals(fiveNotVisible, result.get(3));
        assertEquals(six, result.get(4));
    }

    @Test
    public void testFilterVisiblePositionIncludingFirstAndLast() {
        // ...existing test...
        List<NavigationPosition> positions = new ArrayList<>();
        NavigationPosition one = new SimpleNavigationPosition(0.0, 0.0);
        positions.add(one);
        NavigationPosition two = new SimpleNavigationPosition(0.1, 0.1);
        positions.add(two);
        NavigationPosition threeNotVisible = new SimpleNavigationPosition(45.0, 45.0);
        positions.add(threeNotVisible);
        NavigationPosition fourNotVisible = new SimpleNavigationPosition(45.1, 45.1);
        positions.add(fourNotVisible);
        NavigationPosition fiveNotVisible = new SimpleNavigationPosition(45.1, 45.1);
        positions.add(fiveNotVisible);
        NavigationPosition six = new SimpleNavigationPosition(0.2, 0.2);
        positions.add(six);
        NavigationPosition seven = new SimpleNavigationPosition(0.3, 0.3);
        positions.add(seven);

        List<NavigationPosition> result = reducer.filterVisiblePositions(positions, 1.0, true);
        assertEquals(6, result.size());
        assertEquals(one, result.get(0));
        assertEquals(two, result.get(1));
        assertEquals(threeNotVisible, result.get(2));
        assertEquals(fiveNotVisible, result.get(3));
        assertEquals(six, result.get(4));
        assertEquals(seven, result.get(5));
    }

    // ---- getMaximumSegmentLength ----

    @Test
    public void testGetMaximumSegmentLengthRoute() {
        assertTrue(reducer.getMaximumSegmentLength(Route) > 0);
    }

    @Test
    public void testGetMaximumSegmentLengthTrack() {
        assertTrue(reducer.getMaximumSegmentLength(Track) > 0);
    }

    @Test
    public void testGetMaximumSegmentLengthWaypoints() {
        assertTrue(reducer.getMaximumSegmentLength(Waypoints) > 0);
    }

    // ---- clear / hasFilteredVisibleArea / isWithinVisibleArea ----

    @Test
    public void testHasFilteredVisibleAreaFalseInitially() {
        PositionReducer fresh = new PositionReducer(new PositionReducer.Callback() {
            public int getZoom() { return 10; }
            public NavigationPosition getNorthEastBounds() { return new SimpleNavigationPosition(1.0, 1.0); }
            public NavigationPosition getSouthWestBounds() { return new SimpleNavigationPosition(-1.0, -1.0); }
        });
        assertFalse(fresh.hasFilteredVisibleArea());
    }

    @Test
    public void testIsWithinVisibleAreaReturnsTrueWhenNoFilterApplied() {
        PositionReducer fresh = new PositionReducer(new PositionReducer.Callback() {
            public int getZoom() { return 10; }
            public NavigationPosition getNorthEastBounds() { return new SimpleNavigationPosition(1.0, 1.0); }
            public NavigationPosition getSouthWestBounds() { return new SimpleNavigationPosition(-1.0, -1.0); }
        });
        // no visible area set -> isWithinVisibleArea always returns true
        assertTrue(fresh.isWithinVisibleArea(new BoundingBox(new SimpleNavigationPosition(0.0, 0.0), new SimpleNavigationPosition(0.0, 0.0))));
    }

    @Test
    public void testClearResetsVisibleArea() {
        PositionReducer r = new PositionReducer(new PositionReducer.Callback() {
            public int getZoom() { return 5; }
            public NavigationPosition getNorthEastBounds() { return new SimpleNavigationPosition(1.0, 1.0); }
            public NavigationPosition getSouthWestBounds() { return new SimpleNavigationPosition(-1.0, -1.0); }
        });
        // Just verify clear() ensures hasFilteredVisibleArea() is false afterward (initial state or after clear)
        r.clear();
        assertFalse(r.hasFilteredVisibleArea());
    }

    // ---- filterPositionsWithoutCoordinates (implicitly via reducePositions) ----

    @Test
    public void testFilterPositionsWithoutCoordinatesViaReduce() {
        PositionReducer r = new PositionReducer(new PositionReducer.Callback() {
            public int getZoom() { return 10; }
            public NavigationPosition getNorthEastBounds() { return new SimpleNavigationPosition(1.0, 1.0); }
            public NavigationPosition getSouthWestBounds() { return new SimpleNavigationPosition(-1.0, -1.0); }
        });
        List<NavigationPosition> positions = new ArrayList<>();
        positions.add(new SimpleNavigationPosition(0.0, 0.0));
        positions.add(new SimpleNavigationPosition(null, null));  // no coordinates
        positions.add(new SimpleNavigationPosition(0.1, 0.1));

        // With 3 input positions reduced to those with coordinates, result is ? 3
        List<NavigationPosition> result = r.reducePositions(positions, Waypoints, false);
        for (NavigationPosition pos : result)
            assertTrue("all result positions should have coordinates", pos.hasCoordinates());
    }
}

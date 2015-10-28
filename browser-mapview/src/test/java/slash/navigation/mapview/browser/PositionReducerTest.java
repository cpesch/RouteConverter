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
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PositionReducerTest {
    private PositionReducer reducer = new PositionReducer(new PositionReducer.Callback() {
        public int getZoom() {
            throw new UnsupportedOperationException();
        }
        public NavigationPosition getNorthEastBounds() {
            return asPosition(1.0, 1.0);
        }

        public NavigationPosition getSouthWestBounds() {
            return asPosition(-1.0, -1.0);
        }

    });

    private NavigationPosition asPosition(double longitude, double latitude) {
        return new SimpleNavigationPosition(longitude, latitude);
    }

    private void filterEveryNthPosition(int positionCount, int maximumPositionCount) {
        List<NavigationPosition> positions = new ArrayList<>();
        NavigationPosition first = asPosition(0.0, 0.0);
        positions.add(first);

        for (int i = 1; i < positionCount - 1; i++)
            positions.add(asPosition(i, 0.0));

        NavigationPosition last = asPosition(positionCount - 1, 0.0);
        positions.add(last);

        List<NavigationPosition> result = reducer.filterEveryNthPosition(positions, maximumPositionCount);

        assertEquals(maximumPositionCount, result.size());
        assertEquals(first, result.get(0));
        double increment = (positionCount - 1) / (double) (maximumPositionCount - 1);
        for (int i = 1; i < maximumPositionCount - 1; i++) {
            assertEquals(asPosition((int) (increment * i + 1.0), 0.0), result.get(i));
        }
        assertEquals(last, result.get(maximumPositionCount - 1));
    }

    @Test
    public void testFilterEveryNthPositionEvenIncrement() throws Exception {
        filterEveryNthPosition(7, 4);
        filterEveryNthPosition(10, 4);
    }

    @Test
    public void testFilterEveryNthPosition() throws Exception {
        filterEveryNthPosition(1001, 17);
    }

    @Test
    public void testFilterVisiblePosition() throws Exception {
        List<NavigationPosition> positions = new ArrayList<>();
        NavigationPosition one = asPosition(0.0, 0.0);
        positions.add(one);
        NavigationPosition two = asPosition(0.1, 0.1);
        positions.add(two);
        NavigationPosition threeNotVisible = asPosition(45.0, 45.0);
        positions.add(threeNotVisible);
        NavigationPosition fourNotVisible = asPosition(45.1, 45.1);
        positions.add(fourNotVisible);
        NavigationPosition fiveNotVisible = asPosition(45.1, 45.1);
        positions.add(fiveNotVisible);
        NavigationPosition six = asPosition(0.2, 0.2);
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
    public void testFilterVisiblePositionIncludingFirstAndLast() throws Exception {
        List<NavigationPosition> positions = new ArrayList<>();
        NavigationPosition one = asPosition(0.0, 0.0);
        positions.add(one);
        NavigationPosition two = asPosition(0.1, 0.1);
        positions.add(two);
        NavigationPosition threeNotVisible = asPosition(45.0, 45.0);
        positions.add(threeNotVisible);
        NavigationPosition fourNotVisible = asPosition(45.1, 45.1);
        positions.add(fourNotVisible);
        NavigationPosition fiveNotVisible = asPosition(45.1, 45.1);
        positions.add(fiveNotVisible);
        NavigationPosition six = asPosition(0.2, 0.2);
        positions.add(six);
        NavigationPosition seven = asPosition(0.3, 0.3);
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
}

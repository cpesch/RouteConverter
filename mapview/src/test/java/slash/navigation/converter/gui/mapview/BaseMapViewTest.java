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

package slash.navigation.converter.gui.mapview;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.base.BaseNavigationPosition;

import javax.management.RuntimeErrorException;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static slash.common.io.Transfer.ceiling;

public class BaseMapViewTest {
    private static final int MAXIMUM_DIRECTIONS_SEGMENT_LENGTH = 4;
    private BaseMapView view;

    @Before
    public void setUp() throws Exception {
        view = new BaseMapView() {
            @Override
            protected void initializeBrowser() {
                throw new RuntimeException("not implemented");
            }

            @Override
            protected BaseNavigationPosition getNorthEastBounds() {
                throw new RuntimeException("not implemented");
            }

            @Override
            protected BaseNavigationPosition getSouthWestBounds() {
                throw new RuntimeException("not implemented");
            }

            @Override
            protected BaseNavigationPosition getCurrentMapCenter() {
                throw new RuntimeException("not implemented");
            }

            @Override
            protected Integer getCurrentZoom() {
                throw new RuntimeException("not implemented");
            }

            @Override
            protected void executeScript(String script) {
                throw new RuntimeException("not implemented");
            }

            @Override
            protected String executeScriptWithResult(String script) {
                throw new RuntimeException("not implemented");
            }

            @Override
            public boolean isSupportedPlatform() {
                throw new RuntimeException("not implemented");
            }

            @Override
            public Component getComponent() {
                throw new RuntimeException("not implemented");
            }

            @Override
            public void resize() {
                throw new RuntimeException("not implemented");
            }
        };
    }

    @Test
    public void testFilterEveryNthPosition() throws Exception {

        final Random random = new Random();

        final int outCount = random.nextInt(200) + 2;
        final int inCount = random.nextInt(500000) + outCount + 1;

        final ArrayList<Integer> inList = new ArrayList<Integer>();
        for (int i=0; i<inCount; i++) {
            inList.add(random.nextInt());
        }
        assertEquals(inList.size(), inCount);

        List<Integer> result = view.filterEveryNthPosition(inList, outCount);

        assertEquals(outCount, result.size());
        assertEquals(inList.get(0), result.get(0));
        assertEquals(inList.get(inList.size()-1), result.get(result.size()-1));
    }

    private List<Integer> createIntervals(int size) {
        List<Integer> result = new ArrayList<Integer>();
        int directionsCount = ceiling(size, MAXIMUM_DIRECTIONS_SEGMENT_LENGTH, false);
        for (int j = 0; j < directionsCount; j++) {
            int start = max(0, j * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH - 1);
            int end = min(size, (j + 1) * MAXIMUM_DIRECTIONS_SEGMENT_LENGTH) - 1;
            for (int i = start + 1; i < end; i++) {
                result.add(i);
            }

            result.add(start);
            result.add(end);
        }
        return result;
    }

    @Test
    public void intervalsBelowSegmentLength() {
        assertEquals(asList(0, 1), createIntervals(2));
        assertEquals(asList(1, 0, 2), createIntervals(3));
        assertEquals(asList(1, 2, 0, 3), createIntervals(4));
    }

    @Test
    public void intervalsAboveSegmentLength() {
        assertEquals(asList(1, 2, 0, 3, 3, 4), createIntervals(5));
        assertEquals(asList(1, 2, 0, 3, 4, 3, 5), createIntervals(6));
        assertEquals(asList(1, 2, 0, 3, 4, 5, 3, 6), createIntervals(7));
        assertEquals(asList(1, 2, 0, 3, 4, 5, 6, 3, 7), createIntervals(8));
    }

    @Test
    public void intervalsAboveDoubleSegmentLength() {
        assertEquals(asList(1, 2, 0, 3, 4, 5, 6, 3, 7, 7, 8), createIntervals(9));
        assertEquals(asList(1, 2, 0, 3, 4, 5, 6, 3, 7, 8, 7, 9), createIntervals(10));
    }
}

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
package slash.navigation.converter.gui.helpers;

import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.converter.gui.models.RouteDistanceAndTimeCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link OpenedRouteDistanceAndTimeUpdater}.
 *
 * @author Christian Pesch
 */
public class OpenedRouteDistanceAndTimeUpdaterTest {
    private static final String URL_A = "http://example.com/a.gpx";
    private static final String PATH_A = "/loaded/a.gpx";
    private static final String URL_B = "http://example.com/b.gpx";
    private static final String PATH_B = "/loaded/b.gpx";

    private final DistanceAndTimeAggregator aggregator = new DistanceAndTimeAggregator();
    private final RouteDistanceAndTimeCache cache = new RouteDistanceAndTimeCache();
    private final AtomicReference<String> currentPath = new AtomicReference<>();
    private final List<String> updatedUrls = new ArrayList<>();
    private OpenedRouteDistanceAndTimeUpdater updater;

    @Before
    public void setUp() {
        updater = new OpenedRouteDistanceAndTimeUpdater(aggregator, cache, currentPath::get, updatedUrls::add);
    }

    private void fireAggregatorEvent(double distance, long timeInMillis) {
        aggregator.clearDistancesAndTimes();
        aggregator.updateDistancesAndTimes(Map.of(1, new DistanceAndTime(distance, timeInMillis)));
    }

    @Test
    public void settledTotalIsCachedForOpenedUrl() {
        updater.routeOpened(URL_A, PATH_A);
        currentPath.set(PATH_A);

        fireAggregatorEvent(1000.0, 60000L);

        DistanceAndTime cached = cache.getDistanceAndTime(URL_A);
        assertEquals(new DistanceAndTime(1000.0, 60000L), cached);
        assertEquals(List.of(URL_A), updatedUrls.subList(updatedUrls.size() - 1, updatedUrls.size()));
    }

    @Test
    public void eventsWithoutOpenedRouteAreIgnored() {
        currentPath.set(PATH_A);

        fireAggregatorEvent(1000.0, 60000L);

        assertNull(cache.getDistanceAndTime(URL_A));
        assertEquals(0, updatedUrls.size());
    }

    @Test
    public void lateEventsFromPreviousRouteAreDroppedOnFastSwitch() {
        // route A is opened and loads
        updater.routeOpened(URL_A, PATH_A);
        currentPath.set(PATH_A);
        fireAggregatorEvent(1000.0, 60000L);
        assertEquals(new DistanceAndTime(1000.0, 60000L), cache.getDistanceAndTime(URL_A));

        // route B is opened but has not finished loading: the Convert panel still shows A
        updater.routeOpened(URL_B, PATH_B);
        fireAggregatorEvent(2000.0, 120000L);

        // A's late totals must neither be attributed to B nor clobber A's settled value
        assertNull(cache.getDistanceAndTime(URL_B));
        assertEquals(new DistanceAndTime(1000.0, 60000L), cache.getDistanceAndTime(URL_A));

        // once B is loaded, its totals are cached under B
        currentPath.set(PATH_B);
        fireAggregatorEvent(3000.0, 180000L);
        assertEquals(new DistanceAndTime(3000.0, 180000L), cache.getDistanceAndTime(URL_B));
        assertEquals(new DistanceAndTime(1000.0, 60000L), cache.getDistanceAndTime(URL_A));
    }

    @Test
    public void reopeningSameUrlOverwritesWithLatestTotal() {
        updater.routeOpened(URL_A, PATH_A);
        currentPath.set(PATH_A);
        fireAggregatorEvent(1000.0, 60000L);

        fireAggregatorEvent(1500.0, 90000L);
        assertEquals(new DistanceAndTime(1500.0, 90000L), cache.getDistanceAndTime(URL_A));
    }
}

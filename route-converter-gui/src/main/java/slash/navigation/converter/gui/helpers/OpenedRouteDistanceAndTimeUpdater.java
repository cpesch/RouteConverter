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

import slash.navigation.common.DistanceAndTime;
import slash.navigation.common.DistanceAndTimeAggregator;
import slash.navigation.converter.gui.models.RouteDistanceAndTimeCache;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fills the {@link RouteDistanceAndTimeCache} with the total {@link DistanceAndTime}
 * of the {@link DistanceAndTimeAggregator} once a route opened from the browse panel
 * settles - event-driven, never synchronous at selection time.
 *
 * Guards against a second open racing the first: the aggregator total is written to
 * the cache only while the path captured at open time is still the currently loaded
 * path, i.e. late events from a previously opened route are dropped.
 *
 * @author Christian Pesch
 */

public class OpenedRouteDistanceAndTimeUpdater {
    private final DistanceAndTimeAggregator aggregator;
    private final RouteDistanceAndTimeCache cache;
    private final Supplier<String> currentPathSupplier;
    private final Consumer<String> urlUpdated;
    private volatile OpenedRoute openedRoute;

    public OpenedRouteDistanceAndTimeUpdater(DistanceAndTimeAggregator aggregator, RouteDistanceAndTimeCache cache,
                                             Supplier<String> currentPathSupplier, Consumer<String> urlUpdated) {
        this.aggregator = aggregator;
        this.cache = cache;
        this.currentPathSupplier = currentPathSupplier;
        this.urlUpdated = urlUpdated;
        aggregator.addDistancesAndTimesAggregatorListener((firstIndex, lastIndex) -> distancesAndTimesChanged());
    }

    /**
     * Captures the route that has just been opened from the browse panel.
     *
     * @param url  the URL of the route, used as the cache key
     * @param path the path under which the Convert panel reports the URL once it is loaded
     */
    public void routeOpened(String url, String path) {
        this.openedRoute = new OpenedRoute(url, path);
    }

    void distancesAndTimesChanged() {
        OpenedRoute opened = openedRoute;
        if (opened == null)
            return;
        // guard against correlation races: write only if the opened route is still the loaded one
        if (!opened.path().equals(currentPathSupplier.get()))
            return;

        DistanceAndTime total = aggregator.getTotalDistanceAndTime();
        cache.put(opened.url(), total);
        urlUpdated.accept(opened.url());
    }

    private record OpenedRoute(String url, String path) {
    }
}

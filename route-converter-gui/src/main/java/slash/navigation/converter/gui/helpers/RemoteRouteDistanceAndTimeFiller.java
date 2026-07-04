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
import slash.navigation.converter.gui.models.RouteDistanceAndTimeCache;
import slash.navigation.routes.remote.RemoteRoute;

import java.io.IOException;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Fills a {@link RouteDistanceAndTimeCache} for routes of the remote catalog from the
 * server metadata attributes of the catalog XML (specs/00055): length in meters and
 * duration in seconds arrive with the category listing, so rows show values immediately -
 * no download, no parsing, no server round trip (specs/00012 P3).
 *
 * Routes without metadata (old server, broken or pending analysis, remoteUrl-only) are
 * skipped and keep rendering as no value.
 *
 * @author Christian Pesch
 */

public class RemoteRouteDistanceAndTimeFiller {
    private static final Logger log = Logger.getLogger(RemoteRouteDistanceAndTimeFiller.class.getName());

    private final RouteDistanceAndTimeCache cache;

    public RemoteRouteDistanceAndTimeFiller(RouteDistanceAndTimeCache cache) {
        this.cache = cache;
    }

    public void fill(RemoteRoute route) {
        Long length = route.getLength();
        Long duration = route.getDuration();
        if (length == null && duration == null)
            return;

        String url;
        try {
            // no server round trip: metadata is only present for routes from a category
            // listing, where the URL is a field of the route
            url = route.getUrl();
        } catch (IOException e) {
            log.warning(String.format("Cannot get URL of %s: %s", route, e));
            return;
        }
        if (url == null)
            return;

        cache.put(url, new DistanceAndTime(length != null ? length.doubleValue() : null,
                duration != null ? SECONDS.toMillis(duration) : null));
    }
}

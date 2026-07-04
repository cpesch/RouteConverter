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

import org.junit.Test;
import slash.navigation.common.DistanceAndTime;
import slash.navigation.converter.gui.models.CompositeRouteMetadataSource;
import slash.navigation.converter.gui.models.RouteDistanceAndTimeCache;
import slash.navigation.routes.remote.RemoteCategory;
import slash.navigation.routes.remote.RemoteRoute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link RemoteRouteDistanceAndTimeFiller} and its priority relative
 * to the session cache in the {@link CompositeRouteMetadataSource} (specs/00012 P3).
 *
 * @author Christian Pesch
 */
public class RemoteRouteDistanceAndTimeFillerTest {
    private static final String HREF = "https://api.routeconverter.com/v1/routes/1/";
    private static final String URL = "https://static.routeconverter.com/routes/route.gpx";

    private final RouteDistanceAndTimeCache serverCache = new RouteDistanceAndTimeCache();
    private final RemoteRouteDistanceAndTimeFiller filler = new RemoteRouteDistanceAndTimeFiller(serverCache);

    private static RemoteRoute route(Long length, Long duration) {
        RemoteCategory category = new RemoteCategory(null, "https://api.routeconverter.com/v1/categories/1/");
        return new RemoteRoute(category, HREF, "description", "creator", URL, length, duration);
    }

    @Test
    public void fillsCacheFromServerMetadataInstantly() {
        filler.fill(route(12345L, 3600L));

        // length in meters, duration converted from seconds to milliseconds
        assertEquals(new DistanceAndTime(12345.0, 3600000L), serverCache.getDistanceAndTime(URL));
    }

    @Test
    public void absentServerMetadataFillsNothing() {
        filler.fill(route(null, null));

        assertNull(serverCache.getDistanceAndTime(URL));
    }

    @Test
    public void partialServerMetadataKeepsAbsentValueNull() {
        filler.fill(route(12345L, null));
        assertEquals(new DistanceAndTime(12345.0, null), serverCache.getDistanceAndTime(URL));

        filler.fill(route(null, 3600L));
        assertEquals(new DistanceAndTime(null, 3600000L), serverCache.getDistanceAndTime(URL));
    }

    @Test
    public void sessionCacheOverwritesServerMetadataWithoutFlickeringBack() {
        RouteDistanceAndTimeCache sessionCache = new RouteDistanceAndTimeCache();
        CompositeRouteMetadataSource composite = new CompositeRouteMetadataSource(sessionCache, serverCache);

        // server metadata from the category listing shows before a route is opened
        filler.fill(route(12345L, 3600L));
        assertEquals(new DistanceAndTime(12345.0, 3600000L), composite.getDistanceAndTime(URL));

        // opening the route fills the session cache which overwrites the server value
        DistanceAndTime routed = new DistanceAndTime(13000.0, 3700000L);
        sessionCache.put(URL, routed);
        assertEquals(routed, composite.getDistanceAndTime(URL));

        // re-listing the category re-fills the server cache but never flickers back
        filler.fill(route(12345L, 3600L));
        assertEquals(routed, composite.getDistanceAndTime(URL));
    }
}

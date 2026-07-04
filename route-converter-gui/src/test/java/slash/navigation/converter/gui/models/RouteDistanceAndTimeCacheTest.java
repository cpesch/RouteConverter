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
package slash.navigation.converter.gui.models;

import org.junit.Test;
import slash.navigation.common.DistanceAndTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link RouteDistanceAndTimeCache} and {@link CompositeRouteMetadataSource}.
 *
 * @author Christian Pesch
 */
public class RouteDistanceAndTimeCacheTest {
    private static final String URL = "http://example.com/route.gpx";

    @Test
    public void writeThenRead() {
        RouteDistanceAndTimeCache cache = new RouteDistanceAndTimeCache();
        DistanceAndTime distanceAndTime = new DistanceAndTime(1000.0, 60000L);

        assertNull(cache.getDistanceAndTime(URL));
        cache.put(URL, distanceAndTime);
        assertEquals(distanceAndTime, cache.getDistanceAndTime(URL));
    }

    @Test
    public void overwriteReplacesValue() {
        RouteDistanceAndTimeCache cache = new RouteDistanceAndTimeCache();
        cache.put(URL, new DistanceAndTime(1000.0, 60000L));

        DistanceAndTime routed = new DistanceAndTime(1234.0, 61000L);
        cache.put(URL, routed);
        assertEquals(routed, cache.getDistanceAndTime(URL));
    }

    @Test
    public void missAndNullUrlReturnNull() {
        RouteDistanceAndTimeCache cache = new RouteDistanceAndTimeCache();
        assertNull(cache.getDistanceAndTime("http://example.com/other.gpx"));
        assertNull(cache.getDistanceAndTime(null));
    }

    @Test
    public void compositeQueriesSourcesInOrder() {
        RouteDistanceAndTimeCache sessionCache = new RouteDistanceAndTimeCache();
        RouteDistanceAndTimeCache serverMetadata = new RouteDistanceAndTimeCache();
        CompositeRouteMetadataSource composite = new CompositeRouteMetadataSource(sessionCache, serverMetadata);

        assertNull(composite.getDistanceAndTime(URL));

        DistanceAndTime server = new DistanceAndTime(900.0, null);
        serverMetadata.put(URL, server);
        assertEquals(server, composite.getDistanceAndTime(URL));

        // the session cache overwrites server values with the client's routed ones
        DistanceAndTime routed = new DistanceAndTime(1000.0, 60000L);
        sessionCache.put(URL, routed);
        assertEquals(routed, composite.getDistanceAndTime(URL));
    }
}

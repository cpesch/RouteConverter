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

package slash.navigation.routes.remote;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the server metadata attributes of {@link RemoteRoute} (specs/00012 P3).
 *
 * @author Christian Pesch
 */
public class RemoteRouteTest {
    private static final String HREF = "https://api.routeconverter.com/v1/routes/1/";
    private static final String URL = "https://static.routeconverter.com/routes/route.gpx";

    private static RemoteCategory category() {
        return new RemoteCategory(null, "https://api.routeconverter.com/v1/categories/1/");
    }

    @Test
    public void fromCategoryConstructorCapturesServerMetadata() throws IOException {
        RemoteRoute route = new RemoteRoute(category(), HREF, "description", "creator", URL, 12345L, 3600L);

        assertEquals(URL, route.getUrl());
        assertEquals(Long.valueOf(12345L), route.getLength());
        assertEquals(Long.valueOf(3600L), route.getDuration());
    }

    @Test
    public void absentServerMetadataIsNull() {
        RemoteRoute route = new RemoteRoute(category(), HREF, "description", "creator", URL);

        assertNull(route.getLength());
        assertNull(route.getDuration());
    }

    @Test
    public void routeNotFromCategoryHasNoServerMetadata() {
        RemoteRoute route = new RemoteRoute(category(), HREF);

        assertNull(route.getLength());
        assertNull(route.getDuration());
    }
}

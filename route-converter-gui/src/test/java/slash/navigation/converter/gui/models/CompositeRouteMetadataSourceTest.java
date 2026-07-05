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
 * Unit tests for {@link CompositeRouteMetadataSource}: the first source that knows the
 * route wins, the query order is respected and all-unknown resolves to {@code null}.
 *
 * @author Christian Pesch
 */
public class CompositeRouteMetadataSourceTest {
    private static final DistanceAndTime FIRST = new DistanceAndTime(1000.0, 60_000L);
    private static final DistanceAndTime SECOND = new DistanceAndTime(2000.0, 120_000L);

    @Test
    public void firstNonNullWins() {
        RouteMetadataSource composite = new CompositeRouteMetadataSource(url -> FIRST, url -> SECOND);
        assertEquals(FIRST, composite.getDistanceAndTime("url"));
    }

    @Test
    public void queryOrderIsRespectedWhenFirstIsUnknown() {
        RouteMetadataSource composite = new CompositeRouteMetadataSource(url -> null, url -> SECOND);
        assertEquals(SECOND, composite.getDistanceAndTime("url"));
    }

    @Test
    public void allNullResolvesToNull() {
        RouteMetadataSource composite = new CompositeRouteMetadataSource(url -> null, url -> null);
        assertNull(composite.getDistanceAndTime("url"));
    }

    @Test
    public void emptyCompositeResolvesToNull() {
        RouteMetadataSource composite = new CompositeRouteMetadataSource();
        assertNull(composite.getDistanceAndTime("url"));
    }
}

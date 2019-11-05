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
package slash.navigation.mapview.mapsforge;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.SimpleNavigationPosition;

import static org.junit.Assert.assertEquals;
import static slash.navigation.maps.mapsforge.helpers.MapUtil.toBoundingBox;

public class MapsforgeMapViewTest {
    private MapsforgeMapView mapView = new MapsforgeMapView();

    @Test
    public void testBoundingBox() {
        BoundingBox from = new BoundingBox(new SimpleNavigationPosition(10.18587, 53.49249), new SimpleNavigationPosition(10.06767, 53.40451));
        assertEquals(from, from);
        org.mapsforge.core.model.BoundingBox to = mapView.asBoundingBox(from);
        BoundingBox roundtrip = toBoundingBox(to);
        assertEquals(roundtrip, from);
    }
}

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

package slash.navigation.maps.mapsforge;

import org.junit.Test;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.maps.mapsforge.MapsforgeMapManager.isMissingOnlineMap;

public class MapsforgeMapManagerTest {
    private static final String DEFAULT_URL = "https://openstreetmap.org/tiles";

    @Test
    public void testNullStoredIsNotMissing() {
        assertFalse(isMissingOnlineMap(null, Collections.emptyList(), DEFAULT_URL));
    }

    @Test
    public void testStoredEqualsDefaultIsNotMissing() {
        assertFalse(isMissingOnlineMap(DEFAULT_URL, Collections.emptyList(), DEFAULT_URL));
    }

    @Test
    public void testStoredFileUrlIsNotMissing() {
        assertFalse(isMissingOnlineMap("file:/x.map", Collections.emptyList(), DEFAULT_URL));
    }

    @Test
    public void testStoredAvailableIsNotMissing() {
        assertFalse(isMissingOnlineMap("https://a/tiles", asList("https://a/tiles"), DEFAULT_URL));
    }

    @Test
    public void testStoredNotAvailableIsMissing() {
        assertTrue(isMissingOnlineMap("https://gone/tiles", asList("https://a/tiles"), DEFAULT_URL));
    }
}

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
package slash.navigation.maps.mapsforge.models;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static slash.navigation.maps.mapsforge.models.TileServerMapSource.appendApiKey;

public class TileServerMapSourceTest {

    @Test
    public void testNullApiKeyLeavesUrlUnchanged() {
        assertEquals("https://example.com/tile", appendApiKey("https://example.com/tile", null, false));
    }

    @Test
    public void testNonMapboxWithoutQueryAppendsApiKey() {
        assertEquals("https://example.com/tile?apikey=key", appendApiKey("https://example.com/tile", "key", false));
    }

    @Test
    public void testNonMapboxWithExistingQueryAppendsApiKey() {
        assertEquals("https://example.com/tile?a=b&apikey=key", appendApiKey("https://example.com/tile?a=b", "key", false));
    }

    @Test
    public void testMapboxWithoutQueryAppendsAccessToken() {
        assertEquals("https://example.com/tile?access_token=key", appendApiKey("https://example.com/tile", "key", true));
    }

    @Test
    public void testMapboxWithExistingQueryAppendsAccessToken() {
        assertEquals("https://example.com/tile?a=b&access_token=key", appendApiKey("https://example.com/tile?a=b", "key", true));
    }
}

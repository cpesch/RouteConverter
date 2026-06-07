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

import static org.junit.Assert.*;

public class MapTypeTest {

    @Test
    public void testExactlyThreeValues() {
        assertEquals(3, MapType.values().length);
    }

    @Test
    public void testDownloadIsDownload() {
        assertTrue(MapType.Download.isDownload());
    }

    @Test
    public void testDownloadIsNotThemed() {
        assertFalse(MapType.Download.isThemed());
    }

    @Test
    public void testMapsforgeIsNotDownload() {
        assertFalse(MapType.Mapsforge.isDownload());
    }

    @Test
    public void testMapsforgeIsThemed() {
        assertTrue(MapType.Mapsforge.isThemed());
    }

    @Test
    public void testMBTilesIsNotDownload() {
        assertFalse(MapType.MBTiles.isDownload());
    }

    @Test
    public void testMBTilesIsNotThemed() {
        assertFalse(MapType.MBTiles.isThemed());
    }

    @Test
    public void testValueOfRoundTrip() {
        assertEquals(MapType.Download, MapType.valueOf("Download"));
        assertEquals(MapType.Mapsforge, MapType.valueOf("Mapsforge"));
        assertEquals(MapType.MBTiles, MapType.valueOf("MBTiles"));
    }

    @Test
    public void testOrdinals() {
        assertEquals(0, MapType.Download.ordinal());
        assertEquals(1, MapType.Mapsforge.ordinal());
        assertEquals(2, MapType.MBTiles.ordinal());
    }
}


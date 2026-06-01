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
package slash.navigation.maps.tileserver.helpers;

import org.junit.Test;
import slash.navigation.maps.tileserver.bindingmap.CatalogType;
import slash.navigation.maps.tileserver.bindingmap.MapServerType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapServerUtilTest {
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog xmlns="http://api.routeconverter.com/v1/schemas/mapserver-catalog">
                <mapServer id="osm" name="OpenStreetMap" minZoom="1" maxZoom="18" active="true">
                    <host>a.tile.openstreetmap.org</host>
                    <host>b.tile.openstreetmap.org</host>
                    <urlPattern>/tiles/{z}/{x}/{y}.png</urlPattern>
                    <copyright>osm</copyright>
                </mapServer>
            </catalog>
            """;

    @Test
    public void testUnmarshalMapCatalog() throws Exception {
        CatalogType catalogType = MapServerUtil.unmarshal(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, catalogType.getMapServer().size());
        MapServerType mapServerType = catalogType.getMapServer().get(0);
        assertEquals("osm", mapServerType.getId());
        assertEquals("OpenStreetMap", mapServerType.getName());
        assertEquals(2, mapServerType.getHost().size());
        assertTrue(mapServerType.getHost().contains("a.tile.openstreetmap.org"));
        assertEquals("/tiles/{z}/{x}/{y}.png", mapServerType.getUrlPattern());
        assertEquals("osm", mapServerType.getCopyright());
        assertEquals(1, mapServerType.getMinZoom().intValue());
        assertEquals(18, mapServerType.getMaxZoom().intValue());
        assertEquals(Boolean.TRUE, mapServerType.isActive());
    }
}


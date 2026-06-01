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

package slash.navigation.routes.remote.helpers;

import org.junit.Test;
import slash.navigation.routes.remote.binding.CatalogType;
import slash.navigation.routes.remote.binding.CategoryType;
import slash.navigation.routes.remote.binding.FileType;
import slash.navigation.routes.remote.binding.RouteType;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RoutesUtilTest {
    private static final String NAMESPACE = "http://api.routeconverter.com/v1/schemas/route-catalog";
    private static final String ROOT_CATEGORY_HREF = "https://api.routeconverter.com/v1/categories/1/";
    private static final String SUB_CATEGORY_HREF = "https://api.routeconverter.com/v1/categories/2/";
    private static final String TOP_LEVEL_ROUTE_HREF = "https://api.routeconverter.com/v1/routes/2/";
    private static final String FILE_HREF = "https://api.routeconverter.com/v1/files/1/";

    private static final String SAMPLE_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog xmlns="http://api.routeconverter.com/v1/schemas/route-catalog">
                <category parent="" name="Root" href="https://api.routeconverter.com/v1/categories/1/">
                    <category parent="https://api.routeconverter.com/v1/categories/1/" name="Subcategory" href="https://api.routeconverter.com/v1/categories/2/"/>
                    <route category="https://api.routeconverter.com/v1/categories/1/" description="Overview Route" creator="tester" url="https://static.routeconverter.com/routes/overview.gpx" href="https://api.routeconverter.com/v1/routes/1/"/>
                </category>
                <route category="https://api.routeconverter.com/v1/categories/2/" description="Detailed Route" creator="tester" url="https://static.routeconverter.com/routes/detailed.gpx" href="https://api.routeconverter.com/v1/routes/2/"/>
                <file name="sample.gpx" creator="tester" url="https://static.routeconverter.com/files/sample.gpx" href="https://api.routeconverter.com/v1/files/1/"/>
            </catalog>
            """;

    @Test
    public void testMarshalAndUnmarshalRoundTrip() throws Exception {
        CatalogType catalogType = RoutesUtil.unmarshal(SAMPLE_XML);
        assertCatalog(catalogType);

        StringWriter writer = new StringWriter();
        RoutesUtil.marshal(catalogType, writer);
        String marshalled = writer.toString();

        assertTrue(marshalled.contains("<catalog"));
        assertTrue(marshalled.contains(NAMESPACE));
        assertTrue(marshalled.contains("description=\"Detailed Route\""));
        assertTrue(marshalled.contains("href=\"" + FILE_HREF + "\""));

        CatalogType roundTripped = RoutesUtil.unmarshal(marshalled);
        assertCatalog(roundTripped);
    }

    private void assertCatalog(CatalogType catalogType) {
        CategoryType rootCategory = catalogType.getCategory();
        assertEquals("Root", rootCategory.getName());
        assertEquals("", rootCategory.getParent());
        assertEquals(ROOT_CATEGORY_HREF, rootCategory.getHref());
        assertEquals(1, rootCategory.getCategory().size());
        assertEquals(1, rootCategory.getRoute().size());

        CategoryType subCategory = rootCategory.getCategory().get(0);
        assertEquals("Subcategory", subCategory.getName());
        assertEquals(ROOT_CATEGORY_HREF, subCategory.getParent());
        assertEquals(SUB_CATEGORY_HREF, subCategory.getHref());

        RouteType nestedRoute = rootCategory.getRoute().get(0);
        assertEquals(ROOT_CATEGORY_HREF, nestedRoute.getCategory());
        assertEquals("Overview Route", nestedRoute.getDescription());
        assertEquals("tester", nestedRoute.getCreator());
        assertEquals("https://static.routeconverter.com/routes/overview.gpx", nestedRoute.getUrl());
        assertEquals("https://api.routeconverter.com/v1/routes/1/", nestedRoute.getHref());

        RouteType topLevelRoute = catalogType.getRoute();
        assertEquals(SUB_CATEGORY_HREF, topLevelRoute.getCategory());
        assertEquals("Detailed Route", topLevelRoute.getDescription());
        assertEquals("tester", topLevelRoute.getCreator());
        assertEquals("https://static.routeconverter.com/routes/detailed.gpx", topLevelRoute.getUrl());
        assertEquals(TOP_LEVEL_ROUTE_HREF, topLevelRoute.getHref());

        FileType fileType = catalogType.getFile();
        assertEquals("sample.gpx", fileType.getName());
        assertEquals("tester", fileType.getCreator());
        assertEquals("https://static.routeconverter.com/files/sample.gpx", fileType.getUrl());
        assertEquals(FILE_HREF, fileType.getHref());
    }
}


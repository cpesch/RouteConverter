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
import slash.navigation.maps.tileserver.bindingoverlay.CatalogType;
import slash.navigation.maps.tileserver.bindingoverlay.OverlayServerType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OverlayServerUtilTest {
    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog xmlns="http://api.routeconverter.com/v1/schemas/overlayserver-catalog">
                <overlayServer id="hiking" name="Hiking Overlay" minZoom="3" maxZoom="17" active="false">
                    <host>overlay.example.com</host>
                    <urlPattern>/overlay/{z}/{x}/{y}.png</urlPattern>
                    <copyright>overlay</copyright>
                </overlayServer>
            </catalog>
            """;

    private static final String XML_WITH_ATTRIBUTION = """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog xmlns="http://api.routeconverter.com/v1/schemas/overlayserver-catalog">
                <overlayServer id="hiking" name="Hiking Overlay" minZoom="3" maxZoom="17" active="false">
                    <host>overlay.example.com</host>
                    <urlPattern>/overlay/{z}/{x}/{y}.png</urlPattern>
                    <copyright>overlay</copyright>
                    <attribution>Map data &amp;copy; &lt;a href="https://example.org/"&gt;Example&lt;/a&gt;</attribution>
                </overlayServer>
            </catalog>
            """;

    private static final String XML_WITH_UNKNOWN_ELEMENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog xmlns="http://api.routeconverter.com/v1/schemas/overlayserver-catalog">
                <overlayServer id="hiking" name="Hiking Overlay" minZoom="3" maxZoom="17" active="false">
                    <host>overlay.example.com</host>
                    <urlPattern>/overlay/{z}/{x}/{y}.png</urlPattern>
                    <copyright>overlay</copyright>
                    <futureElement>x</futureElement>
                </overlayServer>
            </catalog>
            """;

    @Test
    public void testUnmarshalOverlayCatalog() throws Exception {
        CatalogType catalogType = OverlayServerUtil.unmarshal(new ByteArrayInputStream(XML.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, catalogType.getOverlayServer().size());
        OverlayServerType overlayServerType = catalogType.getOverlayServer().get(0);
        assertEquals("hiking", overlayServerType.getId());
        assertEquals("Hiking Overlay", overlayServerType.getName());
        assertEquals(1, overlayServerType.getHost().size());
        assertTrue(overlayServerType.getHost().contains("overlay.example.com"));
        assertEquals("/overlay/{z}/{x}/{y}.png", overlayServerType.getUrlPattern());
        assertEquals("overlay", overlayServerType.getCopyright());
        assertEquals(3, overlayServerType.getMinZoom().intValue());
        assertEquals(17, overlayServerType.getMaxZoom().intValue());
        assertEquals(Boolean.FALSE, overlayServerType.isActive());
        assertNull(overlayServerType.getAttribution());
    }

    @Test
    public void testUnmarshalAttribution() throws Exception {
        CatalogType catalogType = OverlayServerUtil.unmarshal(new ByteArrayInputStream(XML_WITH_ATTRIBUTION.getBytes(StandardCharsets.UTF_8)));

        OverlayServerType overlayServerType = catalogType.getOverlayServer().get(0);
        assertEquals("Map data &copy; <a href=\"https://example.org/\">Example</a>", overlayServerType.getAttribution());
    }

    @Test
    public void testUnmarshalIgnoresUnknownElement() throws Exception {
        CatalogType catalogType = OverlayServerUtil.unmarshal(new ByteArrayInputStream(XML_WITH_UNKNOWN_ELEMENT.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, catalogType.getOverlayServer().size());
        OverlayServerType overlayServerType = catalogType.getOverlayServer().get(0);
        assertEquals("hiking", overlayServerType.getId());
        assertEquals("Hiking Overlay", overlayServerType.getName());
        assertEquals("/overlay/{z}/{x}/{y}.png", overlayServerType.getUrlPattern());
        assertEquals("overlay", overlayServerType.getCopyright());
        assertNull(overlayServerType.getAttribution());
    }
}


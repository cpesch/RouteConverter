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

package slash.navigation.nmn;

import org.junit.Test;
import slash.navigation.nmn.bindingcruiser.Root;
import slash.navigation.nmn.bindingcruiser.Route;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link NavigonCruiserUtil} JSON unmarshal/marshal round-trips.
 */
public class NavigonCruiserUtilTest {

    private static final String CRUISER_JSON =
            "{\"route\":{\"v\":1,\"name\":\"Munich\",\"creator\":\"RouteConverter\"," +
            "\"coords\":[\"48.137430,11.574950\",\"48.150000,11.600000\"]," +
            "\"settings\":{\"VT\":1,\"BE\":0,\"FR\":0,\"ROUND\":0,\"RT\":3,\"SR\":0,\"HOV\":0,\"HW\":0,\"TR\":0,\"CU\":1}}}";

    // --- unmarshal ---

    @Test
    public void testUnmarshalReturnsNonNull() throws IOException {
        Root root = NavigonCruiserUtil.unmarshal(
                new ByteArrayInputStream(CRUISER_JSON.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(root);
    }

    @Test
    public void testUnmarshalRouteIsPresent() throws IOException {
        Root root = NavigonCruiserUtil.unmarshal(
                new ByteArrayInputStream(CRUISER_JSON.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(root.getRoute());
    }

    @Test
    public void testUnmarshalRouteName() throws IOException {
        Root root = NavigonCruiserUtil.unmarshal(
                new ByteArrayInputStream(CRUISER_JSON.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Munich", root.getRoute().getName());
    }

    @Test
    public void testUnmarshalCoordCount() throws IOException {
        Root root = NavigonCruiserUtil.unmarshal(
                new ByteArrayInputStream(CRUISER_JSON.getBytes(StandardCharsets.UTF_8)));
        assertEquals(2, root.getRoute().getCoords().size());
    }

    @Test
    public void testUnmarshalCreator() throws IOException {
        Root root = NavigonCruiserUtil.unmarshal(
                new ByteArrayInputStream(CRUISER_JSON.getBytes(StandardCharsets.UTF_8)));
        assertEquals("RouteConverter", root.getRoute().getCreator());
    }

    // --- marshal ---

    @Test
    public void testMarshalProducesNonEmptyOutput() throws Exception {
        Root root = buildMinimalRoot("Test");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NavigonCruiserUtil.marshal(root, out);
        String json = out.toString(StandardCharsets.UTF_8);
        assertNotNull(json);
        assert json.contains("Test") : "Output should contain route name";
    }

    // --- round-trip ---

    @Test
    public void testRoundTrip() throws Exception {
        Root original = buildMinimalRoot("Roundtrip Route");
        original.getRoute().setCoords(Arrays.asList("48.137430,11.574950", "47.999,12.000"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NavigonCruiserUtil.marshal(original, out);

        Root roundtripped = NavigonCruiserUtil.unmarshal(
                new ByteArrayInputStream(out.toByteArray()));
        assertEquals("Roundtrip Route", roundtripped.getRoute().getName());
        assertEquals(2, roundtripped.getRoute().getCoords().size());
    }

    private static Root buildMinimalRoot(String name) {
        Route route = new Route();
        route.setName(name);
        return new Root(route);
    }
}


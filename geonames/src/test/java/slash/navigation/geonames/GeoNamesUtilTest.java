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
package slash.navigation.geonames;

import org.junit.Test;
import slash.navigation.geonames.binding.Geonames;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GeoNamesUtilTest {
    private static final Path EXAMPLES = Path.of("src/main/examples");

    @Test
    public void testUnmarshalNearbyPlaceNameExample() throws Exception {
        Geonames geonames = GeoNamesUtil.unmarshal(readExample("findNearbyPlaceName.xml"));

        assertNotNull(geonames);
        assertEquals(10, geonames.getGeoname().size());
        assertEquals("Gräsalp", geonames.getGeoname().get(0).getName());
        assertEquals("Austria", geonames.getGeoname().get(0).getCountryName());
        assertNull(geonames.getStatus());
    }

    @Test
    public void testUnmarshalOverloadExample() throws Exception {
        Geonames geonames = GeoNamesUtil.unmarshal(readExample("overload.xml"));

        assertNotNull(geonames);
        assertNotNull(geonames.getStatus());
        assertEquals("the free servers are currently overloaded with requests.", geonames.getStatus().getMessage());
        assertEquals(22, geonames.getStatus().getValue());
        assertEquals(0, geonames.getGeoname().size());
        assertEquals(0, geonames.getCode().size());
    }

    private String readExample(String fileName) throws IOException {
        return Files.readString(EXAMPLES.resolve(fileName));
    }
}


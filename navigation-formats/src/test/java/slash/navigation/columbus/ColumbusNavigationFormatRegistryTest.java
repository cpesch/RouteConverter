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
package slash.navigation.columbus;

import org.junit.Test;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormatRegistry;
import slash.navigation.gpx.Gpx11Format;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ColumbusNavigationFormatRegistryTest {
    private NavigationFormatRegistry registry = new ColumbusNavigationFormatRegistry();

    @Test
    public void testNotExistingExtension() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".ov2");
        assertEquals(Gpx11Format.class, formats.get(0).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionGpx() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".gpx");
        assertEquals(Gpx11Format.class, formats.get(0).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionCsv() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".csv");
        assertEquals(ColumbusGpsProfessionalFormat.class, formats.get(0).getClass());
        assertEquals(ColumbusGpsStandardFormat.class, formats.get(1).getClass());
        assertEquals(ColumbusGpsType2Format.class, formats.get(2).getClass());
        assertEquals(GarbleColumbusGpsProfessionalFormat.class, formats.get(3).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionGps() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".gps");
        assertEquals(ColumbusGpsBinaryFormat.class, formats.get(0).getClass());
    }
}

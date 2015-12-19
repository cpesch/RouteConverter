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
package slash.navigation.base;

import org.junit.Test;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.simple.BrokenColumbusV900StandardFormat;
import slash.navigation.simple.ColumbusV900ProfessionalFormat;
import slash.navigation.simple.ColumbusV900StandardFormat;

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
        assertEquals(ColumbusV900ProfessionalFormat.class, formats.get(0).getClass());
        assertEquals(ColumbusV900StandardFormat.class, formats.get(1).getClass());
        assertEquals(BrokenColumbusV900StandardFormat.class, formats.get(2).getClass());
    }
}

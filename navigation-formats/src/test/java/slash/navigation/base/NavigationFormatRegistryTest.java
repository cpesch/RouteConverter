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
import slash.navigation.babel.TomTomPoiFormat;
import slash.navigation.columbus.ColumbusGpsType1Format;
import slash.navigation.columbus.ColumbusGpsType2Format;
import slash.navigation.columbus.GarbleColumbusGpsType1Format;
import slash.navigation.csv.CsvCommaFormat;
import slash.navigation.csv.CsvSemicolonFormat;
import slash.navigation.nmn.NmnUrlFormat;
import slash.navigation.simple.*;
import slash.navigation.url.GoogleMapsUrlFormat;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class NavigationFormatRegistryTest {
    private final NavigationFormatRegistry registry = new NavigationFormatRegistry();

    @Test
    public void testGetReadFormatsSortedByExtension() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".ov2");
        assertEquals(TomTomPoiFormat.class, formats.get(0).getClass());
        assertEquals(NmnUrlFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionIsCaseSensitive() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".OV2");
        assertEquals(NmnUrlFormat.class, formats.get(0).getClass());
        assertEquals(GoogleMapsUrlFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByNotExistingExtension() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".zzz");
        assertEquals(NmnUrlFormat.class, formats.get(0).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionMultipleResults() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".csv");
        int index = 0;
        assertEquals(HaicomLoggerFormat.class, formats.get(index++).getClass());
        assertEquals(Route66Format.class, formats.get(index++).getClass());
        assertEquals(ColumbusGpsType1Format.class, formats.get(index++).getClass());
        assertEquals(ColumbusGpsType2Format.class, formats.get(index++).getClass());
        assertEquals(QstarzQ1000Format.class, formats.get(index++).getClass());
        assertEquals(Iblue747Format.class, formats.get(index++).getClass());
        assertEquals(CsvCommaFormat.class, formats.get(index++).getClass());
        assertEquals(CsvSemicolonFormat.class, formats.get(index++).getClass());
        assertEquals(GarbleColumbusGpsType1Format.class, formats.get(index++).getClass());
        assertEquals(GarbleHaicomLoggerFormat.class, formats.get(index++).getClass());
        assertEquals(NmnUrlFormat.class, formats.get(index).getClass());
    }
}

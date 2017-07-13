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
import slash.navigation.bcr.MTP0809Format;
import slash.navigation.columbus.ColumbusGpsType1Format;
import slash.navigation.columbus.ColumbusGpsType2Format;
import slash.navigation.columbus.GarbleColumbusGpsType1Format;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.simple.GarbleHaicomLoggerFormat;
import slash.navigation.simple.HaicomLoggerFormat;
import slash.navigation.simple.Iblue747Format;
import slash.navigation.simple.QstarzQ1000Format;
import slash.navigation.simple.Route66Format;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class NavigationFormatRegistryTest {
    private NavigationFormatRegistry registry = new NavigationFormatRegistry();

    @Test
    public void testGetReadFormatsSortedByExtension() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".ov2");
        assertEquals(TomTomPoiFormat.class, formats.get(0).getClass());
        assertEquals(NmeaFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionIsCaseSensitive() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".OV2");
        assertEquals(NmeaFormat.class, formats.get(0).getClass());
        assertEquals(MTP0809Format.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByNotExistingExtension() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".zzz");
        assertEquals(NmeaFormat.class, formats.get(0).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionMultipleResults() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".csv");
        assertEquals(HaicomLoggerFormat.class, formats.get(0).getClass());
        assertEquals(Route66Format.class, formats.get(1).getClass());
        assertEquals(ColumbusGpsType1Format.class, formats.get(2).getClass());
        assertEquals(ColumbusGpsType2Format.class, formats.get(3).getClass());
        assertEquals(QstarzQ1000Format.class, formats.get(4).getClass());
        assertEquals(Iblue747Format.class, formats.get(5).getClass());
        assertEquals(GarbleColumbusGpsType1Format.class, formats.get(6).getClass());
        assertEquals(GarbleHaicomLoggerFormat.class, formats.get(7).getClass());
        assertEquals(NmeaFormat.class, formats.get(8).getClass());
    }
}

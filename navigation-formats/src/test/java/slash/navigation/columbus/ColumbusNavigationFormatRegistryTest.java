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
import slash.navigation.kml.Igo8RouteFormat;
import slash.navigation.kml.Kml22Format;
import slash.navigation.kml.Kmz22Format;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ColumbusNavigationFormatRegistryTest {
    private NavigationFormatRegistry registry = new ColumbusNavigationFormatRegistry();

    @Test
    public void testNotExistingExtension() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".ov2");
        assertEquals(NmeaFormat.class, formats.get(0).getClass());
        assertEquals(Kml22Format.class, formats.get(1).getClass());
        assertEquals(Kmz22Format.class, formats.get(2).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionCsv() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".csv");
        assertEquals(ColumbusGpsType1Format.class, formats.get(0).getClass());
        assertEquals(ColumbusGpsType2Format.class, formats.get(1).getClass());
        assertEquals(GarbleColumbusGpsType1Format.class, formats.get(2).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionGps() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".gps");
        assertEquals(ColumbusGpsBinaryFormat.class, formats.get(0).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionGpx() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".gpx");
        assertEquals(Gpx11Format.class, formats.get(0).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionKml() {
        List<NavigationFormat> formats = registry.getReadFormatsPreferredByExtension(".kml");
        assertEquals(Kml22Format.class, formats.get(0).getClass());
    }

    private boolean containsFormat(List<NavigationFormat> formats, Class clazz) {
        for(NavigationFormat format : formats) {
            if(clazz.isInstance(format))
                return true;
        }
        return false;
    }

    @Test
    public void testNoCompetitorFormats() {
        List<NavigationFormat> formats = registry.getReadFormats();
        assertTrue(containsFormat(formats, Kml22Format.class));
        assertFalse(containsFormat(formats, Igo8RouteFormat.class));
        assertFalse(containsFormat(formats, MagellanExploristFormat.class));
        assertFalse(containsFormat(formats, MagellanRouteFormat.class));
    }
}

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

import slash.navigation.tomtom.TomTomPoiFormat;
import org.junit.jupiter.api.Test;
import slash.navigation.columbus.ColumbusFusionFormat;
import slash.navigation.columbus.ColumbusGpsType1Format;
import slash.navigation.columbus.ColumbusGpsType2Format;
import slash.navigation.columbus.GarbleColumbusGpsType1Format;
import slash.navigation.csv.CsvCommaFormat;
import slash.navigation.csv.CsvSemicolonFormat;
import slash.navigation.csv.Flightradar24Format;
import slash.navigation.geojson.GeoJsonFormat;
import slash.navigation.googletakeout.GoogleCommuteRoutesFormat;
import slash.navigation.googletakeout.GoogleSavedListFormat;
import slash.navigation.googletimeline.GoogleTimelineFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.itn.TomTomRouteFormat;
import slash.navigation.garmin.GarminPoiDbFormat;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.nmn.NavigonCruiserFormat;
import slash.navigation.nmn.NmnUrlFormat;
import slash.navigation.photo.PhotoFormat;
import slash.navigation.simple.*;
import slash.navigation.url.GoogleMapsUrlFormat;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static slash.common.io.Files.getExtension;

public class NavigationFormatRegistryTest {
    private final NavigationFormatRegistry registry = new NavigationFormatRegistry();

    @Test
    public void testGetReadFormatsSortedByExtension() {
        List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsPreferredByExtension(".ov2");
        assertEquals(TomTomPoiFormat.class, formats.get(0).getClass());
        assertEquals(NmnUrlFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionIsCaseSensitive() {
        List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsPreferredByExtension(".OV2");
        assertEquals(NmnUrlFormat.class, formats.get(0).getClass());
        assertEquals(GoogleMapsUrlFormat.class, formats.get(1).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionIsLocaleIndependent() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.of("tr", "TR"));
            List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsPreferredByExtension(getExtension("CURRENT.ITN"));
            NavigationFormat<?> first = formats.get(0);
            assertTrue(first instanceof TomTomRouteFormat);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void testGetReadFormatsSortedByNotExistingExtension() {
        List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsPreferredByExtension(".zzz");
        assertEquals(NmnUrlFormat.class, formats.get(0).getClass());
    }

    @Test
    public void testGetReadFormatsSortedByExtensionMultipleResults() {
        List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsPreferredByExtension(".csv");
        int index = 0;
        assertEquals(HaicomLoggerFormat.class, formats.get(index++).getClass());
        assertEquals(Route66Format.class, formats.get(index++).getClass());
        assertEquals(ColumbusGpsType1Format.class, formats.get(index++).getClass());
        assertEquals(ColumbusGpsType2Format.class, formats.get(index++).getClass());
        assertEquals(ColumbusFusionFormat.class, formats.get(index++).getClass());
        assertEquals(QstarzQ1000Format.class, formats.get(index++).getClass());
        assertEquals(Iblue747Format.class, formats.get(index++).getClass());
        assertEquals(GoogleSavedListFormat.class, formats.get(index++).getClass());
        assertEquals(CsvCommaFormat.class, formats.get(index++).getClass());
        assertEquals(CsvSemicolonFormat.class, formats.get(index++).getClass());
        assertEquals(Flightradar24Format.class, formats.get(index++).getClass());
        assertEquals(GarbleColumbusGpsType1Format.class, formats.get(index++).getClass());
        assertEquals(GarbleHaicomLoggerFormat.class, formats.get(index++).getClass());
        assertEquals(NmnUrlFormat.class, formats.get(index).getClass());
    }

    @Test
    public void testGetReadFormatsPreferredByExtensionPutsTomTomRouteFormatFirst() {
        List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsPreferredByExtension(".itn");
        NavigationFormat<?> first = formats.get(0);
        assertTrue(first instanceof TomTomRouteFormat);
    }

    @Test
    public void testGetReadFormatsIsNonEmpty() {
        assertTrue(!registry.getReadFormats().isEmpty());
    }

    @Test
    public void testGetExtensions() {
        Set<String> extensions = registry.getExtensions();
        assertTrue(extensions.contains(".gpx"));
        assertTrue(extensions.contains(".kml"));
        assertTrue(extensions.contains(".itn"));
        assertTrue(extensions.contains(".sbp")); // Navilink, read-only
        assertTrue(extensions.contains(".zip"));

        for (String extension : extensions) {
            assertTrue(extension.startsWith("."));
            assertEquals(extension.toLowerCase(Locale.ROOT), extension);
        }
    }

    @Test
    public void testGetWriteFormatsIsSubsetOfGetReadFormats() {
        // WebPageFormat and TomTom8RouteFormat are pre-existing, write-only exceptions
        // (isSupportsReading() returns false for both); every other write format is also
        // a read format, which is the invariant this change must not disturb.
        // getReadFormats()/getWriteFormats() each reflectively instantiate fresh objects,
        // so the two lists never share instances for the same format class - compare by
        // class instead of relying on List.contains()'s identity-based equals().
        Set<Class<?>> readFormatClasses = new HashSet<>();
        for (NavigationFormat<BaseRoute<?, ?>> readFormat : registry.getReadFormats())
            readFormatClasses.add(readFormat.getClass());

        for (NavigationFormat<BaseRoute<?, ?>> writeFormat : registry.getWriteFormats()) {
            NavigationFormat<?> anyWriteFormat = writeFormat;
            if (anyWriteFormat instanceof WebPageFormat || anyWriteFormat instanceof TomTom8RouteFormat)
                continue;
            assertTrue(readFormatClasses.contains(writeFormat.getClass()));
        }
    }

    private static final List<String> STRICT_EXTENSIONS = List.of(".csv", ".json", ".gpx", ".kml", ".kmz", ".tcx", ".fit",
            ".jpg", ".jpeg", ".png", ".heic");

    private static Set<Class<?>> classesOf(List<NavigationFormat<BaseRoute<?, ?>>> formats) {
        Set<Class<?>> result = new HashSet<>();
        for (NavigationFormat<?> format : formats)
            result.add(format.getClass());
        return result;
    }

    @Test
    public void testReadFormatsForStrictExtensionHaveNoFallback() {
        for (String extension : STRICT_EXTENSIONS) {
            List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsForExtension(extension);
            assertFalse(formats.isEmpty(), extension);
            assertTrue(formats.size() < registry.getReadFormats().size() / 2, extension);
            assertFalse(classesOf(formats).contains(NmnUrlFormat.class), extension);
        }
    }

    @Test
    public void testReadFormatsForCsvAreTheCsvFormatsInRegistryOrderAndThePoiFormats() {
        List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsForExtension(".csv");
        List<NavigationFormat<BaseRoute<?, ?>>> expected = registry.getReadFormatsPreferredByExtension(".csv");
        assertEquals(16, formats.size());
        assertEquals(expected.subList(0, 13).stream().map(Object::getClass).toList(),
                formats.subList(0, 13).stream().map(Object::getClass).toList());
        for (NavigationFormat<?> format : formats.subList(0, 13))
            assertEquals(".csv", format.getExtension());
        assertEquals(Set.of(NavigatingPoiWarnerFormat.class, OpelNaviFormat.class, GarminPoiDbFormat.class),
                classesOf(formats.subList(13, 16)));
    }

    @Test
    public void testReadFormatsForJsonContainAllFormatsReadingJson() {
        Set<Class<?>> classes = classesOf(registry.getReadFormatsForExtension(".json"));
        assertEquals(Set.of(GoogleTimelineFormat.class, GoogleCommuteRoutesFormat.class, GeoJsonFormat.class,
                NavigonCruiserFormat.class), classes);
    }

    @Test
    public void testReadFormatsForStrictExtensionIsCaseInsensitive() {
        for (String extension : List.of(".CSV", ".Json", ".GPX", ".Kml", ".KMZ", ".TCX", ".FIT", ".JPG", ".JPEG", ".PNG", ".HEIC")) {
            assertEquals(classesOf(registry.getReadFormatsForExtension(extension.toLowerCase(Locale.ROOT))),
                    classesOf(registry.getReadFormatsForExtension(extension)), extension);
            assertTrue(registry.getReadFormatsForExtension(extension).size() < registry.getReadFormats().size() / 2, extension);
        }
    }

    @Test
    public void testReadFormatsForPhotoExtensionsAreThePhotoFormat() {
        for (String extension : List.of(".jpg", ".jpeg", ".png", ".heic")) {
            List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsForExtension(extension);
            assertEquals(1, formats.size(), extension);
            assertEquals(PhotoFormat.class, formats.get(0).getClass(), extension);
        }
    }

    @Test
    public void testReadFormatsForOtherExtensionsAreUnchanged() {
        for (String extension : List.of(".txt", ".log", ".xml", ".nmea", ".ov2", ".OV2", ".zip", ".zzz", "")) {
            List<NavigationFormat<BaseRoute<?, ?>>> formats = registry.getReadFormatsForExtension(extension);
            assertEquals(registry.getReadFormats().size(), formats.size(), extension);
            assertEquals(registry.getReadFormatsPreferredByExtension(extension).stream().map(Object::getClass).toList(),
                    formats.stream().map(Object::getClass).toList(), extension);
        }
    }

    @Test
    public void testReadFormatsForNullExtensionAreAllFormats() {
        assertEquals(registry.getReadFormats().size(), registry.getReadFormatsForExtension(null).size());
    }

    @Test
    public void testReadFormatsForStrictExtensionWithoutFormatsFallBackToAllFormats() {
        NavigationFormatRegistry withoutFit = new NavigationFormatRegistry() {
            protected boolean includeReadFormat(NavigationFormat<?> format) {
                return !".fit".equals(format.getExtension());
            }
        };
        assertEquals(withoutFit.getReadFormats().size(), withoutFit.getReadFormatsForExtension(".fit").size());
    }
}

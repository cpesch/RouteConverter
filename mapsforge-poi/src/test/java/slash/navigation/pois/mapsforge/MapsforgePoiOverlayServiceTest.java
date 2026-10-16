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
package slash.navigation.pois.mapsforge;

import org.junit.Test;
import org.mapsforge.map.reader.MapFile;
import slash.navigation.common.BoundingBox;
import slash.navigation.maps.item.ItemModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;

import java.io.File;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static slash.navigation.maps.mapsforge.MapType.Mapsforge;

public class MapsforgePoiOverlayServiceTest {
    private static final BoundingBox MAP_BOUNDS = new BoundingBox(14.0, 53.0, 13.0, 52.0);
    private static final BoundingBox VIEWPORT = new BoundingBox(13.45, 52.55, 13.35, 52.50);
    private static final List<PoiCategory> CATEGORIES = List.of(new PoiCategory("Amenities", "Amenities", emptyList()));

    private final MapsforgePoiLookup poiLookup = mock(MapsforgePoiLookup.class);
    private final MapsforgeMapLookup mapLookup = mock(MapsforgeMapLookup.class);

    @SuppressWarnings("unchecked")
    private MapsforgeMapManager mapManagerDisplaying(LocalMap localMap) {
        MapsforgeMapManager mapsforgeMapManager = mock(MapsforgeMapManager.class);
        ItemModel<LocalMap> displayedMapModel = mock(ItemModel.class);
        when(mapsforgeMapManager.getDisplayedMapModel()).thenReturn(displayedMapModel);
        when(displayedMapModel.getItem()).thenReturn(localMap);
        return mapsforgeMapManager;
    }

    private MapsforgeFileMap displayedMap(MapFile mapFile, BoundingBox boundingBox) {
        MapsforgeFileMap displayedMap = mock(MapsforgeFileMap.class);
        when(displayedMap.getType()).thenReturn(Mapsforge);
        when(displayedMap.getMapFile()).thenReturn(mapFile);
        when(displayedMap.getBoundingBox()).thenReturn(boundingBox);
        when(displayedMap.description()).thenReturn("berlin.map");
        return displayedMap;
    }

    @Test
    public void prefersPoiDatabaseAndDownloadsOnlyForCatalog() throws Exception {
        File file = new File("berlin.poi");
        when(poiLookup.findPoiFile(VIEWPORT, true)).thenReturn(new MapsforgePoiLookup.PoiFile(file, "Mapsforge POIs"));
        when(poiLookup.findPoiFile(VIEWPORT, false)).thenReturn(new MapsforgePoiLookup.PoiFile(file, "Mapsforge POIs"));
        when(poiLookup.readCategories(file)).thenReturn(CATEGORIES);
        CategoryPoiResult result = new CategoryPoiResult(emptyList(), true);
        when(poiLookup.searchCategories(file, VIEWPORT, Set.of("Amenities"), 10)).thenReturn(result);
        MapsforgePoiOverlayService service = new MapsforgePoiOverlayService(poiLookup, mapManagerDisplaying(null), mapLookup);

        PoiCatalog catalog = service.getCatalog(VIEWPORT, true);

        assertEquals("Mapsforge POIs", catalog.sourceName());
        assertEquals(CATEGORIES, catalog.categories());
        assertSame(result, service.search(VIEWPORT, Set.of("Amenities"), 10));
        verify(poiLookup).findPoiFile(VIEWPORT, false);
        verifyNoInteractions(mapLookup);
    }

    @Test
    public void fallsBackToDisplayedMapFile() throws Exception {
        MapFile mapFile = mock(MapFile.class);
        when(mapLookup.readCategories(mapFile)).thenReturn(CATEGORIES);
        CategoryPoiResult result = new CategoryPoiResult(emptyList(), false);
        when(mapLookup.searchCategories(mapFile, VIEWPORT, Set.of("amenity"), 10)).thenReturn(result);
        MapsforgePoiOverlayService service = new MapsforgePoiOverlayService(poiLookup,
                mapManagerDisplaying(displayedMap(mapFile, MAP_BOUNDS)), mapLookup);

        PoiCatalog catalog = service.getCatalog(VIEWPORT, false);

        assertEquals("berlin.map", catalog.sourceName());
        assertEquals(CATEGORIES, catalog.categories());
        assertSame(result, service.search(VIEWPORT, Set.of("amenity"), 10));
    }

    @Test
    public void findsNoDataOutsideDisplayedMapOrWithoutViewport() throws Exception {
        MapFile mapFile = mock(MapFile.class);
        BoundingBox elsewhere = new BoundingBox(-60.0, -10.0, -61.0, -11.0);
        MapsforgePoiOverlayService service = new MapsforgePoiOverlayService(poiLookup,
                mapManagerDisplaying(displayedMap(mapFile, elsewhere)), mapLookup);

        assertNull(service.getCatalog(VIEWPORT, false));
        assertNull(service.search(VIEWPORT, Set.of("amenity"), 10));
        assertNull(service.getCatalog(null, true));
        // the null viewport never reaches the lookup
        verify(poiLookup, times(2)).findPoiFile(any(BoundingBox.class), anyBoolean());
        verifyNoInteractions(mapLookup);
    }
}

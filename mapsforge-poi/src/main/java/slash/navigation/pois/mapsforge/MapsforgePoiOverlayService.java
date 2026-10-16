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

import org.mapsforge.map.reader.MapFile;
import slash.navigation.common.BoundingBox;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static slash.navigation.maps.mapsforge.MapType.Mapsforge;

/**
 * Finds the POIs of categories for the POI overlay of the map, offline only: from the
 * {@code .poi} database covering the viewport or, without one, from the displayed {@code .map} file.
 *
 * @author Christian Pesch
 */
public class MapsforgePoiOverlayService {
    private static final Logger log = Logger.getLogger(MapsforgePoiOverlayService.class.getName());

    private final MapsforgePoiLookup poiLookup;
    private final MapsforgeMapManager mapsforgeMapManager;
    private final MapsforgeMapLookup mapLookup;

    public MapsforgePoiOverlayService(MapsforgePoiLookup poiLookup, MapsforgeMapManager mapsforgeMapManager) {
        this(poiLookup, mapsforgeMapManager, new MapsforgeMapLookup());
    }

    MapsforgePoiOverlayService(MapsforgePoiLookup poiLookup, MapsforgeMapManager mapsforgeMapManager, MapsforgeMapLookup mapLookup) {
        this.poiLookup = poiLookup;
        this.mapsforgeMapManager = mapsforgeMapManager;
        this.mapLookup = mapLookup;
    }

    /**
     * Returns the categories offered for the viewport or {@code null} if there is no POI data.
     *
     * @param download true to download the {@code .poi} database for the viewport if it is missing,
     *                 which blocks until the download completed
     */
    public PoiCatalog getCatalog(BoundingBox viewport, boolean download) throws IOException {
        PoiSource source = findSource(viewport, download);
        return source != null ? new PoiCatalog(source.name(), source.categories()) : null;
    }

    /**
     * Returns the POIs of the selected category keys within the viewport from local data only,
     * or {@code null} if there is no POI data for the viewport.
     */
    public CategoryPoiResult search(BoundingBox viewport, Set<String> keys, int limitPerGroup) throws IOException {
        PoiSource source = findSource(viewport, false);
        return source != null ? source.search(viewport, keys, limitPerGroup) : null;
    }

    private PoiSource findSource(BoundingBox viewport, boolean download) {
        if (viewport == null)
            return null;

        MapsforgePoiLookup.PoiFile poiFile = poiLookup.findPoiFile(viewport, download);
        if (poiFile != null)
            return new PoiDatabase(poiLookup, poiFile.file(), poiFile.dataSourceName());

        return findDisplayedMapFile(viewport);
    }

    private PoiSource findDisplayedMapFile(BoundingBox viewport) {
        LocalMap localMap = mapsforgeMapManager.getDisplayedMapModel().getItem();
        if (localMap == null || localMap.getType() != Mapsforge || !(localMap instanceof MapsforgeFileMap mapsforgeFileMap))
            return null;

        try {
            BoundingBox mapBoundingBox = localMap.getBoundingBox();
            if (mapBoundingBox == null || mapBoundingBox.intersect(viewport) == null)
                return null;
            return new MapFileSource(mapLookup, mapsforgeFileMap.getMapFile(), localMap.description());
        } catch (RuntimeException e) {
            log.log(Level.FINE, "Cannot access displayed Mapsforge map for POI overlay", e);
            return null;
        }
    }

    private interface PoiSource {
        String name();

        List<PoiCategory> categories() throws IOException;

        CategoryPoiResult search(BoundingBox viewport, Set<String> keys, int limitPerGroup) throws IOException;
    }

    private record PoiDatabase(MapsforgePoiLookup poiLookup, File file, String name) implements PoiSource {
        public List<PoiCategory> categories() throws IOException {
            return poiLookup.readCategories(file);
        }

        public CategoryPoiResult search(BoundingBox viewport, Set<String> keys, int limitPerGroup) throws IOException {
            return poiLookup.searchCategories(file, viewport, keys, limitPerGroup);
        }
    }

    private record MapFileSource(MapsforgeMapLookup mapLookup, MapFile mapFile, String name) implements PoiSource {
        public List<PoiCategory> categories() {
            return mapLookup.readCategories(mapFile);
        }

        public CategoryPoiResult search(BoundingBox viewport, Set<String> keys, int limitPerGroup) {
            return mapLookup.searchCategories(mapFile, viewport, keys, limitPerGroup);
        }
    }
}

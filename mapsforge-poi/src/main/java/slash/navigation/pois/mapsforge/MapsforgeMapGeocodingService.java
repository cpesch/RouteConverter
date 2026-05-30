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
import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;
import static slash.navigation.maps.mapsforge.MapType.Mapsforge;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.normalize;

/**
 * Geocoding service adapter for embedded named features in the displayed Mapsforge {@code .map} file.
 * This class is intended to integrate the current map context with the geocoding facade while
 * delegating direct map-file search and reverse lookup to {@link MapsforgeMapLookup}.
 *
 * @author Christian Pesch
 */
public class MapsforgeMapGeocodingService extends BaseGeocodingService {
    private static final Logger log = Logger.getLogger(MapsforgeMapGeocodingService.class.getName());

    private final MapsforgeMapManager mapsforgeMapManager;
    private final Supplier<BoundingBox> visibleBoundsSupplier;
    private final MapsforgeMapLookup lookup;

    public MapsforgeMapGeocodingService(MapsforgeMapManager mapsforgeMapManager,
                                        Supplier<BoundingBox> visibleBoundsSupplier) {
        this.mapsforgeMapManager = mapsforgeMapManager;
        this.visibleBoundsSupplier = visibleBoundsSupplier;
        this.lookup = new MapsforgeMapLookup();
    }

    public String getName() {
        return "Mapsforge Map";
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isOverQueryLimit() {
        return false;
    }

    public List<GeocodingResult> getPositionsFor(String address) {
        String query = normalize(address);
        if (query.isEmpty())
            return emptyList();

        MapContext context = getContext();
        if (context == null)
            return null;

        NavigationPosition center = context.mapBoundingBox().getCenter();
        List<NavigationPosition> positions = lookup.search(context.mapFile(), query, context.visibleBounds(), center);
        if (positions.isEmpty() && !Objects.equals(context.visibleBounds(), context.mapBoundingBox()))
            positions = lookup.search(context.mapFile(), query, context.mapBoundingBox(), center);
        return asGeocodingResults(positions);
    }

    public String getAddressFor(NavigationPosition position) {
        if (position == null || !position.hasCoordinates())
            return null;

        MapContext context = getContext();
        if (context == null)
            return null;

        return lookup.lookup(context.mapFile(), position);
    }

    private MapContext getContext() {
        LocalMap localMap = mapsforgeMapManager.getDisplayedMapModel().getItem();
        if (localMap == null || localMap.getType() != Mapsforge || !(localMap instanceof MapsforgeFileMap mapsforgeFileMap))
            return null;

        try {
            MapFile mapFile = mapsforgeFileMap.getMapFile();
            BoundingBox mapBoundingBox = localMap.getBoundingBox();
            if (mapFile == null || mapBoundingBox == null)
                return null;

            BoundingBox visibleBounds = mapBoundingBox.intersect(visibleBoundsSupplier.get());
            return new MapContext(mapFile, mapBoundingBox, visibleBounds != null ? visibleBounds : mapBoundingBox);
        } catch (RuntimeException e) {
            log.log(Level.FINE, "Cannot access displayed Mapsforge map for geocoding", e);
            return null;
        }
    }

    private record MapContext(MapFile mapFile, BoundingBox mapBoundingBox, BoundingBox visibleBounds) {
    }
}


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

import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.normalize;

/**
 * Searches Mapsforge {@code .poi} databases for offline geocoding.
 *
 * @author Christian Pesch
 */
public class MapsforgePoiGeocodingService extends BaseGeocodingService {
    private final MapsforgeMapManager mapsforgeMapManager;
    private final Supplier<BoundingBox> visibleBoundsSupplier;
    private final MapsforgePoiLookup lookup;

    public MapsforgePoiGeocodingService(DataSourceManager dataSourceManager,
                                        MapsforgeMapManager mapsforgeMapManager,
                                        Supplier<BoundingBox> visibleBoundsSupplier) {
        this.mapsforgeMapManager = mapsforgeMapManager;
        this.visibleBoundsSupplier = visibleBoundsSupplier;
        this.lookup = new MapsforgePoiLookup(dataSourceManager);
    }

    public String getName() {
        return "Mapsforge POI";
    }

    public boolean isDownload() {
        return true;
    }

    public boolean isOverQueryLimit() {
        return false;
    }

    public List<GeocodingResult> getPositionsFor(String address) throws IOException, ServiceUnavailableException {
        String query = normalize(address);
        if (query.isEmpty())
            return emptyList();

        PoiContext context = getContext();
        if (context == null)
            return null;

        MapsforgePoiLookup.PoiFile poiFile = lookup.findPoiFile(context.mapBoundingBox());
        if (poiFile == null)
            return null;

        NavigationPosition center = context.mapBoundingBox().getCenter();
        List<CategorizedNavigationPosition> positions = lookup.search(poiFile.file(), query, context.visibleBounds(), center);
        if (positions.isEmpty() && !Objects.equals(context.visibleBounds(), context.mapBoundingBox()))
            positions = lookup.search(poiFile.file(), query, context.mapBoundingBox(), center);
        return asGeocodingResults(positions, poiFile.dataSourceName());
    }

    public String getAddressFor(NavigationPosition position) throws IOException, ServiceUnavailableException {
        if (position == null || !position.hasCoordinates())
            return null;

        PoiContext context = getContext();
        if (context == null)
            return null;

        MapsforgePoiLookup.PoiFile poiFile = lookup.findPoiFile(context.mapBoundingBox());
        if (poiFile == null)
            return null;

        return lookup.lookup(poiFile.file(), position);
    }

    private PoiContext getContext() {
        LocalMap localMap = mapsforgeMapManager.getDisplayedMapModel().getItem();
        BoundingBox mapBoundingBox = localMap != null ? localMap.getBoundingBox() : null;
        if (mapBoundingBox == null)
            return null;

        BoundingBox visibleBounds = mapBoundingBox.intersect(visibleBoundsSupplier.get());
        return new PoiContext(mapBoundingBox, visibleBounds != null ? visibleBounds : mapBoundingBox);
    }

    private record PoiContext(BoundingBox mapBoundingBox, BoundingBox visibleBounds) {
    }
}

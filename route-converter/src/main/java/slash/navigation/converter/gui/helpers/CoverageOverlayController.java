/*
    This file is part of BaseRouteConverter.

    BaseRouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    BaseRouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BaseRouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.converter.gui.helpers;

import slash.navigation.common.BoundingBox;
import slash.navigation.converter.gui.BaseRouteConverter;
import slash.navigation.converter.gui.RouteConverter;
import slash.navigation.elevation.ElevationService;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;
import slash.navigation.maps.mapsforge.RemoteMap;
import slash.navigation.pois.mapsforge.MapsforgePoiLookup;
import slash.navigation.routing.RoutingService;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static slash.common.io.Directories.getApplicationDirectory;

/**
 * Computes and shows a coverage overlay on the map for offline data (maps, routing, elevation,
 * POI) that is already downloaded, driven by the "Offline Coverage" entry in the View menu.
 *
 * @author Christian Pesch
 */

public class CoverageOverlayController {
    public enum Category { NONE, MAPS, ROUTING, ELEVATION, POI }

    private Category category = Category.NONE;
    private Category lastCategory;
    private BoundingBox lastViewport;

    public CoverageOverlayController() {
        // Coverage is computed for the current map viewport, which changes by panning/zooming
        // without any event this class observes -- poll for it while a category is active.
        Timer timer = new Timer(750, e -> refresh());
        timer.start();
    }

    public void selectCategory(Category category) {
        this.category = category;
        refresh();
    }

    // bypasses the viewport/category dedup below -- used after a download completes, where the
    // underlying data changed even though the viewport and selected category did not
    public void forceRefresh() {
        lastViewport = null;
        refresh();
    }

    private void refresh() {
        BaseRouteConverter r = BaseRouteConverter.getInstance();
        if (category == Category.NONE) {
            r.showCoverageOverlay(null, null, null);
            return;
        }

        BoundingBox viewport = r.getMapView().getBoundingBox();
        if (viewport == null) {
            r.showCoverageOverlay(null, null, null);
            return;
        }

        // the poll timer fires on a fixed interval regardless of whether the map actually moved;
        // recomputing coverage over hundreds of candidate maps is expensive enough (POI took over
        // a second per tick at a whole-continent zoom) that doing it unconditionally every tick
        // froze the EDT repeatedly, which is what showed up as the overlay blinking
        if (category == lastCategory && viewport.equals(lastViewport))
            return;
        lastCategory = category;
        lastViewport = viewport;

        Map<BoundingBox, Boolean> coverageTiles = switch (category) {
            case ROUTING -> computeRoutingCoverage(viewport, r);
            case ELEVATION -> computeElevationCoverage(viewport, r);
            case MAPS -> computeMapsCoverage(viewport);
            case POI -> computePoiCoverage(viewport);
            case NONE -> null;
        };

        if (coverageTiles == null || coverageTiles.isEmpty()) {
            r.showCoverageOverlay(null, null, null);
            return;
        }

        r.showCoverageOverlay(viewport, category.name(), coverageTiles);
    }

    private Map<BoundingBox, Boolean> computeRoutingCoverage(BoundingBox viewport, BaseRouteConverter r) {
        RoutingService routingService = r.getRoutingServiceFacade().getRoutingService();
        if (!routingService.isDownload())
            return null;
        Map<BoundingBox, Boolean> coverageTiles = routingService.getCoverageTiles(viewport);
        // some routing services (e.g. GraphHopper) have no real per-tile grid and always return
        // an empty map; a real grid always yields at least one tile for a non-empty viewport, so
        // fall back to a per-map download check in that case
        return coverageTiles.isEmpty() ? computeRoutingCoverageByMap(viewport, routingService) : coverageTiles;
    }

    private Map<BoundingBox, Boolean> computeElevationCoverage(BoundingBox viewport, BaseRouteConverter r) {
        ElevationService elevationService = r.getElevationServiceFacade().getElevationService();
        return elevationService.isDownload() ? elevationService.getCoverageTiles(viewport) : null;
    }

    // Maps are covered if already downloaded to the offline maps directory. RemoteMap#getUrl()
    // is the http(s) download URL, while LocalMap#getUrl() is a file:// URI of the parsed local
    // file, so the two can never be joined by URL -- check the file the download would land at.
    private Map<BoundingBox, Boolean> computeMapsCoverage(BoundingBox viewport) {
        List<BoundingBox> downloaded = new ArrayList<>();
        for (RemoteMap map : getCoverageCandidateMaps(viewport)) {
            File file = new File(getApplicationDirectory(map.getDataSource().getDirectory()), map.getDownloadable().getUri());
            if (file.exists())
                downloaded.add(map.getBoundingBox());
        }
        return computeTileCoverage(viewport, downloaded);
    }

    // Fallback for routing services without a real getCoverageTiles() grid (e.g. GraphHopper),
    // whose routing data is downloaded per map region rather than per degree tile
    private Map<BoundingBox, Boolean> computeRoutingCoverageByMap(BoundingBox viewport, RoutingService routingService) {
        List<BoundingBox> covered = new ArrayList<>();
        for (RemoteMap map : getCoverageCandidateMaps(viewport)) {
            long remaining = routingService.calculateRemainingDownloadSize(singletonList(new RemoteMapDescriptor(map)));
            if (remaining == 0)
                covered.add(map.getBoundingBox());
        }
        return computeTileCoverage(viewport, covered);
    }

    // POI is covered if a local POI file's own bounding box reaches into the viewport. This scans
    // the POI catalog once (not once per candidate map, like the routing fallback above has to --
    // POI catalog entries carry their own bounding box, so there is no need to go via the downloadable
    // maps list at all here, which is what made the previous, map-driven version of this slow).
    private Map<BoundingBox, Boolean> computePoiCoverage(BoundingBox viewport) {
        return computeTileCoverage(viewport, getMapsforgePoiLookup().findLocalPoiCoverage(viewport));
    }

    // Maps whose bounding box reaches into the viewport, excluding the global background world
    // map: its datasource covers the entire planet by design (see MapsforgeMapManager#scanMaps(),
    // which excludes the same file locally via the "routeconverter" directory check), so treating
    // it as a regular downloadable map would mark every tile in every viewport as covered.
    private List<RemoteMap> getCoverageCandidateMaps(BoundingBox viewport) {
        List<RemoteMap> result = new ArrayList<>();
        for (RemoteMap map : getMapsforgeMapManager().getDownloadableMapsModel().getItems()) {
            BoundingBox mapBoundingBox = map.getBoundingBox();
            if (mapBoundingBox == null || viewport.intersect(mapBoundingBox) == null)
                continue;
            if (map.getDataSource().getDirectory().endsWith("routeconverter"))
                continue;
            result.add(map);
        }
        return result;
    }

    // A map's whole bounding box can be country-sized, so painting it as a single covered
    // rectangle drowns out the fact that only a small part of the viewport is actually backed
    // by it. Break the viewport into a 1x1-degree grid and mark a tile covered only if a covered
    // map's bounding box actually reaches into it.
    private Map<BoundingBox, Boolean> computeTileCoverage(BoundingBox viewport, List<BoundingBox> coveredBoundingBoxes) {
        Map<BoundingBox, Boolean> result = new HashMap<>();
        if (coveredBoundingBoxes.isEmpty())
            return result;

        double longitude = viewport.southWest().getLongitude();
        while (longitude < viewport.northEast().getLongitude()) {
            double latitude = viewport.southWest().getLatitude();
            while (latitude < viewport.northEast().getLatitude()) {
                double west = longitude;
                double east = Math.min(longitude + 1.0, viewport.northEast().getLongitude());
                double south = latitude;
                double north = Math.min(latitude + 1.0, viewport.northEast().getLatitude());
                BoundingBox tile = new BoundingBox(east, north, west, south);
                for (BoundingBox coveredBoundingBox : coveredBoundingBoxes) {
                    if (coveredBoundingBox.intersect(tile) != null) {
                        result.put(tile, true);
                        break;
                    }
                }
                latitude += 1.0;
            }
            longitude += 1.0;
        }
        return result;
    }

    private MapsforgeMapManager getMapsforgeMapManager() {
        return ((RouteConverter) BaseRouteConverter.getInstance()).getMapsforgeMapManager();
    }

    private MapsforgePoiLookup getMapsforgePoiLookup() {
        return ((RouteConverter) BaseRouteConverter.getInstance()).getMapsforgePoiLookup();
    }
}

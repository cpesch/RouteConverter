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

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.geocoding.CategorizedNavigationPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.*;
import static slash.navigation.pois.mapsforge.MapsforgeTagMatcher.*;

/**
 * Searches embedded named features directly from a Mapsforge {@code .map} file.
 * This class owns map-file tile queries, POI/way matching, result ranking and reverse lookup.
 *
 * @author Christian Pesch
 */
class MapsforgeMapLookup {
    static final int MAX_RESULTS = 50;
    static final int MAX_TILES_TO_QUERY = 256;
    static final int TARGET_QUERY_ZOOM = 14;
    static final int MIN_QUERY_ZOOM = 0;
    static final int REVERSE_LOOKUP_RADIUS_METERS = 1000;
    private static final int TILE_SIZE = 256;

    List<CategorizedNavigationPosition> search(MapFile mapFile, String query, BoundingBox bounds, NavigationPosition center) {
        List<FeatureMatch> matches = searchMatches(mapFile, bounds, query, center, true);
        List<CategorizedNavigationPosition> result = new ArrayList<>(min(matches.size(), MAX_RESULTS));
        for (FeatureMatch match : matches) {
            result.add(match.position());
            if (result.size() >= MAX_RESULTS)
                break;
        }
        return result;
    }

    String lookup(MapFile mapFile, NavigationPosition position) {
        BoundingBox searchBounds = createBoundsAround(position, REVERSE_LOOKUP_RADIUS_METERS);
        List<FeatureMatch> matches = searchMatches(mapFile, searchBounds, null, position, false);
        if (matches.isEmpty())
            return null;

        FeatureMatch nearest = matches.get(0);
        return nearest.distanceMeters() <= REVERSE_LOOKUP_RADIUS_METERS ? nearest.description() : null;
    }

    private List<FeatureMatch> searchMatches(MapFile mapFile, BoundingBox bounds, String query,
                                             NavigationPosition reference, boolean requireQueryMatch) {
        if (bounds == null)
            return emptyList();

        TileRange tileRange = determineTileRange(bounds);
        MapReadResult readResult = mapFile.readNamedItems(tileRange.upperLeft(), tileRange.lowerRight());
        if (readResult == null)
            return emptyList();

        MapReadResult deduplicated = readResult.deduplicate();
        List<FeatureMatch> matches = new ArrayList<>();
        collectPoiMatches(matches, deduplicated.pois, query, bounds, reference, requireQueryMatch);
        collectWayMatches(matches, deduplicated.ways, query, bounds, reference, requireQueryMatch);
        matches.sort(Comparator.comparingDouble(FeatureMatch::distanceMeters).thenComparing(FeatureMatch::description));
        return matches;
    }

    private void collectPoiMatches(List<FeatureMatch> matches, List<PointOfInterest> pois, String query,
                                   BoundingBox bounds, NavigationPosition reference, boolean requireQueryMatch) {
        for (PointOfInterest poi : pois) {
            if (poi == null || poi.position == null)
                continue;

            Match tagMatch = findMatch(poi.tags, emptyList(), query, false);
            CategorizedNavigationPosition position = buildDescriptionAndCategory(poi.position, poi.tags, emptyList(), tagMatch, "Unnamed feature");
            if (!bounds.contains(position))
                continue;

            if (requireQueryMatch && tagMatch == null)
                continue;

            matches.add(toFeatureMatch(position, poi.position, reference));
        }
    }

    private void collectWayMatches(List<FeatureMatch> matches, List<Way> ways, String query,
                                   BoundingBox bounds, NavigationPosition reference, boolean requireQueryMatch) {
        for (Way way : ways) {
            LatLong point = determineWayPoint(way);
            if (point == null)
                continue;

            Match tagMatch = findMatch(way.tags, emptyList(), query, false);
            CategorizedNavigationPosition position = buildDescriptionAndCategory(point, way.tags, emptyList(), tagMatch, "Unnamed feature");
            if (!bounds.contains(position))
                continue;

            if (requireQueryMatch && tagMatch == null)
                continue;

            matches.add(toFeatureMatch(position, point, reference));
        }
    }

    private FeatureMatch toFeatureMatch(CategorizedNavigationPosition position, LatLong point, NavigationPosition reference) {
        String description = buildDescription(position.getDescription(), position.getCategory());
        return new FeatureMatch(position, description, distanceMeters(reference, point));
    }

    private LatLong determineWayPoint(Way way) {
        if (way == null)
            return null;
        if (way.labelPosition != null)
            return way.labelPosition;
        if (way.latLongs == null)
            return null;
        for (LatLong[] latLongs : way.latLongs) {
            if (latLongs != null && latLongs.length > 0)
                return latLongs[0];
        }
        return null;
    }

    private TileRange determineTileRange(BoundingBox bounds) {
        byte zoom = TARGET_QUERY_ZOOM;

        TileRange range = createTileRange(bounds, zoom);
        while (countTiles(range) > MAX_TILES_TO_QUERY && zoom > MIN_QUERY_ZOOM) {
            zoom--;
            range = createTileRange(bounds, zoom);
        }
        return range;
    }

    private TileRange createTileRange(BoundingBox bounds, byte zoom) {
        int maxTile = Tile.getMaxTileNumber(zoom);
        int tileXMin = clampTileNumber(MercatorProjection.longitudeToTileX(bounds.southWest().getLongitude(), zoom), maxTile);
        int tileXMax = clampTileNumber(MercatorProjection.longitudeToTileX(bounds.northEast().getLongitude(), zoom), maxTile);
        int tileYMin = clampTileNumber(MercatorProjection.latitudeToTileY(bounds.northEast().getLatitude(), zoom), maxTile);
        int tileYMax = clampTileNumber(MercatorProjection.latitudeToTileY(bounds.southWest().getLatitude(), zoom), maxTile);
        Tile upperLeft = new Tile(min(tileXMin, tileXMax), min(tileYMin, tileYMax), zoom, TILE_SIZE);
        Tile lowerRight = new Tile(max(tileXMin, tileXMax), max(tileYMin, tileYMax), zoom, TILE_SIZE);
        return new TileRange(upperLeft, lowerRight);
    }

    private long countTiles(TileRange range) {
        return ((long) range.lowerRight().tileX - range.upperLeft().tileX + 1L) *
                ((long) range.lowerRight().tileY - range.upperLeft().tileY + 1L);
    }

    private int clampTileNumber(int value, int maxTile) {
        return Math.max(0, Math.min(maxTile, value));
    }

    private record TileRange(Tile upperLeft, Tile lowerRight) {
    }

    private record FeatureMatch(CategorizedNavigationPosition position, String description, double distanceMeters) {
    }
}


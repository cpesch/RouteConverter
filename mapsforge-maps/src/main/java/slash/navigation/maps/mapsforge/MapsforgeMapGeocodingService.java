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
package slash.navigation.maps.mapsforge;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.maps.mapsforge.impl.MapsforgeFileMap;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Searches embedded named features directly from the displayed Mapsforge {@code .map} file.
 *
 * @author Christian Pesch
 */
public class MapsforgeMapGeocodingService extends BaseGeocodingService {
    private static final Logger log = Logger.getLogger(MapsforgeMapGeocodingService.class.getName());

    static final int MAX_RESULTS = 50;
    static final int MAX_TILES_TO_QUERY = 256;
    static final int TARGET_QUERY_ZOOM = 14;
    static final int MIN_QUERY_ZOOM = 0;
    static final int REVERSE_LOOKUP_RADIUS_METERS = 1000;
    private static final int TILE_SIZE = 256;
    private static final Set<String> CATEGORY_TAGS = Set.of(
            "aeroway", "amenity", "building", "craft", "emergency", "historic", "highway", "landuse",
            "leisure", "man_made", "natural", "office", "place", "railway", "shop", "sport",
            "tourism", "waterway"
    );

    private final Supplier<LocalMap> displayedMapSupplier;
    private final Supplier<BoundingBox> visibleBoundsSupplier;
    private final Supplier<NavigationPosition> centerSupplier;

    public MapsforgeMapGeocodingService(Supplier<LocalMap> displayedMapSupplier,
                                        Supplier<BoundingBox> visibleBoundsSupplier,
                                        Supplier<NavigationPosition> centerSupplier) {
        this.displayedMapSupplier = displayedMapSupplier;
        this.visibleBoundsSupplier = visibleBoundsSupplier;
        this.centerSupplier = centerSupplier;
    }

    public String getName() {
        return "Mapsforge";
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
            return Collections.emptyList();

        MapsforgeContext context = getContext();
        if (context == null)
            return null;

        NavigationPosition center = determineCenter(context.mapBoundingBox());
        List<NavigationPosition> positions = search(context.mapFile(), query, context.visibleBounds(), center);
        if (positions.isEmpty() && !sameBounds(context.visibleBounds(), context.mapBoundingBox()))
            positions = search(context.mapFile(), query, context.mapBoundingBox(), center);
        return asGeocodingResults(positions);
    }

    public String getAddressFor(NavigationPosition position) throws IOException, ServiceUnavailableException {
        if (position == null || !position.hasCoordinates())
            return null;

        MapsforgeContext context = getContext();
        if (context == null)
            return null;

        BoundingBox searchBounds = createBoundsAround(position, REVERSE_LOOKUP_RADIUS_METERS);
        List<FeatureMatch> matches = searchMatches(context.mapFile(), searchBounds, null, position, false);
        if (matches.isEmpty())
            return null;

        FeatureMatch nearest = matches.get(0);
        return nearest.distanceMeters() <= REVERSE_LOOKUP_RADIUS_METERS ? nearest.description() : null;
    }

    private MapsforgeContext getContext() {
        LocalMap localMap = displayedMapSupplier.get();
        if (!(localMap instanceof MapsforgeFileMap mapsforgeFileMap))
            return null;

        try {
            MapFile mapFile = mapsforgeFileMap.getMapFile();
            BoundingBox mapBoundingBox = localMap.getBoundingBox();
            if (mapFile == null || mapBoundingBox == null)
                return null;

            BoundingBox visibleBounds = intersect(visibleBoundsSupplier.get(), mapBoundingBox);
            return new MapsforgeContext(mapFile, mapBoundingBox, visibleBounds != null ? visibleBounds : mapBoundingBox);
        } catch (RuntimeException e) {
            log.log(Level.FINE, "Cannot access displayed Mapsforge map for geocoding", e);
            return null;
        }
    }

    private List<NavigationPosition> search(MapFile mapFile, String query, BoundingBox bounds, NavigationPosition center) {
        List<FeatureMatch> matches = searchMatches(mapFile, bounds, query, center, true);
        List<NavigationPosition> result = new ArrayList<>(min(matches.size(), MAX_RESULTS));
        for (FeatureMatch match : matches) {
            result.add(match.position());
            if (result.size() >= MAX_RESULTS)
                break;
        }
        return result;
    }

    private List<FeatureMatch> searchMatches(MapFile mapFile, BoundingBox bounds, String query,
                                             NavigationPosition reference, boolean requireQueryMatch) {
        if (bounds == null)
            return Collections.emptyList();

        TileRange tileRange = determineTileRange(mapFile, bounds);
        MapReadResult readResult = mapFile.readNamedItems(tileRange.upperLeft(), tileRange.lowerRight());
        if (readResult == null)
            return Collections.emptyList();

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

            NavigationPosition position = toPosition(poi.position, buildDescription(poi.tags, query));
            if (!bounds.contains(position))
                continue;

            TagMatch tagMatch = findMatch(poi.tags, query);
            if (requireQueryMatch && tagMatch == null)
                continue;

            matches.add(toFeatureMatch(position, poi.position, poi.tags, tagMatch, reference));
        }
    }

    private void collectWayMatches(List<FeatureMatch> matches, List<Way> ways, String query,
                                   BoundingBox bounds, NavigationPosition reference, boolean requireQueryMatch) {
        for (Way way : ways) {
            LatLong point = determineWayPoint(way);
            if (point == null)
                continue;

            NavigationPosition position = toPosition(point, buildDescription(way.tags, query));
            if (!bounds.contains(position))
                continue;

            TagMatch tagMatch = findMatch(way.tags, query);
            if (requireQueryMatch && tagMatch == null)
                continue;

            matches.add(toFeatureMatch(position, point, way.tags, tagMatch, reference));
        }
    }

    private FeatureMatch toFeatureMatch(NavigationPosition position, LatLong point, List<Tag> tags,
                                        TagMatch tagMatch, NavigationPosition reference) {
        String description = buildDescription(tags, tagMatch);
        position.setDescription(description);
        LatLong referencePoint = toLatLong(reference);
        double distance = referencePoint != null ? referencePoint.vincentyDistance(point) : 0.0;
        return new FeatureMatch(position, description, distance);
    }

    private String buildDescription(List<Tag> tags, String query) {
        return buildDescription(tags, findMatch(tags, query));
    }

    private String buildDescription(List<Tag> tags, TagMatch tagMatch) {
        String matchedName = tagMatch != null && isNameTag(tagMatch.tag()) ? tagMatch.tag().value : null;
        String primaryName = firstTagValue(tags, "name");
        if (primaryName == null)
            primaryName = firstNameVariant(tags);

        String label = matchedName != null ? matchedName : primaryName;
        if (label == null && tagMatch != null)
            label = tagMatch.tag().value;
        if (label == null)
            label = firstCategoryValue(tags);
        if (label == null)
            label = "Unnamed feature";

        String category = firstCategoryValue(tags);
        if (category != null && !category.equalsIgnoreCase(label))
            return label + " (" + category + ")";
        return label;
    }

    private TagMatch findMatch(List<Tag> tags, String query) {
        if (query == null || query.isEmpty() || tags == null)
            return null;

        TagMatch best = null;
        for (Tag tag : tags) {
            if (!isRelevantSearchTag(tag))
                continue;

            String normalizedValue = normalize(tag.value);
            if (normalizedValue.isEmpty() || !normalizedValue.contains(query))
                continue;

            int score = score(tag, query, normalizedValue);
            if (best == null || score < best.score())
                best = new TagMatch(tag, score);
        }
        return best;
    }

    private boolean isRelevantSearchTag(Tag tag) {
        if (tag == null || tag.key == null || tag.value == null)
            return false;
        if (tag.key.startsWith("addr:"))
            return false;
        return isNameTag(tag) || "normalized_name".equals(tag.key) || CATEGORY_TAGS.contains(tag.key);
    }

    private boolean isNameTag(Tag tag) {
        return "name".equals(tag.key) || (tag.key != null && tag.key.startsWith("name:"));
    }

    private int score(Tag tag, String query, String normalizedValue) {
        int score = normalizedValue.equals(query) ? 0 : 10;
        if (isNameTag(tag))
            score -= 5;
        else if ("normalized_name".equals(tag.key))
            score -= 3;
        return score + max(0, normalizedValue.length() - query.length());
    }

    private String firstTagValue(List<Tag> tags, String key) {
        if (tags == null)
            return null;
        for (Tag tag : tags) {
            if (key.equals(tag.key) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        return null;
    }

    private String firstNameVariant(List<Tag> tags) {
        if (tags == null)
            return null;
        for (Tag tag : tags) {
            if (isNameTag(tag) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        return null;
    }

    private String firstCategoryValue(List<Tag> tags) {
        if (tags == null)
            return null;
        for (Tag tag : tags) {
            if (CATEGORY_TAGS.contains(tag.key) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        return null;
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

    private TileRange determineTileRange(MapFile mapFile, BoundingBox bounds) {
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
        int tileXMin = clamp(MercatorProjection.longitudeToTileX(bounds.southWest().getLongitude(), zoom), 0, maxTile);
        int tileXMax = clamp(MercatorProjection.longitudeToTileX(bounds.northEast().getLongitude(), zoom), 0, maxTile);
        int tileYMin = clamp(MercatorProjection.latitudeToTileY(bounds.northEast().getLatitude(), zoom), 0, maxTile);
        int tileYMax = clamp(MercatorProjection.latitudeToTileY(bounds.southWest().getLatitude(), zoom), 0, maxTile);
        Tile upperLeft = new Tile(min(tileXMin, tileXMax), min(tileYMin, tileYMax), zoom, TILE_SIZE);
        Tile lowerRight = new Tile(max(tileXMin, tileXMax), max(tileYMin, tileYMax), zoom, TILE_SIZE);
        return new TileRange(upperLeft, lowerRight);
    }

    private long countTiles(TileRange range) {
        return ((long) range.lowerRight().tileX - range.upperLeft().tileX + 1L) *
                ((long) range.lowerRight().tileY - range.upperLeft().tileY + 1L);
    }

    private BoundingBox intersect(BoundingBox first, BoundingBox second) {
        if (first == null)
            return second;
        if (second == null)
            return first;

        double east = min(first.northEast().getLongitude(), second.northEast().getLongitude());
        double north = min(first.northEast().getLatitude(), second.northEast().getLatitude());
        double west = max(first.southWest().getLongitude(), second.southWest().getLongitude());
        double south = max(first.southWest().getLatitude(), second.southWest().getLatitude());
        if (west > east || south > north)
            return null;
        return new BoundingBox(east, north, west, south);
    }

    private boolean sameBounds(BoundingBox first, BoundingBox second) {
        return Objects.equals(first, second);
    }

    private BoundingBox createBoundsAround(NavigationPosition position, int radiusMeters) {
        LatLong center = toLatLong(position);
        LatLong north = center.destinationPoint(radiusMeters, 0);
        LatLong east = center.destinationPoint(radiusMeters, 90);
        LatLong south = center.destinationPoint(radiusMeters, 180);
        LatLong west = center.destinationPoint(radiusMeters, 270);
        return new BoundingBox(east.longitude, north.latitude, west.longitude, south.latitude);
    }

    private NavigationPosition determineCenter(BoundingBox mapBoundingBox) {
        NavigationPosition center = centerSupplier.get();
        if (center != null && center.hasCoordinates())
            return center;
        return mapBoundingBox != null ? mapBoundingBox.getCenter() : null;
    }

    private NavigationPosition toPosition(LatLong latLong, String description) {
        return new SimpleNavigationPosition(latLong.longitude, latLong.latitude, null, description);
    }

    private LatLong toLatLong(NavigationPosition position) {
        if (position == null || !position.hasCoordinates())
            return null;
        return new LatLong(position.getLatitude(), position.getLongitude());
    }

    private String normalize(String value) {
        if (value == null)
            return "";
        return value.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record MapsforgeContext(MapFile mapFile, BoundingBox mapBoundingBox, BoundingBox visibleBounds) {
    }

    private record TileRange(Tile upperLeft, Tile lowerRight) {
    }

    private record TagMatch(Tag tag, int score) {
    }

    private record FeatureMatch(NavigationPosition position, String description, double distanceMeters) {
    }
}


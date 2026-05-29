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
import org.mapsforge.core.model.Tag;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Download;
import slash.navigation.geocoding.BaseGeocodingService;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;

import javax.naming.ServiceUnavailableException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Collections.singletonList;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.getExtension;

/**
 * Searches Mapsforge {@code .poi} databases for offline geocoding.
 *
 * @author Christian Pesch
 */
public class MapsforgePoiGeocodingService extends BaseGeocodingService {
    private static final Logger log = Logger.getLogger(MapsforgePoiGeocodingService.class.getName());

    public static final String NAME = "Mapsforge POI";
    static final int MAX_RESULTS = 50;
    static final int MAX_DATABASE_ROWS = 2000;
    static final int REVERSE_LOOKUP_RADIUS_METERS = 1000;
    static final String DOT_POI = ".poi";
    private static final Set<String> CATEGORY_TAGS = Set.of(
            "aeroway", "amenity", "building", "craft", "emergency", "historic", "highway", "landuse",
            "leisure", "man_made", "natural", "office", "place", "railway", "shop", "sport",
            "tourism", "waterway"
    );

    private final DataSourceManager dataSourceManager;
    private final MapsforgeMapManager mapsforgeMapManager;
    private final Supplier<BoundingBox> visibleBoundsSupplier;
    private final Supplier<NavigationPosition> centerSupplier;

    public MapsforgePoiGeocodingService(DataSourceManager dataSourceManager,
                                        MapsforgeMapManager mapsforgeMapManager,
                                        Supplier<BoundingBox> visibleBoundsSupplier,
                                        Supplier<NavigationPosition> centerSupplier) {
        this.dataSourceManager = dataSourceManager;
        this.mapsforgeMapManager = mapsforgeMapManager;
        this.visibleBoundsSupplier = visibleBoundsSupplier;
        this.centerSupplier = centerSupplier;
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
            return Collections.emptyList();

        PoiContext context = getContext();
        if (context == null)
            return null;

        PoiFile poiFile = findPoiFile(context);
        if (poiFile == null)
            return null;

        NavigationPosition center = determineCenter(context.mapBoundingBox());
        List<NavigationPosition> positions = search(poiFile.file(), query, context.visibleBounds(), center);
        if (positions.isEmpty() && !sameBounds(context.visibleBounds(), context.mapBoundingBox()))
            positions = search(poiFile.file(), query, context.mapBoundingBox(), center);
        return asGeocodingResults(positions, poiFile.dataSourceName());
    }

    public String getAddressFor(NavigationPosition position) throws IOException, ServiceUnavailableException {
        if (position == null || !position.hasCoordinates())
            return null;

        PoiContext context = getContext();
        if (context == null)
            return null;

        PoiFile poiFile = findPoiFile(context);
        if (poiFile == null)
            return null;

        BoundingBox searchBounds = createBoundsAround(position, REVERSE_LOOKUP_RADIUS_METERS);
        List<PoiMatch> matches = searchMatches(poiFile.file(), searchBounds, null, position, false, false);
        if (matches.isEmpty())
            return null;

        PoiMatch nearest = matches.get(0);
        return nearest.distanceMeters() <= REVERSE_LOOKUP_RADIUS_METERS ? nearest.description() : null;
    }

    private PoiContext getContext() {
        LocalMap localMap = mapsforgeMapManager.getDisplayedMapModel().getItem();
        BoundingBox mapBoundingBox = localMap != null ? localMap.getBoundingBox() : null;
        if (mapBoundingBox == null)
            return null;

        BoundingBox visibleBounds = mapBoundingBox.intersect(visibleBoundsSupplier.get());
        return new PoiContext(mapBoundingBox, visibleBounds != null ? visibleBounds : mapBoundingBox, localMap);
    }

    private PoiFile findPoiFile(PoiContext context) throws IOException {
        PoiFile sibling = findSiblingPoiFile(context.localMap());
        if (sibling != null)
            return sibling;

        List<PoiDescriptor> descriptors = findPoiDescriptors(context.mapBoundingBox(), context.localMap());
        if (descriptors.isEmpty())
            return null;

        PoiDescriptor descriptor = descriptors.get(0);
        if (descriptor.localFile() != null)
            return new PoiFile(descriptor.localFile(), descriptor.dataSourceName());

        if (descriptor.remoteFile() == null)
            return null;

        Download download = dataSourceManager.queueForDownload(descriptor.remoteFile().getDataSource(), descriptor.remoteFile());
        dataSourceManager.getDownloadManager().waitForCompletion(singletonList(download));
        File file = createFile(descriptor.remoteFile());
        return file.exists() ? new PoiFile(file, dataSourceName(descriptor.remoteFile().getDataSource())) : null;
    }

    private PoiFile findSiblingPoiFile(LocalMap localMap) {
        try {
            if (localMap == null || localMap.getUrl() == null || !localMap.getUrl().startsWith("file:"))
                return null;

            File mapFile = new File(URI.create(localMap.getUrl()));
            String name = mapFile.getName();
            int dot = name.lastIndexOf('.');
            File poiFile = new File(mapFile.getParentFile(), (dot != -1 ? name.substring(0, dot) : name) + DOT_POI);
            return poiFile.exists() && poiFile.isFile() ? new PoiFile(poiFile, dataSourceName(localMap)) : null;
        } catch (RuntimeException e) {
            log.log(Level.FINE, "Cannot determine sibling POI file", e);
            return null;
        }
    }

    private List<PoiDescriptor> findPoiDescriptors(BoundingBox bounds, LocalMap localMap) throws IOException {
        List<PoiDescriptor> descriptors = new ArrayList<>(collectLocalPoiDescriptors(bounds));
        for (DataSource dataSource : dataSourceManager.getDataSourceService().getDataSources()) {
            for (slash.navigation.datasources.File file : dataSource.getFiles()) {
                if (DOT_POI.equals(getExtension(file.getUri())) && matches(file.getBoundingBox(), bounds))
                    descriptors.add(new PoiDescriptor(null, file, file.getBoundingBox(), dataSourceName(dataSource)));
            }
        }
        descriptors.sort((d1, d2) -> {
            if (d1.localFile() != null && d2.localFile() == null)
                return -1;
            if (d1.localFile() == null && d2.localFile() != null)
                return 1;
            if (d1.boundingBox() == null && d2.boundingBox() != null)
                return 1;
            if (d1.boundingBox() != null && d2.boundingBox() == null)
                return -1;
            if (d1.boundingBox() == null)
                return 0;
            return Double.compare(d1.boundingBox().getSquareSize(), d2.boundingBox().getSquareSize());
        });
        return descriptors;
    }

    private List<PoiDescriptor> collectLocalPoiDescriptors(BoundingBox bounds) {
        List<PoiDescriptor> result = new ArrayList<>();
        Set<File> files = new LinkedHashSet<>();
        for (DataSource dataSource : dataSourceManager.getDataSourceService().getDataSources()) {
            for (File file : collectFiles(getApplicationDirectory(dataSource.getDirectory()), DOT_POI)) {
                BoundingBox fileBounds = readBounds(file);
                if (matches(fileBounds, bounds))
                    result.add(new PoiDescriptor(file, null, fileBounds, dataSourceName(dataSource)));
                files.add(file);
            }
        }
        for (File file : collectFiles(getApplicationDirectory("maps"), DOT_POI)) {
            if (files.contains(file))
                continue;
            BoundingBox fileBounds = readBounds(file);
            if (matches(fileBounds, bounds))
                result.add(new PoiDescriptor(file, null, fileBounds, file.getName()));
        }
        return new ArrayList<>(result);
    }

    private String dataSourceName(DataSource dataSource) {
        return dataSource != null && dataSource.getName() != null ? dataSource.getName() : NAME;
    }

    private String dataSourceName(LocalMap localMap) {
        return localMap != null && localMap.getProvider() != null ? localMap.getProvider() : NAME;
    }

    private File createFile(Downloadable downloadable) {
        return new File(getApplicationDirectory(downloadable.getDataSource().getDirectory()), downloadable.getUri());
    }

    private boolean matches(BoundingBox fileBounds, BoundingBox queryBounds) {
        if (fileBounds == null || queryBounds == null)
            return true;
        return intersects(fileBounds, queryBounds) || fileBounds.contains(queryBounds.getCenter()) || queryBounds.contains(fileBounds.getCenter());
    }

    private boolean intersects(BoundingBox first, BoundingBox second) {
        return first.southWest().getLongitude() <= second.northEast().getLongitude() &&
                first.northEast().getLongitude() >= second.southWest().getLongitude() &&
                first.southWest().getLatitude() <= second.northEast().getLatitude() &&
                first.northEast().getLatitude() >= second.southWest().getLatitude();
    }

    private BoundingBox readBounds(File file) {
        try (Connection connection = open(file);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT max(lon) east, max(lat) north, min(lon) west, min(lat) south FROM poi_index")) {
            if (resultSet.next())
                return new BoundingBox(resultSet.getDouble("east"), resultSet.getDouble("north"), resultSet.getDouble("west"), resultSet.getDouble("south"));
        } catch (SQLException e) {
            log.log(Level.FINE, "Cannot read POI bounds from " + file, e);
        }
        return null;
    }

    private List<NavigationPosition> search(File poiFile, String query, BoundingBox bounds, NavigationPosition center) throws IOException {
        List<PoiMatch> matches = searchMatches(poiFile, bounds, query, center, true, true);
        if (matches.isEmpty())
            matches = searchMatches(poiFile, bounds, query, center, true, false);

        List<NavigationPosition> result = new ArrayList<>(min(matches.size(), MAX_RESULTS));
        for (PoiMatch match : matches) {
            result.add(match.position());
            if (result.size() >= MAX_RESULTS)
                break;
        }
        return result;
    }

    private List<PoiMatch> searchMatches(File poiFile, BoundingBox bounds, String query,
                                         NavigationPosition reference, boolean requireQueryMatch, boolean exactOnly) throws IOException {
        if (bounds == null)
            return Collections.emptyList();

        List<PoiMatch> matches = new ArrayList<>();
        String sql = "SELECT poi_index.id, poi_index.lat, poi_index.lon, poi_data.data, " +
                "group_concat(poi_categories.name, '\r') categories " +
                "FROM poi_index JOIN poi_data ON poi_index.id = poi_data.id " +
                "LEFT JOIN poi_category_map ON poi_index.id = poi_category_map.id " +
                "LEFT JOIN poi_categories ON poi_category_map.category = poi_categories.id " +
                "WHERE poi_index.lat <= ? AND poi_index.lon <= ? AND poi_index.lat >= ? AND poi_index.lon >= ? " +
                (requireQueryMatch ? "AND (lower(poi_data.data) LIKE ? OR lower(poi_categories.name) LIKE ?) " : "") +
                "GROUP BY poi_index.id, poi_index.lat, poi_index.lon, poi_data.data " +
                "LIMIT ?";

        try (Connection connection = open(poiFile);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, bounds.northEast().getLatitude());
            statement.setDouble(2, bounds.northEast().getLongitude());
            statement.setDouble(3, bounds.southWest().getLatitude());
            statement.setDouble(4, bounds.southWest().getLongitude());
            int index = 5;
            if (requireQueryMatch) {
                String pattern = "%" + query + "%";
                statement.setString(index++, pattern);
                statement.setString(index++, pattern);
            }
            statement.setInt(index, MAX_DATABASE_ROWS);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    LatLong latLong = new LatLong(resultSet.getDouble("lat"), resultSet.getDouble("lon"));
                    List<Tag> tags = parseTags(resultSet.getString("data"));
                    List<String> categories = parseCategories(resultSet.getString("categories"));
                    TagMatch tagMatch = findMatch(tags, categories, query, exactOnly);
                    if (requireQueryMatch && tagMatch == null)
                        continue;
                    if (!requireQueryMatch && !hasUsefulDescription(tags, categories))
                        continue;

                    matches.add(toPoiMatch(toPosition(latLong), latLong, tags, categories, tagMatch, reference));
                }
            }
        } catch (SQLException e) {
            throw new IOException("Cannot search Mapsforge POI database " + poiFile, e);
        }
        matches.sort(Comparator.comparingDouble(PoiMatch::distanceMeters).thenComparing(PoiMatch::description));
        return matches;
    }

    private Connection open(File file) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    private PoiMatch toPoiMatch(NavigationPosition position, LatLong point, List<Tag> tags, List<String> categories,
                               TagMatch tagMatch, NavigationPosition reference) {
        String description = buildDescription(tags, categories, tagMatch);
        position.setDescription(description);
        LatLong referencePoint = toLatLong(reference);
        double distance = referencePoint != null ? referencePoint.vincentyDistance(point) : 0.0;
        return new PoiMatch(position, description, distance);
    }

    private String buildDescription(List<Tag> tags, List<String> categories, TagMatch tagMatch) {
        String matchedName = tagMatch != null && tagMatch.tag() != null && isNameTag(tagMatch.tag()) ? tagMatch.tag().value : null;
        String primaryName = firstTagValue(tags, "name");
        if (primaryName == null)
            primaryName = firstNameVariant(tags);

        String label = matchedName != null ? matchedName : primaryName;
        if (label == null && tagMatch != null)
            label = tagMatch.value();
        if (label == null)
            label = firstCategoryValue(tags, categories);
        if (label == null)
            label = "Unnamed POI";

        String category = firstCategoryValue(tags, categories);
        if (category != null && !category.equalsIgnoreCase(label))
            return label + " (" + category + ")";
        return label;
    }

    private TagMatch findMatch(List<Tag> tags, List<String> categories, String query, boolean exactOnly) {
        if (query == null || query.isEmpty())
            return null;

        TagMatch best = null;
        for (Tag tag : tags) {
            if (!isRelevantSearchTag(tag))
                continue;
            String normalizedValue = normalize(tag.value);
            if (!matches(normalizedValue, query, exactOnly))
                continue;
            int score = score(tag, query, normalizedValue);
            if (best == null || score < best.score())
                best = new TagMatch(tag, tag.value, score);
        }
        for (String category : categories) {
            String normalizedValue = normalize(category);
            if (!matches(normalizedValue, query, exactOnly))
                continue;
            int score = normalizedValue.equals(query) ? 2 : 12 + max(0, normalizedValue.length() - query.length());
            if (best == null || score < best.score())
                best = new TagMatch(null, category, score);
        }
        return best;
    }

    private boolean matches(String value, String query, boolean exactOnly) {
        if (value == null || value.isEmpty())
            return false;
        return exactOnly ? value.equals(query) : value.contains(query);
    }

    private boolean hasUsefulDescription(List<Tag> tags, List<String> categories) {
        return firstNameVariant(tags) != null || firstCategoryValue(tags, categories) != null;
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
        for (Tag tag : tags) {
            if (key.equals(tag.key) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        return null;
    }

    private String firstNameVariant(List<Tag> tags) {
        for (Tag tag : tags) {
            if (isNameTag(tag) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        return null;
    }

    private String firstCategoryValue(List<Tag> tags, List<String> categories) {
        for (Tag tag : tags) {
            if (CATEGORY_TAGS.contains(tag.key) && tag.value != null && !tag.value.isBlank())
                return tag.value;
        }
        for (String category : categories) {
            if (category != null && !category.isBlank())
                return category;
        }
        return null;
    }

    private List<Tag> parseTags(String data) {
        List<Tag> tags = new ArrayList<>();
        if (data == null)
            return tags;
        for (String entry : data.split("\\r")) {
            int index = entry.indexOf('=');
            if (index > 0 && index < entry.length() - 1)
                tags.add(new Tag(entry.substring(0, index), entry.substring(index + 1)));
        }
        return tags;
    }

    private List<String> parseCategories(String categories) {
        if (categories == null || categories.isBlank())
            return Collections.emptyList();
        return Arrays.stream(categories.split("\\r")).filter(category -> category != null && !category.isBlank()).distinct().toList();
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

    private NavigationPosition toPosition(LatLong latLong) {
        return new SimpleNavigationPosition(latLong.longitude, latLong.latitude, null, null);
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

    private record PoiContext(BoundingBox mapBoundingBox, BoundingBox visibleBounds, LocalMap localMap) {
    }

    private record PoiFile(File file, String dataSourceName) {
    }

    private record PoiDescriptor(File localFile, slash.navigation.datasources.File remoteFile, BoundingBox boundingBox, String dataSourceName) {
    }

    private record TagMatch(Tag tag, String value, int score) {
    }

    private record PoiMatch(NavigationPosition position, String description, double distanceMeters) {
    }
}


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
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Download;
import slash.navigation.maps.mapsforge.LocalMap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.collectFiles;
import static slash.common.io.Files.getExtension;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.*;
import static slash.navigation.pois.mapsforge.MapsforgeTagMatcher.*;

/**
 * Finds Mapsforge POI database files and searches them.
 *
 * @author Christian Pesch
 */
class MapsforgePoiLookup {
    private static final Logger log = Logger.getLogger(MapsforgePoiLookup.class.getName());

    static final int MAX_RESULTS = 50;
    static final int MAX_DATABASE_ROWS = 2000;
    static final int REVERSE_LOOKUP_RADIUS_METERS = 1000;
    static final String DOT_POI = ".poi";

    private final DataSourceManager dataSourceManager;

    MapsforgePoiLookup(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    PoiFile findPoiFile(BoundingBox mapBoundingBox, LocalMap localMap) {
        PoiFile sibling = findSiblingPoiFile(localMap);
        if (sibling != null)
            return sibling;

        List<PoiDescriptor> descriptors = findPoiDescriptors(mapBoundingBox);
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

    private List<PoiDescriptor> findPoiDescriptors(BoundingBox bounds) {
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
        return dataSource != null && dataSource.getName() != null ? dataSource.getName() : "Mapsforge POI";
    }

    private String dataSourceName(LocalMap localMap) {
        return localMap != null && localMap.getProvider() != null ? localMap.getProvider() : "Mapsforge POI";
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

    List<NavigationPosition> search(File poiFile, String query, BoundingBox bounds, NavigationPosition center) throws IOException {
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

    String lookup(File poiFile, NavigationPosition position) throws IOException {
        BoundingBox searchBounds = createBoundsAround(position, REVERSE_LOOKUP_RADIUS_METERS);
        List<PoiMatch> matches = searchMatches(poiFile, searchBounds, null, position, false, false);
        if (matches.isEmpty())
            return null;

        PoiMatch nearest = matches.get(0);
        return nearest.distanceMeters() <= REVERSE_LOOKUP_RADIUS_METERS ? nearest.description() : null;
    }

    private List<PoiMatch> searchMatches(File poiFile, BoundingBox bounds, String query,
                                         NavigationPosition reference, boolean requireQueryMatch, boolean exactOnly) throws IOException {
        if (bounds == null)
            return emptyList();

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
                    Match tagMatch = findMatch(tags, categories, query, exactOnly);
                    if (requireQueryMatch && tagMatch == null)
                        continue;
                    if (!requireQueryMatch && !MapsforgeTagMatcher.hasUsefulDescription(tags, categories))
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
                               Match tagMatch, NavigationPosition reference) {
        String description = buildDescription(tags, categories, tagMatch, "Unnamed POI");
        position.setDescription(description);
        return new PoiMatch(position, description, distanceMeters(reference, point));
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
            return emptyList();
        return Arrays.stream(categories.split("\\r")).filter(category -> !category.isBlank()).distinct().toList();
    }

    record PoiFile(File file, String dataSourceName) {
    }

    private record PoiDescriptor(File localFile, slash.navigation.datasources.File remoteFile, BoundingBox boundingBox, String dataSourceName) {
    }


    private record PoiMatch(NavigationPosition position, String description, double distanceMeters) {
    }
}



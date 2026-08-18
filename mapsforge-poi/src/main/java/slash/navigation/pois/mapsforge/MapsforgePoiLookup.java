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
import slash.navigation.common.MapDescriptor;
import slash.navigation.common.NavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Download;
import slash.navigation.geocoding.CategorizedNavigationPosition;

import java.io.File;
import java.io.IOException;
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
 * <p>
 * Supports both the v3 POI schema (plain {@code poi_index} table, {@code LIKE} text search) and the
 * v4 schema introduced in mapsforge 0.29 ({@code poi_index} as an R-Tree, {@code poi_data_fts} FTS5
 * full-text index). The schema version is read per file from its {@code metadata} table.
 *
 * @author Christian Pesch
 */
public class MapsforgePoiLookup {
    private static final Logger log = Logger.getLogger(MapsforgePoiLookup.class.getName());

    static final int MAX_RESULTS = 50;
    static final int MAX_DATABASE_ROWS = 2000;
    static final int REVERSE_LOOKUP_RADIUS_METERS = 1000;
    static final String DOT_POI = ".poi";

    private static final String SELECT_COLUMNS_V4 =
            "SELECT poi_index.id, (poi_index.minLat + poi_index.maxLat) / 2 lat, " +
                    "(poi_index.minLon + poi_index.maxLon) / 2 lon, poi_data.data, " +
                    "group_concat(poi_categories.name, '\r') categories ";

    // Q1: text match, driven from the FTS5 index. poi_data_fts must stay the first table in the
    // FROM clause so SQLite scans the FTS index and probes the R-Tree by rowid, instead of walking
    // every row in the bounding box and evaluating MATCH per row.
    static final String Q1_FTS_SQL = SELECT_COLUMNS_V4 +
            "FROM poi_data_fts " +
            "JOIN poi_index ON poi_index.id = poi_data_fts.rowid " +
            "JOIN poi_data ON poi_index.id = poi_data.id " +
            "LEFT JOIN poi_category_map ON poi_index.id = poi_category_map.id " +
            "LEFT JOIN poi_categories ON poi_category_map.category = poi_categories.id " +
            "WHERE poi_data_fts MATCH ? " +
            "AND poi_index.minLat <= ? AND poi_index.minLon <= ? AND poi_index.maxLat >= ? AND poi_index.maxLon >= ? " +
            "GROUP BY poi_index.id, poi_data.data " +
            "LIMIT ?";

    // Q1 fallback: FTS5 only matches token prefixes, so an infix query (e.g. "stelle" in "Tankstelle")
    // needs a plain LIKE scan of poi_data.
    private static final String Q1_LIKE_SQL = SELECT_COLUMNS_V4 +
            "FROM poi_index " +
            "JOIN poi_data ON poi_index.id = poi_data.id " +
            "LEFT JOIN poi_category_map ON poi_index.id = poi_category_map.id " +
            "LEFT JOIN poi_categories ON poi_category_map.category = poi_categories.id " +
            "WHERE lower(poi_data.data) LIKE ? " +
            "AND poi_index.minLat <= ? AND poi_index.minLon <= ? AND poi_index.maxLat >= ? AND poi_index.maxLon >= ? " +
            "GROUP BY poi_index.id, poi_data.data " +
            "LIMIT ?";

    // Q2: category-name match. A separate query from Q1 because FTS5 MATCH cannot appear inside an
    // OR expression. The membership subquery (rather than filtering the join) keeps group_concat
    // returning each POI's complete category list, matching what Q1 returns for the same POI.
    private static final String Q2_CATEGORY_SQL = SELECT_COLUMNS_V4 +
            "FROM poi_index " +
            "JOIN poi_data ON poi_index.id = poi_data.id " +
            "LEFT JOIN poi_category_map ON poi_index.id = poi_category_map.id " +
            "LEFT JOIN poi_categories ON poi_category_map.category = poi_categories.id " +
            "WHERE poi_index.id IN (SELECT id FROM poi_category_map " +
            "WHERE category IN (SELECT id FROM poi_categories WHERE lower(name) LIKE ?)) " +
            "AND poi_index.minLat <= ? AND poi_index.minLon <= ? AND poi_index.maxLat >= ? AND poi_index.maxLon >= ? " +
            "GROUP BY poi_index.id, poi_data.data " +
            "LIMIT ?";

    // Q3: bounding-box only, used for reverse geocoding where no text/category query is required.
    private static final String Q3_BBOX_SQL = SELECT_COLUMNS_V4 +
            "FROM poi_index " +
            "JOIN poi_data ON poi_index.id = poi_data.id " +
            "LEFT JOIN poi_category_map ON poi_index.id = poi_category_map.id " +
            "LEFT JOIN poi_categories ON poi_category_map.category = poi_categories.id " +
            "WHERE poi_index.minLat <= ? AND poi_index.minLon <= ? AND poi_index.maxLat >= ? AND poi_index.maxLon >= ? " +
            "GROUP BY poi_index.id, poi_data.data " +
            "LIMIT ?";

    // Datasource ids known to serve the v4 schema; every id not listed here defaults to version 3.
    private static final Map<String, Integer> DATASOURCE_SPEC_VERSIONS = Map.of("mapsforge-pois-4", 4);
    private static final int DEFAULT_SPEC_VERSION = 3;

    private final DataSourceManager dataSourceManager;

    public MapsforgePoiLookup(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    public long calculateRemainingDownloadSize(List<MapDescriptor> mapDescriptors) {
        long size = 0L;
        for (slash.navigation.datasources.File file : findRemotePoiFiles(mapDescriptors)) {
            Long contentLength = file.getLatestChecksum() != null ? file.getLatestChecksum().getContentLength() : null;
            if (contentLength != null)
                size += contentLength;
        }
        return size;
    }

    public void downloadPoiData(List<MapDescriptor> mapDescriptors) {
        for (slash.navigation.datasources.File file : findRemotePoiFiles(mapDescriptors))
            dataSourceManager.queueForDownload(file.getDataSource(), file);
    }

    private Set<slash.navigation.datasources.File> findRemotePoiFiles(List<MapDescriptor> mapDescriptors) {
        Set<slash.navigation.datasources.File> result = new LinkedHashSet<>();
        for (MapDescriptor mapDescriptor : mapDescriptors) {
            BoundingBox bounds = mapDescriptor.getBoundingBox();
            for (DataSource dataSource : dataSourceManager.getDataSourceService().getDataSources()) {
                for (slash.navigation.datasources.File file : dataSource.getFiles()) {
                    if (DOT_POI.equals(getExtension(file.getUri())) && matches(file.getBoundingBox(), bounds) && !createFile(file).exists())
                        result.add(file);
                }
            }
        }
        return result;
    }

    PoiFile findPoiFile(BoundingBox mapBoundingBox) {
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
        return file.exists() ? new PoiFile(file, descriptor.dataSourceName()) : null;
    }

    private List<PoiDescriptor> findPoiDescriptors(BoundingBox bounds) {
        List<PoiDescriptor> descriptors = new ArrayList<>(collectLocalPoiDescriptors(bounds));
        for (DataSource dataSource : dataSourceManager.getDataSourceService().getDataSources()) {
            for (slash.navigation.datasources.File file : dataSource.getFiles()) {
                if (DOT_POI.equals(getExtension(file.getUri())) && matches(file.getBoundingBox(), bounds))
                    descriptors.add(new PoiDescriptor(null, file, file.getBoundingBox(),
                            DATASOURCE_SPEC_VERSIONS.getOrDefault(dataSource.getId(), DEFAULT_SPEC_VERSION), dataSource.getName()));
            }
        }
        descriptors.sort(descriptorPreference());
        return descriptors;
    }

    static Comparator<PoiDescriptor> descriptorPreference() {
        return (d1, d2) -> {
            if (d1.boundingBox() == null && d2.boundingBox() != null)
                return 1;
            if (d1.boundingBox() != null && d2.boundingBox() == null)
                return -1;
            if (d1.boundingBox() != null) {
                int bySize = Double.compare(d1.boundingBox().getSquareSize(), d2.boundingBox().getSquareSize());
                if (bySize != 0)
                    return bySize;
            }
            int byVersion = Integer.compare(d2.specVersion(), d1.specVersion());
            if (byVersion != 0)
                return byVersion;
            if (d1.localFile() != null && d2.localFile() == null)
                return -1;
            if (d1.localFile() == null && d2.localFile() != null)
                return 1;
            return 0;
        };
    }

    private List<PoiDescriptor> collectLocalPoiDescriptors(BoundingBox bounds) {
        List<PoiDescriptor> result = new ArrayList<>();
        Set<File> files = new LinkedHashSet<>();
        for (DataSource dataSource : dataSourceManager.getDataSourceService().getDataSources()) {
            for (File file : collectFiles(getApplicationDirectory(dataSource.getDirectory()), DOT_POI)) {
                BoundsAndVersion boundsAndVersion = readBoundsAndVersion(file);
                if (matches(boundsAndVersion.bounds(), bounds))
                    result.add(new PoiDescriptor(file, null, boundsAndVersion.bounds(), boundsAndVersion.specVersion(), dataSource.getName()));
                files.add(file);
            }
        }
        for (File file : collectFiles(getApplicationDirectory("maps"), DOT_POI)) {
            if (files.contains(file))
                continue;
            BoundsAndVersion boundsAndVersion = readBoundsAndVersion(file);
            if (matches(boundsAndVersion.bounds(), bounds))
                result.add(new PoiDescriptor(file, null, boundsAndVersion.bounds(), boundsAndVersion.specVersion(), file.getName()));
        }
        return new ArrayList<>(result);
    }


    private File createFile(Downloadable downloadable) {
        return new File(getApplicationDirectory(downloadable.getDataSource().getDirectory()), downloadable.getUri());
    }

    private boolean matches(BoundingBox fileBounds, BoundingBox queryBounds) {
        if (fileBounds == null || queryBounds == null)
            return true;
        return fileBounds.intersect(queryBounds) != null || fileBounds.contains(queryBounds.getCenter()) || queryBounds.contains(fileBounds.getCenter());
    }

    BoundingBox readBounds(File file) {
        return readBoundsAndVersion(file).bounds();
    }

    private BoundsAndVersion readBoundsAndVersion(File file) {
        try (Connection connection = open(file)) {
            BoundingBox fromMetadata = readBoundsFromMetadata(connection);
            int specVersion = readVersion(connection);
            BoundingBox bounds = fromMetadata != null ? fromMetadata : readBoundsFromIndex(connection, specVersion);
            return new BoundsAndVersion(bounds, specVersion);
        } catch (SQLException e) {
            log.log(Level.FINE, "Cannot read POI bounds from " + file, e);
        }
        return new BoundsAndVersion(null, DEFAULT_SPEC_VERSION);
    }

    private BoundingBox readBoundsFromMetadata(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT value FROM metadata WHERE name = 'bounds'")) {
            if (!resultSet.next())
                return null;
            String[] parts = resultSet.getString("value").split(",");
            if (parts.length != 4)
                return null;
            double minLat = Double.parseDouble(parts[0]);
            double minLon = Double.parseDouble(parts[1]);
            double maxLat = Double.parseDouble(parts[2]);
            double maxLon = Double.parseDouble(parts[3]);
            return new BoundingBox(maxLon, maxLat, minLon, minLat);
        } catch (SQLException | NumberFormatException e) {
            return null;
        }
    }

    private BoundingBox readBoundsFromIndex(Connection connection, int specVersion) throws SQLException {
        String sql = specVersion >= 4 ?
                "SELECT max(maxLon) east, max(maxLat) north, min(minLon) west, min(minLat) south FROM poi_index" :
                "SELECT max(lon) east, max(lat) north, min(lon) west, min(lat) south FROM poi_index";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next())
                return new BoundingBox(resultSet.getDouble("east"), resultSet.getDouble("north"), resultSet.getDouble("west"), resultSet.getDouble("south"));
        }
        return null;
    }

    private int readVersion(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT value FROM metadata WHERE name = 'version'")) {
            String value = resultSet.next() ? resultSet.getString("value") : null;
            if (value != null)
                return Integer.parseInt(value.trim());
        } catch (SQLException | NumberFormatException e) {
            log.log(Level.FINE, "Cannot read POI database version", e);
        }
        return 3;
    }

    private boolean hasFullTextSearchTable(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'poi_data_fts'")) {
            return resultSet.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Splits an already-{@link MapsforgeGeocodingHelper#normalize normalized} query into FTS5 prefix
     * terms, e.g. {@code "st. peter-ording"} becomes {@code "st"* "peter"* "ording"*}. Quoting each
     * token neutralizes FTS5 operators (AND/OR/NOT/NEAR), so nothing in the query can be interpreted
     * as query syntax. Returns {@code null} if no token survives.
     */
    static String toFtsMatchExpression(String normalizedQuery) {
        if (normalizedQuery == null)
            return null;
        StringBuilder builder = new StringBuilder();
        for (String token : normalizedQuery.split("(?U)[^\\p{Alnum}]+")) {
            if (token.isEmpty())
                continue;
            if (builder.length() > 0)
                builder.append(' ');
            builder.append('"').append(token).append("\"*");
        }
        return builder.length() > 0 ? builder.toString() : null;
    }

    List<CategorizedNavigationPosition> search(File poiFile, String query, BoundingBox bounds, NavigationPosition center) throws IOException {
        List<PoiMatch> matches = searchMatches(poiFile, bounds, query, center, true, true, false);
        if (matches.isEmpty())
            matches = searchMatches(poiFile, bounds, query, center, true, false, false);
        if (matches.isEmpty())
            matches = searchMatches(poiFile, bounds, query, center, true, false, true);

        List<CategorizedNavigationPosition> result = new ArrayList<>(min(matches.size(), MAX_RESULTS));
        for (PoiMatch match : matches) {
            result.add(match.position());
            if (result.size() >= MAX_RESULTS)
                break;
        }
        return result;
    }

    String lookup(File poiFile, NavigationPosition position) throws IOException {
        BoundingBox searchBounds = createBoundsAround(position, REVERSE_LOOKUP_RADIUS_METERS);
        List<PoiMatch> matches = searchMatches(poiFile, searchBounds, null, position, false, false, false);
        if (matches.isEmpty())
            return null;

        PoiMatch nearest = matches.get(0);
        return nearest.distanceMeters() <= REVERSE_LOOKUP_RADIUS_METERS ? nearest.description() : null;
    }

    /**
     * @param useLikeFallback forces the text branch (v4 only) to use a plain {@code LIKE} scan instead
     *                         of the FTS5 index, catching infix matches that FTS5's prefix matching misses.
     */
    private List<PoiMatch> searchMatches(File poiFile, BoundingBox bounds, String query, NavigationPosition reference,
                                         boolean requireQueryMatch, boolean exactOnly, boolean useLikeFallback) throws IOException {
        if (bounds == null)
            return emptyList();

        List<PoiMatch> matches = new ArrayList<>();
        try (Connection connection = open(poiFile)) {
            Map<Long, SqlRow> rows = readVersion(connection) >= 4 ?
                    searchV4(connection, bounds, query, requireQueryMatch, useLikeFallback) :
                    searchV3(connection, bounds, query, requireQueryMatch);

            for (SqlRow row : rows.values()) {
                LatLong latLong = new LatLong(row.lat(), row.lon());
                List<Tag> tags = parseTags(row.data());
                List<String> categories = parseCategories(row.categories());
                Match tagMatch = findMatch(tags, categories, query, exactOnly);
                if (requireQueryMatch && tagMatch == null)
                    continue;
                if (!requireQueryMatch && !MapsforgeTagMatcher.hasUsefulDescription(tags, categories))
                    continue;

                CategorizedNavigationPosition position = buildDescriptionAndCategory(latLong, tags, categories, tagMatch, "Unnamed POI");
                matches.add(toPoiMatch(position, latLong, reference));
            }
        } catch (SQLException e) {
            throw new IOException("Cannot search Mapsforge POI database " + poiFile, e);
        }
        matches.sort(Comparator.comparingDouble(PoiMatch::distanceMeters).thenComparing(PoiMatch::description));
        return matches;
    }

    private LinkedHashMap<Long, SqlRow> searchV3(Connection connection, BoundingBox bounds, String query, boolean requireQueryMatch) throws SQLException {
        String sql = "SELECT poi_index.id, poi_index.lat, poi_index.lon, poi_data.data, " +
                "group_concat(poi_categories.name, '\r') categories " +
                "FROM poi_index JOIN poi_data ON poi_index.id = poi_data.id " +
                "LEFT JOIN poi_category_map ON poi_index.id = poi_category_map.id " +
                "LEFT JOIN poi_categories ON poi_category_map.category = poi_categories.id " +
                "WHERE poi_index.lat <= ? AND poi_index.lon <= ? AND poi_index.lat >= ? AND poi_index.lon >= ? " +
                (requireQueryMatch ? "AND (lower(poi_data.data) LIKE ? OR lower(poi_categories.name) LIKE ?) " : "") +
                "GROUP BY poi_index.id, poi_index.lat, poi_index.lon, poi_data.data " +
                "LIMIT ?";

        List<Object> params = new ArrayList<>();
        params.add(bounds.northEast().getLatitude());
        params.add(bounds.northEast().getLongitude());
        params.add(bounds.southWest().getLatitude());
        params.add(bounds.southWest().getLongitude());
        if (requireQueryMatch) {
            String pattern = "%" + query + "%";
            params.add(pattern);
            params.add(pattern);
        }
        params.add(MAX_DATABASE_ROWS);
        return runQuery(connection, sql, params.toArray());
    }

    private LinkedHashMap<Long, SqlRow> searchV4(Connection connection, BoundingBox bounds, String query,
                                                 boolean requireQueryMatch, boolean useLikeFallback) throws SQLException {
        double north = bounds.northEast().getLatitude();
        double east = bounds.northEast().getLongitude();
        double south = bounds.southWest().getLatitude();
        double west = bounds.southWest().getLongitude();

        if (!requireQueryMatch)
            return runQuery(connection, Q3_BBOX_SQL, north, east, south, west, MAX_DATABASE_ROWS);

        LinkedHashMap<Long, SqlRow> rows = new LinkedHashMap<>();
        String ftsExpression = useLikeFallback ? null : toFtsMatchExpression(query);
        if (ftsExpression != null && hasFullTextSearchTable(connection))
            mergeInto(rows, runQuery(connection, Q1_FTS_SQL, ftsExpression, north, east, south, west, MAX_DATABASE_ROWS));
        else
            mergeInto(rows, runQuery(connection, Q1_LIKE_SQL, "%" + query + "%", north, east, south, west, MAX_DATABASE_ROWS));

        mergeInto(rows, runQuery(connection, Q2_CATEGORY_SQL, "%" + query + "%", north, east, south, west, MAX_DATABASE_ROWS));
        return rows;
    }

    private void mergeInto(LinkedHashMap<Long, SqlRow> target, Map<Long, SqlRow> source) {
        for (Map.Entry<Long, SqlRow> entry : source.entrySet())
            target.putIfAbsent(entry.getKey(), entry.getValue());
    }

    private LinkedHashMap<Long, SqlRow> runQuery(Connection connection, String sql, Object... params) throws SQLException {
        LinkedHashMap<Long, SqlRow> rows = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param instanceof String string)
                    statement.setString(i + 1, string);
                else if (param instanceof Double number)
                    statement.setDouble(i + 1, number);
                else if (param instanceof Integer number)
                    statement.setInt(i + 1, number);
                else
                    throw new IllegalArgumentException("Unsupported parameter type " + param);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    rows.putIfAbsent(id, new SqlRow(resultSet.getDouble("lat"), resultSet.getDouble("lon"),
                            resultSet.getString("data"), resultSet.getString("categories")));
                }
            }
        }
        return rows;
    }

    private Connection open(File file) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    private PoiMatch toPoiMatch(CategorizedNavigationPosition position, LatLong point, NavigationPosition reference) {
        String description = buildDescription(position.getDescription(), position.getCategory());
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

    record PoiDescriptor(File localFile, slash.navigation.datasources.File remoteFile, BoundingBox boundingBox, int specVersion, String dataSourceName) {
    }

    private record BoundsAndVersion(BoundingBox bounds, int specVersion) {
    }

    private record PoiMatch(CategorizedNavigationPosition position, String description, double distanceMeters) {
    }

    private record SqlRow(double lat, double lon, String data, String categories) {
    }
}

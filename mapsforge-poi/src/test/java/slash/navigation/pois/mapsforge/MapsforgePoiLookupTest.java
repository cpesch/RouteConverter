package slash.navigation.pois.mapsforge;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.geocoding.CategorizedNavigationPosition;

import java.io.File;
import java.sql.*;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.normalize;

public class MapsforgePoiLookupTest {
    private static final BoundingBox MAP_BOUNDS = new BoundingBox(14.0, 53.0, 13.0, 52.0);
    private static final BoundingBox VISIBLE_BOUNDS = new BoundingBox(13.60, 52.60, 13.30, 52.30);
    private static final NavigationPosition CENTER = MAP_BOUNDS.getCenter();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void normalizesQueryText() {
        assertEquals("", normalize(null));
        assertEquals("", normalize("   "));
        assertEquals("new york", normalize(" New_York "));
    }

    @Test
    public void searchesNamesNameVariantsCategoriesAndIgnoresAddressOnlyTags() throws Exception {
        File poiFile = createPoiFile();
        insertPoi(poiFile, 1, 52.5200, 13.4050, tags("name", "Prague", "name:cs", "Praha", "place", "city"));
        insertPoi(poiFile, 2, 52.5210, 13.4100, tags("name", "Tankstelle"), "fuel");
        insertPoi(poiFile, 3, 52.5220, 13.4110, tags("addr:street", "Main Street", "addr:housenumber", "1"));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> nameVariant = lookup.search(poiFile, normalize("Praha"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, nameVariant.size());
        assertEquals("Praha", nameVariant.get(0).getDescription());
        assertEquals("city", categoryOf(nameVariant.get(0)));

        List<CategorizedNavigationPosition> partialName = lookup.search(poiFile, normalize("Prag"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, partialName.size());
        assertEquals("Prague", partialName.get(0).getDescription());
        assertEquals("city", categoryOf(partialName.get(0)));

        List<CategorizedNavigationPosition> category = lookup.search(poiFile, normalize("fuel"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, category.size());
        assertEquals("Tankstelle", category.get(0).getDescription());
        assertEquals("fuel", categoryOf(category.get(0)));

        assertTrue(lookup.search(poiFile, normalize("Main Street"), VISIBLE_BOUNDS, CENTER).isEmpty());
    }

    @Test
    public void reverseLookupReturnsNearestNamedPoiWithinRadius() throws Exception {
        File poiFile = createPoiFile();
        insertPoi(poiFile, 1, 52.5005, 13.4005, tags("name", "Near Place", "place", "hamlet"));
        insertPoi(poiFile, 2, 52.5060, 13.4060, tags("name", "Farther Place", "place", "hamlet"));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        String address = lookup.lookup(poiFile, new SimpleNavigationPosition(13.4000, 52.5000));

        assertEquals("Near Place (hamlet)", address);
    }

    @Test
    public void sortsByDistanceAndLimitsResults() throws Exception {
        File poiFile = createPoiFile();
        for (int i = 0; i < 60; i++) {
            double offset = i * 0.001;
            insertPoi(poiFile, i + 1, CENTER.getLatitude() + offset, CENTER.getLongitude() + offset, tags("name", "Test " + i));
        }
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> results = lookup.search(poiFile, normalize("test"), MAP_BOUNDS, CENTER);

        assertEquals(50, results.size());
        assertEquals("Test 0", results.get(0).getDescription());
        assertEquals("Test 49", results.get(49).getDescription());
    }

    @Test
    public void searchesNamesNameVariantsCategoriesAndIgnoresAddressOnlyTagsV4() throws Exception {
        File poiFile = createPoiFileV4();
        insertPoiV4(poiFile, 1, 52.5200, 13.4050, tags("name", "Prague", "name:cs", "Praha", "place", "city"));
        insertPoiV4(poiFile, 2, 52.5210, 13.4100, tags("name", "Tankstelle"), "fuel");
        insertPoiV4(poiFile, 3, 52.5220, 13.4110, tags("addr:street", "Main Street", "addr:housenumber", "1"));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> nameVariant = lookup.search(poiFile, normalize("Praha"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, nameVariant.size());
        assertEquals("Praha", nameVariant.get(0).getDescription());
        assertEquals("city", categoryOf(nameVariant.get(0)));

        List<CategorizedNavigationPosition> partialName = lookup.search(poiFile, normalize("Prag"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, partialName.size());
        assertEquals("Prague", partialName.get(0).getDescription());
        assertEquals("city", categoryOf(partialName.get(0)));

        List<CategorizedNavigationPosition> category = lookup.search(poiFile, normalize("fuel"), VISIBLE_BOUNDS, CENTER);
        assertEquals(1, category.size());
        assertEquals("Tankstelle", category.get(0).getDescription());
        assertEquals("fuel", categoryOf(category.get(0)));

        assertTrue(lookup.search(poiFile, normalize("Main Street"), VISIBLE_BOUNDS, CENTER).isEmpty());
    }

    @Test
    public void findsInfixMatchViaLikeFallbackOnV4() throws Exception {
        File poiFile = createPoiFileV4();
        insertPoiV4(poiFile, 1, 52.5210, 13.4100, tags("name", "Tankstelle"), "fuel");
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> result = lookup.search(poiFile, normalize("stelle"), VISIBLE_BOUNDS, CENTER);

        assertEquals(1, result.size());
        assertEquals("Tankstelle", result.get(0).getDescription());
    }

    @Test
    public void mergesTextAndCategoryMatchesWithoutDuplicatesOnV4() throws Exception {
        File poiFile = createPoiFileV4();
        insertPoiV4(poiFile, 1, 52.5210, 13.4100, tags("name", "Fuel Depot"), "fuel");
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> result = lookup.search(poiFile, normalize("fuel"), VISIBLE_BOUNDS, CENTER);

        assertEquals(1, result.size());
        assertEquals("Fuel Depot", result.get(0).getDescription());
    }

    @Test
    public void reverseLookupReturnsNearestNamedPoiWithinRadiusV4() throws Exception {
        File poiFile = createPoiFileV4();
        insertPoiV4(poiFile, 1, 52.5005, 13.4005, tags("name", "Near Place", "place", "hamlet"));
        insertPoiV4(poiFile, 2, 52.5060, 13.4060, tags("name", "Farther Place", "place", "hamlet"));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        String address = lookup.lookup(poiFile, new SimpleNavigationPosition(13.4000, 52.5000));

        assertEquals("Near Place (hamlet)", address);
    }

    @Test
    public void sortsByDistanceAndLimitsResultsV4() throws Exception {
        File poiFile = createPoiFileV4();
        for (int i = 0; i < 60; i++) {
            double offset = i * 0.001;
            insertPoiV4(poiFile, i + 1, CENTER.getLatitude() + offset, CENTER.getLongitude() + offset, tags("name", "Test " + i));
        }
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> results = lookup.search(poiFile, normalize("test"), MAP_BOUNDS, CENTER);

        assertEquals(50, results.size());
        assertEquals("Test 0", results.get(0).getDescription());
        assertEquals("Test 49", results.get(49).getDescription());
    }

    @Test
    public void readsBoundsFromMetadata() throws Exception {
        File poiFile = createPoiFileV4();
        insertMetadata(poiFile, "bounds", "52.3,13.3,52.6,13.6");
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        BoundingBox bounds = lookup.readBounds(poiFile);

        assertEquals(52.6, bounds.northEast().getLatitude(), 1e-9);
        assertEquals(13.6, bounds.northEast().getLongitude(), 1e-9);
        assertEquals(52.3, bounds.southWest().getLatitude(), 1e-9);
        assertEquals(13.3, bounds.southWest().getLongitude(), 1e-9);
    }

    @Test
    public void readsBoundsFromRtreeWhenMetadataMissing() throws Exception {
        File poiFile = createPoiFileV4();
        insertPoiV4(poiFile, 1, 52.30, 13.30, tags("name", "Southwest"));
        insertPoiV4(poiFile, 2, 52.60, 13.60, tags("name", "Northeast"));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        BoundingBox bounds = lookup.readBounds(poiFile);

        assertEquals(52.60, bounds.northEast().getLatitude(), 1e-5);
        assertEquals(13.60, bounds.northEast().getLongitude(), 1e-5);
        assertEquals(52.30, bounds.southWest().getLatitude(), 1e-5);
        assertEquals(13.30, bounds.southWest().getLongitude(), 1e-5);
    }

    @Test
    public void treatsMissingVersionAsV3() throws Exception {
        File poiFile = createPoiFile();
        insertPoi(poiFile, 1, 52.5200, 13.4050, tags("name", "Prague"));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> result = lookup.search(poiFile, normalize("Prague"), VISIBLE_BOUNDS, CENTER);

        assertEquals(1, result.size());
        assertEquals("Prague", result.get(0).getDescription());
    }

    @Test
    public void sanitizesQueryWithFtsOperators() throws Exception {
        File poiFile = createPoiFileV4();
        insertPoiV4(poiFile, 1, 52.5200, 13.4050, tags("name", "a-b OR c"));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(mockDataSourceManager());

        List<CategorizedNavigationPosition> result = lookup.search(poiFile, normalize("a-b OR c"), VISIBLE_BOUNDS, CENTER);

        assertNotNull(result);
    }

    @Test
    public void explainPlanForFtsSearchIsDrivenByFts5Index() throws Exception {
        File poiFile = createPoiFileV4();
        insertPoiV4(poiFile, 1, 52.5200, 13.4050, tags("name", "Prague"));

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + poiFile.getAbsolutePath());
             PreparedStatement statement = connection.prepareStatement("EXPLAIN QUERY PLAN " + MapsforgePoiLookup.Q1_FTS_SQL)) {
            statement.setString(1, "\"prague\"*");
            statement.setDouble(2, 90);
            statement.setDouble(3, 180);
            statement.setDouble(4, -90);
            statement.setDouble(5, -180);
            statement.setInt(6, MapsforgePoiLookup.MAX_DATABASE_ROWS);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                assertTrue("plan should scan poi_data_fts first, was: " + resultSet.getString("detail"),
                        resultSet.getString("detail").contains("poi_data_fts"));
            }
        }
    }

    @Test
    public void buildsFtsMatchExpression() {
        assertEquals("\"prag\"*", MapsforgePoiLookup.toFtsMatchExpression("prag"));
        assertEquals("\"new\"* \"york\"*", MapsforgePoiLookup.toFtsMatchExpression("new york"));
        assertEquals("\"st\"* \"peter\"* \"ording\"*", MapsforgePoiLookup.toFtsMatchExpression("st. peter-ording"));
        assertEquals("\"a\"* \"b\"* \"or\"* \"c\"*", MapsforgePoiLookup.toFtsMatchExpression("a-b or c"));
        assertNull(MapsforgePoiLookup.toFtsMatchExpression("---"));
        assertNull(MapsforgePoiLookup.toFtsMatchExpression(""));
        assertNull(MapsforgePoiLookup.toFtsMatchExpression(null));
    }

    private DataSourceManager mockDataSourceManager() {
        DataSourceManager dataSourceManager = mock(DataSourceManager.class);
        when(dataSourceManager.getDataSourceService()).thenReturn(new DataSourceService());
        return dataSourceManager;
    }

    private String categoryOf(NavigationPosition position) {
        return position instanceof CategorizedNavigationPosition categorized ? categorized.getCategory() : null;
    }

    private File createPoiFile() throws Exception {
        File file = temporaryFolder.newFile("test.poi");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath()); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE poi_index (id INTEGER, lat REAL, lon REAL, PRIMARY KEY (id))");
            statement.executeUpdate("CREATE TABLE poi_data (id INTEGER, data TEXT, PRIMARY KEY (id))");
            statement.executeUpdate("CREATE TABLE poi_categories (id INTEGER, name TEXT, parent INTEGER, PRIMARY KEY (id))");
            statement.executeUpdate("CREATE TABLE poi_category_map (id INTEGER, category INTEGER, PRIMARY KEY (id, category))");
        }
        return file;
    }

    private void insertPoi(File file, long id, double latitude, double longitude, String data, String... categories) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath())) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_index VALUES (?, ?, ?)")) {
                statement.setLong(1, id); statement.setDouble(2, latitude); statement.setDouble(3, longitude); statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_data VALUES (?, ?)")) {
                statement.setLong(1, id); statement.setString(2, data); statement.executeUpdate();
            }
            for (int i = 0; i < categories.length; i++) {
                int categoryId = (int) (id * 100 + i);
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_categories VALUES (?, ?, NULL)")) {
                    statement.setInt(1, categoryId); statement.setString(2, categories[i]); statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_category_map VALUES (?, ?)")) {
                    statement.setLong(1, id); statement.setInt(2, categoryId); statement.executeUpdate();
                }
            }
        }
    }

    private File createPoiFileV4() throws Exception {
        File file = temporaryFolder.newFile("test-v4.poi");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath()); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon)");
            statement.executeUpdate("CREATE TABLE poi_data (id INTEGER, data TEXT, PRIMARY KEY (id))");
            statement.executeUpdate("CREATE VIRTUAL TABLE poi_data_fts USING fts5(data, content='poi_data', content_rowid='id')");
            statement.executeUpdate("CREATE TABLE poi_categories (id INTEGER, name TEXT, parent INTEGER, PRIMARY KEY (id))");
            statement.executeUpdate("CREATE TABLE poi_category_map (id INTEGER, category INTEGER, PRIMARY KEY (id, category))");
            statement.executeUpdate("CREATE TABLE metadata (name TEXT, value TEXT)");
            statement.executeUpdate("INSERT INTO metadata VALUES ('version', '4')");
        }
        return file;
    }

    private void insertPoiV4(File file, long id, double latitude, double longitude, String data, String... categories) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath())) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?)")) {
                statement.setLong(1, id);
                statement.setDouble(2, latitude);
                statement.setDouble(3, latitude);
                statement.setDouble(4, longitude);
                statement.setDouble(5, longitude);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_data VALUES (?, ?)")) {
                statement.setLong(1, id); statement.setString(2, data); statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_data_fts(rowid, data) VALUES (?, ?)")) {
                statement.setLong(1, id); statement.setString(2, data); statement.executeUpdate();
            }
            for (int i = 0; i < categories.length; i++) {
                int categoryId = (int) (id * 100 + i);
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_categories VALUES (?, ?, NULL)")) {
                    statement.setInt(1, categoryId); statement.setString(2, categories[i]); statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO poi_category_map VALUES (?, ?)")) {
                    statement.setLong(1, id); statement.setInt(2, categoryId); statement.executeUpdate();
                }
            }
        }
    }

    private void insertMetadata(File file, String name, String value) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
             PreparedStatement statement = connection.prepareStatement("INSERT INTO metadata VALUES (?, ?)")) {
            statement.setString(1, name); statement.setString(2, value); statement.executeUpdate();
        }
    }

    private String tags(String... keyValues) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (builder.length() > 0)
                builder.append('\r');
            builder.append(keyValues[i]).append('=').append(keyValues[i + 1]);
        }
        return builder.toString();
    }
}



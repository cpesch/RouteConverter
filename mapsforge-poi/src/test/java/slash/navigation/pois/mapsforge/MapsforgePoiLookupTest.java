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



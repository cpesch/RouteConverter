package slash.navigation.pois.mapsforge;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.MapDescriptor;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.datasources.binding.DatasourceType;
import slash.navigation.datasources.binding.FileType;
import slash.navigation.datasources.binding.ObjectFactory;
import slash.navigation.datasources.helpers.DataSourceService;
import slash.navigation.datasources.helpers.DataSourcesUtil;
import slash.navigation.datasources.impl.DataSourceImpl;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.geocoding.CategorizedNavigationPosition;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.recursiveDelete;
import static slash.navigation.pois.mapsforge.MapsforgeGeocodingHelper.normalize;

public class MapsforgePoiLookupTest {
    private static final BoundingBox MAP_BOUNDS = new BoundingBox(14.0, 53.0, 13.0, 52.0);
    private static final BoundingBox VISIBLE_BOUNDS = new BoundingBox(13.60, 52.60, 13.30, 52.30);
    private static final NavigationPosition CENTER = MAP_BOUNDS.getCenter();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private final List<File> applicationDirectoriesToDelete = new ArrayList<>();

    @After
    public void deleteApplicationDirectories() throws Exception {
        for (File directory : applicationDirectoriesToDelete)
            recursiveDelete(directory);
    }

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
    public void remoteV4BeatsLocalV3WithIdenticalBoundingBox() {
        MapsforgePoiLookup.PoiDescriptor localV3 = new MapsforgePoiLookup.PoiDescriptor(
                new File("germany.poi"), null, MAP_BOUNDS, 3, "mapsforge-pois");
        MapsforgePoiLookup.PoiDescriptor remoteV4 = new MapsforgePoiLookup.PoiDescriptor(
                null, null, MAP_BOUNDS, 4, "mapsforge-pois-4");

        List<MapsforgePoiLookup.PoiDescriptor> descriptors = sorted(localV3, remoteV4);

        assertEquals(remoteV4, descriptors.get(0));
        assertEquals(localV3, descriptors.get(1));
    }

    @Test
    public void tighterBoundingBoxBeatsHigherSpecVersion() {
        MapsforgePoiLookup.PoiDescriptor smallV3 = new MapsforgePoiLookup.PoiDescriptor(
                new File("region.poi"), null, VISIBLE_BOUNDS, 3, "mapsforge-pois");
        MapsforgePoiLookup.PoiDescriptor largeV4 = new MapsforgePoiLookup.PoiDescriptor(
                null, null, MAP_BOUNDS, 4, "mapsforge-pois-4");

        List<MapsforgePoiLookup.PoiDescriptor> descriptors = sorted(largeV4, smallV3);

        assertEquals(smallV3, descriptors.get(0));
        assertEquals(largeV4, descriptors.get(1));
    }

    @Test
    public void localBeatsRemoteWhenBoundingBoxAndSpecVersionAreEqual() {
        MapsforgePoiLookup.PoiDescriptor local = new MapsforgePoiLookup.PoiDescriptor(
                new File("germany.poi"), null, MAP_BOUNDS, 4, "mapsforge-pois-4");
        MapsforgePoiLookup.PoiDescriptor remote = new MapsforgePoiLookup.PoiDescriptor(
                null, null, MAP_BOUNDS, 4, "mapsforge-pois-4");

        List<MapsforgePoiLookup.PoiDescriptor> descriptors = sorted(remote, local);

        assertEquals(local, descriptors.get(0));
        assertEquals(remote, descriptors.get(1));
    }

    @Test
    public void nullBoundingBoxSortsLastRegardlessOfSpecVersion() {
        MapsforgePoiLookup.PoiDescriptor withoutBounds = new MapsforgePoiLookup.PoiDescriptor(
                new File("world.poi"), null, null, 4, "mapsforge-pois-4");
        MapsforgePoiLookup.PoiDescriptor withBounds = new MapsforgePoiLookup.PoiDescriptor(
                new File("region.poi"), null, MAP_BOUNDS, 3, "mapsforge-pois");

        List<MapsforgePoiLookup.PoiDescriptor> descriptors = sorted(withoutBounds, withBounds);

        assertEquals(withBounds, descriptors.get(0));
        assertEquals(withoutBounds, descriptors.get(1));
    }

    @Test
    public void unmappedDataSourceIdDefaultsToVersion3ForRemoteInferenceAndLocalReadFailure() {
        MapsforgePoiLookup.PoiDescriptor localReadFailure = new MapsforgePoiLookup.PoiDescriptor(
                new File("unreadable.poi"), null, MAP_BOUNDS, 3, "some-local-datasource");
        MapsforgePoiLookup.PoiDescriptor remoteInference = new MapsforgePoiLookup.PoiDescriptor(
                null, null, MAP_BOUNDS, 3, "some-unmapped-datasource");

        List<MapsforgePoiLookup.PoiDescriptor> descriptors = sorted(remoteInference, localReadFailure);

        assertEquals(localReadFailure, descriptors.get(0));
        assertEquals(remoteInference, descriptors.get(1));
    }

    @Test
    public void remoteDataSourceKnownToServeV4IsPreferredOverLocalV3ViaRealDataSourceId() throws Exception {
        String remoteDirectory = "test-pois/" + UUID.randomUUID();
        applicationDirectoriesToDelete.add(getApplicationDirectory(remoteDirectory));
        DataSource remoteDataSource = mock(DataSource.class);
        when(remoteDataSource.getId()).thenReturn("mapsforge-pois-4");
        when(remoteDataSource.getName()).thenReturn("mapsforge-pois-4");
        when(remoteDataSource.getDirectory()).thenReturn(remoteDirectory);
        slash.navigation.datasources.File remoteFile = mock(slash.navigation.datasources.File.class);
        when(remoteFile.getUri()).thenReturn("region.poi");
        when(remoteFile.getBoundingBox()).thenReturn(MAP_BOUNDS);
        when(remoteFile.getDataSource()).thenReturn(remoteDataSource);
        when(remoteDataSource.getFiles()).thenReturn(List.of(remoteFile));

        String localDirectory = "test-pois/" + UUID.randomUUID();
        applicationDirectoriesToDelete.add(getApplicationDirectory(localDirectory));
        DataSource localDataSource = mock(DataSource.class);
        when(localDataSource.getId()).thenReturn("some-local-datasource");
        when(localDataSource.getName()).thenReturn("some-local-datasource");
        when(localDataSource.getDirectory()).thenReturn(localDirectory);
        when(localDataSource.getFiles()).thenReturn(emptyList());
        File localPoiFile = new File(getApplicationDirectory(localDirectory), "germany.poi");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + localPoiFile.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE metadata (name TEXT, value TEXT)");
        }
        insertMetadata(localPoiFile, "bounds", "52,13,53,14");

        DataSourceService dataSourceService = new DataSourceService();
        dataSourceService.getDataSources().add(remoteDataSource);
        dataSourceService.getDataSources().add(localDataSource);
        DataSourceManager dataSourceManager = mock(DataSourceManager.class);
        when(dataSourceManager.getDataSourceService()).thenReturn(dataSourceService);
        when(dataSourceManager.queueForDownload(any(), any())).thenReturn(mock(Download.class));
        when(dataSourceManager.getDownloadManager()).thenReturn(mock(DownloadManager.class));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(dataSourceManager);

        lookup.findPoiFile(MAP_BOUNDS);

        verify(dataSourceManager).queueForDownload(remoteDataSource, remoteFile);
    }

    private List<MapsforgePoiLookup.PoiDescriptor> sorted(MapsforgePoiLookup.PoiDescriptor... descriptors) {
        List<MapsforgePoiLookup.PoiDescriptor> result = new ArrayList<>(List.of(descriptors));
        result.sort(MapsforgePoiLookup.descriptorPreference());
        return result;
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

    @Test
    public void calculatesRemainingDownloadSizeForMissingRemoteFile() throws Exception {
        String directory = "test-pois/" + UUID.randomUUID();
        applicationDirectoriesToDelete.add(getApplicationDirectory(directory));
        DataSource dataSource = poiDataSource(directory, poiFileType("missing.poi", MAP_BOUNDS, 12345L));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(dataSourceManagerWith(dataSource));

        long size = lookup.calculateRemainingDownloadSize(List.of(mapDescriptor(MAP_BOUNDS)));

        assertEquals(12345L, size);
    }

    @Test
    public void calculatesZeroWhenRemoteFileAlreadyExistsLocally() throws Exception {
        String directory = "test-pois/" + UUID.randomUUID();
        applicationDirectoriesToDelete.add(getApplicationDirectory(directory));
        DataSource dataSource = poiDataSource(directory, poiFileType("present.poi", MAP_BOUNDS, 12345L));
        markAsExistingLocally(directory, "present.poi");
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(dataSourceManagerWith(dataSource));

        long size = lookup.calculateRemainingDownloadSize(List.of(mapDescriptor(MAP_BOUNDS)));

        assertEquals(0L, size);
    }

    @Test
    public void calculatesZeroWhenBoundingBoxIntersectsNoRemoteFile() throws Exception {
        String directory = "test-pois/" + UUID.randomUUID();
        applicationDirectoriesToDelete.add(getApplicationDirectory(directory));
        BoundingBox farAway = new BoundingBox(-60.0, -10.0, -61.0, -11.0);
        DataSource dataSource = poiDataSource(directory, poiFileType("far.poi", farAway, 999L));
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(dataSourceManagerWith(dataSource));

        long size = lookup.calculateRemainingDownloadSize(List.of(mapDescriptor(MAP_BOUNDS)));

        assertEquals(0L, size);
    }

    @Test
    public void sumsOnlyMissingFilesAcrossMultipleRemotePoiFiles() throws Exception {
        String directory = "test-pois/" + UUID.randomUUID();
        applicationDirectoriesToDelete.add(getApplicationDirectory(directory));
        DataSource dataSource = poiDataSource(directory,
                poiFileType("missing.poi", MAP_BOUNDS, 500L),
                poiFileType("present.poi", MAP_BOUNDS, 700L));
        markAsExistingLocally(directory, "present.poi");
        MapsforgePoiLookup lookup = new MapsforgePoiLookup(dataSourceManagerWith(dataSource));

        long size = lookup.calculateRemainingDownloadSize(List.of(mapDescriptor(MAP_BOUNDS)));

        assertEquals(500L, size);
    }

    private DataSource poiDataSource(String directory, FileType... fileTypes) {
        ObjectFactory factory = new ObjectFactory();
        DatasourceType datasourceType = factory.createDatasourceType();
        datasourceType.setId("mapsforge-pois");
        datasourceType.setDirectory(directory);
        for (FileType fileType : fileTypes)
            datasourceType.getFile().add(fileType);
        return new DataSourceImpl(datasourceType);
    }

    private FileType poiFileType(String uri, BoundingBox boundingBox, long contentLength) {
        return DataSourcesUtil.createFileType(uri, List.of(new Checksum(null, contentLength, null)), boundingBox);
    }

    private void markAsExistingLocally(String directory, String uri) throws Exception {
        File file = new File(getApplicationDirectory(directory), uri);
        assertTrue(file.createNewFile());
    }

    private MapDescriptor mapDescriptor(BoundingBox boundingBox) {
        return new MapDescriptor() {
            public String getIdentifier() {
                return "test-map";
            }

            public BoundingBox getBoundingBox() {
                return boundingBox;
            }
        };
    }

    private DataSourceManager dataSourceManagerWith(DataSource dataSource) {
        DataSourceService dataSourceService = new DataSourceService();
        dataSourceService.getDataSources().add(dataSource);
        DataSourceManager dataSourceManager = mock(DataSourceManager.class);
        when(dataSourceManager.getDataSourceService()).thenReturn(dataSourceService);
        return dataSourceManager;
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



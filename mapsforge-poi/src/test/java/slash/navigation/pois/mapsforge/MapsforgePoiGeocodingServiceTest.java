package slash.navigation.pois.mapsforge;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;
import slash.navigation.datasources.DataSourceManager;
import slash.navigation.geocoding.GeocodingResult;
import slash.navigation.maps.item.ItemModel;
import slash.navigation.maps.mapsforge.LocalMap;
import slash.navigation.maps.mapsforge.MapType;
import slash.navigation.maps.mapsforge.MapsforgeMapManager;

import java.io.File;
import java.sql.*;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static slash.navigation.maps.mapsforge.MapType.Mapsforge;

public class MapsforgePoiGeocodingServiceTest {
	private static final BoundingBox MAP_BOUNDS = new BoundingBox(14.0, 53.0, 13.0, 52.0);
	private static final BoundingBox VISIBLE_BOUNDS = new BoundingBox(13.60, 52.60, 13.30, 52.30);
	private static final NavigationPosition CENTER = MAP_BOUNDS.getCenter();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void returnsNullWhenNoDisplayedMapExistsAndEmptyListForBlankSearch() throws Exception {
		MapsforgePoiGeocodingService service = new MapsforgePoiGeocodingService(mockDataSourceManager(), mockMapManager(null), () -> VISIBLE_BOUNDS);
		assertNull(service.getPositionsFor("Berlin"));
		assertNull(service.getAddressFor(CENTER));
		assertTrue(service.getPositionsFor("   ").isEmpty());
	}

	@Test
	public void searchesPoiDatabaseWithVisibleFallbackNamesCategoriesAndReverseLookup() throws Exception {
		File poiFile = createPoiFile();
		insertPoi(poiFile, 1, 52.8000, 13.8000, tags("name", "Outside View", "place", "village"));
		insertPoi(poiFile, 2, 52.5200, 13.4050, tags("name", "Prague", "name:cs", "Praha", "place", "city"));
		insertPoi(poiFile, 3, 52.5210, 13.4100, tags("name", "Tankstelle"), "fuel");
		insertPoi(poiFile, 4, 52.5220, 13.4110, tags("addr:street", "Main Street", "addr:housenumber", "1"));
		insertPoi(poiFile, 5, 52.5005, 13.4005, tags("name", "Near Place", "place", "hamlet"));
		MapsforgePoiGeocodingService service = serviceFor(poiFile, VISIBLE_BOUNDS);

		List<GeocodingResult> outside = service.getPositionsFor("Outside");
		assertEquals(1, outside.size());
		assertEquals("Outside View (village)", outside.get(0).position().getDescription());
		assertEquals("test", outside.get(0).geocodingServiceName());

		assertEquals("Praha (city)", service.getPositionsFor("Praha").get(0).position().getDescription());
		assertEquals("Prague (city)", service.getPositionsFor("Prag").get(0).position().getDescription());
		assertEquals("Tankstelle (fuel)", service.getPositionsFor("fuel").get(0).position().getDescription());
		assertTrue(service.getPositionsFor("Main Street").isEmpty());
		assertEquals("Near Place (hamlet)", service.getAddressFor(new SimpleNavigationPosition(13.4000, 52.5000)));
	}

	@Test
	public void sortsByDistanceAndLimitsResults() throws Exception {
		File poiFile = createPoiFile();
		NavigationPosition center = MAP_BOUNDS.getCenter();
		for (int i = 0; i < 60; i++) {
			double offset = i * 0.001;
			insertPoi(poiFile, i + 1, center.getLatitude() + offset, center.getLongitude() + offset, tags("name", "Test " + i));
		}
		List<GeocodingResult> results = serviceFor(poiFile, MAP_BOUNDS).getPositionsFor("test");
		assertEquals(50, results.size());
		assertEquals("Test 0", results.get(0).position().getDescription());
		assertEquals("Test 49", results.get(49).position().getDescription());
	}

	private MapsforgePoiGeocodingService serviceFor(File poiFile, BoundingBox visibleBounds) throws Exception {
		File mapFile = new File(poiFile.getParentFile(), poiFile.getName().replaceFirst("\\.poi$", ".map"));
		assertTrue(mapFile.createNewFile());
		return new MapsforgePoiGeocodingService(mockDataSourceManager(), mockMapManager(new TestLocalMap(MAP_BOUNDS, mapFile.toURI().toString())), () -> visibleBounds);
	}

	private DataSourceManager mockDataSourceManager() {
		return mock(DataSourceManager.class);
	}

	@SuppressWarnings("unchecked")
	private MapsforgeMapManager mockMapManager(LocalMap localMap) {
		MapsforgeMapManager mapsforgeMapManager = mock(MapsforgeMapManager.class);
		ItemModel<LocalMap> displayedMapModel = mock(ItemModel.class);
		when(mapsforgeMapManager.getDisplayedMapModel()).thenReturn(displayedMapModel);
		when(displayedMapModel.getItem()).thenReturn(localMap);
		return mapsforgeMapManager;
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

	private record TestLocalMap(BoundingBox boundingBox, String url) implements LocalMap {
		public MapType getType() { return Mapsforge; }
		public String getProvider() { return "test"; }
		public Integer getZoomLevelMin() { return null; }
		public Integer getZoomLevelMax() { return null; }
		public BoundingBox getBoundingBox() { return boundingBox; }
		public String getCopyrightText() { return null; }
		public void close() { }
		public void delete() { }
		public String description() { return "test"; }
		public String getUrl() { return url; }
	}
}


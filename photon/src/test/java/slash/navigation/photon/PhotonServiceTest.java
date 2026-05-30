package slash.navigation.photon;

import org.geojson.Feature;
import org.geojson.Point;
import org.junit.Test;
import slash.navigation.geocoding.CategorizedNavigationPosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PhotonServiceTest {
	private final PhotonService service = new PhotonService();

	@Test
	public void extractsCategorizedPositionUsingPhotonType() {
		Feature feature = new Feature();
		feature.setGeometry(new Point(13.4050, 52.5200));
		feature.setProperty("name", "Berlin");
		feature.setProperty("city", "Berlin");
		feature.setProperty("country", "Deutschland");
		feature.setProperty("type", "city");

		CategorizedNavigationPosition position = service.extractPosition(feature);

		assertEquals(13.4050, position.getLongitude(), 0.0);
		assertEquals(52.5200, position.getLatitude(), 0.0);
		assertEquals("Berlin, Berlin, Deutschland", position.getDescription());
		assertEquals("city", position.getCategory());
	}

	@Test
	public void fallsBackToOsmValueThenOsmKeyForPhotonCategory() {
		Feature feature = new Feature();
		feature.setGeometry(new Point(13.4050, 52.5200));
		feature.setProperty("name", "Checkpoint");
		feature.setProperty("osm_value", "bus_stop");

		CategorizedNavigationPosition position = service.extractPosition(feature);

		assertEquals("bus_stop", position.getCategory());

		Feature fallbackFeature = new Feature();
		fallbackFeature.setGeometry(new Point(13.4050, 52.5200));
		fallbackFeature.setProperty("name", "Unnamed");
		fallbackFeature.setProperty("osm_key", "highway");

		CategorizedNavigationPosition fallbackPosition = service.extractPosition(fallbackFeature);

		assertEquals("highway", fallbackPosition.getCategory());
	}

	@Test
	public void returnsNullForNonPointPhotonFeatures() {
		Feature feature = new Feature();

		assertNull(service.extractPosition(feature));
	}
}


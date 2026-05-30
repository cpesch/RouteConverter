package slash.navigation.googlemaps;

import org.junit.Test;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.googlemaps.geocode.GeocodeResponse;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GoogleServiceTest {
    private final GoogleService service = new GoogleService();

    @Test
    public void extractsCategorizedPositionFromGeocodeResult() {
        GeocodeResponse.Result result = new GeocodeResponse.Result();
        result.setFormattedAddress("Berlin, Germany");
        result.getType().add("locality");
        result.setGeometry(createGeometry(13.4050, 52.5200));

        CategorizedNavigationPosition position = service.extractAddress(result);

        assertEquals(13.4050, position.getLongitude(), 0.0);
        assertEquals(52.5200, position.getLatitude(), 0.0);
        assertEquals("Berlin, Germany", position.getDescription());
        assertEquals("locality", position.getCategory());
    }

    @Test
    public void leavesGoogleCategoryNullWhenNoTypeExists() {
        GeocodeResponse.Result result = new GeocodeResponse.Result();
        result.setFormattedAddress("Berlin, Germany");
        result.setGeometry(createGeometry(13.4050, 52.5200));

        CategorizedNavigationPosition position = service.extractAddress(result);

        assertNull(position.getCategory());
    }

    private GeocodeResponse.Result.Geometry createGeometry(double longitude, double latitude) {
        GeocodeResponse.Result.Geometry geometry = new GeocodeResponse.Result.Geometry();
        GeocodeResponse.Result.Geometry.Location location = new GeocodeResponse.Result.Geometry.Location();
        location.setLng(BigDecimal.valueOf(longitude));
        location.setLat(BigDecimal.valueOf(latitude));
        geometry.setLocation(location);
        return geometry;
    }
}


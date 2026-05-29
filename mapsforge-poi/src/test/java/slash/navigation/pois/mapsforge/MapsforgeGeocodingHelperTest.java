package slash.navigation.pois.mapsforge;

import org.junit.Test;
import org.mapsforge.core.model.LatLong;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.NavigationPosition;
import slash.navigation.common.SimpleNavigationPosition;

import static org.junit.Assert.*;

public class MapsforgeGeocodingHelperTest {
    @Test
    public void convertsBetweenNavigationPositionAndLatLong() {
        NavigationPosition position = new SimpleNavigationPosition(13.4, 52.5, null, "Berlin");
        LatLong latLong = MapsforgeGeocodingHelper.toLatLong(position);

        assertNotNull(latLong);
        assertEquals(52.5, latLong.latitude, 0.0);
        assertEquals(13.4, latLong.longitude, 0.0);

        NavigationPosition converted = MapsforgeGeocodingHelper.toPosition(latLong, "Berlin");
        assertEquals(13.4, converted.getLongitude(), 0.0);
        assertEquals(52.5, converted.getLatitude(), 0.0);
        assertEquals("Berlin", converted.getDescription());
    }

    @Test
    public void createsBoundsAroundPosition() {
        NavigationPosition position = new SimpleNavigationPosition(13.4, 52.5);

        BoundingBox bounds = MapsforgeGeocodingHelper.createBoundsAround(position, 1000);

        assertTrue(bounds.contains(position));
        assertTrue(bounds.northEast().getLatitude() > position.getLatitude());
        assertTrue(bounds.northEast().getLongitude() > position.getLongitude());
        assertTrue(bounds.southWest().getLatitude() < position.getLatitude());
        assertTrue(bounds.southWest().getLongitude() < position.getLongitude());
    }

    @Test
    public void calculatesDistanceFromReference() {
        NavigationPosition reference = new SimpleNavigationPosition(13.4, 52.5);
        LatLong samePoint = new LatLong(52.5, 13.4);
        LatLong differentPoint = new LatLong(52.501, 13.401);

        assertEquals(0.0, MapsforgeGeocodingHelper.distanceMeters(reference, samePoint), 0.0001);
        assertTrue(MapsforgeGeocodingHelper.distanceMeters(reference, differentPoint) > 0.0);
        assertEquals(0.0, MapsforgeGeocodingHelper.distanceMeters(null, differentPoint), 0.0);
    }
}


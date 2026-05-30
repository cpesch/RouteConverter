package slash.navigation.nominatim;

import org.junit.Test;
import slash.navigation.geocoding.CategorizedNavigationPosition;
import slash.navigation.nominatim.search.PlaceType;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class NominatimServiceTest {
    private final NominatimService service = new NominatimService();

    @Test
    public void extractsCategorizedPositionFromPlaceType() {
        PlaceType place = new PlaceType();
        place.setLon(BigDecimal.valueOf(13.4050));
        place.setLat(BigDecimal.valueOf(52.5200));
        place.setDisplayName("Berlin, Deutschland");
        place.setType("city");

        CategorizedNavigationPosition position = service.extractPosition(place);

        assertEquals(13.4050, position.getLongitude(), 0.0);
        assertEquals(52.5200, position.getLatitude(), 0.0);
        assertEquals("Berlin, Deutschland", position.getDescription());
        assertEquals("city", position.getCategory());
    }

    @Test
    public void leavesCategoryNullWhenNominatimTypeIsBlank() {
        PlaceType place = new PlaceType();
        place.setLon(BigDecimal.valueOf(13.4050));
        place.setLat(BigDecimal.valueOf(52.5200));
        place.setDisplayName("Berlin, Deutschland");
        place.setType("  ");

        CategorizedNavigationPosition position = service.extractPosition(place);

        assertNull(position.getCategory());
    }
}


package slash.navigation.mapview.mapsforge;

import static org.junit.Assert.assertEquals;

import static slash.common.type.HexadecimalNumber.decodeInt;

import java.awt.*;

import org.junit.Test;

public class MapsforceMapViewTest {
    private static final Color ROUTE_COLOR = new Color(decodeInt("C86CB1F3"), true);
    private static final Color TRACK_COLOR = new Color(decodeInt("FF0033FF"), true);
    private MapsforgeMapView view = new MapsforgeMapView();

    @Test
    public void testAsRGBA() {
        assertEquals(-76762637, view.asRGBA(ROUTE_COLOR));
        assertEquals(-16763905, view.asRGBA(TRACK_COLOR));
    }

    @Test
    public void testMinimumAlpha() {
        assertEquals((int)(256 * 0.3) << 24, view.asRGBA(new Color(decodeInt("00000000"), true)), 0.05);
        assertEquals(((int)(256 * 0.3) << 24) | 2 << 16 | 3 << 8 | 4, view.asRGBA(new Color(decodeInt("00020304"), true)), 0.05);
    }
}

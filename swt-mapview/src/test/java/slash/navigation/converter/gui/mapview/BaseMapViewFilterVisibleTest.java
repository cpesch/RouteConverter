package slash.navigation.converter.gui.mapview;

import org.junit.Test;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.Wgs84Position;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.navigation.util.Positions.asPosition;

public class BaseMapViewFilterVisibleTest {
    private BaseMapView mapView = new EclipseSWTMapView() {
        protected NavigationPosition getNorthEastBounds() {
            return asPosition(1.0, 1.0);
        }

        protected NavigationPosition getSouthWestBounds() {
            return asPosition(-1.0, -1.0);
        }
    };

    @Test
    public void testFilterVisiblePosition() throws Exception {
        List<NavigationPosition> positions = new ArrayList<NavigationPosition>();
        Wgs84Position one = asPosition(0.0, 0.0);
        positions.add(one);
        Wgs84Position two = asPosition(0.1, 0.1);
        positions.add(two);
        Wgs84Position threeNotVisible = asPosition(45.0, 45.0);
        positions.add(threeNotVisible);
        Wgs84Position fourNotVisible = asPosition(45.1, 45.1);
        positions.add(fourNotVisible);
        Wgs84Position fiveNotVisible = asPosition(45.1, 45.1);
        positions.add(fiveNotVisible);
        Wgs84Position six = asPosition(0.2, 0.2);
        positions.add(six);

        List<NavigationPosition> result = mapView.filterVisiblePositions(positions, 1.0, false);
        assertEquals(5, result.size());
        assertEquals(one, result.get(0));
        assertEquals(two, result.get(1));
        assertEquals(threeNotVisible, result.get(2));
        assertEquals(fiveNotVisible, result.get(3));
        assertEquals(six, result.get(4));
    }

    @Test
    public void testFilterVisiblePositionIncludingFirstAndLast() throws Exception {
        List<NavigationPosition> positions = new ArrayList<NavigationPosition>();
        Wgs84Position one = asPosition(0.0, 0.0);
        positions.add(one);
        Wgs84Position two = asPosition(0.1, 0.1);
        positions.add(two);
        Wgs84Position threeNotVisible = asPosition(45.0, 45.0);
        positions.add(threeNotVisible);
        Wgs84Position fourNotVisible = asPosition(45.1, 45.1);
        positions.add(fourNotVisible);
        Wgs84Position fiveNotVisible = asPosition(45.1, 45.1);
        positions.add(fiveNotVisible);
        Wgs84Position six = asPosition(0.2, 0.2);
        positions.add(six);
        Wgs84Position seven = asPosition(0.3, 0.3);
        positions.add(seven);

        List<NavigationPosition> result = mapView.filterVisiblePositions(positions, 1.0, true);
        assertEquals(6, result.size());
        assertEquals(one, result.get(0));
        assertEquals(two, result.get(1));
        assertEquals(threeNotVisible, result.get(2));
        assertEquals(fiveNotVisible, result.get(3));
        assertEquals(six, result.get(4));
        assertEquals(seven, result.get(5));
    }
}


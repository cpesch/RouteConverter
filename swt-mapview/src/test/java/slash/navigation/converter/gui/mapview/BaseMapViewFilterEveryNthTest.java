package slash.navigation.converter.gui.mapview;

import org.junit.Test;
import slash.navigation.base.NavigationPosition;
import slash.navigation.base.Wgs84Position;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static slash.navigation.util.Positions.asPosition;

public class BaseMapViewFilterEveryNthTest {
    private BaseMapView mapView = new EclipseSWTMapView();

    private void filterEveryNthPosition(int positionCount, int maximumPositionCount) {
        List<NavigationPosition> positions = new ArrayList<NavigationPosition>();
        Wgs84Position first = asPosition(0.0, 0.0);
        positions.add(first);

        for (int i = 1; i < positionCount - 1; i++)
            positions.add(asPosition(i, 0.0));

        Wgs84Position last = asPosition(positionCount - 1, 0.0);
        positions.add(last);

        List<NavigationPosition> result = mapView.filterEveryNthPosition(positions, maximumPositionCount);

        assertEquals(maximumPositionCount, result.size());
        assertEquals(first, result.get(0));
        double increment = (positionCount - 1) / (double) (maximumPositionCount - 1);
        for (int i = 1; i < maximumPositionCount - 1; i++) {
            assertEquals(asPosition((int)(increment * i + 1.0), 0.0), result.get(i));
        }
        assertEquals(last, result.get(maximumPositionCount - 1));
    }

    @Test
    public void testFilterEveryNthPositionEvenIncrement() throws Exception {
        filterEveryNthPosition(7, 4);
        filterEveryNthPosition(10, 4);
    }

    @Test
    public void testFilterEveryNthPosition() throws Exception {
        filterEveryNthPosition(1001, 17);
    }
}

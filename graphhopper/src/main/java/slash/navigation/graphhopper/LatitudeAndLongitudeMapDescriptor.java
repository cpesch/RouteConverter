package slash.navigation.graphhopper;

import slash.navigation.common.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

class LatitudeAndLongitudeMapDescriptor implements MapDescriptor {
    private final String mapIdentifier;
    private final LongitudeAndLatitude l1;
    private final LongitudeAndLatitude l2;

    public LatitudeAndLongitudeMapDescriptor(String mapIdentifier, LongitudeAndLatitude l1, LongitudeAndLatitude l2) {
        this.mapIdentifier = mapIdentifier;
        this.l1 = l1;
        this.l2 = l2;
    }

    public String getIdentifier() {
        return mapIdentifier;
    }

    private BoundingBox createBoundingBox(List<LongitudeAndLatitude> longitudeAndLatitudes) {
        List<NavigationPosition> positions = new ArrayList<>();
        for (LongitudeAndLatitude longitudeAndLatitude : longitudeAndLatitudes) {
            positions.add(new SimpleNavigationPosition(longitudeAndLatitude.longitude, longitudeAndLatitude.latitude));
        }
        return new BoundingBox(positions);
    }

    public BoundingBox getBoundingBox() {
        return createBoundingBox(asList(l1, l2));
    }

    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        LatitudeAndLongitudeMapDescriptor that = (LatitudeAndLongitudeMapDescriptor) o;
        return mapIdentifier.equals(that.mapIdentifier) && l1.equals(that.l1) && l2.equals(that.l2);
    }

    public int hashCode() {
        int result = mapIdentifier.hashCode();
        result = 31 * result + l1.hashCode();
        result = 31 * result + l2.hashCode();
        return result;
    }

    public String toString() {
        return "LatitudeAndLongitudeMapDescriptor[identifier=" + getIdentifier() + ", boundingBox=" + getBoundingBox() + "]";
    }
}

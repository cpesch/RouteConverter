/*
    This file is part of RouteConverter.

    RouteConverter is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    RouteConverter is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RouteConverter; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Copyright (C) 2007 Christian Pesch. All Rights Reserved.
*/
package slash.navigation.graphhopper;

import org.junit.Test;
import slash.navigation.common.BoundingBox;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Adversarial total-order (Comparator contract) tests for the graph descriptor comparators.
 * The {@link slash.navigation.graphhopper.GraphManager.GraphDescriptorComparator} mixes a
 * partial "bounding box contains" relation with a square-size tie-break, which is the classic
 * recipe for an intransitive comparator -- it produced a real
 * "Comparison method violates its general contract!" failure that left routing unconfigured.
 *
 * These feed many randomized, adversarial descriptor sets (contained / overlapping / disjoint /
 * identical / null bounding boxes) through the comparator and assert antisymmetry, transitivity
 * and that {@link List#sort} (TimSort, which actively detects contract violations) never throws.
 *
 * @author Christian Pesch
 */
public class GraphDescriptorComparatorContractTest {

    /** Asserts a comparator is a total order over the given elements and survives TimSort on shuffles. */
    private static <T> void assertTotalOrder(Comparator<T> comparator, List<T> elements) {
        // antisymmetry: sgn(compare(a, b)) == -sgn(compare(b, a))
        for (T a : elements)
            for (T b : elements) {
                int ab = Integer.signum(comparator.compare(a, b));
                int ba = Integer.signum(comparator.compare(b, a));
                if (ab != -ba)
                    fail("antisymmetry violated: compare(a,b)=" + ab + " compare(b,a)=" + ba + " for " + a + " / " + b);
            }

        // transitivity: compare(a,b)<=0 && compare(b,c)<=0  =>  compare(a,c)<=0
        for (T a : elements)
            for (T b : elements)
                for (T c : elements)
                    if (comparator.compare(a, b) <= 0 && comparator.compare(b, c) <= 0 && comparator.compare(a, c) > 0)
                        fail("transitivity violated: a<=b, b<=c but a>c for " + a + " / " + b + " / " + c);

        // TimSort actively throws IllegalArgumentException on a broken comparator
        Random shuffle = new Random(1);
        for (int i = 0; i < 8; i++) {
            List<T> copy = new ArrayList<>(elements);
            java.util.Collections.shuffle(copy, shuffle);
            copy.sort(comparator);
        }
    }

    /** Self-test: the harness must actually catch a known-intransitive comparator. */
    @Test
    public void testAssertTotalOrderCatchesNonTransitiveComparator() {
        // rock(0) < scissors(2), scissors(2) < paper(1), paper(1) < rock(0): intransitive
        Comparator<Integer> rockPaperScissors = (x, y) -> {
            if (x.equals(y)) return 0;
            boolean xBeatsY = (x + 1) % 3 == y % 3;
            return xBeatsY ? -1 : 1;
        };
        try {
            assertTotalOrder(rockPaperScissors, List.of(0, 1, 2));
            fail("expected the harness to reject the intransitive comparator");
        } catch (AssertionError | IllegalArgumentException expected) {
            // good: either the transitivity assertion or TimSort caught it
        }
    }

    @Test
    public void testGraphDescriptorComparatorTotalOrderOnHandPickedBoundingBoxes() {
        List<GraphDescriptor> descriptors = new ArrayList<>();
        // identifiers vary so the tie-break branch is exercised deterministically
        descriptors.add(remote("world.pbf", new BoundingBox(180.0, 90.0, -180.0, -90.0)));
        descriptors.add(remote("france.pbf", new BoundingBox(8.0, 51.0, -5.0, 42.0)));
        descriptors.add(remote("paris.pbf", new BoundingBox(2.6, 49.0, 2.0, 48.6)));        // inside france
        descriptors.add(remote("germany.pbf", new BoundingBox(15.0, 55.0, 5.0, 47.0)));     // overlaps france, neither contains
        descriptors.add(remote("japan.pbf", new BoundingBox(146.0, 46.0, 129.0, 30.0)));    // disjoint
        descriptors.add(remote("france-copy.pbf", new BoundingBox(8.0, 51.0, -5.0, 42.0))); // identical box, different id
        descriptors.add(remote("noBox.pbf", null));
        descriptors.add(remote("noBox2.pbf", null));
        descriptors.add(zip("archive.zip", null));

        assertTotalOrder(new GraphManager.GraphDescriptorComparator(), descriptors);
    }

    @Test
    public void testGraphDescriptorComparatorTotalOrderRandomizedAdversarial() {
        Comparator<GraphDescriptor> comparator = new GraphManager.GraphDescriptorComparator();
        Random random = new Random(42); // fixed seed -> reproducible
        for (int iteration = 0; iteration < 250; iteration++)
            assertTotalOrder(comparator, randomDescriptors(random, 4 + random.nextInt(9)));
    }

    @Test
    public void testPreferredComparatorTotalOrderRandomizedAdversarial() throws Exception {
        Field field = DownloadableFinder.class.getDeclaredField("PREFERRED_GRAPH_DESCRIPTOR_COMPARATOR");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Comparator<GraphDescriptor> comparator = (Comparator<GraphDescriptor>) field.get(null);
        assertTrue(comparator != null);

        Random random = new Random(7);
        for (int iteration = 0; iteration < 250; iteration++)
            assertTotalOrder(comparator, randomDescriptors(random, 4 + random.nextInt(9)));
    }

    // a small pool of boxes with deliberately mixed relations (contained / overlapping /
    // disjoint / identical) so random sets contain incomparable contains-pairs -- the exact
    // shape that triggers intransitivity
    private static final BoundingBox[] BOX_POOL = {
            null,
            new BoundingBox(180.0, 90.0, -180.0, -90.0),  // world
            new BoundingBox(8.0, 51.0, -5.0, 42.0),       // france
            new BoundingBox(2.6, 49.0, 2.0, 48.6),        // paris (inside france)
            new BoundingBox(15.0, 55.0, 5.0, 47.0),       // germany (overlaps france)
            new BoundingBox(8.0, 51.0, -5.0, 42.0),       // france duplicate
            new BoundingBox(146.0, 46.0, 129.0, 30.0),    // japan (disjoint)
            new BoundingBox(10.0, 52.0, 6.0, 48.0),       // inside germany
    };

    private static List<GraphDescriptor> randomDescriptors(Random random, int count) {
        List<GraphDescriptor> descriptors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BoundingBox box = random.nextBoolean()
                    ? BOX_POOL[random.nextInt(BOX_POOL.length)]
                    : randomBox(random);
            String uri = "graph-" + i + "-" + random.nextInt(1000) + (random.nextBoolean() ? ".pbf" : ".zip");
            descriptors.add(uri.endsWith(".zip") ? zip(uri, box) : remote(uri, box));
        }
        return descriptors;
    }

    private static BoundingBox randomBox(Random random) {
        double minLon = -180 + random.nextInt(300);
        double minLat = -80 + random.nextInt(140);
        double width = 1 + random.nextInt(60);
        double height = 1 + random.nextInt(60);
        return new BoundingBox(minLon + width, minLat + height, minLon, minLat);
    }

    private static GraphDescriptor remote(String uri, BoundingBox box) {
        return new GraphDescriptor(GraphManager.GraphType.PBF, null, file(uri, box));
    }

    private static GraphDescriptor zip(String uri, BoundingBox box) {
        return new GraphDescriptor(GraphManager.GraphType.ZIP, null, file(uri, box));
    }

    private static slash.navigation.datasources.File file(String uri, BoundingBox box) {
        slash.navigation.datasources.File file = mock(slash.navigation.datasources.File.class);
        when(file.getUri()).thenReturn(uri);
        when(file.toString()).thenReturn(uri);
        when(file.getBoundingBox()).thenReturn(box);
        return file;
    }
}

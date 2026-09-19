package slash.navigation.graphhopper;

import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.MapDescriptor;
import slash.navigation.datasources.DataSource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getTemporaryDirectory;

public class DownloadableFinderTest {
    private static final BoundingBox PLANET_SPANNING = new BoundingBox(180.0, 90.0, -180.0, -90.0);
    private static final BoundingBox GERMANY = new BoundingBox(15.0, 55.0, 5.0, 47.0);
    private static final BoundingBox HAMBURG = new BoundingBox(10.5, 54.0, 9.5, 53.3);

    private static MapDescriptor createMapDescriptor() {
        MapDescriptor mapDescriptor = mock(MapDescriptor.class);
        when(mapDescriptor.getIdentifier()).thenReturn("mapsforge/europe/germany/hamburg.map");
        when(mapDescriptor.getBoundingBox()).thenReturn(HAMBURG);
        return mapDescriptor;
    }

    private static slash.navigation.datasources.File createRemoteFile(String uri, BoundingBox boundingBox) {
        slash.navigation.datasources.File file = mock(slash.navigation.datasources.File.class);
        when(file.getUri()).thenReturn(uri);
        when(file.toString()).thenReturn(uri);
        when(file.getBoundingBox()).thenReturn(boundingBox);
        return file;
    }

    private static GraphManager createGraphManager(List<GraphDescriptor> localDescriptors,
                                                   slash.navigation.datasources.File... remoteFiles) throws IOException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getFiles()).thenReturn(asList(remoteFiles));
        return new GraphManager(null, null, dataSource) {
            List<GraphDescriptor> getLocalGraphDescriptors() {
                return localDescriptors;
            }
        };
    }

    private static GraphDescriptor createLocalGraphDescriptorWithInvalidBoundingBox() {
        return new GraphDescriptor(GraphManager.GraphType.PBF,
                new File(getTemporaryDirectory(), "planet-latest.osm.pbf"), null) {
            public BoundingBox getBoundingBox() {
                return PLANET_SPANNING;
            }
        };
    }

    @Test
    public void prefersDescriptorsWithATrustworthyBoundingBox() throws IOException {
        slash.navigation.datasources.File germany = createRemoteFile("germany-latest.osm.pbf", GERMANY);
        GraphManager graphManager = createGraphManager(emptyList(),
                createRemoteFile("planet-latest.osm.pbf", PLANET_SPANNING), germany);

        List<GraphDescriptor> descriptors = new DownloadableFinder(graphManager)
                .getGraphDescriptorsFor(singletonList(createMapDescriptor()));

        assertEquals(1, descriptors.size());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, germany), descriptors.get(0));
    }

    @Test
    public void fallsBackToUntrustworthyBoundingBoxesWhenThereIsNoOtherChoice() throws IOException {
        slash.navigation.datasources.File planet = createRemoteFile("planet-latest.osm.pbf", PLANET_SPANNING);
        GraphManager graphManager = createGraphManager(emptyList(), planet);

        List<GraphDescriptor> descriptors = new DownloadableFinder(graphManager)
                .getGraphDescriptorsFor(singletonList(createMapDescriptor()));

        assertEquals(1, descriptors.size());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, planet), descriptors.get(0));
    }

    @Test
    public void localDescriptorsAreNotFilteredByBoundingBoxValidity() throws IOException {
        GraphDescriptor local = createLocalGraphDescriptorWithInvalidBoundingBox();
        slash.navigation.datasources.File germany = createRemoteFile("germany-latest.osm.pbf", GERMANY);
        GraphManager graphManager = createGraphManager(singletonList(local), germany);

        List<GraphDescriptor> descriptors = new DownloadableFinder(graphManager)
                .getGraphDescriptorsFor(singletonList(createMapDescriptor()));

        assertEquals(2, descriptors.size());
        assertSame(local, descriptors.get(0));
        assertFalse(descriptors.get(0).hasValidBoundingBox());
        assertTrue(descriptors.get(1).hasValidBoundingBox());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, germany), descriptors.get(1));
    }

    @Test
    public void returnsEmptyWhenNothingMatches() throws IOException {
        GraphManager graphManager = createGraphManager(emptyList(),
                createRemoteFile("australia-latest.osm.pbf", new BoundingBox(155.0, -10.0, 112.0, -40.0)));

        List<GraphDescriptor> descriptors = new DownloadableFinder(graphManager)
                .getGraphDescriptorsFor(singletonList(createMapDescriptor()));

        assertTrue(descriptors.isEmpty());
    }
}

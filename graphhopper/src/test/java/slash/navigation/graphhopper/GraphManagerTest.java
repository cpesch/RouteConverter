package slash.navigation.graphhopper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.MapDescriptor;
import slash.navigation.datasources.DataSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.Files.removeExtension;

public class GraphManagerTest {
    private File croatia, france, franceProperties, germany, germanyGraphDirectory, hamburg;
    private GraphManager graphManager;
    private String originalPathPreference;
    private final List<File> createdDirectories = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        graphManager = new GraphManager(null, null, null);
        originalPathPreference = graphManager.getPath();
        java.io.File directory = getTemporaryDirectory();
        croatia = File.createTempFile("croatia", ".pbf", directory);
        france = File.createTempFile("france", ".pbf", directory);
        franceProperties = new File(ensureDirectory(removeExtension(france.getAbsolutePath())), PbfUtil.PROPERTIES);
        assertTrue(franceProperties.createNewFile());
        germany = File.createTempFile("germany", ".pbf", directory);
        germanyGraphDirectory = ensureDirectory(new File(directory, "germany"));
        hamburg = File.createTempFile("hamburg", ".pbf", germanyGraphDirectory);
    }

    @After
    public void tearDown() {
        graphManager.setPath(originalPathPreference);
        assertTrue(croatia.delete());
        assertTrue(france.delete());
        assertTrue(franceProperties.delete());
        assertTrue(franceProperties.getParentFile().delete());
        assertTrue(germany.delete());
        assertTrue(hamburg.delete());
        assertTrue(germanyGraphDirectory.delete());
        for (File directory : createdDirectories) {
            if (directory.exists())
                assertTrue(directory.delete());
        }
    }

    @Test
    public void testLocalOrder() throws IOException {
        DataSource kurviger = mock(DataSource.class);
        GraphManager graphManager = new GraphManager(mock(DataSource.class), kurviger, mock(DataSource.class)) {
            List<File> collectPbfFiles(DataSource dataSource) {
                return asList(hamburg, germany, croatia, france);
            }
            List<File> collectGraphDirectories(DataSource dataSource) {
                return dataSource == kurviger ? singletonList(germanyGraphDirectory) : emptyList();
            }
        };

        System.out.println("local graph descriptors: " + graphManager.getLocalGraphDescriptors());

        List<GraphDescriptor> descriptors = graphManager.getLocalGraphDescriptors();
        assertEquals(5, descriptors.size());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, france, null), descriptors.get(0));
        assertTrue(descriptors.get(0).hasGraphDirectory());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, croatia, null), descriptors.get(1));
        assertFalse(descriptors.get(1).hasGraphDirectory());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, hamburg, null), descriptors.get(2));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, germany, null), descriptors.get(3));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.Directory, germanyGraphDirectory, null), descriptors.get(4));
    }

    @Test
    public void testRemoteOrder() throws IOException {
        slash.navigation.datasources.File croatia = mock(slash.navigation.datasources.File.class);
        when(croatia.getUri()).thenReturn("croatia.pbf");
        when(croatia.toString()).thenReturn("croatia.pbf");

        slash.navigation.datasources.File austria = mock(slash.navigation.datasources.File.class);
        when(austria.getUri()).thenReturn("austria.pbf");
        when(austria.toString()).thenReturn("austria.pbf");

        slash.navigation.datasources.File france = mock(slash.navigation.datasources.File.class);
        when(france.getUri()).thenReturn("france.pbf");
        when(france.toString()).thenReturn("france.pbf");
        when(france.getBoundingBox()).thenReturn(new BoundingBox(1.0, 1.0, -1.0, -1.0));

        slash.navigation.datasources.File paris = mock(slash.navigation.datasources.File.class);
        when(paris.getUri()).thenReturn("paris.pbf");
        when(paris.toString()).thenReturn("paris.pbf");
        when(paris.getBoundingBox()).thenReturn(new BoundingBox(0.2, 0.2, -0.2, -0.2));
        assertTrue(france.getBoundingBox().contains(paris.getBoundingBox()));
        assertFalse(paris.getBoundingBox().contains(france.getBoundingBox()));

        slash.navigation.datasources.File germany = mock(slash.navigation.datasources.File.class);
        when(germany.getUri()).thenReturn("germany.pbf");
        when(germany.toString()).thenReturn("germany.pbf");
        when(germany.getBoundingBox()).thenReturn(new BoundingBox(20.0, 20.0, 5.0, 5.0));
        assertFalse(france.getBoundingBox().contains(germany.getBoundingBox()));
        assertFalse(germany.getBoundingBox().contains(france.getBoundingBox()));
        assertFalse(germany.getBoundingBox().contains(paris.getBoundingBox()));

        slash.navigation.datasources.File zip = mock(slash.navigation.datasources.File.class);
        when(zip.getUri()).thenReturn("croatia.zip");
        when(zip.toString()).thenReturn("croatia.zip");

        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getFiles()).thenReturn(asList(austria, croatia, france, germany, paris, zip));

        GraphManager graphManager = new GraphManager(mock(DataSource.class), mock(DataSource.class), dataSource);

        List<GraphDescriptor> descriptors = graphManager.getRemoteGraphDescriptors();
        assertEquals(6, descriptors.size());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.ZIP, null, zip), descriptors.get(0));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, paris), descriptors.get(1));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, france), descriptors.get(2));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, germany), descriptors.get(3));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, austria), descriptors.get(4));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, croatia), descriptors.get(5));
    }

    @Test
    public void testRemoteComparatorUsesTotalOrderForContainedAndIncomparableBoundingBoxes() {
        slash.navigation.datasources.File france = mock(slash.navigation.datasources.File.class);
        when(france.getUri()).thenReturn("france.pbf");
        when(france.getBoundingBox()).thenReturn(new BoundingBox(1.0, 1.0, -1.0, -1.0));

        slash.navigation.datasources.File paris = mock(slash.navigation.datasources.File.class);
        when(paris.getUri()).thenReturn("paris.pbf");
        when(paris.getBoundingBox()).thenReturn(new BoundingBox(0.2, 0.2, -0.2, -0.2));

        slash.navigation.datasources.File germany = mock(slash.navigation.datasources.File.class);
        when(germany.getUri()).thenReturn("germany.pbf");
        when(germany.getBoundingBox()).thenReturn(new BoundingBox(20.0, 20.0, 5.0, 5.0));

        GraphDescriptor descriptorFrance = new GraphDescriptor(GraphManager.GraphType.PBF, null, france);
        GraphDescriptor descriptorParis = new GraphDescriptor(GraphManager.GraphType.PBF, null, paris);
        GraphDescriptor descriptorGermany = new GraphDescriptor(GraphManager.GraphType.PBF, null, germany);

        GraphManager.GraphDescriptorComparator comparator = new GraphManager.GraphDescriptorComparator();

        assertTrue(comparator.compare(descriptorParis, descriptorFrance) < 0);
        assertTrue(comparator.compare(descriptorFrance, descriptorGermany) < 0);
        assertTrue(comparator.compare(descriptorParis, descriptorGermany) < 0);
    }

    @Test
    public void testNullDirectoryPreferenceFallsBackToApplicationDirectory() throws IOException {
        String directoryName = "graph-manager-test-" + UUID.randomUUID();
        GraphManager graphManager = new GraphManager(null, null, null) {
            @Override
            public String getPath() {
                return null;
            }
        };

        File directory = graphManager.getDirectory(createDataSource(directoryName));
        createdDirectories.add(directory);

        assertEquals(getApplicationDirectory(directoryName).getAbsolutePath(), directory.getAbsolutePath());
    }

    @Test
    public void testEmptyDirectoryPreferenceFallsBackToApplicationDirectory() {
        String directoryName = "graph-manager-test-" + UUID.randomUUID();
        graphManager.setPath("");

        File directory = graphManager.getDirectory(createDataSource(directoryName));
        createdDirectories.add(directory);

        assertEquals(getApplicationDirectory(directoryName).getAbsolutePath(), directory.getAbsolutePath());
    }

    @Test
    public void testBlankDirectoryPreferenceFallsBackToApplicationDirectory() {
        String directoryName = "graph-manager-test-" + UUID.randomUUID();
        graphManager.setPath("   ");

        File directory = graphManager.getDirectory(createDataSource(directoryName));
        createdDirectories.add(directory);

        assertEquals(getApplicationDirectory(directoryName).getAbsolutePath(), directory.getAbsolutePath());
    }

    @Test
    public void testMissingDirectoryPreferenceFallsBackToApplicationDirectory() {
        String directoryName = "graph-manager-test-" + UUID.randomUUID();
        graphManager.setPath(new File(getTemporaryDirectory(), "missing-" + UUID.randomUUID()).getAbsolutePath());

        File directory = graphManager.getDirectory(createDataSource(directoryName));
        createdDirectories.add(directory);

        assertEquals(getApplicationDirectory(directoryName).getAbsolutePath(), directory.getAbsolutePath());
    }

    @Test
    public void testExistingDirectoryPreferenceIsUsed() {
        File existingDirectory = ensureDirectory(new File(getTemporaryDirectory(), "graph-manager-existing-" + UUID.randomUUID()));
        createdDirectories.add(existingDirectory);
        graphManager.setPath(existingDirectory.getAbsolutePath());

        File directory = graphManager.getDirectory(createDataSource("unused"));

        assertEquals(existingDirectory.getAbsolutePath(), directory.getAbsolutePath());
    }

    @Test
    public void testMatchesReturnsFalseForMapWithoutBoundingBoxInsteadOfThrowing() {
        slash.navigation.datasources.File germany = mock(slash.navigation.datasources.File.class);
        when(germany.getUri()).thenReturn("germany.pbf");
        when(germany.getBoundingBox()).thenReturn(new BoundingBox(20.0, 20.0, 5.0, 5.0));

        GraphDescriptor descriptor = new GraphDescriptor(GraphManager.GraphType.PBF, null, germany);

        MapDescriptor mapDescriptorWithoutBoundingBox = mock(MapDescriptor.class);
        when(mapDescriptorWithoutBoundingBox.getIdentifier()).thenReturn("unrelated-map");
        when(mapDescriptorWithoutBoundingBox.getBoundingBox()).thenReturn(null);

        assertFalse(descriptor.matches(mapDescriptorWithoutBoundingBox));
    }

    private static DataSource createDataSource(String directoryName) {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getDirectory()).thenReturn(directoryName);
        return dataSource;
    }
}

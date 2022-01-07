package slash.navigation.graphhopper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.datasources.DataSource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.Files.removeExtension;

public class GraphManagerTest {
    private File croatia, france, franceProperties, germany, germanyGraphDirectory, hamburg;

    @Before
    public void setUp() throws Exception {
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
        assertTrue(croatia.delete());
        assertTrue(france.delete());
        assertTrue(franceProperties.delete());
        assertTrue(franceProperties.getParentFile().delete());
        assertTrue(germany.delete());
        assertTrue(hamburg.delete());
        assertTrue(germanyGraphDirectory.delete());
    }

    @Test
    public void testLocalOrder() throws IOException {
        GraphManager graphManager = new GraphManager(singletonList(mock(DataSource.class))) {
            List<File> collectPbfFiles() {
                return asList(hamburg, germany, croatia, france);
            }
            List<File> collectGraphDirectories() {
                return singletonList(germanyGraphDirectory);
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
        slash.navigation.datasources.File france = mock(slash.navigation.datasources.File.class);

        when(france.getUri()).thenReturn("france.pbf");
        when(france.getBoundingBox()).thenReturn(new BoundingBox(1.0, 1.0, -1.0, -1.0));
        slash.navigation.datasources.File paris = mock(slash.navigation.datasources.File.class);
        when(paris.getUri()).thenReturn("paris.pbf");
        when(paris.getBoundingBox()).thenReturn(new BoundingBox(0.2, 0.2, -0.2, -0.2));
        assertTrue(france.getBoundingBox().contains(paris.getBoundingBox()));
        assertFalse(paris.getBoundingBox().contains(france.getBoundingBox()));

        slash.navigation.datasources.File germany = mock(slash.navigation.datasources.File.class);
        when(germany.getUri()).thenReturn("germany.pbf");
        when(germany.getBoundingBox()).thenReturn(new BoundingBox(10.0, 10.0, 7.0, 7.0));
        assertFalse(france.getBoundingBox().contains(germany.getBoundingBox()));
        assertFalse(germany.getBoundingBox().contains(france.getBoundingBox()));
        assertFalse(germany.getBoundingBox().contains(paris.getBoundingBox()));

        slash.navigation.datasources.File zip = mock(slash.navigation.datasources.File.class);
        when(zip.getUri()).thenReturn("croatia.zip");

        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getFiles()).thenReturn(asList(croatia, france, germany, paris, zip));

        GraphManager graphManager = new GraphManager(singletonList(dataSource));

        List<GraphDescriptor> descriptors = graphManager.getRemoteGraphDescriptors();
        assertEquals(5, descriptors.size());
        assertEquals(new GraphDescriptor(GraphManager.GraphType.ZIP, null, zip), descriptors.get(0));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, germany), descriptors.get(1));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, paris), descriptors.get(2));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, france), descriptors.get(3));
        assertEquals(new GraphDescriptor(GraphManager.GraphType.PBF, null, croatia), descriptors.get(4));
    }
}

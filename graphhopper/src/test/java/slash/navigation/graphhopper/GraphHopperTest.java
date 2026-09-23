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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.datasources.DataSource;
import slash.navigation.download.Action;
import slash.navigation.download.Checksum;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.recursiveDelete;

public class GraphHopperTest {
    private static final String MALA_FATRA_URI = "europe/slovakia/mala-fatra-latest.osm.pbf";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    // the production code resolves graph files below the application directory, so use a name
    // unique per test and delete whatever the test created below it again in tearDown()
    private String dataSourceDirectoryName;

    @Before
    public void setUp() {
        GraphHopper.TEST_MODE = true;

        dataSourceDirectoryName = "graphhopper-test-" + UUID.randomUUID();
    }

    @After
    public void tearDown() throws IOException {
        File directory = new File(getApplicationDirectory(), dataSourceDirectoryName);
        if (directory.exists())
            recursiveDelete(directory);
    }

    // rc#105: a graph already loaded for one region must not make GraphHopper believe a later,
    // geographically distant route is already covered. isRequiresDownload() has to check the
    // graph descriptor computed for the CURRENT route (next), not whatever osmPbfFile/hopper
    // happens to still be loaded from an earlier, unrelated route.
    @Test
    public void isRequiresDownloadChecksTheGraphNeededForTheCurrentRouteNotTheOneAlreadyLoaded() throws IOException {
        slash.navigation.datasources.File malaFatraFile = mock(slash.navigation.datasources.File.class);
        when(malaFatraFile.getUri()).thenReturn(MALA_FATRA_URI);
        when(malaFatraFile.getBoundingBox()).thenReturn(new BoundingBox(19.5, 49.6, 18.5, 48.9));

        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn(dataSourceDirectoryName);
        when(graphHopperDataSource.getAction()).thenReturn(Action.Copy.name());
        when(graphHopperDataSource.getBaseUrl()).thenReturn("http://download.geofabrik.de/");
        when(graphHopperDataSource.getFiles()).thenReturn(singletonList(malaFatraFile));
        when(malaFatraFile.getDataSource()).thenReturn(graphHopperDataSource);

        GraphHopper hopper = new GraphHopper(new DownloadManager(temporaryFolder.newFile("queueFile.xml")));
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);

        // simulate a graph for a distant, unrelated region that is already loaded from an
        // earlier route drawn in this session -- the file still exists on disk
        File previouslyLoadedFile = temporaryFolder.newFile("denmark-latest.osm.pbf");
        hopper.setOsmPbfFile(previouslyLoadedFile);

        // now route through Mala Fatra, Slovakia: nowhere near the previously loaded graph
        // and not downloaded yet
        DownloadFuture future = hopper.downloadRoutingDataFor("test-map", asList(
                new LongitudeAndLatitude(19.0663111, 49.2578493),
                new LongitudeAndLatitude(19.0763111, 49.2678493)));

        assertTrue("GraphHopper must recognize it needs to download the graph for the route's " +
                        "actual region instead of trusting the previously loaded, unrelated graph",
                future.isRequiresDownload());
    }

    // rc#105: when the graph descriptor computed for the current route is already fully present
    // locally, isRequiresDownload() must not just report "no download needed" -- it must also
    // switch osmPbfFile to that graph so getRouteBetween()'s initializeHopper() call loads it,
    // instead of leaving the stale, previously loaded graph in place.
    @Test
    public void isRequiresDownloadSwitchesToAnAlreadyPresentGraphForTheCurrentRoute() throws IOException {
        slash.navigation.datasources.File malaFatraFile = mock(slash.navigation.datasources.File.class);
        when(malaFatraFile.getUri()).thenReturn(MALA_FATRA_URI);
        when(malaFatraFile.getBoundingBox()).thenReturn(new BoundingBox(19.5, 49.6, 18.5, 48.9));

        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn(dataSourceDirectoryName);
        when(graphHopperDataSource.getAction()).thenReturn(Action.Copy.name());
        when(graphHopperDataSource.getBaseUrl()).thenReturn("http://download.geofabrik.de/");
        when(graphHopperDataSource.getFiles()).thenReturn(singletonList(malaFatraFile));
        when(malaFatraFile.getDataSource()).thenReturn(graphHopperDataSource);

        // the file GraphHopper resolves for the Mala Fatra graph descriptor is already fully
        // downloaded at the location the production code would compute for it
        File malaFatraLocalFile = new File(getApplicationDirectory(dataSourceDirectoryName), MALA_FATRA_URI);
        ensureDirectory(malaFatraLocalFile.getParentFile());
        assertTrue(malaFatraLocalFile.createNewFile());

        GraphHopper hopper = new GraphHopper(new DownloadManager(temporaryFolder.newFile("queueFile.xml")));
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);

        // simulate a graph for a distant, unrelated region that is already loaded from an
        // earlier route drawn in this session -- the file still exists on disk
        File previouslyLoadedFile = temporaryFolder.newFile("denmark-latest.osm.pbf");
        hopper.setOsmPbfFile(previouslyLoadedFile);

        // now route through Mala Fatra, Slovakia, whose graph is already present locally
        DownloadFuture future = hopper.downloadRoutingDataFor("test-map", asList(
                new LongitudeAndLatitude(19.0663111, 49.2578493),
                new LongitudeAndLatitude(19.0763111, 49.2678493)));

        assertFalse("GraphHopper must not require a download when the graph for the route's " +
                        "actual region is already present locally",
                future.isRequiresDownload());
        assertEquals("GraphHopper must switch osmPbfFile to the already present graph for the " +
                        "current route instead of leaving the previously loaded graph in place",
                malaFatraLocalFile, hopper.getOsmPbfFile());
    }

    // rc#178: calculateRemainingDownloadSize(...) == 0 used to double as "covered", but it is also
    // 0 when GraphHopper has nothing published for the region at all -- the headline regression,
    // green-on-nothing. isRoutingDataAvailable() must tell the two apart.
    @Test
    public void isRoutingDataAvailableIsFalseWhenNoGraphIsPublishedForTheRegion() throws IOException {
        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn(dataSourceDirectoryName);
        when(graphHopperDataSource.getFiles()).thenReturn(emptyList());

        GraphHopper hopper = new GraphHopper(new DownloadManager(temporaryFolder.newFile("queueFile.xml")));
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);

        MapDescriptor mapDescriptor = mock(MapDescriptor.class);
        when(mapDescriptor.getIdentifier()).thenReturn("test-map");
        when(mapDescriptor.getBoundingBox()).thenReturn(new BoundingBox(19.5, 49.6, 18.5, 48.9));

        assertFalse("No graph descriptor at all for the region must not be reported as available",
                hopper.isRoutingDataAvailable(mapDescriptor));
    }

    @Test
    public void isRoutingDataAvailableIsFalseWhenTheRemoteFileHasNoChecksum() throws IOException {
        slash.navigation.datasources.File malaFatraFile = mock(slash.navigation.datasources.File.class);
        when(malaFatraFile.getUri()).thenReturn(MALA_FATRA_URI);
        when(malaFatraFile.getBoundingBox()).thenReturn(new BoundingBox(19.5, 49.6, 18.5, 48.9));
        // getLatestChecksum() is left unstubbed, i.e. null -- unknown content length

        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn(dataSourceDirectoryName);
        when(graphHopperDataSource.getFiles()).thenReturn(singletonList(malaFatraFile));
        when(malaFatraFile.getDataSource()).thenReturn(graphHopperDataSource);

        GraphHopper hopper = new GraphHopper(new DownloadManager(temporaryFolder.newFile("queueFile.xml")));
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);

        MapDescriptor mapDescriptor = mock(MapDescriptor.class);
        when(mapDescriptor.getIdentifier()).thenReturn("test-map");
        when(mapDescriptor.getBoundingBox()).thenReturn(new BoundingBox(19.2, 49.5, 18.7, 49.0));

        assertFalse("A remote file without a checksum must not be reported as available",
                hopper.isRoutingDataAvailable(mapDescriptor));
    }

    @Test
    public void isRoutingDataAvailableIsFalseWhenTheGraphIsPublishedButNotDownloaded() throws IOException {
        slash.navigation.datasources.File malaFatraFile = mock(slash.navigation.datasources.File.class);
        when(malaFatraFile.getUri()).thenReturn(MALA_FATRA_URI);
        when(malaFatraFile.getBoundingBox()).thenReturn(new BoundingBox(19.5, 49.6, 18.5, 48.9));
        Checksum checksum = mock(Checksum.class);
        when(checksum.getContentLength()).thenReturn(1234L);
        when(malaFatraFile.getLatestChecksum()).thenReturn(checksum);

        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn(dataSourceDirectoryName);
        when(graphHopperDataSource.getFiles()).thenReturn(singletonList(malaFatraFile));
        when(malaFatraFile.getDataSource()).thenReturn(graphHopperDataSource);

        GraphHopper hopper = new GraphHopper(new DownloadManager(temporaryFolder.newFile("queueFile.xml")));
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);

        MapDescriptor mapDescriptor = mock(MapDescriptor.class);
        when(mapDescriptor.getIdentifier()).thenReturn("test-map");
        when(mapDescriptor.getBoundingBox()).thenReturn(new BoundingBox(19.2, 49.5, 18.7, 49.0));

        assertFalse("A graph that is published but not yet downloaded must not be reported as available",
                hopper.isRoutingDataAvailable(mapDescriptor));
    }

    @Test
    public void isRoutingDataAvailableIsTrueWhenTheGraphDirectoryExistsLocally() throws IOException {
        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn(dataSourceDirectoryName);

        // a graph directory ("mala-fatra") with its properties marker file, sitting next to the
        // PBF it was imported from -- the genuine "already downloaded and processed" case
        File graphHopperDirectory = getApplicationDirectory(dataSourceDirectoryName);
        File pbfFile = new File(graphHopperDirectory, "mala-fatra-latest.osm.pbf");
        assertTrue(pbfFile.createNewFile());
        File graphDirectory = ensureDirectory(new File(graphHopperDirectory, "mala-fatra"));
        assertTrue(new File(graphDirectory, "properties").createNewFile());

        GraphHopper hopper = new GraphHopper(new DownloadManager(temporaryFolder.newFile("queueFile.xml")));
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);

        MapDescriptor mapDescriptor = mock(MapDescriptor.class);
        when(mapDescriptor.getIdentifier()).thenReturn("mala-fatra");

        assertTrue("A graph directory that already exists locally must be reported as available",
                hopper.isRoutingDataAvailable(mapDescriptor));
    }
}

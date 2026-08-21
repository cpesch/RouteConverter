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
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.datasources.DataSource;
import slash.navigation.download.Action;
import slash.navigation.download.DownloadManager;
import slash.navigation.routing.DownloadFuture;

import java.io.File;
import java.io.IOException;

import static java.io.File.createTempFile;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphHopperTest {
    private static final String MALA_FATRA_URI = "europe/slovakia/mala-fatra-latest.osm.pbf";

    // rc#105: a graph already loaded for one region must not make GraphHopper believe a later,
    // geographically distant route is already covered. isRequiresDownload() has to check the
    // graph descriptor computed for the CURRENT route (next), not whatever osmPbfFile/hopper
    // happens to still be loaded from an earlier, unrelated route.
    @Test
    public void isRequiresDownloadChecksTheGraphNeededForTheCurrentRouteNotTheOneAlreadyLoaded() throws IOException {
        GraphHopper.TEST_MODE = true;

        slash.navigation.datasources.File malaFatraFile = mock(slash.navigation.datasources.File.class);
        when(malaFatraFile.getUri()).thenReturn(MALA_FATRA_URI);
        when(malaFatraFile.getBoundingBox()).thenReturn(new BoundingBox(19.5, 49.6, 18.5, 48.9));

        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn("graphhopper-test-" + hashCode());
        when(graphHopperDataSource.getAction()).thenReturn(Action.Copy.name());
        when(graphHopperDataSource.getBaseUrl()).thenReturn("http://download.geofabrik.de/");
        when(graphHopperDataSource.getFiles()).thenReturn(singletonList(malaFatraFile));
        when(malaFatraFile.getDataSource()).thenReturn(graphHopperDataSource);

        GraphHopper hopper = new GraphHopper(new DownloadManager(createTempFile("queueFile", ".xml")));
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);

        // simulate a graph for a distant, unrelated region that is already loaded from an
        // earlier route drawn in this session -- the file still exists on disk
        File previouslyLoadedFile = createTempFile("denmark-latest", ".osm.pbf");
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
}

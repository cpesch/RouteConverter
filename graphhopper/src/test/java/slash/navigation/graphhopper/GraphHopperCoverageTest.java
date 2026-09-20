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
import org.junit.Test;
import slash.navigation.common.BoundingBox;
import slash.navigation.common.LongitudeAndLatitude;
import slash.navigation.common.MapDescriptor;
import slash.navigation.common.Polygon;
import slash.navigation.datasources.DataSource;
import slash.navigation.download.Action;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.Files.recursiveDelete;

public class GraphHopperCoverageTest {
    private static final String GERMANY_URI = "europe/germany-latest.osm.pbf";
    private static final String GERMANY_POLY_URI = "europe/germany.poly";
    private static final String BASE_URL = "http://download.geofabrik.de/";

    private String dataSourceDirectoryName;
    private DownloadManager downloadManager;
    private MapDescriptor mapDescriptor;

    @Before
    public void setUp() {
        GraphHopper.TEST_MODE = true;
        dataSourceDirectoryName = "graphhopper-coverage-test-" + UUID.randomUUID();
        downloadManager = mock(DownloadManager.class);
        mapDescriptor = new LatitudeAndLongitudeMapDescriptor("test-map",
                new LongitudeAndLatitude(10.0, 51.0), new LongitudeAndLatitude(10.1, 51.1));
    }

    @After
    public void tearDown() throws IOException {
        File directory = new File(getApplicationDirectory(), dataSourceDirectoryName);
        if (directory.exists())
            recursiveDelete(directory);
    }

    private GraphHopper createHopper() throws IOException {
        slash.navigation.datasources.File germanyFile = mock(slash.navigation.datasources.File.class);
        when(germanyFile.getUri()).thenReturn(GERMANY_URI);
        when(germanyFile.getBoundingBox()).thenReturn(new BoundingBox(15.0, 55.0, 5.0, 47.0));

        DataSource graphHopperDataSource = mock(DataSource.class);
        when(graphHopperDataSource.getDirectory()).thenReturn(dataSourceDirectoryName);
        when(graphHopperDataSource.getAction()).thenReturn(Action.Copy.name());
        when(graphHopperDataSource.getBaseUrl()).thenReturn(BASE_URL);
        when(graphHopperDataSource.getFiles()).thenReturn(singletonList(germanyFile));
        when(germanyFile.getDataSource()).thenReturn(graphHopperDataSource);

        GraphHopper hopper = new GraphHopper(downloadManager);
        hopper.setDataSources(mock(DataSource.class), mock(DataSource.class), graphHopperDataSource);
        return hopper;
    }

    private File expectedPolyFile() {
        return new File(getApplicationDirectory(dataSourceDirectoryName), GERMANY_POLY_URI);
    }

    private static void writePolygon(File file) throws IOException {
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
            throw new IOException("Cannot create " + file.getParentFile());
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(("none\n1\n" +
                    "5.0 47.0\n" +
                    "15.0 47.0\n" +
                    "15.0 55.0\n" +
                    "5.0 55.0\n" +
                    "5.0 47.0\n" +
                    "END\nEND\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Test
    public void getRoutingCoverageReadsOnDiskPolygonWithoutDownloading() throws IOException {
        writePolygon(expectedPolyFile());

        GraphHopper hopper = createHopper();
        Polygon polygon = hopper.getRoutingCoverage(mapDescriptor, null);

        assertNotNull("An on-disk .poly must be parsed without a download", polygon);
        verify(downloadManager, never()).queueForDownload(anyString(), anyString(), any(), any(), any());
    }

    @Test
    public void getRoutingCoverageCachesInMemoryAfterFirstRead() throws IOException {
        writePolygon(expectedPolyFile());

        GraphHopper hopper = createHopper();
        Polygon first = hopper.getRoutingCoverage(mapDescriptor, null);
        assertNotNull(first);

        // remove the file: a second call must still return the cached polygon, not re-read the file
        assertTrue(expectedPolyFile().delete());
        Polygon second = hopper.getRoutingCoverage(mapDescriptor, null);

        assertSame("A cache hit must skip re-reading the file", first, second);
        verify(downloadManager, never()).queueForDownload(anyString(), anyString(), any(), any(), any());
    }

    @Test
    public void getRoutingCoverageCachesNegativeResultAndNeverFiresOnAvailableWhenDownloadFails() throws IOException, InterruptedException {
        Download download = new Download("Coverage", BASE_URL + GERMANY_POLY_URI, Action.Copy,
                new FileAndChecksum(expectedPolyFile(), null), null);
        when(downloadManager.queueForDownload(anyString(), anyString(), eq(Action.Copy), any(), isNull()))
                .thenReturn(download);
        // waitForCompletion() is a no-op mock: simulates a download that never produces a file

        GraphHopper hopper = createHopper();
        CountDownLatch onAvailable = new CountDownLatch(1);
        Polygon first = hopper.getRoutingCoverage(mapDescriptor, onAvailable::countDown);
        assertNull("No cache yet, so the first call must return null while the download is in flight", first);

        verify(downloadManager, timeout(2000)).queueForDownload(anyString(), anyString(), eq(Action.Copy), any(), isNull());
        verify(downloadManager, timeout(2000)).waitForCompletion(any());

        assertFalse("onAvailable must not fire when no polygon becomes available",
                onAvailable.await(300, TimeUnit.MILLISECONDS));

        // further calls must not trigger another download attempt
        for (int i = 0; i < 5; i++)
            assertNull(hopper.getRoutingCoverage(mapDescriptor, onAvailable::countDown));
        verify(downloadManager, after(200).times(1))
                .queueForDownload(anyString(), anyString(), eq(Action.Copy), any(), isNull());
    }

    @Test
    public void getRoutingCoverageFetchesInBackgroundAndFiresOnAvailableOnceThePolygonArrives() throws IOException, InterruptedException {
        File polyFile = expectedPolyFile();
        Download download = new Download("Coverage", BASE_URL + GERMANY_POLY_URI, Action.Copy,
                new FileAndChecksum(polyFile, null), null);
        when(downloadManager.queueForDownload(anyString(), anyString(), eq(Action.Copy), any(), isNull()))
                .thenReturn(download);
        // simulate a successful background download: waitForCompletion() writes the target file
        doAnswer(invocation -> {
            writePolygon(polyFile);
            return null;
        }).when(downloadManager).waitForCompletion(any());

        GraphHopper hopper = createHopper();
        CountDownLatch onAvailable = new CountDownLatch(1);
        Polygon first = hopper.getRoutingCoverage(mapDescriptor, onAvailable::countDown);
        assertNull("No cache yet, so the first call must return null while the download is in flight", first);

        assertTrue("onAvailable must fire once the polygon becomes available",
                onAvailable.await(2, TimeUnit.SECONDS));

        Polygon second = hopper.getRoutingCoverage(mapDescriptor, null);
        assertNotNull("The polygon fetched in the background must be cached and returned afterwards", second);

        verify(downloadManager, after(200).times(1))
                .queueForDownload(anyString(), anyString(), eq(Action.Copy), any(), isNull());
    }
}

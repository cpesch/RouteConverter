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
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;

import java.io.File;
import java.io.IOException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getTemporaryDirectory;
import static slash.common.io.Files.recursiveDelete;

public class DownloadableFinderIT {
    private static final String SMALL_URI = "small";
    private static final String MEDIUM_URI = "medium";
    private static final String LARGE_URI = "large";

    private DownloadableFinder finder;
    private File temporaryDirectory;

    @Before
    public void setUp() {
        temporaryDirectory = getTemporaryDirectory();
    }

    @After
    public void tearDown() throws IOException {
        recursiveDelete(temporaryDirectory);
    }

    @Test
    public void testSelectTheBoundingBox() {
        DataSource dataSource = mock(DataSource.class);
        slash.navigation.datasources.File small = mock(slash.navigation.datasources.File.class);
        when(small.getBoundingBox()).thenReturn(new BoundingBox(0.2, 0.2, -0.2, -0.2));
        when(small.getUri()).thenReturn(SMALL_URI);
        when(dataSource.getFiles()).thenReturn(singletonList(small));
        finder = new DownloadableFinder(dataSource, temporaryDirectory);
        finder.setDataSource(dataSource);

        Downloadable downloadable = finder.getDownloadableFor(new BoundingBox(0.1, 0.1, -0.1, -0.1));
        assertEquals(SMALL_URI, downloadable.getUri());
    }

    @Test
    public void testSameCenterSelectSmallestBoundingBox() {
        DataSource dataSource = mock(DataSource.class);
        slash.navigation.datasources.File small = mock(slash.navigation.datasources.File.class);
        when(small.getBoundingBox()).thenReturn(new BoundingBox(0.2, 0.2, -0.2, -0.2));
        when(small.getUri()).thenReturn(SMALL_URI);
        slash.navigation.datasources.File medium = mock(slash.navigation.datasources.File.class);
        when(medium.getBoundingBox()).thenReturn(new BoundingBox(1.0, 1.0, -1.0, -1.0));
        when(medium.getUri()).thenReturn(MEDIUM_URI);
        slash.navigation.datasources.File large = mock(slash.navigation.datasources.File.class);
        when(large.getBoundingBox()).thenReturn(new BoundingBox(2.0, 2.0, -2.0, -2.0));
        when(large.getUri()).thenReturn(LARGE_URI);
        when(dataSource.getFiles()).thenReturn(asList(medium, small, large));
        finder = new DownloadableFinder(dataSource, temporaryDirectory);
        finder.setDataSource(dataSource);

        Downloadable downloadable = finder.getDownloadableFor(new BoundingBox(0.1, 0.1, -0.1, -0.1));
        assertEquals(SMALL_URI, downloadable.getUri());
    }

    @Test
    public void testSelectLargeBoundingBoxThatExists() throws IOException {
        DataSource dataSource = mock(DataSource.class);
        slash.navigation.datasources.File small = mock(slash.navigation.datasources.File.class);
        when(small.getBoundingBox()).thenReturn(new BoundingBox(0.2, 0.2, -0.2, -0.2));
        when(small.getUri()).thenReturn(SMALL_URI);
        slash.navigation.datasources.File medium = mock(slash.navigation.datasources.File.class);
        when(medium.getBoundingBox()).thenReturn(new BoundingBox(1.0, 1.0, -1.0, -1.0));
        when(medium.getUri()).thenReturn(MEDIUM_URI);
        slash.navigation.datasources.File large = mock(slash.navigation.datasources.File.class);
        when(large.getBoundingBox()).thenReturn(new BoundingBox(3.0, 3.0, -2.0, -2.0));
        when(large.getUri()).thenReturn(LARGE_URI);
        when(dataSource.getFiles()).thenReturn(asList(medium, small, large));
        assertTrue(new File(temporaryDirectory, LARGE_URI).createNewFile());
        finder = new DownloadableFinder(dataSource, temporaryDirectory);
        finder.setDataSource(dataSource);

        Downloadable downloadable = finder.getDownloadableFor(new BoundingBox(0.1, 0.1, -0.1, -0.1));
        assertEquals(LARGE_URI, downloadable.getUri());
    }

    @Test
    public void testSelectOnlyCenterFileIfItCoversTheRoute() throws IOException {
        DataSource dataSource = mock(DataSource.class);
        slash.navigation.datasources.File small = mock(slash.navigation.datasources.File.class);
        when(small.getBoundingBox()).thenReturn(new BoundingBox(0.1, 0.1, -0.1, -0.1));
        when(small.getUri()).thenReturn(SMALL_URI);
        slash.navigation.datasources.File medium = mock(slash.navigation.datasources.File.class);
        when(medium.getBoundingBox()).thenReturn(new BoundingBox(1.0, 1.0, -1.0, -1.0));
        when(medium.getUri()).thenReturn(MEDIUM_URI);
        when(dataSource.getFiles()).thenReturn(asList(medium, small));
        assertTrue(new File(temporaryDirectory, SMALL_URI).createNewFile());
        finder = new DownloadableFinder(dataSource, temporaryDirectory);
        finder.setDataSource(dataSource);

        Downloadable downloadable = finder.getDownloadableFor(new BoundingBox(0.2, 0.2, -0.2, -0.2));
        assertEquals(MEDIUM_URI, downloadable.getUri());
    }
}

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
package slash.navigation.brouter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.datasources.DataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getApplicationDirectory;

/**
 * Hermetic test for {@link BRouter#removeOutdatedSegments()}: a locally cached {@code .rd5} segment
 * whose embedded lookup version differs from the bundled {@code lookups.dat} must be deleted, while
 * a matching segment is kept.
 *
 * @author Christian Pesch
 */
public class BRouterRemoveOutdatedSegmentsTest {
    private static final String DIRECTORY = "brouter-remove-outdated-test";
    private static final int EXPECTED_VERSION = 11;
    private static final int OUTDATED_VERSION = 10;

    private static final int NEWER_VERSION = 12;

    private BRouter router;
    private File directory, current, outdated, newer;

    @Before
    public void setUp() throws IOException {
        directory = getApplicationDirectory(DIRECTORY);

        writeLookups(new File(directory, "lookups.dat"), EXPECTED_VERSION);
        current = writeSegment(new File(directory, "E0_N0.rd5"), EXPECTED_VERSION);
        outdated = writeSegment(new File(directory, "W5_N0.rd5"), OUTDATED_VERSION);
        newer = writeSegment(new File(directory, "E5_N0.rd5"), NEWER_VERSION);

        DataSource profiles = mock(DataSource.class);
        when(profiles.getDirectory()).thenReturn(DIRECTORY);
        DataSource segments = mock(DataSource.class);
        when(segments.getDirectory()).thenReturn(DIRECTORY);

        router = new BRouter(null);
        router.setProfilesAndSegments(profiles, segments);
    }

    @After
    public void tearDown() {
        File[] files = directory.listFiles();
        if (files != null)
            for (File file : files)
                file.delete();
        directory.delete();
    }

    @Test
    public void testRemoveOutdatedSegments() {
        assertTrue("segment with matching lookup version must be kept", current.exists());
        assertFalse("segment with outdated lookup version must be removed", outdated.exists());
        assertTrue("segment newer than a stale lookups.dat must be kept to avoid a re-download loop", newer.exists());
    }

    private static void writeLookups(File file, int version) throws IOException {
        Files.writeString(file.toPath(), "---lookupversion:" + version + "\n---minorversion:2\n");
    }

    private static File writeSegment(File file, int version) throws IOException {
        // PhysicalFile.checkVersionIntegrity() reads the first 8 bytes as a big-endian long and takes
        // the top 16 bits as the lookup version, then needs at least 200 bytes to read the header.
        byte[] header = new byte[200];
        header[0] = (byte) (version >> 8);
        header[1] = (byte) version;
        Files.write(file.toPath(), header);
        return file;
    }
}

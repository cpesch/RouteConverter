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

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.datasources.DataSource;
import slash.navigation.datasources.Downloadable;
import slash.navigation.download.Action;
import slash.navigation.download.Checksum;
import slash.navigation.download.DownloadManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static slash.common.io.Directories.getApplicationDirectory;
import static slash.common.io.InputOutput.readFileToString;

/**
 * Hermetic (no external network) test for {@link BRouter#setProfilesAndSegments(DataSource, DataSource)}:
 * a locally cached {@code lookups.dat} that no longer matches the latest expected checksum must be
 * refreshed -- via a local {@link HttpServer} -- and that refresh must complete <em>before</em>
 * {@link BRouter#removeOutdatedSegments()} reads its embedded lookup version. Otherwise the stale
 * local file makes {@code removeOutdatedSegments()} misjudge an already-current segment as outdated
 * and delete it, with the refreshed {@code lookups.dat} only taking effect after a restart.
 *
 * @author Christian Pesch
 */
public class BRouterRefreshStaleLookupsTest {
    private static final String DIRECTORY = "brouter-refresh-lookups-test";
    private static final int STALE_VERSION = 10;
    private static final int CURRENT_VERSION = 11;
    private static final String LOOKUPS_DAT = "lookups.dat";

    private HttpServer server;
    private DownloadManager downloadManager;
    private File directory, lookups, segment, queueFile;

    @Before
    public void setUp() throws IOException {
        directory = getApplicationDirectory(DIRECTORY);

        lookups = new File(directory, LOOKUPS_DAT);
        writeLookups(lookups, STALE_VERSION);
        segment = writeSegment(new File(directory, "E0_N0.rd5"), CURRENT_VERSION);

        byte[] freshLookups = lookupsBytes(CURRENT_VERSION);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/" + LOOKUPS_DAT, exchange -> {
            exchange.sendResponseHeaders(200, freshLookups.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(freshLookups);
            }
            exchange.close();
        });
        server.start();

        queueFile = createTempFile("queueFile", ".xml");
        downloadManager = new DownloadManager(queueFile);
    }

    @After
    public void tearDown() {
        downloadManager.dispose();
        server.stop(0);
        File[] files = directory.listFiles();
        if (files != null)
            for (File file : files)
                file.delete();
        directory.delete();
        if (queueFile.exists() && !queueFile.delete())
            queueFile.deleteOnExit();
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    private static byte[] lookupsBytes(int version) {
        return ("---lookupversion:" + version + "\n---minorversion:2\n").getBytes(StandardCharsets.UTF_8);
    }

    private static void writeLookups(File file, int version) throws IOException {
        Files.write(file.toPath(), lookupsBytes(version));
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

    @Test
    public void staleLookupsAreRefreshedBeforeOutdatedSegmentsAreRemoved() throws IOException {
        // no checksum in the list matches the stale local file, so a refresh is triggered; a null
        // SHA-1/length in the *expected* checksum makes the post-download validation lenient, so this
        // test doesn't need to precompute the served content's real SHA-1
        Downloadable downloadable = mock(Downloadable.class);
        when(downloadable.getUri()).thenReturn(LOOKUPS_DAT);
        when(downloadable.getChecksums()).thenReturn(singletonList(new Checksum(null, null, null)));

        DataSource profiles = mock(DataSource.class);
        when(profiles.getDirectory()).thenReturn(DIRECTORY);
        when(profiles.getBaseUrl()).thenReturn(baseUrl());
        when(profiles.getAction()).thenReturn(Action.Copy.name());
        when(profiles.getDownloadable(LOOKUPS_DAT)).thenReturn(downloadable);

        DataSource segments = mock(DataSource.class);
        when(segments.getDirectory()).thenReturn(DIRECTORY);

        BRouter router = new BRouter(downloadManager);
        router.setProfilesAndSegments(profiles, segments);

        assertTrue("segment matching the refreshed lookup version must be kept", segment.exists());
        String refreshed = readFileToString(lookups);
        assertTrue("lookups.dat must be refreshed to the latest version before segments are checked",
                refreshed.contains("---lookupversion:" + CURRENT_VERSION));
    }
}

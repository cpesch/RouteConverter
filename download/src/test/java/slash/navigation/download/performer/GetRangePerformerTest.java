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
package slash.navigation.download.performer;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.FileAndChecksum;
import slash.navigation.download.State;
import slash.navigation.download.executor.DownloadExecutor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static slash.navigation.download.Action.GetRange;

/**
 * Hermetic tests for {@link GetRangePerformer}: the 16 KB range GET that
 * {@code UpdateCatalog} uses to read zip and map headers (rc/RouteConverter#362).
 *
 * @author Christian Pesch
 */
public class GetRangePerformerTest {
    private static final int RANGE = 16 * 1024;
    private static final byte[] BODY = new byte[40_000];
    private static final String ETAG = "\"v1\"";

    static {
        new Random(362).nextBytes(BODY);
    }

    @Rule
    public final Timeout testTimeout = Timeout.seconds(30);

    private HttpServer server;
    private final AtomicReference<String> rangeHeader = new AtomicReference<>();
    private DownloadManager manager;
    private File target, queueFile;

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/range", exchange -> {
            rangeHeader.set(exchange.getRequestHeaders().getFirst("Range"));
            exchange.getResponseHeaders().add("Content-Range", "bytes 0-" + (RANGE - 1) + "/" + BODY.length);
            exchange.sendResponseHeaders(206, RANGE);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(BODY, 0, RANGE);
            }
            exchange.close();
        });
        server.createContext("/no-range", exchange -> {
            exchange.sendResponseHeaders(200, BODY.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(BODY);
            }
            exchange.close();
        });
        server.createContext("/etag", exchange -> {
            if (ETAG.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                exchange.sendResponseHeaders(304, -1);
            } else {
                exchange.sendResponseHeaders(500, -1);
            }
            exchange.close();
        });
        server.start();

        queueFile = createTempFile("queueFile", ".xml");
        manager = new DownloadManager(queueFile);
        target = createTempFile("partial", ".zip");
        assertTrue(target.delete());
    }

    @After
    public void tearDown() {
        manager.dispose();
        server.stop(0);
        if (target.exists())
            target.delete();
        if (queueFile.exists() && !queueFile.delete())
            queueFile.deleteOnExit();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private FileAndChecksum expectingFullLength() {
        return new FileAndChecksum(target, new Checksum(null, (long) BODY.length, null));
    }

    @Test
    public void partialContentWritesRangePaddedToFullLength() throws IOException {
        Download download = manager.queueForDownload("range", url("/range"), GetRange, expectingFullLength(), null);
        manager.waitForCompletion(singletonList(download));

        assertEquals(State.Succeeded, download.getState());
        assertEquals("bytes=0-" + (RANGE - 1), rangeHeader.get());
        byte[] written = Files.readAllBytes(target.toPath());
        assertEquals(BODY.length, written.length);
        assertArrayEquals(Arrays.copyOf(BODY, RANGE), Arrays.copyOf(written, RANGE));
        assertArrayEquals(new byte[BODY.length - RANGE], Arrays.copyOfRange(written, RANGE, written.length));
    }

    @Test
    public void ignoredRangeWritesWholeBody() throws IOException {
        Download download = manager.queueForDownload("no range", url("/no-range"), GetRange, expectingFullLength(), null);
        manager.waitForCompletion(singletonList(download));

        assertEquals(State.Succeeded, download.getState());
        assertArrayEquals(BODY, Files.readAllBytes(target.toPath()));
    }

    @Test
    public void notModifiedWithoutEntity() {
        // registered but not started, so the ETag is in place before the request runs
        Download download = manager.addOrUpdateInQueue("etag", url("/etag"), GetRange, expectingFullLength(), null);
        download.setETag(ETAG);
        new DownloadExecutor(download, manager).run();

        assertEquals(State.NotModified, download.getState());
    }
}

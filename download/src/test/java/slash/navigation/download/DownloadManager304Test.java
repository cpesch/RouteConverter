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
package slash.navigation.download;

import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static slash.common.io.InputOutput.readFileToString;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.State.NotModified;
import static slash.navigation.download.State.Outdated;
import static slash.navigation.download.State.Succeeded;

/**
 * Hermetic (no external network) test for the conditional-GET path of {@link slash.navigation.download.performer.GetPerformer}:
 * once a target and its ETag are known, the next download sends {@code If-None-Match} and a
 * server {@code 304 Not Modified} resolves the download as {@link State#NotModified} without
 * re-fetching the body or overwriting the existing target.
 *
 * Uses a local {@link HttpServer} so the assertion runs in the unit (Surefire) phase rather
 * than only in the network-gated {@link DownloadManagerIT}.
 *
 * @author Christian Pesch
 */
public class DownloadManager304Test {
    private static final String ETAG = "\"abc123\"";
    private static final String BODY = "Lorem ipsum dolor sit amet";

    private HttpServer server;
    private final AtomicInteger bodiesServed = new AtomicInteger();
    private DownloadManager manager;
    private File target, queueFile;

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/resource", exchange -> {
            String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
            exchange.getResponseHeaders().add("ETag", ETAG);
            if (ETAG.equals(ifNoneMatch)) {
                exchange.sendResponseHeaders(304, -1); // no body
            } else {
                byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
                bodiesServed.incrementAndGet();
            }
            exchange.close();
        });
        server.start();

        queueFile = createTempFile("queueFile", ".xml");
        manager = new DownloadManager(queueFile);
        target = createTempFile("local", ".txt");
        assertEquals(true, target.delete());
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

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/resource";
    }

    @Test
    public void test304YieldsNotModifiedWithoutOverwritingTarget() throws IOException {
        Download first = manager.queueForDownload("resource", url(), Copy, new FileAndChecksum(target, null), null);
        manager.waitForCompletion(singletonList(first));
        assertEquals(Succeeded, first.getState());
        assertEquals(BODY, readFileToString(target));
        assertNotNull(first.getETag());
        assertEquals(1, bodiesServed.get());

        long lengthAfterFirst = target.length();
        long lastModifiedAfterFirst = target.lastModified();

        Download second = manager.queueForDownload("resource", url(), Copy, new FileAndChecksum(target, null), null);
        manager.waitForCompletion(singletonList(second));
        assertEquals(NotModified, second.getState());
        // server returned 304: no second body served, target untouched
        assertEquals(1, bodiesServed.get());
        assertEquals(lengthAfterFirst, target.length());
        assertEquals(lastModifiedAfterFirst, target.lastModified());
        assertEquals(BODY, readFileToString(target));
    }

    @Test
    public void testOutdatedFileDropsETagSoReDownloadIsUnconditional() throws IOException {
        // first download stores the ETag and a valid target
        Checksum expected = new Checksum(null, (long) BODY.length(), null);
        Download download = manager.queueForDownload("resource", url(), Copy, new FileAndChecksum(target, expected), null);
        manager.waitForCompletion(singletonList(download));
        assertEquals(Succeeded, download.getState());
        assertNotNull(download.getETag());
        assertEquals(1, bodiesServed.get());

        // corrupt the target on disk so its checksum no longer matches the expected one;
        // keep its last-modified time so the mismatch is detected via content length / SHA-1
        // alone and the test never depends on whether the rewrite crosses a one-second
        // boundary (otherwise the "locally later than remote" heuristic can mask the corruption)
        long lastModified = target.lastModified();
        Files.writeString(target.toPath(), "corrupt local content of a different length");
        assertEquals(true, target.setLastModified(lastModified));

        // scan detects the mismatch, marks the download Outdated and drops the now-stale ETag
        manager.scanForOutdatedFilesInQueue();
        assertEquals(Outdated, download.getState());
        assertNull(download.getETag());

        // re-download must send no If-None-Match -> server answers 200 and overwrites the corrupt file
        Download second = manager.queueForDownload("resource", url(), Copy, new FileAndChecksum(target, expected), null);
        manager.waitForCompletion(singletonList(second));
        assertEquals(Succeeded, second.getState());
        assertEquals(2, bodiesServed.get());
        assertEquals(BODY, readFileToString(target));
    }
}

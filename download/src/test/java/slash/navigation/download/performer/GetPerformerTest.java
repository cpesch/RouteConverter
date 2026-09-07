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
import slash.navigation.rest.RFC2616;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.ChecksumReportPolicy.isReportableChecksum;

/**
 * Hermetic (no external network) tests that a download's scratch temp file is deleted on the
 * failure paths added for the "delete download scratch temp files on failure/removal" fix:
 * a completed-but-invalid transfer ({@link GetPerformer#postProcess}) and a failed transfer
 * that never reaches {@code postProcess} ({@link GetPerformer#run}'s final {@code else} branch).
 *
 * @author Christian Pesch
 */
public class GetPerformerTest {
    private static final String BODY = "Lorem ipsum dolor sit amet";
    // second precision: HTTP dates carry no milliseconds, so this round-trips through RFC 1123
    private static final long LAST_MODIFIED = 1_700_000_123_000L;

    @Rule
    public final Timeout testTimeout = Timeout.seconds(30);

    private HttpServer server;
    private final AtomicInteger bodiesServed = new AtomicInteger();
    private DownloadManager manager;
    private File target, queueFile;

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
            bodiesServed.incrementAndGet();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
            exchange.close();
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.createContext("/ok-last-modified", exchange -> {
            byte[] body = BODY.getBytes(StandardCharsets.UTF_8);
            bodiesServed.incrementAndGet();
            exchange.getResponseHeaders().add("Last-Modified", RFC2616.formatDate(LAST_MODIFIED));
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
            exchange.close();
        });
        server.start();

        queueFile = createTempFile("queueFile", ".xml");
        manager = new DownloadManager(queueFile);
        target = createTempFile("local", ".txt");
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

    @Test
    public void testValidateFailureDeletesTempFile() {
        // a checksum that can never match the downloaded bytes forces validate() to fail
        // after a fully completed transfer, exercising postProcess()'s cleanup
        Checksum wrongChecksum = new Checksum(null, (long) BODY.length() + 999L, "wrong-sha1");
        Download download = manager.queueForDownload("mismatching checksum", url("/ok"), Copy,
                new FileAndChecksum(target, wrongChecksum), null);
        manager.waitForCompletion(singletonList(download));

        assertEquals(State.ChecksumError, download.getState());
        assertEquals(1, bodiesServed.get());
        assertFalse("temp file must be deleted after a failed validation", download.getTempFile().exists());
    }

    @Test
    public void testValidateFailureCarriesAnnouncedContentLength() {
        // the Content-Length the response announced survives the failed validation, so a listener
        // can tell a completely transferred file from a truncated one (see GitHub #382)
        Checksum wrongChecksum = new Checksum(null, (long) BODY.length() + 999L, "wrong-sha1");
        Download download = manager.queueForDownload("mismatching checksum", url("/ok"), Copy,
                new FileAndChecksum(target, wrongChecksum), null);
        manager.waitForCompletion(singletonList(download));

        assertEquals(State.ChecksumError, download.getState());
        assertEquals((long) BODY.length(), (long) download.getAnnouncedContentLength());
        assertEquals((long) BODY.length(), (long) download.getFile().getActualChecksum().getContentLength());
    }

    @Test
    public void testValidateFailureCarriesAnnouncedLastModifiedEndToEnd() {
        // the genuine-rebuild path (see GitHub #382): a completed transfer whose validation fails
        // must carry the announced Last-Modified through to the actual checksum, because that is
        // what lets a listener prove the file is the build the server announced rather than a
        // truncated fragment of it
        Checksum wrongChecksum = new Checksum(null, (long) BODY.length() + 999L, "wrong-sha1");
        Download download = manager.queueForDownload("mismatching checksum", url("/ok-last-modified"),
                Copy, new FileAndChecksum(target, wrongChecksum), null);
        manager.waitForCompletion(singletonList(download));

        assertEquals(State.ChecksumError, download.getState());
        assertEquals((long) BODY.length(), (long) download.getAnnouncedContentLength());
        assertEquals(LAST_MODIFIED, (long) download.getAnnouncedLastModified());

        Checksum actual = download.getFile().getActualChecksum();
        assertEquals((long) BODY.length(), (long) actual.getContentLength());
        // copy() stamps the target with the announced Last-Modified and createChecksum() reads it
        // back, so the actual checksum's mtime mirrors the announced one
        assertEquals(LAST_MODIFIED, actual.getLastModified().getTimeInMillis());

        // exactly what ChecksumSender.failed() asks the policy, with the real download's values
        assertTrue(isReportableChecksum(download.getState(), download.getAnnouncedContentLength(),
                actual.getContentLength(), download.getAnnouncedLastModified(),
                actual.getLastModified().getTimeInMillis()));
    }

    @Test
    public void testFailedTransferDoesNotCarryAnnouncedValues() throws IOException {
        // a transport failure must not leave announced values behind that could vouch for
        // the content of the failed attempt
        Download download = new Download("missing resource", url("/missing"), Copy,
                new FileAndChecksum(target, null), null);
        manager.getModel().setDownloads(singletonList(download));
        DownloadExecutor executor = new DownloadExecutor(download, manager);
        GetPerformer performer = new GetPerformer();
        performer.setDownloadExecutor(executor);
        performer.run();

        assertEquals(State.Failed, download.getState());
        assertNull(download.getAnnouncedContentLength());
        assertNull(download.getAnnouncedLastModified());
    }

    @Test
    public void testFailedTransferDeletesTempFile() throws IOException {
        // a stale temp file left over from an earlier attempt must not survive a subsequent
        // failed transfer that never reaches postProcess() (run()'s final else branch)
        Download download = new Download("missing resource", url("/missing"), Copy,
                new FileAndChecksum(target, null), null);
        Files.write(download.getTempFile().toPath(), "leftover".getBytes(StandardCharsets.UTF_8));
        assertTrue(download.getTempFile().exists());

        // seed the model directly (queueForDownload()/queue() would build their own Download
        // with a fresh, empty temp file, defeating the pre-populated stale-file setup above)
        // and drive GetPerformer synchronously, exactly as DownloadExecutor.run() does
        manager.getModel().setDownloads(singletonList(download));
        DownloadExecutor executor = new DownloadExecutor(download, manager);
        GetPerformer performer = new GetPerformer();
        performer.setDownloadExecutor(executor);
        performer.run();

        assertEquals(State.Failed, download.getState());
        assertFalse("stale temp file must be deleted after a failed download", download.getTempFile().exists());
    }
}

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static slash.common.io.Files.generateChecksum;
import static slash.common.io.InputOutput.readFileToString;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.download.State.Outdated;
import static slash.navigation.download.State.Succeeded;

/**
 * Hermetic (no external network) tests of the download state machine, covering the paths that
 * recently broke: ETag/304 conditional GET with a corrupt local target, Extract with/without
 * fragments, and resume/partial-content. Uses a local {@link HttpServer} so they run in the
 * unit (Surefire) phase rather than the network-gated {@link DownloadManagerIT}.
 *
 * @author Christian Pesch
 */
public class DownloadManagerStateMachineTest {
    private static final String ENTRY_NAME = "first/second/447bytes.txt";
    private static final String ENTRY_BODY = "Lorem ipsum dolor sit amet, consectetur adipisicing elit";
    private static final String PLAIN_BODY = "0123456789ABCDEFGHIJ0123456789ABCDEFGHIJ"; // 40 bytes
    private static final String ETAG = "\"state-machine\"";

    @Rule
    public final Timeout testTimeout = Timeout.seconds(30);

    private HttpServer server;
    private final AtomicInteger bodiesServed = new AtomicInteger();
    private final AtomicInteger partialServed = new AtomicInteger();
    private final AtomicInteger conditional304 = new AtomicInteger();
    private byte[] zipBytes;

    private DownloadManager manager;
    private File queueFile, target, targetDirectory;

    @Before
    public void setUp() throws IOException {
        zipBytes = buildZip(ENTRY_NAME, ENTRY_BODY.getBytes(StandardCharsets.UTF_8));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/archive.zip", exchange -> {
            String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
            exchange.getResponseHeaders().add("ETag", ETAG);
            if (ETAG.equals(ifNoneMatch)) {
                conditional304.incrementAndGet();
                exchange.sendResponseHeaders(304, -1);
            } else {
                exchange.sendResponseHeaders(200, zipBytes.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(zipBytes); }
                bodiesServed.incrementAndGet();
            }
            exchange.close();
        });

        server.createContext("/plain.txt", exchange -> {
            byte[] body = PLAIN_BODY.getBytes(StandardCharsets.UTF_8);
            String range = exchange.getRequestHeaders().getFirst("Range");
            if (range != null && range.startsWith("bytes=")) {
                int start = parseRangeStart(range);
                byte[] remainder = new byte[body.length - start];
                System.arraycopy(body, start, remainder, 0, remainder.length);
                exchange.getResponseHeaders().add("Content-Range",
                        "bytes " + start + "-" + (body.length - 1) + "/" + body.length);
                exchange.sendResponseHeaders(206, remainder.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(remainder); }
                partialServed.incrementAndGet();
            } else {
                exchange.getResponseHeaders().add("ETag", ETAG);
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
                bodiesServed.incrementAndGet();
            }
            exchange.close();
        });

        server.createContext("/conditional.txt", exchange -> {
            byte[] body = PLAIN_BODY.getBytes(StandardCharsets.UTF_8);
            String ifNoneMatch = exchange.getRequestHeaders().getFirst("If-None-Match");
            exchange.getResponseHeaders().add("ETag", ETAG);
            if (ETAG.equals(ifNoneMatch)) {
                conditional304.incrementAndGet();
                exchange.sendResponseHeaders(304, -1);
            } else {
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(body); }
                bodiesServed.incrementAndGet();
            }
            exchange.close();
        });

        server.start();

        queueFile = createTempFile("queueFile", ".xml");
        manager = new DownloadManager(queueFile);
        target = createTempFile("local", ".txt");
        assertTrue(target.delete());
        targetDirectory = createTempFile("extract", "");
        assertTrue(targetDirectory.delete());
        assertTrue(targetDirectory.mkdirs());
    }

    @After
    public void tearDown() {
        manager.dispose();
        server.stop(0);
        if (target.exists()) target.delete();
        deleteRecursively(targetDirectory);
        if (queueFile.exists() && !queueFile.delete()) queueFile.deleteOnExit();
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    private static int parseRangeStart(String range) {
        String value = range.substring("bytes=".length());
        return Integer.parseInt(value.substring(0, value.indexOf('-')));
    }

    private static byte[] buildZip(String entryName, byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            ZipEntry entry = new ZipEntry(entryName);
            entry.setTime(0); // deterministic
            zip.putNextEntry(entry);
            zip.write(content);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteRecursively(child);
        file.delete();
    }

    private static String sha1Of(byte[] bytes) throws IOException {
        return generateChecksum(new ByteArrayInputStream(bytes));
    }

    @Test
    public void testExtractWithMatchingFragmentSucceeds() throws IOException {
        File extracted = new File(targetDirectory, ENTRY_NAME);
        byte[] entryBytes = ENTRY_BODY.getBytes(StandardCharsets.UTF_8);
        Checksum fragmentChecksum = new Checksum(null, (long) entryBytes.length, sha1Of(entryBytes));
        FileAndChecksum fragment = new FileAndChecksum(extracted, fragmentChecksum);

        Download download = manager.queueForDownload("zip with fragment", url("/archive.zip"), Extract,
                new FileAndChecksum(targetDirectory, null), singletonList(fragment));
        manager.waitForCompletion(singletonList(download));

        assertEquals(Succeeded, download.getState());
        assertEquals(1, bodiesServed.get());
        assertTrue(extracted.exists());
        assertEquals(ENTRY_BODY, readFileToString(extracted));
    }

    @Test
    public void testExtractWithoutFragmentsDoesNotCrashManager() {
        Download download = manager.queueForDownload("zip without fragments", url("/archive.zip"), Extract,
                new FileAndChecksum(targetDirectory, null), null);
        manager.waitForCompletion(singletonList(download));

        Set<State> terminal = EnumSet.of(Succeeded, Outdated, State.Failed, State.NoFileError, State.ChecksumError);
        assertTrue("expected a terminal state but was " + download.getState(),
                terminal.contains(download.getState()));
        assertEquals(1, bodiesServed.get());
    }

    @Test
    public void testResumePartialContentCompletesDownload() throws IOException {
        byte[] full = PLAIN_BODY.getBytes(StandardCharsets.UTF_8);
        int prefixLength = 16;
        Checksum expected = new Checksum(null, (long) full.length, sha1Of(full));
        Download download = new Download("resumable plain", url("/plain.txt"), Copy,
                new FileAndChecksum(target, expected), null);

        byte[] prefix = new byte[prefixLength];
        System.arraycopy(full, 0, prefix, 0, prefixLength);
        Files.write(download.getTempFile().toPath(), prefix);

        Download queued = manager.queue(download, true);
        manager.waitForCompletion(singletonList(queued));

        assertEquals(Succeeded, queued.getState());
        assertEquals(1, partialServed.get());
        assertEquals(0, bodiesServed.get());
        assertEquals(PLAIN_BODY, readFileToString(target));
        assertFalse(download.getTempFile().exists());
    }

    @Test
    public void test304WithCorruptLocalTargetReFetchesUnconditionally() throws IOException {
        byte[] body = PLAIN_BODY.getBytes(StandardCharsets.UTF_8);
        Checksum expected = new Checksum(null, (long) body.length, sha1Of(body));

        Download download = manager.queueForDownload("conditional", url("/conditional.txt"), Copy,
                new FileAndChecksum(target, expected), null);
        manager.waitForCompletion(singletonList(download));
        assertEquals(Succeeded, download.getState());
        assertNotNull(download.getETag());
        assertEquals(1, bodiesServed.get());
        assertEquals(0, conditional304.get());

        Files.writeString(target.toPath(), "short");
        manager.scanForOutdatedFilesInQueue();
        assertEquals(Outdated, download.getState());
        assertNull(download.getETag());

        Download second = manager.queueForDownload("conditional", url("/conditional.txt"), Copy,
                new FileAndChecksum(target, expected), null);
        manager.waitForCompletion(singletonList(second));
        assertEquals(Succeeded, second.getState());
        assertEquals(2, bodiesServed.get());
        assertEquals(0, conditional304.get());
        assertEquals(PLAIN_BODY, readFileToString(target));
    }
}

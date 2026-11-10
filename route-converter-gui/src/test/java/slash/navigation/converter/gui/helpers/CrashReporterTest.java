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

package slash.navigation.converter.gui.helpers;

import org.junit.Test;
import slash.common.log.CrashReportSpool;
import slash.navigation.feedback.domain.CrashReportSender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.Files.createTempDirectory;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CrashReporterTest {

    private static class RecordingSender extends CrashReportSender {
        final List<String> sent = new ArrayList<>();
        private final boolean accept;

        RecordingSender(boolean accept) {
            this.accept = accept;
        }

        public boolean send(String apiUrl, String json) {
            sent.add(json);
            return accept;
        }
    }

    /**
     * Blocks the first {@code send} call until released, so a test can issue further
     * flush requests while that first send is still in flight. Counts sends per payload
     * to detect duplicate sends.
     */
    private static class BlockingSender extends CrashReportSender {
        final Map<String, Integer> sendCountByPayload = new ConcurrentHashMap<>();
        final CountDownLatch firstSendEntered = new CountDownLatch(1);
        private final CountDownLatch releaseFirstSend;
        private final AtomicBoolean blockedOnce = new AtomicBoolean(false);

        BlockingSender(CountDownLatch releaseFirstSend) {
            this.releaseFirstSend = releaseFirstSend;
        }

        public boolean send(String apiUrl, String json) {
            if (blockedOnce.compareAndSet(false, true)) {
                firstSendEntered.countDown();
                try {
                    releaseFirstSend.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            sendCountByPayload.merge(json, 1, Integer::sum);
            return true;
        }
    }

    /** Counts calls to {@link #list()} so a test can assert how many flush passes ran. */
    private static class CountingSpool extends CrashReportSpool {
        final AtomicInteger listCalls = new AtomicInteger();

        CountingSpool(File directory) {
            super(directory);
        }

        public List<File> list() {
            listCalls.incrementAndGet();
            return super.list();
        }
    }

    /** Runs every submitted task on its own real thread, so a test can wait for them all. */
    private static class ThreadPerTaskExecutor implements Executor {
        private final List<Thread> threads = new CopyOnWriteArrayList<>();

        public void execute(Runnable command) {
            Thread thread = new Thread(command, "test-crash-report-sender");
            threads.add(thread);
            thread.start();
        }

        void awaitIdle() throws InterruptedException {
            for (Thread thread : threads)
                thread.join(SECONDS.toMillis(5));
        }
    }

    private CrashReportSpool spoolWithReports(int count) throws IOException {
        File directory = createTempDirectory("crash-reporter-test").toFile();
        CrashReportSpool spool = new CrashReportSpool(directory);
        for (int i = 0; i < count; i++)
            spool.write("{\"schema_version\":1,\"n\":" + i + "}");
        return spool;
    }

    private CountingSpool countingSpoolWithReports(int count) throws IOException {
        File directory = createTempDirectory("crash-reporter-test").toFile();
        CountingSpool spool = new CountingSpool(directory);
        for (int i = 0; i < count; i++)
            spool.write("{\"schema_version\":1,\"n\":" + i + "}");
        return spool;
    }

    private CrashReporter optedOutReporter(CrashReportSpool spool) {
        return new CrashReporter(spool, new RecordingSender(true), () -> false, () -> "http://localhost/");
    }

    @Test
    public void testOptOutDoesNotSend() throws IOException {
        CrashReportSpool spool = spoolWithReports(2);
        RecordingSender sender = new RecordingSender(true);
        CrashReporter reporter = new CrashReporter(spool, sender, () -> false, () -> "http://localhost/");

        reporter.flushSpooledReports();

        assertEquals(0, sender.sent.size());
        assertEquals(2, spool.list().size());
    }

    @Test
    public void testOptOutOfferSpooledReportsDoesNothing() throws IOException {
        CrashReportSpool spool = spoolWithReports(3);
        RecordingSender sender = new RecordingSender(true);
        CrashReporter reporter = new CrashReporter(spool, sender, () -> false, () -> "http://localhost/");

        // opted out: the backlog is neither sent nor offered in a dialog -- it just stays spooled
        reporter.offerSpooledReports();

        assertEquals("opted out sends nothing", 0, sender.sent.size());
        assertEquals("opted out keeps the whole backlog spooled", 3, spool.list().size());
    }

    @Test
    public void testOptedInSendsAndDeletesOnSuccess() throws IOException {
        CrashReportSpool spool = spoolWithReports(2);
        RecordingSender sender = new RecordingSender(true);
        CrashReporter reporter = new CrashReporter(spool, sender, () -> true, () -> "http://localhost/");

        reporter.flushSpooledReports();

        assertEquals(2, sender.sent.size());
        assertEquals(0, spool.list().size());
    }

    @Test
    public void testOptedInFailureRetainsSpoolFiles() throws IOException {
        CrashReportSpool spool = spoolWithReports(2);
        RecordingSender sender = new RecordingSender(false);
        CrashReporter reporter = new CrashReporter(spool, sender, () -> true, () -> "http://localhost/");

        reporter.flushSpooledReports();

        assertEquals(2, sender.sent.size());
        assertEquals("a failed send must leave the report for the next launch", 2, spool.list().size());
    }

    @Test
    public void testConcurrentRequestsSendEachReportOnce() throws Exception {
        CrashReportSpool spool = spoolWithReports(3);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        BlockingSender sender = new BlockingSender(releaseFirstSend);
        ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        CrashReporter reporter = new CrashReporter(spool, sender, () -> true, () -> "http://localhost/", executor);

        reporter.flushSpooledReportsAsync();
        assertTrue(sender.firstSendEntered.await(5, SECONDS));

        for (int i = 0; i < 4; i++)
            reporter.flushSpooledReportsAsync();

        releaseFirstSend.countDown();
        executor.awaitIdle();

        assertEquals(3, sender.sendCountByPayload.size());
        for (int count : sender.sendCountByPayload.values())
            assertEquals(1, (int) count);
        assertEquals(0, spool.list().size());
    }

    @Test
    public void testCrashDuringFlushIsStillSent() throws Exception {
        CrashReportSpool spool = spoolWithReports(3);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        BlockingSender sender = new BlockingSender(releaseFirstSend);
        ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        CrashReporter reporter = new CrashReporter(spool, sender, () -> true, () -> "http://localhost/", executor);

        reporter.flushSpooledReportsAsync();
        assertTrue(sender.firstSendEntered.await(5, SECONDS));

        spool.write("{\"schema_version\":1,\"n\":3}");
        reporter.flushSpooledReportsAsync();

        releaseFirstSend.countDown();
        executor.awaitIdle();

        assertEquals(4, sender.sendCountByPayload.size());
        for (int count : sender.sendCountByPayload.values())
            assertEquals(1, (int) count);
        assertEquals(0, spool.list().size());
    }

    @Test
    public void testRequestsCollapseIntoAtMostOneExtraPass() throws Exception {
        CountingSpool spool = countingSpoolWithReports(3);
        spool.listCalls.set(0); // CrashReportSpool.write() also calls list() via cap() -- count only the flushes
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        BlockingSender sender = new BlockingSender(releaseFirstSend);
        ThreadPerTaskExecutor executor = new ThreadPerTaskExecutor();
        CrashReporter reporter = new CrashReporter(spool, sender, () -> true, () -> "http://localhost/", executor);

        reporter.flushSpooledReportsAsync();
        assertTrue(sender.firstSendEntered.await(5, SECONDS));

        for (int i = 0; i < 4; i++)
            reporter.flushSpooledReportsAsync();

        releaseFirstSend.countDown();
        executor.awaitIdle();

        assertTrue("spool.list() must be called at most twice by the flushes", spool.listCalls.get() <= 2);
    }

    @Test
    public void testOnlyOneOutOfMemoryReportPerSession() throws IOException {
        File directory = createTempDirectory("crash-reporter-test").toFile();
        CrashReportSpool spool = new CrashReportSpool(directory);
        CrashReporter reporter = optedOutReporter(spool);
        Thread thread = Thread.currentThread();

        reporter.handleCrash(thread, new OutOfMemoryError("Java heap space"));
        reporter.handleCrash(thread, new OutOfMemoryError("Java heap space"));

        assertEquals(1, spool.list().size());
    }

    @Test
    public void testOutOfMemoryAsCauseIsAlsoSuppressed() throws IOException {
        File directory = createTempDirectory("crash-reporter-test").toFile();
        CrashReportSpool spool = new CrashReportSpool(directory);
        CrashReporter reporter = optedOutReporter(spool);
        Thread thread = Thread.currentThread();

        reporter.handleCrash(thread, new RuntimeException("wrapper", new OutOfMemoryError("Java heap space")));
        reporter.handleCrash(thread, new OutOfMemoryError("Java heap space"));

        assertEquals(1, spool.list().size());
    }

    @Test
    public void testNonOutOfMemoryCrashesAfterOutOfMemoryAreStillReported() throws IOException {
        File directory = createTempDirectory("crash-reporter-test").toFile();
        CrashReportSpool spool = new CrashReportSpool(directory);
        CrashReporter reporter = optedOutReporter(spool);
        Thread thread = Thread.currentThread();

        reporter.handleCrash(thread, new OutOfMemoryError("Java heap space"));
        reporter.handleCrash(thread, new NullPointerException());
        reporter.handleCrash(thread, new NullPointerException());

        assertEquals(3, spool.list().size());
    }

    @Test
    public void testNonOutOfMemoryCrashesAreNeverSuppressed() throws IOException {
        File directory = createTempDirectory("crash-reporter-test").toFile();
        CrashReportSpool spool = new CrashReportSpool(directory);
        CrashReporter reporter = optedOutReporter(spool);
        Thread thread = Thread.currentThread();

        reporter.handleCrash(thread, new NullPointerException());
        reporter.handleCrash(thread, new NullPointerException());

        assertEquals(2, spool.list().size());
    }
}

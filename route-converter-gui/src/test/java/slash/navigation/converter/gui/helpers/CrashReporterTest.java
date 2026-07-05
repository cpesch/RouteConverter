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

import org.junit.After;
import org.junit.Test;
import slash.common.log.CrashReportSpool;
import slash.navigation.feedback.domain.CrashReportSender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrashReporterTest {

    @After
    public void tearDown() {
        CrashReporter.resetForTesting();
    }

    @Test
    public void testOnlyOneDialogPerSession() {
        CrashReporter.resetForTesting();

        // the first captured crash claims the single dialog slot
        assertTrue(CrashReporter.claimDialogSlot());
        // further crashes in the same session only spool -- no second dialog
        assertFalse(CrashReporter.claimDialogSlot());
        assertFalse(CrashReporter.claimDialogSlot());
    }

    @Test
    public void testResetReopensSlot() {
        CrashReporter.resetForTesting();
        assertTrue(CrashReporter.claimDialogSlot());

        CrashReporter.resetForTesting();
        assertTrue(CrashReporter.claimDialogSlot());
    }

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

    private CrashReportSpool spoolWithReports(int count) throws IOException {
        File directory = createTempDirectory("crash-reporter-test").toFile();
        CrashReportSpool spool = new CrashReportSpool(directory);
        for (int i = 0; i < count; i++)
            spool.write("{\"schema_version\":1,\"n\":" + i + "}");
        return spool;
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
    public void testManualDialogDeletesOnlyReportsTheUserSent() throws IOException {
        CrashReportSpool spool = spoolWithReports(3);
        RecordingSender sender = new RecordingSender(true);
        // the injected opener stands in for the Swing dialog: the user "sends" only the
        // n:1 report and dismisses the rest
        CrashReporter.DialogOpener opener = json -> json.contains("\"n\":1");
        CrashReporter reporter = new CrashReporter(spool, sender, () -> false, () -> "http://localhost/", opener);

        reporter.offerSpooledReportsInDialog();

        assertEquals("only the sent report is deleted; dismissed ones are kept", 2, spool.list().size());
        assertEquals("the manual path never uses the silent sender", 0, sender.sent.size());
    }

    @Test
    public void testManualDialogOffersEveryReportAndDismissedKeepsAll() throws IOException {
        CrashReportSpool spool = spoolWithReports(3);
        RecordingSender sender = new RecordingSender(true);
        List<String> opened = new ArrayList<>();
        CrashReporter.DialogOpener opener = json -> {
            opened.add(json);
            return false;
        };
        CrashReporter reporter = new CrashReporter(spool, sender, () -> false, () -> "http://localhost/", opener);

        reporter.offerSpooledReportsInDialog();

        assertEquals("every spooled report is offered, not just the newest", 3, opened.size());
        assertEquals("a report the user dismisses is kept for a later offer", 3, spool.list().size());
        assertEquals(0, sender.sent.size());
    }
}

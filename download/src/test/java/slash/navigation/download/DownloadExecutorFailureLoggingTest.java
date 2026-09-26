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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import slash.navigation.download.executor.DownloadExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static slash.navigation.download.State.Queued;

/**
 * Tests that {@link DownloadExecutor} logs a genuine (non-offline) failure with the
 * causing {@link Throwable} attached to the {@link LogRecord}, so a {@link Handler} --
 * and the crash telemetry built on one -- can see it (issue #211).
 *
 * @author Christian Pesch
 */
public class DownloadExecutorFailureLoggingTest {
    private static final Logger LOG = Logger.getLogger(DownloadExecutor.class.getName());

    private final List<LogRecord> records = new ArrayList<>();
    private final Handler handler = new Handler() {
        public void publish(LogRecord record) {
            records.add(record);
        }

        public void flush() {
        }

        public void close() {
        }
    };

    private File queueFile, target, tempFile;
    private DownloadManager manager;

    @Before
    public void setUp() throws IOException {
        LOG.addHandler(handler);

        queueFile = createTempFile("queueFile", ".xml");
        manager = new DownloadManager(queueFile);
        target = createTempFile("local", ".txt");
        tempFile = createTempFile("download", ".tmp");
    }

    @After
    public void tearDown() {
        LOG.removeHandler(handler);
        manager.dispose();
        if (target.exists())
            assertTrue(target.delete());
        if (tempFile.exists())
            assertTrue(tempFile.delete());
        if (queueFile.exists())
            assertTrue(queueFile.delete());
    }

    @Test
    public void testFailureIsLoggedWithThrowableAttached() {
        // a syntactically invalid URL fails synchronously, in HttpHead's own constructor,
        // before any network access -- so this is a genuine, non-offline failure, exercising
        // the log.log(Level.SEVERE, message, e) branch instead of the offline one
        Download download = new Download("desc", "not a url", Action.Head,
                new FileAndChecksum(target, null), null, null, Queued, tempFile);
        // register with the manager's model first, without starting its own executor,
        // since DownloadExecutor#downloadFailed() reports the state change back through it
        manager.queue(download, false);
        DownloadExecutor executor = new DownloadExecutor(download, manager);

        executor.run();

        LogRecord record = findRecord(Level.SEVERE);
        assertNotNull("expected a SEVERE log record for the failed download", record);
        assertNotNull("the throwable must be attached to the log record, not just its text", record.getThrown());
    }

    private LogRecord findRecord(Level level) {
        for (LogRecord record : records) {
            if (record.getLevel().equals(level))
                return record;
        }
        return null;
    }
}

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
package slash.navigation.download.queue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.io.File.createTempFile;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that {@link QueuePersister} logs a failure to unmarshall a queue file with the
 * causing {@link Throwable} attached to the {@link LogRecord}, so a {@link Handler} --
 * and the crash telemetry built on one -- can see it, in addition to wrapping and
 * rethrowing it (issue #211).
 *
 * @author Christian Pesch
 */
public class QueuePersisterTest {
    private static final Logger LOG = Logger.getLogger(QueuePersister.class.getName());

    private final QueuePersister persister = new QueuePersister();
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

    private File queueFile;

    @Before
    public void setUp() throws IOException {
        LOG.addHandler(handler);
        queueFile = createTempFile("queueFile", ".xml");
    }

    @After
    public void tearDown() {
        LOG.removeHandler(handler);
        if (queueFile.exists())
            assertTrue(queueFile.delete());
    }

    @Test
    public void testLoadFailureIsLoggedWithThrowableAttachedBeforeItIsWrapped() throws IOException {
        try (FileOutputStream out = new FileOutputStream(queueFile)) {
            out.write("not xml at all".getBytes(StandardCharsets.UTF_8));
        }

        try {
            persister.load(queueFile);
            org.junit.Assert.fail("expected an IOException wrapping the unmarshall failure");
        } catch (IOException expected) {
            // wrapping and rethrowing is the existing, unchanged behavior
        }

        LogRecord record = findRecord(Level.WARNING);
        assertNotNull("expected a WARNING log record for the failed unmarshall", record);
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

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
import slash.navigation.download.queue.QueuePersister;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.State.Succeeded;

/**
 * Tests that {@link DownloadManager} doesn't overwrite a persisted queue before it has been
 * loaded: an early download (e.g. BRouter's lookups.dat refresh) would otherwise save a queue
 * with just that entry and lose the stored ETags of all other entries (see GitHub #440).
 *
 * @author Christian Pesch
 */
public class DownloadManagerQueuePersistenceTest {
    private static final String STORED_URL = "http://example.com/stored";
    private static final String EARLY_URL = "http://example.com/early";

    private File queueFile, storedTarget, earlyTarget;
    private DownloadManager manager;

    @Before
    public void setUp() throws IOException {
        queueFile = createTempFile("queueFile", ".xml");
        storedTarget = createTempFile("stored", ".txt");
        earlyTarget = createTempFile("early", ".txt");

        Download stored = new Download("stored", STORED_URL, Copy, new FileAndChecksum(storedTarget, null), null,
                "\"stored-etag\"", Succeeded, createTempFile("stored", ".tmp"));
        new QueuePersister().save(queueFile, singletonList(stored));

        manager = new DownloadManager(queueFile);
    }

    @After
    public void tearDown() {
        manager.dispose();
        for (File file : new File[]{queueFile, storedTarget, earlyTarget})
            if (file.exists() && !file.delete())
                file.deleteOnExit();
    }

    private Download addEarlyDownload() {
        return manager.addOrUpdateInQueue("early", EARLY_URL, Copy, new FileAndChecksum(earlyTarget, null), null);
    }

    private List<Download> persisted() throws IOException {
        return new QueuePersister().load(queueFile);
    }

    private static Download find(List<Download> downloads, String url) {
        for (Download download : downloads)
            if (download.getUrl().equals(url))
                return download;
        return null;
    }

    @Test
    public void saveBeforeLoadKeepsPersistedQueue() throws IOException {
        Download early = addEarlyDownload();
        manager.saveQueue();
        // the DownloadManager's own listener saves on success, as after BRouter's lookups.dat refresh
        manager.fireSucceeded(early);

        List<Download> downloads = persisted();
        assertEquals(1, downloads.size());
        Download stored = find(downloads, STORED_URL);
        assertNotNull("persisted entry must survive a save before load", stored);
        assertEquals("\"stored-etag\"", stored.getETag());
    }

    @Test
    public void saveAfterLoadPersistsLoadedAndNewEntries() throws IOException {
        manager.loadQueue();
        addEarlyDownload();
        manager.saveQueue();

        List<Download> downloads = persisted();
        assertEquals(2, downloads.size());
        assertEquals("\"stored-etag\"", find(downloads, STORED_URL).getETag());
        assertNotNull(find(downloads, EARLY_URL));
    }

    @Test
    public void saveAfterLoadOfMissingFileCreatesQueue() throws IOException {
        if (!queueFile.delete())
            throw new IOException("Cannot delete " + queueFile);

        manager.loadQueue();
        addEarlyDownload();
        manager.saveQueue();

        List<Download> downloads = persisted();
        assertEquals(1, downloads.size());
        assertNotNull(find(downloads, EARLY_URL));
    }

    @Test
    public void saveAfterClearReplacesPersistedQueue() throws IOException {
        manager.clearQueue();
        addEarlyDownload();
        manager.saveQueue();

        List<Download> downloads = persisted();
        assertEquals(1, downloads.size());
        assertNotNull(find(downloads, EARLY_URL));
    }
}

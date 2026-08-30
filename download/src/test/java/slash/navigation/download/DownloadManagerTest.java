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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static java.io.File.createTempFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.State.Queued;

/**
 * Tests that {@link DownloadManager} deletes a download's scratch temp file when it's explicitly
 * removed from the queue, but never when it's merely stopped (a stopped-but-not-removed download
 * stays restartable via {@link DownloadManager#restartDownloads} and resumes from that temp file).
 *
 * @author Christian Pesch
 */
public class DownloadManagerTest {
    private File queueFile, target, tempFile;
    private DownloadManager manager;

    @Before
    public void setUp() throws IOException {
        queueFile = createTempFile("queueFile", ".xml");
        manager = new DownloadManager(queueFile);
        target = createTempFile("local", ".txt");
        tempFile = createTempFile("download", ".tmp");
        Files.write(tempFile.toPath(), "partial content".getBytes(StandardCharsets.UTF_8));
    }

    @After
    public void tearDown() {
        manager.dispose();
        if (target.exists())
            target.delete();
        if (tempFile.exists())
            tempFile.delete();
        if (queueFile.exists() && !queueFile.delete())
            queueFile.deleteOnExit();
    }

    private Download queuedDownload() {
        Download download = new Download("desc", "http://example.com/f", Copy,
                new FileAndChecksum(target, null), null, null, Queued, tempFile);
        manager.queue(download, false);
        return download;
    }

    @Test
    public void testRemoveDownloadsDeletesTempFile() {
        Download download = queuedDownload();
        assertTrue(download.getTempFile().exists());

        manager.removeDownloads(singletonList(download));

        assertFalse("temp file must be deleted when a download is removed", download.getTempFile().exists());
    }

    @Test
    public void testStopDownloadsDoesNotDeleteTempFile() {
        Download download = queuedDownload();
        assertTrue(download.getTempFile().exists());

        manager.stopDownloads(singletonList(download));

        assertTrue("temp file must survive a stop so a restart can resume from it", download.getTempFile().exists());
    }
}

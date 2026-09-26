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

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.Action.Copy;
import static slash.navigation.download.State.Queued;

public class DownloadTableModelTest {
    private static final int ITERATIONS = 10_000;
    private static final int DOWNLOADS_PER_ITERATION = 5;

    private static File tempFile() {
        try {
            File f = File.createTempFile("download-table-model-test", ".tmp");
            f.deleteOnExit();
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Download> createDownloads() {
        List<Download> result = new ArrayList<>();
        for (int i = 0; i < DOWNLOADS_PER_ITERATION; i++) {
            FileAndChecksum file = new FileAndChecksum(tempFile(), new Checksum(fromMillis(1_000_000_000_000L), 100L, "sha1"));
            result.add(new Download("desc" + i, "http://example.com/file" + i, Copy, file, null, null, Queued, tempFile()));
        }
        return result;
    }

    @Test
    public void getDownloadsIsConsistentWhileDownloadsAreReplaced() throws InterruptedException {
        DownloadTableModel model = new DownloadTableModel();
        List<Download> downloads = createDownloads();
        AtomicBoolean writing = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < ITERATIONS; i++) {
                    model.setDownloads(new ArrayList<>());
                    for (Download download : downloads)
                        model.addOrUpdateDownload(download);
                    model.removeDownload(downloads.get(i % DOWNLOADS_PER_ITERATION));
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                writing.set(false);
            }
        }, "DownloadTableModelTest-writer");

        Thread reader = new Thread(() -> {
            try {
                while (writing.get()) {
                    List<Download> snapshot = model.getDownloads();
                    assertTrue("Snapshot too large: " + snapshot.size(), snapshot.size() <= DOWNLOADS_PER_ITERATION);
                    for (Download download : snapshot)
                        assertNotNull("Snapshot contains partially published element: " + snapshot, download);
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        }, "DownloadTableModelTest-reader");

        writer.start();
        reader.start();
        writer.join();
        reader.join();

        if (failure.get() != null)
            throw new AssertionError("Concurrent access failed", failure.get());
        assertEquals(DOWNLOADS_PER_ITERATION - 1, model.getDownloads().size());
    }

    @Test
    public void getDownloadsReturnsSnapshot() {
        DownloadTableModel model = new DownloadTableModel();
        model.setDownloads(new ArrayList<>(createDownloads()));

        List<Download> snapshot = model.getDownloads();
        snapshot.clear();

        assertEquals(DOWNLOADS_PER_ITERATION, model.getRowCount());
    }
}

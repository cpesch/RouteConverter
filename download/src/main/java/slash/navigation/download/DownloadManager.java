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

import slash.common.type.CompactCalendar;
import slash.navigation.download.queue.QueuePersister;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.download.State.*;

/**
 * Manages {@link Download}s
 *
 * @author Christian Pesch
 */

public class DownloadManager {
    private static final Logger log = Logger.getLogger(DownloadManager.class.getName());
    static final int WAIT_TIMEOUT = 15 * 1000;
    private static final int PARALLEL_DOWNLOAD_COUNT = 4;
    private final DownloadTableModel model = new DownloadTableModel();
    private final ThreadPoolExecutor pool;

    public DownloadManager() {
        BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>(1, new DownloadExecutorComparator());
        pool = new ThreadPoolExecutor(PARALLEL_DOWNLOAD_COUNT, PARALLEL_DOWNLOAD_COUNT * 2, 60, SECONDS, queue);
        pool.allowCoreThreadTimeOut(true);
    }

    public void restartQueue(File file) {
        try {
            List<Download> downloads = new QueuePersister(file).load();
            if (downloads != null)
                model.setDownloads(downloads);
        } catch (Exception e) {
            log.severe(format("Could not load '%s': %s", file, e.getMessage()));
        }

        restartDownloadsWithState(Resuming);
        restartDownloadsWithState(Downloading);
        restartDownloadsWithState(Processing);
        restartDownloadsWithState(Queued);
    }

    private void restartDownloadsWithState(State state) {
        for (Download download : model.getDownloads()) {
            if (state.equals(download.getState()))
                startExecutor(download);
        }
    }

    public void saveQueue(File file) {
        try {
            new QueuePersister(file).save(model.getDownloads());
        } catch (Exception e) {
            log.severe(format("Could not save %d downloads to '%s': %s", model.getRowCount(), file, e.getMessage()));
        }
    }
    public void dispose() {
        pool.shutdownNow();
    }

    public DownloadTableModel getModel() {
        return model;
    }

    private void startExecutor(Download download) {
        DownloadExecutor executor = new DownloadExecutor(download, model);
        model.addOrUpdateDownload(download);
        pool.execute(executor);
    }

    public CompactCalendar getLastSync(String url) {
        Download queued = getModel().getDownload(url);
        return queued != null ? queued.getLastSync() : null;
    }

    public Download queueForDownload(Download download) {
        Download queued = getModel().getDownload(download.getUrl());
        if(queued != null) {
            if(Failed.equals(queued.getState()))
                startExecutor(download);
            return queued;
        }

        if(Extract.equals(download.getAction()) && !download.getTarget().isDirectory())
            throw new IllegalArgumentException(format("Need a directory for extraction but got %s", download.getTarget()));
        startExecutor(download);
        return download;
    }

    public Download queueForDownload(String description, String url, Long size, String checksum, Action action, File target) {
        return queueForDownload(new Download(description, url, size, checksum, action, target));
    }

    private static final Object LOCK = new Object();

    public void waitForCompletion(final Collection<Download> downloads) {
        final boolean[] found = new boolean[1];
        found[0] = false;
        final long[] lastEvent = new long[1];
        lastEvent[0] = currentTimeMillis();

        TableModelListener l = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                synchronized (LOCK) {
                    lastEvent[0] = currentTimeMillis();

                    for (Download download : downloads) {
                        if (!(Succeeded.equals(download.getState()) || Failed.equals(download.getState())))
                            return;
                    }
                    found[0] = true;
                    LOCK.notifyAll();
                }
            }
        };

        model.addTableModelListener(l);
        try {
            while (true) {
                synchronized (LOCK) {
                    if (found[0] || currentTimeMillis() - lastEvent[0] > WAIT_TIMEOUT)
                        break;
                    try {
                        LOCK.wait(1000);
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        } finally {
            model.removeTableModelListener(l);
        }
    }
}

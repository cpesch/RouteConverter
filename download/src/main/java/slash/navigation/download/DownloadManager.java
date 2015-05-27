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
import slash.navigation.download.executor.DownloadExecutor;
import slash.navigation.download.executor.DownloadExecutorComparator;
import slash.navigation.download.queue.QueuePersister;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static slash.navigation.download.Action.*;
import static slash.navigation.download.State.*;

/**
 * Manages {@link Download}s
 *
 * @author Christian Pesch
 */

public class DownloadManager {
    private static final Logger log = Logger.getLogger(DownloadManager.class.getName());
    static final int WAIT_TIMEOUT = 60 * 1000;
    private static final int PARALLEL_DOWNLOAD_COUNT = 4;

    private final File queueFile;

    private final List<DownloadListener> downloadListeners = new CopyOnWriteArrayList<>();
    private final DownloadTableModel model = new DownloadTableModel();
    private final ThreadPoolExecutor pool;
    private CompactCalendar lastSync;

    public DownloadManager(File queueFile) {
        this.queueFile = queueFile;
        BlockingQueue<Runnable> queue = new PriorityBlockingQueue<>(1, new DownloadExecutorComparator());
        pool = new ThreadPoolExecutor(PARALLEL_DOWNLOAD_COUNT, PARALLEL_DOWNLOAD_COUNT * 2, 60, SECONDS, queue);
        pool.allowCoreThreadTimeOut(true);
        addDownloadListener(new DownloadListener() {
            public void progressed(Download download) {
            }

            public void failed(Download download) {
                saveQueue();
            }

            public void succeeded(Download download) {
                saveQueue();
            }
        });
    }

    public void loadQueue() {
        try {
            log.info(format("Loading download queue from '%s'", queueFile));
            QueuePersister.Result result = new QueuePersister().load(queueFile);
            if (result == null)
                return;

            List<Download> downloads = result.getDownloads();
            if (downloads != null)
                model.setDownloads(downloads);
            lastSync = result.getLastSync();
        } catch (Exception e) {
            e.printStackTrace();
            log.severe(format("Could not load download queue from '%s': %s", queueFile, e));
        }

        restartDownloadsWithState(Running, Resuming, Downloading, Processing, Queued);
    }

    private void restartDownloadsWithState(State... states) {
        List<State> restartStates = asList(states);
        for (Download download : new ArrayList<>(model.getDownloads())) {
            if (restartStates.contains(download.getState())) {
                log.info("Restarting download " + download + " from state " + download.getState());
                startExecutor(download);
            }
        }
    }

    public void setLastSync(CompactCalendar lastSync) {
        this.lastSync = lastSync;
    }

    public void saveQueue() {
        try {
            new QueuePersister().save(queueFile, new ArrayList<>(model.getDownloads()), lastSync);
        } catch (Exception e) {
            e.printStackTrace();
            log.severe(format("Could not save %d download queue to '%s': %s", model.getRowCount(), queueFile, e));
        }
    }

    public void clearQueue() {
        for(Download download : new ArrayList<>(model.getDownloads()))
            model.removeDownload(download);
    }

    public void dispose() {
        pool.shutdownNow();
    }

    public DownloadTableModel getModel() {
        return model;
    }

    public void addDownloadListener(DownloadListener listener) {
        downloadListeners.add(listener);
    }

    public void updateDownload(Download download) {
        model.updateDownload(download);
    }

    public void fireDownloadProgressed(Download download) {
        for (DownloadListener listener : downloadListeners) {
            listener.progressed(download);
        }
    }

    public void fireDownloadFailed(Download download) {
        for (DownloadListener listener : downloadListeners) {
            listener.failed(download);
        }
    }

    public void fireDownloadSucceeded(Download download) {
        for (DownloadListener listener : downloadListeners) {
            listener.succeeded(download);
        }
    }

    private void startExecutor(Download download) {
        DownloadExecutor executor = new DownloadExecutor(download, this);
        model.addOrUpdateDownload(download);
        pool.execute(executor);
    }

    private static final Set<State> COMPLETED = new HashSet<>(asList(NotModified, Succeeded, NoFileError, ChecksumError, Failed));

    private Download queueForDownload(Download download) {
        if (download.getFile().getFile() == null)
            throw new IllegalArgumentException("No file given for " + download);
        if (download.getAction().equals(Extract) || download.getAction().equals(Flatten)) {
            if (!download.getFile().getFile().isDirectory())
                throw new IllegalArgumentException(format("Need a directory for extraction but got %s", download.getFile().getFile()));

            List<FileAndChecksum> fragments = download.getFragments();
            if (fragments == null || fragments.size() == 0)
                throw new IllegalArgumentException("No fragments given for " + download);
            for (FileAndChecksum fragmentTarget : fragments) {
                if (fragmentTarget == null)
                    throw new IllegalArgumentException("No fragment target given for " + download);
            }
        }

        Download queued = model.getDownload(download.getUrl());
        if (queued != null) {
            // let a GET replace a HEAD
            if (queued.getAction().equals(Head) || queued.getAction().equals(GetRange))
                model.removeDownload(queued);
            else {
                if (COMPLETED.contains(queued.getState())) {
                    log.info("Starting failed download " + download);
                    startExecutor(queued);
                }
                return queued;
            }
        }

        log.info("Starting new download " + download);
        startExecutor(download);
        fireDownloadProgressed(download);
        return download;
    }

    public Download queueForDownload(String description, String url, Action action, String eTag, FileAndChecksum file,
                                     List<FileAndChecksum> fragments) {
        return queueForDownload(new Download(description, url, action, eTag, file, fragments));
    }

    private boolean isCompleted(Collection<Download> downloads) {
        for (Download download : downloads) {
            if (!COMPLETED.contains(download.getState()))
                return false;
        }
        return true;
    }

    private static final Object notificationMutex = new Object();

    public void waitForCompletion(final Collection<Download> downloads) {
        if (isCompleted(downloads))
            return;

        final boolean[] found = new boolean[1];
        found[0] = false;
        final long[] lastEvent = new long[1];
        lastEvent[0] = currentTimeMillis();

        TableModelListener l = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                synchronized (notificationMutex) {
                    lastEvent[0] = currentTimeMillis();

                    if (!isCompleted(downloads))
                        return;

                    found[0] = true;
                    notificationMutex.notifyAll();
                }
            }
        };

        model.addTableModelListener(l);
        try {
            while (true) {
                synchronized (notificationMutex) {
                    if (found[0] || currentTimeMillis() - lastEvent[0] > WAIT_TIMEOUT)
                        break;
                    try {
                        notificationMutex.wait(1000);
                    } catch (InterruptedException e) {
                        // intentionally left empty
                    }
                }
            }
        } finally {
            model.removeTableModelListener(l);
        }

        if (!isCompleted(downloads))
            throw new IllegalStateException(format("Waited %d seconds without all downloads to finish", WAIT_TIMEOUT / 1000));
    }
}

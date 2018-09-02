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

import slash.navigation.download.actions.Validator;
import slash.navigation.download.executor.DownloadExecutor;
import slash.navigation.download.executor.DownloadExecutorComparator;
import slash.navigation.download.queue.QueuePersister;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.navigation.download.Action.Extract;
import static slash.navigation.download.Action.Flatten;
import static slash.navigation.download.Action.GetRange;
import static slash.navigation.download.Action.Head;
import static slash.navigation.download.State.ChecksumError;
import static slash.navigation.download.State.Downloading;
import static slash.navigation.download.State.Failed;
import static slash.navigation.download.State.NoFileError;
import static slash.navigation.download.State.NotModified;
import static slash.navigation.download.State.Outdated;
import static slash.navigation.download.State.Processing;
import static slash.navigation.download.State.Queued;
import static slash.navigation.download.State.Resuming;
import static slash.navigation.download.State.Running;
import static slash.navigation.download.State.Stopped;
import static slash.navigation.download.State.Succeeded;

/**
 * Manages {@link Download}s
 *
 * @author Christian Pesch
 */

public class DownloadManager {
    private static final Logger log = Logger.getLogger(DownloadManager.class.getName());
    static final int WAIT_TIMEOUT = 600 * 1000;
    private static final int PARALLEL_DOWNLOAD_COUNT = 4;

    private final File queueFile;

    private final List<DownloadListener> downloadListeners = new CopyOnWriteArrayList<>();
    private final DownloadTableModel model = new DownloadTableModel();
    private final Map<Download,Future> downloadToFutures = new HashMap<>();
    private final Map<Download,DownloadExecutor> downloadToExecutors = new HashMap<>();
    private final ThreadPoolExecutor pool;

    public DownloadManager(File queueFile) {
        this.queueFile = queueFile;
        BlockingQueue<Runnable> queue = new PriorityBlockingQueue<>(1, new DownloadExecutorComparator());
        pool = new ThreadPoolExecutor(PARALLEL_DOWNLOAD_COUNT, PARALLEL_DOWNLOAD_COUNT * 2, 60, SECONDS, queue);
        pool.allowCoreThreadTimeOut(true);
        addDownloadListener(new DownloadListener() {
            public void initialized(Download download) {
                saveQueue();
            }

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
            List<Download> downloads = new QueuePersister().load(queueFile);
            if (downloads == null)
                return;
            model.setDownloads(downloads);
        } catch (Exception e) {
            log.severe(format("Could not load download queue from '%s': %s", queueFile, e));
        }

        restartDownloadsWithState(Running, Resuming, Downloading, Processing, Queued);
    }

    private void restartDownloadsWithState(State... states) {
        List<State> restartStates = asList(states);
        for (Download download : model.getDownloads()) {
            if (restartStates.contains(download.getState())) {
                log.info("Restarting download " + download + " from state " + download.getState());
                startExecutor(download);
            }
        }
    }


    public void restartDownloads(List<Download> downloads) {
        for (Download download : downloads) {
            if(!COMPLETED.contains(download.getState()))
                continue;

            log.info("Restarting download " + download);
            startExecutor(download);
        }
    }

    public void stopDownloads(List<Download> downloads) {
        for (Download download : downloads) {
            if(COMPLETED.contains(download.getState()))
                continue;

            log.info("Stopping download " + download);
            Future future = downloadToFutures.get(download);
            if(future != null)
                future.cancel(true);

            DownloadExecutor executor = downloadToExecutors.get(download);
            if(executor != null)
                executor.stopped();
        }

        pool.purge();
    }

    public void saveQueue() {
        try {
            new QueuePersister().save(queueFile, model.getDownloads());
        } catch (Exception e) {
            log.severe(format("Could not save %d download queue to '%s': %s, %s", model.getRowCount(), queueFile, e, printStackTrace(e)));
        }
    }

    public void clearQueue() {
        for (Download download : model.getDownloads())
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

    public void fireDownloadInitialized(Download download) {
        for (DownloadListener listener : downloadListeners) {
            listener.initialized(download);
        }
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
        Future<?> future = pool.submit(executor);
        downloadToFutures.put(download, future);
        downloadToExecutors.put(download, executor);
        fireDownloadInitialized(download);
    }


    public void finishedExecutor(DownloadExecutor executor) {
        Download download = executor.getDownload();
        downloadToFutures.remove(download);
        downloadToExecutors.remove(download);
    }

    private static final Set<State> COMPLETED = new HashSet<>(asList(NotModified, Outdated, Succeeded, Stopped, NoFileError, ChecksumError, Failed));

    Download queue(Download download, boolean startExecutor) {
        if (download.getFile().getFile() == null)
            throw new IllegalArgumentException("No file given for " + download);
        if (download.getAction().equals(Extract) || download.getAction().equals(Flatten)) {
            if (!download.getFile().getFile().isDirectory())
                throw new IllegalArgumentException(format("Need a directory for extraction but got %s", download.getFile().getFile()));

            List<FileAndChecksum> fragments = download.getFragments();
            if (fragments == null || fragments.size() == 0)
                log.severe("No fragments given for " + download);
            else
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
                if (COMPLETED.contains(queued.getState()) && startExecutor) {
                    log.info("Restarting completed download " + download);
                    startExecutor(queued);
                }
                return queued;
            }
        }

        if(startExecutor) {
            log.info("Starting new download " + download);
            startExecutor(download);
        } else {
            log.info("Adding to queue " + download);
            model.addOrUpdateDownload(download);
        }
        return download;
    }

    public Download queueForDownload(String description, String url, Action action, FileAndChecksum file,
                                     List<FileAndChecksum> fragments) {
        return queue(new Download(description, url, action, file, fragments), true);
    }

    public Download addOrUpdateInQueue(String description, String url, Action action, FileAndChecksum file,
                                       List<FileAndChecksum> fragments) {
        Download queued = model.getDownload(url);
        if(queued != null) {
            queued.setAction(action);
            queued.setFile(file);
            queued.setFragments(fragments);
            model.updateDownload(queued);
            return queued;
        } else {
            Download download = new Download(description, url, action, file, fragments);
            download.setState(Succeeded);
            return queue(download, false);
        }
    }

    public void scanForOutdatedFilesInQueue() throws IOException {
        for(Download download : model.getDownloads()) {
            if (COMPLETED.contains(download.getState()) && !Outdated.equals(download.getState())) {

                Validator validator = new Validator(download);
                if (!validator.isChecksumsValid()) {
                    log.info("Found outdated download " + download);

                    download.setState(Outdated);
                    getModel().updateDownload(download);

                } else
                    // set expected to actual checksum to avoid endless "locally later than remote"
                    validator.expectedChecksumIsCurrentChecksum();
            }
        }
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
            while (!isCompleted(downloads)) {
                synchronized (notificationMutex) {
                    if (found[0] || currentTimeMillis() - lastEvent[0] > WAIT_TIMEOUT) {
                        break;
                    }
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

    private static final Set<State> SUCCESSFUL = new HashSet<>(asList(NotModified, Succeeded));

    public void executeDownload(String description, String url, Action action, File file, Runnable invokeAfterSuccessfulDownloadRunnable) {
        Download download = queueForDownload(description, url, action, new FileAndChecksum(file, null), null);
        if(!file.exists()) {
            waitForCompletion(singletonList(download));

            if (!SUCCESSFUL.contains(download.getState()))
                return;
        }
        if (invokeAfterSuccessfulDownloadRunnable != null)
            invokeAfterSuccessfulDownloadRunnable.run();
    }
}

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

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;
import static slash.navigation.download.DownloadState.Failed;
import static slash.navigation.download.DownloadState.Succeeded;

/**
 * Manages {@link Download}s
 *
 * @author Christian Pesch
 */

public class DownloadManager {
    static final int WAIT_TIMEOUT = 15 * 1000;
    private static final int PARALLEL_DOWNLOAD_COUNT = 4;
    private DownloadTableModel model = new DownloadTableModel();
    private ThreadPoolExecutor pool;

    public DownloadManager() {
        BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>(1, new DownloadExecutorComparator());
        pool = new ThreadPoolExecutor(PARALLEL_DOWNLOAD_COUNT, PARALLEL_DOWNLOAD_COUNT * 2, 60, SECONDS, queue);
        pool.allowCoreThreadTimeOut(true);
    }

    public void interrupt() {
        pool.shutdownNow();
    }

    public DownloadTableModel getModel() {
        return model;
    }

    public Download queueForDownloadAndProcess(String description, String url, File target, DownloadProcessor downloadProcessor) {
        DownloadExecutor executor = new DownloadExecutor(description, url, target, downloadProcessor, model);
        Download download = executor.getDownload();
        model.addDownload(download);
        pool.execute(executor);
        return download;
    }

    public Download queueForDownload(String description, String url, File target) {
        return queueForDownloadAndProcess(description, url, target, null);
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

                    for(Download download : downloads) {
                        if(!(Succeeded.equals(download.getState()) || Failed.equals(download.getState())))
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

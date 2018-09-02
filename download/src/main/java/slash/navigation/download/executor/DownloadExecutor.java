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

package slash.navigation.download.executor;

import slash.navigation.download.Download;
import slash.navigation.download.DownloadManager;
import slash.navigation.download.State;
import slash.navigation.download.performer.ActionPerformer;
import slash.navigation.download.performer.GetPerformer;
import slash.navigation.download.performer.GetRangePerformer;
import slash.navigation.download.performer.HeadPerformer;

import java.net.UnknownHostException;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.common.helpers.ExceptionHelper.getLocalizedMessage;
import static slash.common.helpers.ExceptionHelper.printStackTrace;
import static slash.navigation.download.State.Failed;
import static slash.navigation.download.State.NotModified;
import static slash.navigation.download.State.Queued;
import static slash.navigation.download.State.Running;
import static slash.navigation.download.State.Stopped;
import static slash.navigation.download.State.Succeeded;

/**
 * Performs the {@link Download} of an URL to local file.
 *
 * @author Christian Pesch
 */

public class DownloadExecutor implements Runnable {
    private static final Logger log = getLogger(DownloadExecutor.class.getName());

    private final Download download;
    private final DownloadManager downloadManager;
    private final ModelUpdater modelUpdater;

    public DownloadExecutor(Download download, DownloadManager downloadManager) {
        this.download = download;
        this.downloadManager = downloadManager;
        download.setState(Queued);
        modelUpdater = new ModelUpdater(download, downloadManager);
    }

    public Download getDownload() {
        return download;
    }

    public ModelUpdater getModelUpdater() {
        return modelUpdater;
    }

    public void run() {
        updateState(Running);

        try {
            ActionPerformer performer;

            switch (download.getAction()) {
                case Copy:
                case Flatten:
                case Extract:
                    performer = new GetPerformer();
                    break;
                case GetRange:
                    performer = new GetRangePerformer();
                    break;
                case Head:
                    performer = new HeadPerformer();
                    break;
                default:
                    throw new IllegalArgumentException("Action " + download.getAction() + " is not supported");
            }

            performer.setDownloadExecutor(this);
            performer.run();
        } catch (Exception e) {
            log.severe(format("Failed to download content from %s: %s %s", download.getUrl(), getLocalizedMessage(e),
                    e instanceof UnknownHostException ? "" : printStackTrace(e)));
            downloadFailed();
        }

        downloadManager.finishedExecutor(this);
    }


    public void stopped() {
        download.setState(Stopped);
    }

    public void updateState(State state) {
        download.setState(state);
        downloadManager.updateDownload(download);
        log.fine(format("State for download from %s changed to %s", download.getUrl(), state));
    }

    public void downloadFailed() {
        updateState(Failed);
        downloadManager.fireDownloadFailed(download);
    }

    public void postProcessFailed() {
        downloadManager.fireDownloadFailed(download);
    }

    public void notModified() {
        updateState(NotModified);
    }

    public void succeeded() {
        updateState(Succeeded);
        downloadManager.fireDownloadSucceeded(download);
    }
}

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

import slash.navigation.rest.Get;
import slash.navigation.rest.Head;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.navigation.download.DownloadState.*;

/**
 * Performs the {@link Download} of an URL to local file.
 *
 * @author Christian Pesch
 */

public class DownloadExecutor implements Runnable {
    private static final Logger log = getLogger(DownloadExecutor.class.getName());

    private Download download;
    private DownloadProcessor downloadProcessor;
    private DownloadTableModel model;

    public DownloadExecutor(String description, String url, File target, DownloadProcessor downloadProcessor, DownloadTableModel model) {
        this.download = new Download(description, url, target);
        this.downloadProcessor = downloadProcessor;
        this.model = model;
    }

    public Download getDownload() {
        return download;
    }

    public void run() {
        try {
            if (download.getTarget().exists() && isTargetComplete())
                return;

            download();
        } catch (Exception e) {
            log.severe(format("Could not download content from %s: %s", download.getURL(), e.getMessage()));
            updateState(download, Failed);
        }
    }

    private void updateState(Download download, DownloadState state) {
        download.setState(state);
        model.updateDownload(download);
    }

    private boolean isTargetComplete() throws IOException {
        long fileLength = download.getTarget().length();
        long fileLastModified = download.getTarget().lastModified();
        Head head = new Head(download.getURL());
        head.setIfModifiedSince(fileLastModified);
        head.execute(true);
        if (!head.isOk()) {
            log.warning("HEAD request not successful, trying to download");
            return false;
        }

        Long contentLength = head.getContentLength();
        long contentLastModified = head.getLastModified();
        download.setExpectedBytes(contentLength);

        if (contentLength != null && contentLength == fileLength) {
            if (contentLastModified > fileLastModified)
                log.warning("Content modified after file, need to download again");
            else {
                updateState(download, NotModified);
                process();
                updateState(download, Succeeded);
                return true;
            }
        } else if (head.getAcceptByteRanges()) {
            if (resume(fileLength, contentLength))
                return true;
        } else if (fileLength > 0)
            log.warning("Ranges not accepted, no resume possible");
        return false;
    }

    private boolean resume(long fileLength, Long contentLength) throws IOException {
        updateState(download, Resuming);
        Get get = new Get(download.getURL());
        get.setRange(fileLength, contentLength);
        InputStream inputStream = get.executeAsStream(true);
        if (get.isSuccessful() && get.isPartialContent()) {
            new Copier(download, model).copyAndClose(inputStream, new FileOutputStream(download.getTarget(), true), fileLength);
            process();
            updateState(download, Succeeded);
            return true;
        } else {
            log.warning("Resuming not successful, downloading");
            updateState(download, Failed);
            return false;
        }
    }

    private void download() throws IOException {
        updateState(download, Downloading);
        Get get = new Get(download.getURL());
        InputStream inputStream = get.executeAsStream(true);
        if (get.isSuccessful()) {
            new Copier(download, model).copyAndClose(inputStream, new FileOutputStream(download.getTarget()), 0);
            process();
            updateState(download, Succeeded);
        } else {
            log.severe(format("Cannot copy content from %s", download.getURL()));
            updateState(download, Failed);
        }
    }

    private void process() throws IOException {
        if (downloadProcessor != null) {
            updateState(download, Processing);
            downloadProcessor.process(download, new Copier(download, model));
            if(!download.getTarget().delete())
                throw new IOException(format("Cannot delete %s", download.getTarget()));
        }
    }
}

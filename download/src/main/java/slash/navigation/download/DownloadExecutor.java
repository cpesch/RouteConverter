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

import slash.navigation.download.actions.Copier;
import slash.navigation.download.actions.CopierListener;
import slash.navigation.download.actions.Extractor;
import slash.navigation.rest.Get;
import slash.navigation.rest.Head;

import java.io.*;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.common.io.Files.generateChecksum;
import static slash.navigation.download.State.*;

/**
 * Performs the {@link Download} of an URL to local file.
 *
 * @author Christian Pesch
 */

public class DownloadExecutor implements Runnable {
    private static final Logger log = getLogger(DownloadExecutor.class.getName());

    private Download download;
    private DownloadTableModel model;

    public DownloadExecutor(Download download, DownloadTableModel model) {
        this.download = download;
        this.model = model;
    }

    public Download getDownload() {
        return download;
    }

    public void run() {
        try {
            if (resume())
                return;

            download();
        } catch (Exception e) {
            log.severe(format("Could not download content from %s: %s", download.getUrl(), e.getMessage()));
            updateState(download, Failed);
        }
    }

    private void updateState(Download download, State state) {
        download.setState(state);
        model.updateDownload(download);
    }

    private boolean resume() throws IOException {
        // TODO think about better validating target
        if (download.getTarget().exists())
            return false;

        long tempSize = download.getTempFile().length();
        long tempLastModified = download.getTempFile().lastModified();
        Head head = new Head(download.getUrl());
        head.setIfModifiedSince(tempLastModified);
        head.execute(true);
        if (!head.isOk()) {
            log.warning("HEAD request not successful, trying to download");
            return false;
        }

        Long contentLength = head.getContentLength();
        long contentLastModified = head.getLastModified();
        if (contentLength != null && contentLength == tempSize) {
            if (contentLastModified > tempLastModified)
                log.warning("Content modified after file, need to download again");
            else {
                updateState(download, NotModified);
                postProcess();
                updateState(download, Succeeded);
                return true;
            }
        } else if (head.getAcceptByteRanges()) {
            if (resume(tempSize, contentLength))
                return true;
        } else if (tempSize > 0)
            log.warning("Ranges not accepted, no resume possible");
        return false;
    }

    private boolean validate(File file, Long expectedSize, String expectedChecksum) throws IOException {
        if (!file.exists()) {
            log.warning("File " + file + " does not exist");
            return false;
        }
        if (expectedSize != null && file.length() != expectedSize) {
            log.warning("File " + file + " size is " + file.length() + " but expected " + expectedSize + " bytes");
            return false;
        }
        if (expectedChecksum != null) {
            String actualChecksum = generateChecksum(file);
            boolean result = actualChecksum.equals(expectedChecksum);
            if (!result)
                log.warning("File " + file + " checksum is " + actualChecksum + " but expected " + expectedChecksum);
            return result;
        }
        return true;
    }

    private class ModelUpdater implements CopierListener {
        public void expectingBytes(long byteCount) {
            download.setExpectedBytes(byteCount);
        }

        public void processedBytes(long byteCount) {
            download.setProcessedBytes(byteCount);
            model.updateDownload(download);
        }
    }

    private ModelUpdater modelUpdater = new ModelUpdater();

    private boolean resume(long fileSize, Long contentLength) throws IOException {
        updateState(download, Resuming);

        Get get = new Get(download.getUrl());
        get.setRange(fileSize, contentLength);
        InputStream inputStream = get.executeAsStream(true);
        if (get.isSuccessful() && get.isPartialContent()) {
            // resume download
            new Copier(modelUpdater).copyAndClose(inputStream, new FileOutputStream(download.getTempFile(), true), fileSize, contentLength);

            // validate download
            if (!validate(download.getTempFile(), download.getSize(), download.getChecksum())) {
                log.warning("Resuming produced invalid file, downloading");
                updateState(download, Failed);
                return false;
            }

            // post process download
            postProcess();
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
        Get get = new Get(download.getUrl());
        InputStream inputStream = get.executeAsStream(true);
        if (get.isSuccessful()) {
            // download
            new Copier(modelUpdater).copyAndClose(inputStream, new FileOutputStream(download.getTempFile()), 0, download.getSize());

            // validate download
            if (!validate(download.getTempFile(), download.getSize(), download.getChecksum())) {
                log.severe("Download produced invalid file");
                updateState(download, Failed);
                return;
            }

            // post process download
            postProcess();
            updateState(download, Succeeded);

        } else {
            log.severe(format("Cannot copy content from %s", download.getUrl()));
            updateState(download, Failed);
        }
    }

    private void postProcess() throws IOException {
        updateState(download, Processing);

        Action action = download.getAction();
        switch (action) {
            case Copy:
                new Copier(modelUpdater).copyAndClose(new FileInputStream(download.getTempFile()), new FileOutputStream(download.getTarget()), 0, download.getTempFile().length());
                break;
            case Extract:
                new Extractor(modelUpdater).extract(download.getTempFile(), download.getTarget());
                break;
            default:
                throw new IllegalArgumentException("Unknown Action " + action);
        }

        if (!download.getTempFile().delete())
            throw new IOException(format("Cannot delete temp file %s", download.getTempFile()));
    }
}

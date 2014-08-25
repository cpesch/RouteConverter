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
import slash.navigation.download.actions.Validator;
import slash.navigation.rest.Get;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.common.io.Directories.ensureDirectory;
import static slash.common.io.Files.setLastModified;
import static slash.common.type.CompactCalendar.fromMillis;
import static slash.navigation.download.State.*;

/**
 * Performs the {@link Download} of an URL to local file.
 *
 * @author Christian Pesch
 */

public class DownloadExecutor implements Runnable {
    private static final Logger log = getLogger(DownloadExecutor.class.getName());

    private final Download download;
    private final DownloadManager downloadManager;

    private Get get;

    public DownloadExecutor(Download download, DownloadManager downloadManager) {
        this.download = download;
        this.downloadManager = downloadManager;
        download.setState(Queued);
    }

    public Download getDownload() {
        return download;
    }

    public void run() {
        updateState(download, Running);

        try {
            boolean success = false;
            if (canResume())
                success = resume();
            if (!success)
                success = download();
            if (success) {
                if (postProcess())
                    succeeded(get.isNotModified());

            } else if (get.isNotModified())
                updateState(download, NotModified);
            else
                failed();
        } catch (Exception e) {
            e.printStackTrace();
            log.severe(format("Could not download content from %s: %s", download.getUrl(), e));
            failed();
        }
    }

    private boolean canResume() {
        Checksum checksum = download.getFile().getExpectedChecksum();
        return download.getTempFile().exists() && download.getTempFile().length() > 0 &&
                checksum != null && checksum.getContentLength() != null &&
                checksum.getContentLength() > download.getTempFile().length();
    }

    private void updateState(Download download, State state) {
        download.setState(state);
        downloadManager.getModel().updateDownload(download);
        log.fine(format("State for download from %s changed to %s", download.getUrl(), state));
    }

    private void failed() {
        updateState(download, Failed);
        downloadManager.fireDownloadFailed(download);
    }

    private void succeeded(boolean notModified) {
        updateState(download, Succeeded);
        if (!notModified)
            downloadManager.fireDownloadSucceeded(download);
    }

    private class ModelUpdater implements CopierListener {
        public void expectingBytes(long byteCount) {
            download.setExpectedBytes(byteCount);
        }

        public void processedBytes(long byteCount) {
            download.setProcessedBytes(byteCount);
            downloadManager.getModel().updateDownload(download);
            if (download.getState().equals(Downloading))
                downloadManager.fireDownloadProgressed(download);
        }
    }

    private ModelUpdater modelUpdater = new ModelUpdater();

    private boolean resume() throws IOException {
        updateState(download, Resuming);
        long fileSize = download.getTempFile().length();
        Long contentLength = download.getFile().getExpectedChecksum() != null ? download.getFile().getExpectedChecksum().getContentLength() : null;
        log.info(format("Resuming bytes %d-%d from %s", fileSize, contentLength, download.getUrl()));

        get = new Get(download.getUrl());
        get.setRange(fileSize, contentLength);

        InputStream inputStream = get.executeAsStream();
        log.info(format("Resume from %s returned with status code %s", download.getUrl(), get.getStatusCode()));
        if (get.isPartialContent()) {
            modelUpdater.expectingBytes(contentLength != null ? contentLength : get.getContentLength() != null ? get.getContentLength() : 0);
            new Copier(modelUpdater).copyAndClose(inputStream, new FileOutputStream(download.getTempFile(), true), fileSize, contentLength);
            return true;
        }
        return false;
    }

    private boolean download() throws IOException {
        updateState(download, Downloading);
        Long contentLength = download.getFile().getExpectedChecksum() != null ? download.getFile().getExpectedChecksum().getContentLength() : null;
        log.info(format("Downloading %d bytes from %s with ETag %s", contentLength, download.getUrl(), download.getETag()));

        get = new Get(download.getUrl());
        if (new Validator(download).existTargets() && download.getETag() != null)
            get.setIfNoneMatch(download.getETag());

        InputStream inputStream = get.executeAsStream();
        log.info(format("Download from %s returned with status code %s", download.getUrl(), get.getStatusCode()));
        if (get.isSuccessful() && inputStream != null) {
            modelUpdater.expectingBytes(contentLength != null ? contentLength : get.getContentLength() != null ? get.getContentLength() : 0);
            new Copier(modelUpdater).copyAndClose(inputStream, new FileOutputStream(download.getTempFile()), 0, contentLength);
            download.setETag(get.getETag());
            return true;
        }
        return false;
    }

    private boolean postProcess() throws IOException {
        updateState(download, Processing);

        if (!bringToTarget())
            return false;

        if (!validate())
            return false;

        if (download.getTempFile().exists())
            if (!download.getTempFile().delete())
                throw new IOException(format("Cannot delete temp file %s", download.getTempFile()));

        log.fine(format("Postprocess from %s successful: %s", download.getUrl(), get.isSuccessful()));
        return true;
    }

    private boolean bringToTarget() throws IOException {
        Action action = download.getAction();
        switch (action) {
            case Copy:
                return copy();
            case Flatten:
                return flatten();
            case Extract:
                return extract();
            default:
                throw new IllegalArgumentException("Unknown Action " + action);
        }
    }

    private boolean copy() throws IOException {
        File target = download.getFile().getFile();
        ensureDirectory(target.getParent());
        new Copier(modelUpdater).copyAndClose(download.getTempFile(), target);
        setLastModified(target, fromMillis(get.getLastModified()));
        return true;
    }

    private boolean flatten() throws IOException {
        File target = download.getFile().getFile();
        new Extractor(modelUpdater).flatten(download.getTempFile(), target);
        setLastModified(download.getTempFile(), fromMillis(get.getLastModified()));
        return true;
    }

    private boolean extract() throws IOException {
        File target = download.getFile().getFile();
        new Extractor(modelUpdater).extract(download.getTempFile(), target);
        setLastModified(download.getTempFile(), fromMillis(get.getLastModified()));
        return true;
    }

    private boolean validate() throws IOException {
        updateState(download, Validating);

        Validator validator = new Validator(download);
        validator.validate();

        if (!validator.existTargets()) {
            updateState(download, NoFileError);
            return false;
        } else if (!validator.isChecksumValid()) {
            updateState(download, ChecksumError);
            return false;
        }
        return true;
    }
}

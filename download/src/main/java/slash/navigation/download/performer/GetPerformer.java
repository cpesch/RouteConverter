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
package slash.navigation.download.performer;

import slash.navigation.download.Action;
import slash.navigation.download.Checksum;
import slash.navigation.download.Download;
import slash.navigation.download.actions.Copier;
import slash.navigation.download.actions.Extractor;
import slash.navigation.download.actions.Validator;
import slash.navigation.download.executor.DownloadExecutor;
import slash.navigation.download.executor.ModelUpdater;
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
import static slash.navigation.download.State.ChecksumError;
import static slash.navigation.download.State.Downloading;
import static slash.navigation.download.State.NoFileError;
import static slash.navigation.download.State.Processing;
import static slash.navigation.download.State.Resuming;
import static slash.navigation.download.State.Validating;

/**
 * What the {@link DownloadExecutor} performs for {@link Action#Copy}, {@link Action#Extract}, {@link Action#Flatten}.
 */
public class GetPerformer implements ActionPerformer {
    private static final Logger log = getLogger(GetPerformer.class.getName());
    private static final int SOCKET_TIMEOUT = 15 * 60 * 1000;

    private DownloadExecutor downloadExecutor;

    public void setDownloadExecutor(DownloadExecutor downloadExecutor) {
        this.downloadExecutor = downloadExecutor;
    }

    private Download getDownload() {
        return downloadExecutor.getDownload();
    }

    private ModelUpdater getModelUpdater() {
        return downloadExecutor.getModelUpdater();
    }

    private boolean canResume() {
        Checksum checksum = getDownload().getFile().getExpectedChecksum();
        return getDownload().getTempFile().exists() && getDownload().getTempFile().length() > 0 &&
                checksum != null && checksum.getContentLength() != null &&
                checksum.getContentLength() > getDownload().getTempFile().length();
    }

    private Result resume() throws IOException {
        downloadExecutor.updateState(Resuming);

        long fileSize = getDownload().getTempFile().length();
        Long contentLength = getDownload().getFile().getExpectedChecksum() != null ? getDownload().getFile().getExpectedChecksum().getContentLength() : null;
        log.info(format("Resuming bytes %d-%d from %s", fileSize, contentLength, getDownload().getUrl()));

        Get get = new Get(getDownload().getUrl());
        get.setRange(fileSize, contentLength);

        InputStream inputStream = get.executeAsStream();
        log.info(format("Resume from %s returned with status code %s", getDownload().getUrl(), get.getStatusCode()));
        if (get.isPartialContent()) {
            getModelUpdater().expectingBytes(contentLength != null ? contentLength : get.getContentLength() != null ? get.getContentLength() : 0);
            new Copier(getModelUpdater()).copyAndClose(inputStream, new FileOutputStream(getDownload().getTempFile(), true), fileSize, contentLength);
            return new Result(true);
        }
        return new Result(false);
    }

    private Result download() throws IOException {
        downloadExecutor.updateState(Downloading);

        Long contentLength = getDownload().getFile().getExpectedChecksum() != null ? getDownload().getFile().getExpectedChecksum().getContentLength() : null;
        log.info(format("Downloading %d bytes from %s with ETag %s", contentLength, getDownload().getUrl(), getDownload().getETag()));

        Get get = new Get(getDownload().getUrl());
        get.setSocketTimeout(SOCKET_TIMEOUT);
        if (new Validator(getDownload()).isExistsTargets() && getDownload().getETag() != null)
            get.setIfNoneMatch(getDownload().getETag());

        InputStream inputStream = get.executeAsStream();
        log.info(format("Download from %s returned with status code %s and content length %d", getDownload().getUrl(), get.getStatusCode(), get.getContentLength()));
        if (get.isSuccessful() && inputStream != null) {
            if(contentLength == null)
                contentLength = get.getContentLength();
            if (contentLength != null)
                getModelUpdater().expectingBytes(contentLength);
            new Copier(getModelUpdater()).copyAndClose(inputStream, new FileOutputStream(getDownload().getTempFile()), 0, contentLength);
            getDownload().setETag(get.getETag());
            return new Result(true, get.getLastModified());
        }
        return new Result(get.isSuccessful(), get.isNotModified());
    }

    public void run() throws IOException {
        Result result = new Result(false);
        if (canResume())
            result = resume();
        if (!result.success)
            result = download();

        if (result.notModified) {
            downloadExecutor.notModified();

        } else if (result.success) {
            if(!getDownload().getTempFile().exists())
                downloadExecutor.downloadFailed();

            if (postProcess(result.lastModified))
                downloadExecutor.succeeded();
            else
                downloadExecutor.postProcessFailed();

        } else
            downloadExecutor.downloadFailed();
    }

    private boolean postProcess(Long lastModified) throws IOException {
        downloadExecutor.updateState(Processing);

        bringToTarget(lastModified);

        if (!validate())
            return false;

        if (getDownload().getTempFile().exists())
            if (!getDownload().getTempFile().delete())
                throw new IOException(format("Cannot delete temp file %s", getDownload().getTempFile()));

        log.fine(format("Postprocess from %s successful", getDownload().getUrl()));
        return true;
    }

    private void bringToTarget(Long lastModified) throws IOException {
        Action action = getDownload().getAction();
        switch (action) {
            case Copy:
                copy(lastModified);
                break;
            case Flatten:
                flatten(lastModified);
                break;
            case Extract:
                extract(lastModified);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Action " + action);
        }
    }

    private void copy(Long lastModified) throws IOException {
        File target = getDownload().getFile().getFile();
        ensureDirectory(target.getParent());
        new Copier(getModelUpdater()).copyAndClose(getDownload().getTempFile(), target);
        setLastModified(target, lastModified);
    }

    private void flatten(Long lastModified) throws IOException {
        File target = getDownload().getFile().getFile();
        new Extractor(getModelUpdater()).flatten(getDownload().getTempFile(), target);
        setLastModified(getDownload().getTempFile(), lastModified);
    }

    private void extract(Long lastModified) throws IOException {
        File target = getDownload().getFile().getFile();
        new Extractor(getModelUpdater()).extract(getDownload().getTempFile(), target);
        setLastModified(getDownload().getTempFile(), lastModified);
    }

    private boolean validate() throws IOException {
        downloadExecutor.updateState(Validating);

        Validator validator = new Validator(getDownload());
        if (!validator.isExistsTargets()) {
            downloadExecutor.updateState(NoFileError);
            return false;
        } else if (!validator.isChecksumsValid()) {
            downloadExecutor.updateState(ChecksumError);
            return false;
        }

        // set expected to actual checksum for sending the expected checksum to the server later
        validator.expectedChecksumIsCurrentChecksum();
        return true;
    }

    private static class Result {
        public final boolean success;
        public final boolean notModified;
        public final Long lastModified;

        public Result(boolean success) {
            this(success, null);
        }

        public Result(boolean success, Long lastModified) {
            this(success, false, lastModified);
        }

        private Result(boolean success, boolean notModified) {
            this(success, notModified, null);
        }

        private Result(boolean success, boolean notModified, Long lastModified) {
            this.success = success;
            this.notModified = notModified;
            this.lastModified = lastModified;
        }
    }
}

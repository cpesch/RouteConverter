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
import slash.navigation.download.Download;
import slash.navigation.download.executor.DownloadExecutor;
import slash.navigation.rest.Head;

import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.logging.Logger.getLogger;
import static slash.navigation.download.performer.GetPerformer.updateDownload;

/**
 * What the {@link DownloadExecutor} performs for {@link Action#Head}.
 */
public class HeadPerformer implements ActionPerformer {
    private static final Logger log = getLogger(HeadPerformer.class.getName());

    private DownloadExecutor downloadExecutor;

    public void setDownloadExecutor(DownloadExecutor downloadExecutor) {
        this.downloadExecutor = downloadExecutor;
    }

    private Download getDownload() {
        return downloadExecutor.getDownload();
    }

    public void run() throws IOException {
        Head request = new Head(getDownload().getUrl());
        if (getDownload().getETag() != null)
            request.setIfNoneMatch(getDownload().getETag());

        String body = request.executeAsString();
        log.info(format("HEAD for %s returned with status code %s and body %s", getDownload().getUrl(), request.getStatusCode(), body));

        if (request.isNotModified()) {
            updateDownload(getDownload(), request);
            downloadExecutor.notModified();

        } else if (request.isSuccessful()) {
            updateDownload(getDownload(), request);
            downloadExecutor.succeeded();

        } else
            downloadExecutor.downloadFailed();
    }
}
